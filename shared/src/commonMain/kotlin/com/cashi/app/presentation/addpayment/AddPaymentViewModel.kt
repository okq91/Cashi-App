package com.cashi.app.presentation.addpayment

import androidx.lifecycle.viewModelScope
import com.cashi.app.core.mvi.MviViewModel
import com.cashi.app.core.mvi.UiEffect
import com.cashi.app.core.mvi.UiIntent
import com.cashi.app.core.mvi.UiState
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.usecase.CreatePaymentResult
import com.cashi.app.domain.usecase.CreatePaymentUseCase
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.domain.validation.PaymentValidation
import com.cashi.app.domain.validation.PaymentValidator
import com.cashi.app.presentation.format.PaymentFormatter
import kotlinx.coroutines.launch

data class AddPaymentState(
    val recipientEmail: String = "",
    val amount: String = "",
    val currency: Currency = Currency.Default,
    val description: String = "",
    val errors: Map<PaymentField, String> = emptyMap(),
    /** Errors are only shown once the user tried to send, then they update live. */
    val submitAttempted: Boolean = false,
    val isSending: Boolean = false,
) : UiState {
    val currencies: List<Currency> get() = Currency.entries

    fun draft() = PaymentDraft(recipientEmail, amount, currency.code, description)
}

sealed interface AddPaymentIntent : UiIntent {
    data class RecipientEmailChanged(val value: String) : AddPaymentIntent
    data class AmountChanged(val value: String) : AddPaymentIntent
    data class CurrencySelected(val currency: Currency) : AddPaymentIntent
    data class DescriptionChanged(val value: String) : AddPaymentIntent
    data object Send : AddPaymentIntent
    data object Back : AddPaymentIntent
}

sealed interface AddPaymentEffect : UiEffect {
    data class PaymentSent(val message: String) : AddPaymentEffect
    data class ShowError(val message: String) : AddPaymentEffect
    data object NavigateBack : AddPaymentEffect
}

class AddPaymentViewModel(
    private val createPayment: CreatePaymentUseCase,
    private val validator: PaymentValidator,
    private val formatter: PaymentFormatter,
) : MviViewModel<AddPaymentState, AddPaymentIntent, AddPaymentEffect>(AddPaymentState()) {

    override fun handleIntent(intent: AddPaymentIntent) {
        when (intent) {
            is AddPaymentIntent.RecipientEmailChanged -> edit { copy(recipientEmail = intent.value) }
            is AddPaymentIntent.AmountChanged -> edit { copy(amount = sanitizeAmount(intent.value)) }
            is AddPaymentIntent.CurrencySelected -> edit { copy(currency = intent.currency) }
            is AddPaymentIntent.DescriptionChanged ->
                edit { copy(description = intent.value.take(PaymentValidator.MAX_DESCRIPTION_LENGTH)) }
            AddPaymentIntent.Send -> send()
            AddPaymentIntent.Back -> sendEffect(AddPaymentEffect.NavigateBack)
        }
    }

    private fun edit(reducer: AddPaymentState.() -> AddPaymentState) {
        setState {
            val next = reducer()
            next.copy(errors = if (next.submitAttempted) localErrors(next) else emptyMap())
        }
    }

    private fun send() {
        if (currentState.isSending) return
        setState { copy(submitAttempted = true, errors = localErrors(this)) }
        if (currentState.errors.isNotEmpty()) return

        val draft = currentState.draft()
        setState { copy(isSending = true) }
        viewModelScope.launch {
            val result = createPayment(draft)
            setState { copy(isSending = false) }
            when (result) {
                is CreatePaymentResult.Success -> {
                    val payment = result.payment
                    val amount = formatter.amountWithSymbol(payment.amount, payment.currency)
                    sendEffect(AddPaymentEffect.PaymentSent("Sent $amount to ${payment.recipientEmail}"))
                }
                is CreatePaymentResult.Invalid -> setState { copy(errors = result.errors) }
                is CreatePaymentResult.Failure -> sendEffect(AddPaymentEffect.ShowError(result.message))
            }
        }
    }

    private fun localErrors(state: AddPaymentState): Map<PaymentField, String> =
        when (val validation = validator.validate(state.draft())) {
            is PaymentValidation.Valid -> emptyMap()
            is PaymentValidation.Invalid -> validation.errors.mapValues { (_, error) -> error.message }
        }

    /** Keeps only digits and the first decimal point, as typed on a decimal keypad. */
    private fun sanitizeAmount(value: String): String {
        val filtered = value.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        val firstDot = filtered.indexOf('.')
        return if (firstDot < 0) filtered else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }
}
