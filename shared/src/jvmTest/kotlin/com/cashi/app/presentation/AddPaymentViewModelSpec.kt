package com.cashi.app.presentation

import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.domain.validation.PaymentValidator
import com.cashi.app.presentation.addpayment.AddPaymentEffect
import com.cashi.app.presentation.addpayment.AddPaymentIntent
import com.cashi.app.presentation.addpayment.AddPaymentViewModel
import com.cashi.app.testing.MainDispatcherListener
import com.cashi.app.testing.TestEnv
import com.cashi.app.testing.awaitState
import com.cashi.app.testing.testFormatter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first

class AddPaymentViewModelSpec : FunSpec({
    extension(MainDispatcherListener)

    suspend fun viewModel(env: TestEnv = TestEnv()): Pair<AddPaymentViewModel, TestEnv> {
        env.signIn()
        return AddPaymentViewModel(env.createPayment, PaymentValidator(), testFormatter()) to env
    }

    fun AddPaymentViewModel.fill(email: String, amount: String, currency: Currency = Currency.USD, description: String = "") {
        onIntent(AddPaymentIntent.RecipientEmailChanged(email))
        onIntent(AddPaymentIntent.AmountChanged(amount))
        onIntent(AddPaymentIntent.CurrencySelected(currency))
        onIntent(AddPaymentIntent.DescriptionChanged(description))
    }

    test("starts empty with JOD selected, as in the mockup") {
        val (vm, _) = viewModel()
        with(vm.state.value) {
            currency shouldBe Currency.JOD
            amount shouldBe ""
            errors.shouldBeEmpty()
        }
    }

    test("does not show errors before the first send") {
        val (vm, _) = viewModel()
        vm.fill("not-an-email", "0")
        vm.state.value.errors.shouldBeEmpty()
    }

    test("sending an invalid form shows field errors and calls nothing") {
        val (vm, env) = viewModel()
        vm.fill("ahmad@", "0")

        vm.onIntent(AddPaymentIntent.Send)

        vm.state.value.errors shouldContainExactly mapOf(
            PaymentField.RECIPIENT_EMAIL to "Enter a valid email address",
            PaymentField.AMOUNT to "Amount must be greater than zero",
        )
        env.backend.paymentPosts shouldBe 0
    }

    test("errors clear as soon as the field is fixed") {
        val (vm, _) = viewModel()
        vm.fill("ahmad@", "30")
        vm.onIntent(AddPaymentIntent.Send)

        vm.onIntent(AddPaymentIntent.RecipientEmailChanged("ahmad@gmail.com"))

        vm.state.value.errors.shouldBeEmpty()
    }

    test("a valid payment is sent and reported") {
        val (vm, env) = viewModel()
        vm.fill("ahmad@gmail.com", "30", Currency.USD, "Money transfer")

        vm.onIntent(AddPaymentIntent.Send)

        vm.effects.first() shouldBe AddPaymentEffect.PaymentSent("Sent $30.00 to ahmad@gmail.com")
        vm.state.value.isSending shouldBe false
        env.backend.payments.single().recipientEmail shouldBe "ahmad@gmail.com"
    }

    test("backend validation errors land on the matching field") {
        val (vm, env) = viewModel()
        env.backend.rejectedRecipients["blocked@example.com"] = "Recipient cannot receive payments"
        vm.fill("blocked@example.com", "5")

        vm.onIntent(AddPaymentIntent.Send)

        vm.state.awaitState { it.errors.isNotEmpty() }.errors shouldContainExactly
            mapOf(PaymentField.RECIPIENT_EMAIL to "Recipient cannot receive payments")
    }

    test("network failures are reported as a message") {
        val (vm, env) = viewModel()
        vm.fill("ahmad@gmail.com", "30")
        env.backend.unreachable = true

        vm.onIntent(AddPaymentIntent.Send)

        (vm.effects.first() as AddPaymentEffect.ShowError).message shouldBe
            "Unable to reach the server. Check your connection and try again."
    }

    test("amount input keeps digits and a single decimal point") {
        val (vm, _) = viewModel()
        vm.onIntent(AddPaymentIntent.AmountChanged("1a2.3.4"))
        vm.state.value.amount shouldBe "12.34"
        vm.onIntent(AddPaymentIntent.AmountChanged("7,5"))
        vm.state.value.amount shouldBe "7.5"
    }

    test("description is capped at 80 characters") {
        val (vm, _) = viewModel()
        vm.onIntent(AddPaymentIntent.DescriptionChanged("x".repeat(100)))
        vm.state.value.description.length shouldBe 80
    }

    test("back navigates away") {
        val (vm, _) = viewModel()
        vm.onIntent(AddPaymentIntent.Back)
        vm.effects.first() shouldBe AddPaymentEffect.NavigateBack
    }
})
