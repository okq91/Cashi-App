package com.cashi.app.domain

import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.usecase.CreatePaymentResult
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.testing.TestEnv
import com.cashi.app.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class CreatePaymentUseCaseTest {

    @Test
    fun `a valid draft is sent and added to the top of the history`() = runTest {
        val env = TestEnv()
        env.signIn()
        env.backend.addPayment("old", "10", "JOD", "x@y.io", "Old", Instant.parse("2026-10-01T00:00:00Z"))
        env.getHistory.refresh()

        val result = env.createPayment(PaymentDraft("ahmad@gmail.com", "30", "USD", "Money transfer"))

        val payment = assertIs<CreatePaymentResult.Success>(result).payment
        assertEquals(Amount(3000), payment.amount)
        assertEquals("USD", payment.currency)
        assertEquals(listOf(payment.id, "old"), env.getHistory.observe().value.map { it.id })
    }

    @Test
    fun `an invalid draft never reaches the backend`() = runTest {
        val env = TestEnv()
        env.signIn()

        val result = env.createPayment(PaymentDraft("ahmad", "0", "EUR"))

        assertEquals(
            mapOf(
                PaymentField.RECIPIENT_EMAIL to "Enter a valid email address",
                PaymentField.AMOUNT to "Amount must be greater than zero",
                PaymentField.CURRENCY to "Choose a supported currency",
            ),
            assertIs<CreatePaymentResult.Invalid>(result).errors,
        )
        assertEquals(0, env.backend.paymentPosts)
    }

    @Test
    fun `backend field errors are mapped onto form fields`() = runTest {
        val env = TestEnv()
        env.signIn()
        env.backend.rejectedRecipients["blocked@example.com"] = "Recipient cannot receive payments"

        val result = env.createPayment(PaymentDraft("blocked@example.com", "5", "JOD"))

        val invalid = assertIs<CreatePaymentResult.Invalid>(result)
        assertEquals(mapOf(PaymentField.RECIPIENT_EMAIL to "Recipient cannot receive payments"), invalid.errors)
        assertEquals("Validation failed", invalid.message)
        assertTrue(env.getHistory.observe().value.isEmpty())
    }

    @Test
    fun `connection problems are reported as network failures`() = runTest {
        val env = TestEnv()
        env.signIn()
        env.backend.unreachable = true

        val failure = assertIs<CreatePaymentResult.Failure>(env.createPayment(PaymentDraft("ahmad@gmail.com", "30", "USD")))
        assertTrue(failure.isNetworkError)
    }
}
