package com.cashi.app.presentation.payments

import androidx.lifecycle.viewModelScope
import com.cashi.app.core.mvi.MviViewModel
import com.cashi.app.core.mvi.UiEffect
import com.cashi.app.core.mvi.UiIntent
import com.cashi.app.core.mvi.UiState
import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.usecase.GetPaymentHistoryUseCase
import com.cashi.app.presentation.format.PaymentFormatter
import kotlinx.coroutines.launch

data class PaymentItem(
    val id: String,
    val title: String,
    val recipientEmail: String,
    val date: String,
    val amount: String,
    val currency: String,
    val status: String = "Sent",
)

/** The "Sent this month" card: JOD total up top, number of payments and the USD total below. */
data class MonthSummary(
    val month: String,
    val total: String,
    val totalCurrency: String,
    val count: Int,
    val usdTotal: String,
)

data class PaymentsState(
    val greeting: String = "",
    val payments: List<PaymentItem> = emptyList(),
    val summary: MonthSummary? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val showAll: Boolean = false,
    val error: String? = null,
) : UiState {
    val visiblePayments: List<PaymentItem> get() = if (showAll) payments else payments.take(COLLAPSED_COUNT)
    val canExpand: Boolean get() = payments.size > COLLAPSED_COUNT
    val isEmpty: Boolean get() = !isLoading && error == null && payments.isEmpty()

    companion object {
        const val COLLAPSED_COUNT = 10
    }
}

sealed interface PaymentsIntent : UiIntent {
    data object Refresh : PaymentsIntent
    data object Retry : PaymentsIntent
    data object ToggleShowAll : PaymentsIntent
    data object AddPaymentClicked : PaymentsIntent
}

sealed interface PaymentsEffect : UiEffect {
    data object OpenAddPayment : PaymentsEffect
    data class ShowMessage(val message: String) : PaymentsEffect
}

class PaymentsViewModel(
    private val history: GetPaymentHistoryUseCase,
    private val formatter: PaymentFormatter,
) : MviViewModel<PaymentsState, PaymentsIntent, PaymentsEffect>(PaymentsState(greeting = formatter.greeting())) {

    init {
        viewModelScope.launch {
            history.observe().collect { payments -> setState { withPayments(payments) } }
        }
        load(initial = true)
    }

    override fun handleIntent(intent: PaymentsIntent) {
        when (intent) {
            PaymentsIntent.Refresh -> load(initial = false)
            PaymentsIntent.Retry -> load(initial = true)
            PaymentsIntent.ToggleShowAll -> setState { copy(showAll = !showAll) }
            PaymentsIntent.AddPaymentClicked -> sendEffect(PaymentsEffect.OpenAddPayment)
        }
    }

    private fun load(initial: Boolean) {
        if (currentState.isRefreshing) return
        setState {
            copy(
                greeting = formatter.greeting(),
                isLoading = initial && payments.isEmpty(),
                isRefreshing = !initial,
                error = null,
            )
        }
        viewModelScope.launch {
            val result = history.refresh()
            val message = result.exceptionOrNull()?.let { it.message ?: "Could not load payments" }
            setState {
                val loaded = result.getOrNull()?.let { withPayments(it) } ?: this
                // Keep showing what we already have; only an empty screen turns into an error state.
                loaded.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = message?.takeIf { loaded.payments.isEmpty() },
                )
            }
            if (message != null && currentState.error == null) sendEffect(PaymentsEffect.ShowMessage(message))
        }
    }

    private fun PaymentsState.withPayments(payments: List<Payment>) =
        copy(payments = payments.map(::toItem), summary = summarize(payments))

    private fun toItem(payment: Payment) = PaymentItem(
        id = payment.id,
        title = payment.description ?: "Payment",
        recipientEmail = payment.recipientEmail,
        date = formatter.date(payment.createdAt),
        amount = formatter.amount(payment.amount),
        currency = payment.currency,
    )

    private fun summarize(payments: List<Payment>): MonthSummary {
        val thisMonth = payments.filter { formatter.isInCurrentMonth(it.createdAt) }
        fun total(currency: Currency) = thisMonth
            .filter { it.currency.equals(currency.code, ignoreCase = true) }
            .fold(Amount.ZERO) { sum, payment -> sum + payment.amount }
        return MonthSummary(
            month = formatter.currentMonthName(),
            total = formatter.amount(total(Currency.JOD)),
            totalCurrency = Currency.JOD.code,
            count = thisMonth.size,
            usdTotal = formatter.amountWithSymbol(total(Currency.USD), Currency.USD.code),
        )
    }
}
