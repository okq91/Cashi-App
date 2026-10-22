package com.cashi.app.presentation

import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.domain.validation.PaymentValidator
import com.cashi.app.presentation.addpayment.AddPaymentEffect
import com.cashi.app.presentation.addpayment.AddPaymentIntent
import com.cashi.app.presentation.addpayment.AddPaymentViewModel
import com.cashi.app.testing.MainDispatcherExtension
import com.cashi.app.testing.TestEnv
import com.cashi.app.testing.awaitState
import com.cashi.app.testing.runTest
import com.cashi.app.testing.testFormatter
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@ExtendWith(MainDispatcherExtension::class)
class AddPaymentViewModelTest {
    private val env = TestEnv()

    private suspend fun viewModel(): AddPaymentViewModel {
        env.signIn()
        return AddPaymentViewModel(env.createPayment, PaymentValidator(), testFormatter())
    }

    private fun AddPaymentViewModel.fill(email: String, amount: String, currency: Currency = Currency.USD, description: String = "") {
        onIntent(AddPaymentIntent.RecipientEmailChanged(email))
        onIntent(AddPaymentIntent.AmountChanged(amount))
        onIntent(AddPaymentIntent.CurrencySelected(currency))
        onIntent(AddPaymentIntent.DescriptionChanged(description))
    }

    @Test
    fun `starts empty with JOD selected, as in the mockup`() = runTest {
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
        assertEquals(0, env.backend.paymentPosts)
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
        val vm = viewModel()
        vm.fill("ahmad@gmail.com", "30", Currency.USD, "Money transfer")

        vm.onIntent(AddPaymentIntent.Send)

        assertEquals(AddPaymentEffect.PaymentSent("Sent $30.00 to ahmad@gmail.com"), vm.effects.first())
        assertFalse(vm.state.value.isSending)
        assertEquals("ahmad@gmail.com", env.backend.payments.single().recipientEmail)
    }

    @Test
    fun `backend validation errors land on the matching field`() = runTest {
        val vm = viewModel()
        env.backend.rejectedRecipients["blocked@example.com"] = "Recipient cannot receive payments"
        vm.fill("blocked@example.com", "5")

        vm.onIntent(AddPaymentIntent.Send)

        assertEquals(
            mapOf(PaymentField.RECIPIENT_EMAIL to "Recipient cannot receive payments"),
            vm.state.awaitState { it.errors.isNotEmpty() }.errors,
        )
    }

    @Test
    fun `network failures are reported as a message`() = runTest {
        val vm = viewModel()
        vm.fill("ahmad@gmail.com", "30")
        env.backend.unreachable = true

        vm.onIntent(AddPaymentIntent.Send)

        assertEquals(
            "Unable to reach the server. Check your connection and try again.",
            assertIs<AddPaymentEffect.ShowError>(vm.effects.first()).message,
        )
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
