package com.cashi.app.presentation

import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.usecase.CreatePaymentResult
import com.cashi.app.domain.usecase.CreatePaymentUseCase
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.domain.validation.PaymentValidator
import com.cashi.app.presentation.addpayment.AddPaymentEffect
import com.cashi.app.presentation.addpayment.AddPaymentIntent
import com.cashi.app.presentation.addpayment.AddPaymentViewModel
import com.cashi.app.testing.MainDispatcherExtension
import com.cashi.app.testing.runTest
import com.cashi.app.testing.testFormatter
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Instant

@ExtendWith(MainDispatcherExtension::class)
class AddPaymentViewModelTest {
    private val createPayment = mock<CreatePaymentUseCase>()

    private fun viewModel() = AddPaymentViewModel(createPayment, PaymentValidator(), testFormatter())

    private fun payment(email: String = "ahmad@gmail.com") =
        Payment("p1", Amount(3000), "USD", email, "Money transfer", Instant.parse("2026-10-22T20:00:00Z"))

    private fun AddPaymentViewModel.fill(email: String, amount: String, currency: Currency = Currency.USD, description: String = "") {
        onIntent(AddPaymentIntent.RecipientEmailChanged(email))
        onIntent(AddPaymentIntent.AmountChanged(amount))
        onIntent(AddPaymentIntent.CurrencySelected(currency))
        onIntent(AddPaymentIntent.DescriptionChanged(description))
    }

    @Test
    fun `starts empty with JOD selected, as in the mockup`() {
        with(viewModel().state.value) {
            assertEquals(Currency.JOD, currency)
            assertEquals("", amount)
            assertTrue(errors.isEmpty())
        }
    }

    @Test
    fun `does not show errors before the first send`() = runTest {
        val vm = viewModel()
        vm.fill("not-an-email", "0")
        assertTrue(vm.state.value.errors.isEmpty())
    }

    @Test
    fun `sending an invalid form shows field errors and calls nothing`() = runTest {
        val vm = viewModel()
        vm.fill("ahmad@", "0")

        vm.onIntent(AddPaymentIntent.Send)

        assertEquals(
            mapOf(
                PaymentField.RECIPIENT_EMAIL to "Enter a valid email address",
                PaymentField.AMOUNT to "Amount must be greater than zero",
            ),
            vm.state.value.errors,
        )
        verifyNoInteractions(createPayment)
    }

    @Test
    fun `errors clear as soon as the field is fixed`() = runTest {
        val vm = viewModel()
        vm.fill("ahmad@", "30")
        vm.onIntent(AddPaymentIntent.Send)

        vm.onIntent(AddPaymentIntent.RecipientEmailChanged("ahmad@gmail.com"))

        assertTrue(vm.state.value.errors.isEmpty())
    }

    @Test
    fun `a valid payment is sent and reported`() = runTest {
        whenever(createPayment(any())).thenReturn(CreatePaymentResult.Success(payment()))
        val vm = viewModel()
        vm.fill("ahmad@gmail.com", "30", Currency.USD, "Money transfer")

        vm.onIntent(AddPaymentIntent.Send)

        assertEquals(AddPaymentEffect.PaymentSent("Sent $30.00 to ahmad@gmail.com"), vm.effects.first())
        assertFalse(vm.state.value.isSending)
        verify(createPayment)(PaymentDraft("ahmad@gmail.com", "30", "USD", "Money transfer"))
    }

    @Test
    fun `backend validation errors land on the matching field`() = runTest {
        val rejected = CreatePaymentResult.Invalid(
            mapOf(PaymentField.RECIPIENT_EMAIL to "Recipient cannot receive payments"),
            "Validation failed",
        )
        whenever(createPayment(any())).thenReturn(rejected)
        val vm = viewModel()
        vm.fill("blocked@example.com", "5")

        vm.onIntent(AddPaymentIntent.Send)

        assertEquals(mapOf(PaymentField.RECIPIENT_EMAIL to "Recipient cannot receive payments"), vm.state.value.errors)
        assertFalse(vm.state.value.isSending)
    }

    @Test
    fun `network failures are reported as a message`() = runTest {
        val message = "Unable to reach the server. Check your connection and try again."
        whenever(createPayment(any())).thenReturn(CreatePaymentResult.Failure(message, isNetworkError = true))
        val vm = viewModel()
        vm.fill("ahmad@gmail.com", "30")

        vm.onIntent(AddPaymentIntent.Send)

        assertEquals(message, assertInstanceOf<AddPaymentEffect.ShowError>(vm.effects.first()).message)
        assertFalse(vm.state.value.isSending)
    }

    @Test
    fun `amount input keeps digits and a single decimal point`() = runTest {
        val vm = viewModel()
        vm.onIntent(AddPaymentIntent.AmountChanged("1a2.3.4"))
        assertEquals("12.34", vm.state.value.amount)
        vm.onIntent(AddPaymentIntent.AmountChanged("7,5"))
        assertEquals("7.5", vm.state.value.amount)
    }

    @Test
    fun `description is capped at 80 characters`() = runTest {
        val vm = viewModel()
        vm.onIntent(AddPaymentIntent.DescriptionChanged("x".repeat(100)))
        assertEquals(80, vm.state.value.description.length)
    }

    @Test
    fun `back navigates away`() = runTest {
        val vm = viewModel()
        vm.onIntent(AddPaymentIntent.Back)
        assertEquals(AddPaymentEffect.NavigateBack, vm.effects.first())
    }
}
