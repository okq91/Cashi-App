package com.cashi.app.domain.usecase

import com.cashi.app.core.network.ApiException
import com.cashi.app.data.repository.PaymentsRepository
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.domain.validation.PaymentValidation
import com.cashi.app.domain.validation.PaymentValidator
import kotlin.coroutines.cancellation.CancellationException

sealed interface CreatePaymentResult {
    data class Success(val payment: Payment) : CreatePaymentResult

    /** Rejected locally or by the backend's validation; nothing was sent. */
    data class Invalid(val errors: Map<PaymentField, String>, val message: String? = null) : CreatePaymentResult

    data class Failure(val message: String, val isNetworkError: Boolean) : CreatePaymentResult
}

/** Validates the draft and, only if it is valid, sends it to the backend. */
class CreatePaymentUseCase(
    private val validator: PaymentValidator,
    private val repository: PaymentsRepository,
) {
    suspend operator fun invoke(draft: PaymentDraft): CreatePaymentResult {
        val payment = when (val validation = validator.validate(draft)) {
            is PaymentValidation.Invalid ->
                return CreatePaymentResult.Invalid(validation.errors.mapValues { (_, error) -> error.message })
            is PaymentValidation.Valid -> validation.payment
        }
        return try {
            CreatePaymentResult.Success(repository.create(payment))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            val fieldErrors = e.fieldErrors.mapNotNull { (key, message) -> serverField(key)?.let { it to message } }.toMap()
            if (fieldErrors.isNotEmpty()) {
                CreatePaymentResult.Invalid(fieldErrors, e.message)
            } else {
                CreatePaymentResult.Failure(e.message, e.isNetworkError)
            }
        } catch (e: Throwable) {
            CreatePaymentResult.Failure(e.message ?: "Something went wrong", isNetworkError = false)
        }
    }

    private fun serverField(key: String): PaymentField? = when (key) {
        "recipientEmail" -> PaymentField.RECIPIENT_EMAIL
        "amount" -> PaymentField.AMOUNT
        "currency" -> PaymentField.CURRENCY
        "description" -> PaymentField.DESCRIPTION
        else -> null
    }
}
