package com.cashi.app.testing

import com.cashi.app.data.remote.dto.PaymentDto
import com.cashi.app.domain.model.Amount
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * In-memory stand-in for the Cashi backend (cashi-api), served through Ktor's [MockEngine].
 * It follows the real API contract: login/refresh issue rotating tokens, payment endpoints
 * require a valid bearer token, and invalid payments get the backend's validation error body.
 */
class FakeBackend(
    private val email: String = "omar@example.com",
    private val password: String = "Password123",
) {
    private val json = Json { ignoreUnknownKeys = true }

    val payments = mutableListOf<PaymentDto>()
    val requests = mutableListOf<HttpRequestData>()
    val createdPaymentBodies = mutableListOf<JsonObject>()

    var loginCalls = 0; private set
    var refreshCalls = 0; private set
    var unreachable = false
    var now: Instant = Instant.parse("2026-10-22T20:00:00Z")

    /** recipient email -> message the backend answers with as a field error. */
    val rejectedRecipients = mutableMapOf<String, String>()

    private val accessTokens = mutableSetOf<String>()
    private val refreshTokens = mutableSetOf<String>()
    private var tokenCounter = 0
    private var idCounter = 0

    val engine = MockEngine { request ->
        requests += request
        if (unreachable) throw IOException("Connection refused")
        val path = request.url.encodedPath
        when {
            request.method == HttpMethod.Post && path == "/api/auth/login" -> login(request)
            request.method == HttpMethod.Post && path == "/api/auth/refresh" -> refresh(request)
            path == "/api/payments" && !isAuthorized(request) -> error(HttpStatusCode.Unauthorized, "Unauthorized")
            request.method == HttpMethod.Get && path == "/api/payments" -> listPayments(request)
            request.method == HttpMethod.Post && path == "/api/payments" -> createPayment(request)
            else -> error(HttpStatusCode.NotFound, "Not found")
        }
    }

    fun expireAccessTokens() = accessTokens.clear()

    fun revokeRefreshTokens() = refreshTokens.clear()

    fun addPayment(id: String, amount: String, currency: String, recipient: String, description: String?, createdAt: Instant) {
        payments += PaymentDto(id, Amount.fromServer(amount), currency, recipient, description, createdAt)
    }

    val paymentPosts: Int get() = requests.count { it.method == HttpMethod.Post && it.url.encodedPath == "/api/payments" }

    private fun isAuthorized(request: HttpRequestData): Boolean {
        val token = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: return false
        return token in accessTokens
    }

    private suspend fun MockRequestHandleScope.login(request: HttpRequestData): HttpResponseData {
        loginCalls++
        val body = request.jsonBody()
        if (body.string("email") != email || body.string("password") != password) {
            return error(HttpStatusCode.Unauthorized, "Invalid email or password")
        }
        return issueTokens()
    }

    private suspend fun MockRequestHandleScope.refresh(request: HttpRequestData): HttpResponseData {
        refreshCalls++
        val token = request.jsonBody().string("refreshToken")
        if (token == null || !refreshTokens.remove(token)) {
            return error(HttpStatusCode.Unauthorized, "Invalid refresh token")
        }
        return issueTokens()
    }

    private fun MockRequestHandleScope.issueTokens(): HttpResponseData {
        tokenCounter++
        val access = "access-$tokenCounter"
        val refresh = "refresh-$tokenCounter"
        accessTokens += access
        refreshTokens += refresh
        return ok(
            buildJsonObject {
                putJsonObject("user") {
                    put("id", "qb1vYEcg4g2f")
                    put("email", email)
                    put("fullName", "Omar")
                }
                put("accessToken", access)
                put("tokenType", "Bearer")
                put("expiresIn", 900)
                put("refreshToken", refresh)
            }.toString(),
        )
    }

    private fun MockRequestHandleScope.listPayments(request: HttpRequestData): HttpResponseData {
        val limit = request.url.parameters["limit"]?.toIntOrNull() ?: 50
        val page = payments.sortedByDescending { it.createdAt }.take(limit)
        return ok(json.encodeToString(page))
    }

    private suspend fun MockRequestHandleScope.createPayment(request: HttpRequestData): HttpResponseData {
        val body = request.jsonBody()
        createdPaymentBodies += body
        val amountText = (body["amount"] as? JsonPrimitive)?.content
        val currency = body.string("currency").orEmpty()
        val recipient = body.string("recipientEmail").orEmpty()

        val errors = buildMap {
            val amount = amountText?.toBigDecimalOrNull()
            if (amount == null || amount.signum() <= 0) put("amount", "Amount must be greater than zero")
            if (!Regex("^[A-Z]{3}$").matches(currency)) put("currency", "Currency must be a 3-letter ISO code, e.g. USD")
            if ('@' !in recipient) put("recipientEmail", "Invalid recipient email")
            rejectedRecipients[recipient]?.let { put("recipientEmail", it) }
        }
        if (errors.isNotEmpty()) return error(HttpStatusCode.BadRequest, "Validation failed", errors)

        now += 1.minutes
        val created = PaymentDto(
            id = "pay-${++idCounter}",
            amount = Amount.fromServer(amountText!!),
            currency = currency,
            recipientEmail = recipient,
            description = body.string("description"),
            createdAt = now,
        )
        payments += created
        return ok(json.encodeToString(created), HttpStatusCode.Created)
    }

    private fun MockRequestHandleScope.ok(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.error(
        status: HttpStatusCode,
        message: String,
        errors: Map<String, String>? = null,
    ) = ok(
        buildJsonObject {
            put("status", status.value)
            put("message", message)
            if (errors != null) putJsonObject("errors") { errors.forEach { (k, v) -> put(k, v) } }
        }.toString(),
        status,
    )

    private suspend fun HttpRequestData.jsonBody(): JsonObject =
        json.parseToJsonElement(body.toByteArray().decodeToString()).jsonObject

    private fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
}
