package com.cashi.app.data

import com.cashi.app.domain.error.ApiException
import com.cashi.app.core.network.createApiClient
import com.cashi.app.core.storage.TokenStorage
import com.cashi.app.data.remote.PaymentsApi
import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Credentials
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.testing.FakeBackend
import com.cashi.app.testing.TestEnv
import com.cashi.app.testing.runTest
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ApiResponseHandlingTest {

    private fun jsonEngine(status: HttpStatusCode, body: String) = MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private fun paymentsApi(engine: MockEngine): PaymentsApi {
        val json = Json { ignoreUnknownKeys = true }
        val tokens = TokenStorage(MapSettings()).apply { save("token", "refresh") }
        return PaymentsApi(createApiClient(engine, json, tokens, "http://localhost:8080"), json)
    }

    @Test
    fun `decodes the payment history response sample`() = runTest {
        val api = paymentsApi(
            jsonEngine(
                HttpStatusCode.OK,
                """
                {"items":[{"id":"T1X7LoBy5Vxm","amount":30,"currency":"USD","recipientEmail":"ahmad@gmail.com",
                  "description":"Money transfer","createdAt":"2026-10-22T20:06:25.221845Z","extra":"ignored"},
                 {"id":"x2","amount":42.75,"currency":"JOD","recipientEmail":"b@c.io","description":null,
                  "createdAt":"2026-10-21T08:00:00Z"}],"nextCursor":"6GNM20mSxqrT"}
                """.trimIndent(),
            ),
        )
        val page = api.getPayments()
        val payments = page.items

        assertEquals("6GNM20mSxqrT", page.nextCursor)
        assertEquals(2, payments.size)
        with(payments[0]) {
            assertEquals("T1X7LoBy5Vxm", id)
            assertEquals(Amount(3000), amount)
            assertEquals("USD", currency)
            assertEquals("ahmad@gmail.com", recipientEmail)
            assertEquals("Money transfer", description)
            assertEquals(Instant.parse("2026-10-22T20:06:25.221845Z"), createdAt)
        }
        assertEquals(Amount(4275), payments[1].amount)
        assertNull(payments[1].description)
    }

    @Test
    fun `sends the bearer token and no cursor for the first page`() = runTest {
        val env = TestEnv()
        assertTrue(env.signIn().isSuccess)
        env.paymentsApi.getPayments()

        val request = env.backend.requests.last()
        assertEquals(HttpMethod.Get, request.method)
        assertNull(request.url.parameters["cursor"])
        assertEquals("Bearer ${env.tokens.accessToken}", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `sends the payment as JSON matching the backend contract`() = runTest {
        val env = TestEnv()
        env.signIn()
        env.createPayment(PaymentDraft("ahmad@gmail.com", "30", "USD", "Money transfer"))

        assertEquals(
            mapOf(
                "amount" to JsonUnquotedLiteral("30.00"),
                "currency" to JsonPrimitive("USD"),
                "recipientEmail" to JsonPrimitive("ahmad@gmail.com"),
                "description" to JsonPrimitive("Money transfer"),
            ),
            env.backend.createdPaymentBodies.single().toMap(),
        )
    }

    @Test
    fun `sends the cursor only when asking for a later page`() = runTest {
        val env = TestEnv()
        env.signIn()
        repeat(3) { i -> env.backend.addPayment("p$i", "1", "JOD", "a@b.io", null, Instant.fromEpochSeconds(1_790_000_000L + i)) }
        env.backend.pageSize = 2

        val first = env.paymentsApi.getPayments()
        val second = env.paymentsApi.getPayments(first.nextCursor)

        val urls = env.backend.requests.filter { it.url.encodedPath == "/api/payments" }.map { it.url.parameters["cursor"] }
        assertEquals(listOf(null, first.nextCursor), urls)
        assertEquals(listOf("p2", "p1"), first.items.map { it.id })
        assertEquals(listOf("p0"), second.items.map { it.id })
        assertNull(second.nextCursor)
    }

    @Test
    fun `maps a validation error response to field errors`() = runTest {
        val api = paymentsApi(
            jsonEngine(
                HttpStatusCode.BadRequest,
                """{"status":400,"message":"Validation failed","errors":{"currency":"Currency must be a 3-letter ISO code, e.g. USD"}}""",
            ),
        )
        val error = assertFailsWith<ApiException> { api.getPayments() }
        assertEquals(400, error.status)
        assertEquals(mapOf("currency" to "Currency must be a 3-letter ISO code, e.g. USD"), error.fieldErrors)
    }

    @Test
    fun `maps a server error with a non-JSON body`() = runTest {
        val api = paymentsApi(MockEngine { respond("upstream down", HttpStatusCode.BadGateway) })
        val error = assertFailsWith<ApiException> { api.getPayments() }
        assertEquals(502, error.status)
        assertEquals("upstream down", error.message)
    }

    @Test
    fun `maps a malformed success body to a network error`() = runTest {
        val api = paymentsApi(jsonEngine(HttpStatusCode.OK, """["not","a page"]"""))
        assertTrue(assertFailsWith<ApiException> { api.getPayments() }.isNetworkError)
    }

    @Test
    fun `maps connection failures to a network error`() = runTest {
        val env = TestEnv()
        env.backend.unreachable = true
        assertTrue(assertFailsWith<ApiException> { env.paymentsApi.getPayments() }.isNetworkError)
    }

    @Test
    fun `login stores both tokens`() = runTest {
        val env = TestEnv()
        val user = env.signIn().getOrThrow()
        assertEquals("omar@example.com", user?.email)
        assertEquals("access-1", env.tokens.accessToken)
        assertEquals("refresh-1", env.tokens.refreshToken)
    }

    @Test
    fun `wrong credentials surface the backend message`() = runTest {
        val env = TestEnv()
        val error = assertFailsWith<ApiException> { env.authRepository.login(Credentials("omar@example.com", "nope")) }
        assertEquals(401, error.status)
        assertEquals("Invalid email or password", error.message)
        assertNull(env.tokens.accessToken)
    }

    @Test
    fun `an expired access token is refreshed and the request retried`() = runTest {
        val env = TestEnv()
        env.signIn()
        env.backend.expireAccessTokens()

        assertEquals(emptyList(), env.paymentsApi.getPayments().items)

        assertEquals(1, env.backend.refreshCalls)
        assertEquals(1, env.backend.loginCalls)
        assertEquals("access-2", env.tokens.accessToken)
        assertEquals("refresh-2", env.tokens.refreshToken)
    }

    @Test
    fun `signs in again when the refresh token is rejected`() = runTest {
        val env = TestEnv()
        env.signIn()
        env.backend.expireAccessTokens()
        env.backend.revokeRefreshTokens()

        assertEquals(emptyList(), env.paymentsApi.getPayments().items)

        assertEquals(1, env.backend.refreshCalls)
        assertEquals(2, env.backend.loginCalls)
        assertEquals("access-2", env.tokens.accessToken)
    }

    @Test
    fun `fails with 401 and clears the session when it cannot re-authenticate`() = runTest {
        val env = TestEnv(FakeBackend(password = "rotated"))
        env.tokens.save("stale", "stale-refresh")

        assertEquals(401, assertFailsWith<ApiException> { env.paymentsApi.getPayments() }.status)
        assertNull(env.tokens.accessToken)
    }
}
