package com.cashi.app.presentation

import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.presentation.payments.MonthSummary
import com.cashi.app.presentation.payments.PaymentsEffect
import com.cashi.app.presentation.payments.PaymentsIntent
import com.cashi.app.presentation.payments.PaymentsState
import com.cashi.app.presentation.payments.PaymentsViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@ExtendWith(MainDispatcherExtension::class)
class PaymentsViewModelTest {

    private suspend fun signedInEnv() = TestEnv().also { it.signIn() }

    @Test
    fun `loads the history and the monthly summary`() = runTest {
        val env = signedInEnv()
        env.backend.addPayment("p1", "42.75", "JOD", "billing@jepco.example", "Electricity bill", Instant.parse("2026-10-22T18:42:00Z"))
        env.backend.addPayment("p2", "450", "JOD", "landlord@example.com", "Rent", Instant.parse("2026-10-20T10:15:00Z"))
        env.backend.addPayment("p3", "120", "USD", "academy@example.com", null, Instant.parse("2026-10-18T21:03:00Z"))
        env.backend.addPayment("old", "999", "JOD", "old@example.com", "September", Instant.parse("2026-09-30T10:00:00Z"))

        val state = PaymentsViewModel(env.getHistory, testFormatter()).state.awaitState { !it.isLoading }

        assertNull(state.error)
        assertEquals("Good evening", state.greeting)
        assertEquals(listOf("p1", "p2", "p3", "old"), state.payments.map { it.id })
        with(state.payments.first()) {
            assertEquals("Electricity bill", title)
            assertEquals("Today, 6:42 PM", date)
            assertEquals("42.75", amount)
            assertEquals("JOD", currency)
            assertEquals("Sent", status)
        }
        assertEquals("Payment", state.payments[2].title)
        assertEquals(
            MonthSummary(month = "October", total = "492.75", totalCurrency = "JOD", count = 3, usdTotal = "$120.00"),
            state.summary,
        )
    }

    @Test
    fun `shows the empty state when there are no payments`() = runTest {
        val state = PaymentsViewModel(signedInEnv().getHistory, testFormatter()).state.awaitState { !it.isLoading }
        assertTrue(state.isEmpty)
        assertEquals(0, state.summary?.count)
    }

    @Test
    fun `shows an error and recovers on retry`() = runTest {
        val env = signedInEnv()
        env.backend.unreachable = true
        val viewModel = PaymentsViewModel(env.getHistory, testFormatter())
        assertNotNull(viewModel.state.awaitState { !it.isLoading }.error)

        env.backend.unreachable = false
        env.backend.addPayment("p1", "1", "JOD", "a@b.io", null, Instant.parse("2026-10-22T10:00:00Z"))
        viewModel.onIntent(PaymentsIntent.Retry)

        val state = viewModel.state.awaitState { it.payments.isNotEmpty() }
        assertNull(state.error)
        assertEquals(1, state.payments.size)
    }

    @Test
    fun `a failed refresh keeps the list and reports a message`() = runTest {
        val env = signedInEnv()
        env.backend.addPayment("p1", "1", "JOD", "a@b.io", null, Instant.parse("2026-10-22T10:00:00Z"))
        val viewModel = PaymentsViewModel(env.getHistory, testFormatter())
        viewModel.state.awaitState { it.payments.isNotEmpty() }
        env.backend.unreachable = true

        viewModel.onIntent(PaymentsIntent.Refresh)

        assertEquals(
            "Unable to reach the server. Check your connection and try again.",
            assertIs<PaymentsEffect.ShowMessage>(viewModel.effects.first()).message,
        )
        assertEquals(1, viewModel.state.value.payments.size)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `new payments appear at the top without reloading`() = runTest {
        val env = signedInEnv()
        val viewModel = PaymentsViewModel(env.getHistory, testFormatter())
        viewModel.state.awaitState { !it.isLoading }

        env.createPayment(PaymentDraft("ahmad@gmail.com", "30", "USD", "Money transfer"))

        val state = viewModel.state.awaitState { it.payments.isNotEmpty() }
        assertEquals("ahmad@gmail.com", state.payments.first().recipientEmail)
        assertEquals("$30.00", state.summary?.usdTotal)
    }

    @Test
    fun `long histories are collapsed until See all is tapped`() = runTest {
        val env = signedInEnv()
        repeat(12) { i -> env.backend.addPayment("p$i", "1", "JOD", "a@b.io", null, Instant.parse("2026-10-0${i % 9 + 1}T10:00:00Z")) }
        val viewModel = PaymentsViewModel(env.getHistory, testFormatter())
        viewModel.state.awaitState { !it.isLoading }

        assertEquals(PaymentsState.COLLAPSED_COUNT, viewModel.state.value.visiblePayments.size)
        assertTrue(viewModel.state.value.canExpand)
        viewModel.onIntent(PaymentsIntent.ToggleShowAll)
        assertEquals(12, viewModel.state.value.visiblePayments.size)
    }

    @Test
    fun `tapping Add new payment opens the form`() = runTest {
        val viewModel = PaymentsViewModel(signedInEnv().getHistory, testFormatter())
        viewModel.onIntent(PaymentsIntent.AddPaymentClicked)
        assertEquals(PaymentsEffect.OpenAddPayment, viewModel.effects.first())
    }
}
