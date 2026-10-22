package com.cashi.app.domain

import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.usecase.CreatePaymentResult
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.testing.TestEnv
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Instant

class CreatePaymentUseCaseSpec : FunSpec({

    test("a valid draft is sent and added to the top of the history") {
        val env = TestEnv()
        env.signIn()
        env.backend.addPayment("old", "10", "JOD", "x@y.io", "Old", Instant.parse("2026-10-01T00:00:00Z"))
        env.getHistory.refresh()

        val result = env.createPayment(PaymentDraft("ahmad@gmail.com", "30", "USD", "Money transfer"))

        val payment = result.shouldBeInstanceOf<CreatePaymentResult.Success>().payment
        payment.amount shouldBe Amount(3000)
        payment.currency shouldBe "USD"
        env.getHistory.observe().value.map { it.id } shouldBe listOf(payment.id, "old")
    }

    test("an invalid draft never reaches the backend") {
        val env = TestEnv()
        env.signIn()

        val result = env.createPayment(PaymentDraft("ahmad", "0", "EUR"))

        result.shouldBeInstanceOf<CreatePaymentResult.Invalid>().errors shouldContainExactly mapOf(
            PaymentField.RECIPIENT_EMAIL to "Enter a valid email address",
            PaymentField.AMOUNT to "Amount must be greater than zero",
            PaymentField.CURRENCY to "Choose a supported currency",
        )
        env.backend.paymentPosts shouldBe 0
    }

    test("backend field errors are mapped onto form fields") {
        val env = TestEnv()
        env.signIn()
        env.backend.rejectedRecipients["blocked@example.com"] = "Recipient cannot receive payments"

        val result = env.createPayment(PaymentDraft("blocked@example.com", "5", "JOD"))

        val invalid = result.shouldBeInstanceOf<CreatePaymentResult.Invalid>()
        invalid.errors shouldContainExactly mapOf(PaymentField.RECIPIENT_EMAIL to "Recipient cannot receive payments")
        invalid.message shouldBe "Validation failed"
        env.getHistory.observe().value shouldHaveSize 0
    }

    test("connection problems are reported as network failures") {
        val env = TestEnv()
        env.signIn()
        env.backend.unreachable = true

        val failure = env.createPayment(PaymentDraft("ahmad@gmail.com", "30", "USD"))
            .shouldBeInstanceOf<CreatePaymentResult.Failure>()
        failure.isNetworkError shouldBe true
    }
})
