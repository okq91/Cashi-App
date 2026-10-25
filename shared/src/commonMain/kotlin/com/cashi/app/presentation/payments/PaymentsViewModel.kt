package com.cashi.app.presentation.payments

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.cashi.app.core.mvi.MviViewModel
import com.cashi.app.core.mvi.UiEffect
import com.cashi.app.core.mvi.UiIntent
import com.cashi.app.core.mvi.UiState
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.usecase.GetPaymentHistoryUseCase
import com.cashi.app.presentation.format.PaymentFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PaymentItem(
    val id: String,
    val title: String,
    val recipientEmail: String,
    val date: String,
    val amount: String,
    val currency: String,
    val status: String = "Sent",
)

/** Screen state around the list; the list itself is paged and comes from [PaymentsViewModel.payments]. */
data class PaymentsState(val greeting: String = "") : UiState

sealed interface PaymentsIntent : UiIntent {
    /** The user pulled to refresh (the list reloads itself through Paging). */
    data object Refresh : PaymentsIntent
    data object AddPaymentClicked : PaymentsIntent
}

sealed interface PaymentsEffect : UiEffect {
    data object OpenAddPayment : PaymentsEffect
}

class PaymentsViewModel(
    history: GetPaymentHistoryUseCase,
    private val formatter: PaymentFormatter,
) : MviViewModel<PaymentsState, PaymentsIntent, PaymentsEffect>(PaymentsState(greeting = formatter.greeting())) {

    /** The paged history; cached so it survives recomposition and rotation. */
    val payments: Flow<PagingData<PaymentItem>> = history.paged()
        .map { data -> data.map(::toItem) }
        .cachedIn(viewModelScope)

    override fun handleIntent(intent: PaymentsIntent) {
        when (intent) {
            PaymentsIntent.Refresh -> setState { copy(greeting = formatter.greeting()) }
            PaymentsIntent.AddPaymentClicked -> sendEffect(PaymentsEffect.OpenAddPayment)
        }
    }

    private fun toItem(payment: Payment) = PaymentItem(
        id = payment.id,
        title = payment.description ?: "Payment",
        recipientEmail = payment.recipientEmail,
        date = formatter.date(payment.createdAt),
        amount = formatter.amount(payment.amount),
        currency = payment.currency,
    )
}
