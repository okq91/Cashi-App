package com.cashi.app.presentation

import androidx.paging.testing.asSnapshot
import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.usecase.GetPaymentHistoryUseCase
import com.cashi.app.presentation.payments.PaymentsEffect
import com.cashi.app.presentation.payments.PaymentsIntent
import com.cashi.app.presentation.payments.PaymentsViewModel
import com.cashi.app.testing.MainDispatcherExtension
import com.cashi.app.testing.pagedOf
import com.cashi.app.testing.runTest
import com.cashi.app.testing.testFormatter
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Instant

@ExtendWith(MainDispatcherExtension::class)
class PaymentsViewModelTest {
    private val history = mock<GetPaymentHistoryUseCase>()

    private fun viewModel(payments: List<Payment>): PaymentsViewModel {
        whenever(history.paged()).thenReturn(pagedOf(payments))
        return PaymentsViewModel(history, testFormatter())
    }

    @Test
    fun `maps the paged history to display items`() = runTest {
        val viewModel = viewModel(
            listOf(
                Payment("p1", Amount(4275), "JOD", "billing@jepco.example", "Electricity bill", Instant.parse("2026-10-22T18:42:00Z")),
                Payment("p2", Amount(45000), "JOD", "landlord@example.com", "Rent", Instant.parse("2026-10-20T10:15:00Z")),
                Payment("p3", Amount(12000), "USD", "academy@example.com", null, Instant.parse("2026-10-18T21:03:00Z")),
            ),
        )

        val items = viewModel.payments.asSnapshot()

        assertEquals(listOf("p1", "p2", "p3"), items.map { it.id })
        with(items.first()) {
            assertEquals("Electricity bill", title)
            assertEquals("billing@jepco.example", recipientEmail)
            assertEquals("Today, 6:42 PM", date)
            assertEquals("42.75", amount)
            assertEquals("JOD", currency)
            assertEquals("Sent", status)
        }
        assertEquals("Payment", items[2].title)
    }

    @Test
    fun `greets by time of day`() {
        assertEquals("Good evening", viewModel(emptyList()).state.value.greeting)
    }

    @Test
    fun `an empty history has no items`() = runTest {
        assertTrue(viewModel(emptyList()).payments.asSnapshot().isEmpty())
    }

    @Test
    fun `pull to refresh updates the greeting`() {
        val viewModel = viewModel(emptyList())
        viewModel.onIntent(PaymentsIntent.Refresh)
        assertEquals("Good evening", viewModel.state.value.greeting)
    }

    @Test
    fun `tapping Add new payment opens the form`() = runTest {
        val viewModel = viewModel(emptyList())
        viewModel.onIntent(PaymentsIntent.AddPaymentClicked)
        assertEquals(PaymentsEffect.OpenAddPayment, viewModel.effects.first())
    }
}
