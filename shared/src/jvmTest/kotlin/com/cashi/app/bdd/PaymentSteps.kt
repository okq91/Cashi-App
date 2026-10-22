package com.cashi.app.bdd

import com.cashi.app.core.network.ApiException
import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.usecase.CreatePaymentResult
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.testing.TestEnv
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Step definitions for payment creation and transaction listing. Each scenario gets a new
 * instance, i.e. a fresh [TestEnv]: the real shared client, repositories and use cases talking
 * to an in-memory backend through Ktor's MockEngine.
 */
class PaymentSteps {
    private val env = TestEnv()
    private var createResult: CreatePaymentResult? = null
    private var historyResult: Result<List<Payment>>? = null

    private val history: List<Payment> get() = env.getHistory.observe().value

    // Session

    @Given("the user is signed in to Cashi")
    fun signedIn() = runBlocking {
        env.signIn().getOrThrow()
        Unit
    }

    @Given("the access token has expired")
    fun accessTokenExpired() = env.backend.expireAccessTokens()

    @Given("the refresh token has been revoked")
    fun refreshTokenRevoked() = env.backend.revokeRefreshTokens()

    @Given("the backend is unreachable")
    fun backendUnreachable() {
        env.backend.unreachable = true
    }

    @Then("the session was refreshed {int} time(s)")
    fun sessionRefreshed(times: Int) {
        assertEquals(times, env.backend.refreshCalls)
    }

    @Then("the user was signed in again automatically")
    fun signedInAgain() {
        assertEquals(2, env.backend.loginCalls)
    }

    // Backend data

    @Given("the backend has these payments:")
    fun backendHasPayments(table: DataTable) {
        table.asMaps().forEach { row ->
            fun cell(column: String): String = requireNotNull(row[column]) { "Missing $column" }
            env.backend.addPayment(
                id = cell("id"),
                amount = cell("amount"),
                currency = cell("currency"),
                recipient = cell("recipientEmail"),
                description = row["description"],
                createdAt = Instant.parse(cell("createdAt")),
            )
        }
    }

    @Given("the backend has {int} payments")
    fun backendHasNPayments(count: Int) {
        repeat(count) { i ->
            env.backend.addPayment("p$i", "${i + 1}", "JOD", "user$i@example.com", "Payment $i", Instant.fromEpochSeconds(1_790_000_000L + i * 60))
        }
    }

    @Given("the backend refuses payments to {string} with {string}")
    fun backendRefuses(email: String, message: String) {
        env.backend.rejectedRecipients[email] = message
    }

    // Creating payments

    @When("the user sends {string} {string} to {string} with description {string}")
    fun sendPayment(amount: String, currency: String, email: String, description: String) = runBlocking {
        createResult = env.createPayment(PaymentDraft(email, amount, currency, description))
    }

    @Then("the payment is accepted")
    fun paymentAccepted() {
        assertIs<CreatePaymentResult.Success>(createResult)
    }

    @Then("the payment is rejected with a {string} error {string}")
    fun paymentRejected(field: String, message: String) {
        val invalid = assertIs<CreatePaymentResult.Invalid>(createResult)
        assertEquals(message, invalid.errors[PaymentField.valueOf(field)])
    }

    @Then("the payment fails with a network error")
    fun paymentFailsOffline() {
        assertTrue(assertIs<CreatePaymentResult.Failure>(createResult).isNetworkError)
    }

    @Then("no payment is sent to the backend")
    fun noPaymentSent() {
        assertEquals(0, env.backend.paymentPosts)
    }

    @Then("the backend received a payment of {string} {string} for {string}")
    fun backendReceived(amount: String, currency: String, email: String) {
        val body = env.backend.createdPaymentBodies.last()
        assertEquals(amount, (body["amount"] as JsonPrimitive).content)
        assertEquals(currency, (body["currency"] as JsonPrimitive).content)
        assertEquals(email, (body["recipientEmail"] as JsonPrimitive).content)
    }

    // Listing payments

    @When("the user opens the transaction history")
    fun openHistory() = runBlocking {
        historyResult = env.getHistory.refresh()
    }

    @Then("the history shows {int} payments")
    fun historyCount(count: Int) {
        assertEquals(count, history.size)
    }

    @Then("the transaction history is empty")
    fun historyEmpty() {
        assertTrue(history.isEmpty())
    }

    @Then("the payments are listed in the order {string}")
    fun historyOrder(ids: String) {
        assertEquals(ids.split(",").map { it.trim() }, history.map { it.id })
    }

    @Then("payment {string} shows {string} {string} to {string} described as {string}")
    fun paymentShows(id: String, amount: String, currency: String, email: String, description: String) {
        val payment = history.single { it.id == id }
        assertEquals(amount, payment.amount.toPlainString())
        assertEquals(currency, payment.currency)
        assertEquals(email, payment.recipientEmail)
        assertEquals(description, payment.description)
    }

    @Then("the transaction history starts with a payment of {string} {string} to {string}")
    fun historyStartsWith(amount: String, currency: String, email: String) {
        val first = history.first()
        assertEquals(Amount.parseOrNull(amount), first.amount)
        assertEquals(currency, first.currency)
        assertEquals(email, first.recipientEmail)
    }

    @Then("the backend was asked for at most {int} payments")
    fun askedForAtMost(limit: Int) {
        val listRequest = env.backend.requests.last { it.url.encodedPath == "/api/payments" }
        assertTrue(listRequest.url.parameters["limit"]!!.toInt() <= limit)
    }

    @Then("loading the history fails with a network error")
    fun historyFailsOffline() {
        assertTrue((historyResult!!.exceptionOrNull() as ApiException).isNetworkError)
    }
}
