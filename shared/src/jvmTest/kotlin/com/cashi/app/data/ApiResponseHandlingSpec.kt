package com.cashi.app.data

import com.cashi.app.core.network.ApiException
import com.cashi.app.core.network.createApiClient
import com.cashi.app.core.storage.TokenStorage
import com.cashi.app.data.remote.PaymentsApi
import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Credentials
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.testing.FakeBackend
import com.cashi.app.testing.TestEnv
import com.russhwolf.settings.MapSettings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlin.time.Instant

class ApiResponseHandlingSpec : FunSpec({

    fun jsonEngine(status: HttpStatusCode, body: String) = MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    fun paymentsApi(engine: MockEngine): PaymentsApi {
        val json = Json { ignoreUnknownKeys = true }
        val tokens = TokenStorage(MapSettings()).apply { save("token", "refresh") }
        return PaymentsApi(createApiClient(engine, json, tokens, "http://localhost:8080"), json)
    }

    test("decodes the payment history response sample") {
        val api = paymentsApi(
            jsonEngine(
                HttpStatusCode.OK,
                """
                [{"id":"T1X7LoBy5Vxm","amount":30,"currency":"USD","recipientEmail":"ahmad@gmail.com",
                  "description":"Money transfer","createdAt":"2026-10-22T20:06:25.221845Z","extra":"ignored"},
                 {"id":"x2","amount":42.75,"currency":"JOD","recipientEmail":"b@c.io","description":null,
                  "createdAt":"2026-10-21T08:00:00Z"}]
                """.trimIndent(),
            ),
        )
        val payments = api.getPayments(50)
        payments shouldHaveSize 2
        with(payments[0]) {
            id shouldBe "T1X7LoBy5Vxm"
            amount shouldBe Amount(3000)
            currency shouldBe "USD"
            recipientEmail shouldBe "ahmad@gmail.com"
            description shouldBe "Money transfer"
            createdAt shouldBe Instant.parse("2026-10-22T20:06:25.221845Z")
        }
        payments[1].amount shouldBe Amount(4275)
        payments[1].description.shouldBeNull()
    }

    test("sends the limit and the bearer token when listing") {
        val env = TestEnv()
        env.signIn().isSuccess shouldBe true
        env.paymentsApi.getPayments(50)

        val request = env.backend.requests.last()
        request.method shouldBe HttpMethod.Get
        request.url.parameters["limit"] shouldBe "50"
        request.headers[HttpHeaders.Authorization] shouldBe "Bearer ${env.tokens.accessToken}"
    }

    test("sends the payment as JSON matching the backend contract") {
        val env = TestEnv()
        env.signIn()
        env.createPayment(PaymentDraft("ahmad@gmail.com", "30", "USD", "Money transfer"))

        env.backend.createdPaymentBodies.single() shouldContainExactly mapOf(
            "amount" to JsonUnquotedLiteral("30.00"),
            "currency" to JsonPrimitive("USD"),
            "recipientEmail" to JsonPrimitive("ahmad@gmail.com"),
            "description" to JsonPrimitive("Money transfer"),
        )
    }

    test("maps a validation error response to field errors") {
        val api = paymentsApi(
            jsonEngine(
                HttpStatusCode.BadRequest,
                """{"status":400,"message":"Validation failed","errors":{"currency":"Currency must be a 3-letter ISO code, e.g. USD"}}""",
            ),
        )
        val error = shouldThrow<ApiException> { api.getPayments(50) }
        error.status shouldBe 400
        error.fieldErrors shouldContainExactly mapOf("currency" to "Currency must be a 3-letter ISO code, e.g. USD")
    }

    test("maps a server error with a non-JSON body") {
        val api = paymentsApi(MockEngine { respond("upstream down", HttpStatusCode.BadGateway) })
        val error = shouldThrow<ApiException> { api.getPayments(50) }
        error.status shouldBe 502
        error.message shouldBe "upstream down"
    }

    test("maps a malformed success body to a network error") {
        val api = paymentsApi(jsonEngine(HttpStatusCode.OK, """{"not":"a list"}"""))
        shouldThrow<ApiException> { api.getPayments(50) }.isNetworkError shouldBe true
    }

    test("maps connection failures to a network error") {
        val env = TestEnv()
        env.backend.unreachable = true
        shouldThrow<ApiException> { env.paymentsApi.getPayments(50) }.isNetworkError shouldBe true
    }

    context("token handling") {
        test("login stores both tokens") {
            val env = TestEnv()
            val user = env.signIn().getOrThrow()
            user?.email shouldBe "omar@example.com"
            env.tokens.accessToken shouldBe "access-1"
            env.tokens.refreshToken shouldBe "refresh-1"
        }

        test("wrong credentials surface the backend message") {
            val env = TestEnv()
            val error = env.authRepository.runCatching { login(Credentials("omar@example.com", "nope")) }.exceptionOrNull()
            (error as ApiException).status shouldBe 401
            error.message shouldBe "Invalid email or password"
            env.tokens.accessToken.shouldBeNull()
        }

        test("an expired access token is refreshed and the request retried") {
            val env = TestEnv()
            env.signIn()
            env.backend.expireAccessTokens()

            env.paymentsApi.getPayments(50) shouldBe emptyList()

            env.backend.refreshCalls shouldBe 1
            env.backend.loginCalls shouldBe 1
            env.tokens.accessToken shouldBe "access-2"
            env.tokens.refreshToken shouldBe "refresh-2"
        }

        test("signs in again when the refresh token is rejected") {
            val env = TestEnv()
            env.signIn()
            env.backend.expireAccessTokens()
            env.backend.revokeRefreshTokens()

            env.paymentsApi.getPayments(50) shouldBe emptyList()

            env.backend.refreshCalls shouldBe 1
            env.backend.loginCalls shouldBe 2
            env.tokens.accessToken shouldBe "access-2"
        }

        test("fails with 401 and clears the session when it cannot re-authenticate") {
            val backend = FakeBackend(password = "rotated")
            val env = TestEnv(backend)
            env.tokens.save("stale", "stale-refresh")

            shouldThrow<ApiException> { env.paymentsApi.getPayments(50) }.status shouldBe 401
            env.tokens.accessToken.shouldBeNull()
        }
    }
})
