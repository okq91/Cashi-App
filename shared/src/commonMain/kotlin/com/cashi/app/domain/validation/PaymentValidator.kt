package com.cashi.app.domain.validation

import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.model.NewPayment

/** Raw form input, exactly as typed. */
data class PaymentDraft(
    val recipientEmail: String,
    val amount: String,
    val currency: String,
    val description: String = "",
)

enum class PaymentField { RECIPIENT_EMAIL, AMOUNT, CURRENCY, DESCRIPTION }

enum class PaymentError(val field: PaymentField, val message: String) {
    EMAIL_REQUIRED(PaymentField.RECIPIENT_EMAIL, "Recipient email is required"),
    EMAIL_INVALID(PaymentField.RECIPIENT_EMAIL, "Enter a valid email address"),
    EMAIL_TOO_LONG(PaymentField.RECIPIENT_EMAIL, "Email must be at most ${PaymentValidator.MAX_EMAIL_LENGTH} characters"),
    AMOUNT_REQUIRED(PaymentField.AMOUNT, "Amount is required"),
    AMOUNT_INVALID(PaymentField.AMOUNT, "Enter a valid amount"),
    AMOUNT_TOO_MANY_DECIMALS(PaymentField.AMOUNT, "Up to 2 decimal places"),
    AMOUNT_NOT_POSITIVE(PaymentField.AMOUNT, "Amount must be greater than zero"),
    AMOUNT_TOO_LARGE(PaymentField.AMOUNT, "Amount is too large"),
    CURRENCY_UNSUPPORTED(PaymentField.CURRENCY, "Choose a supported currency"),
    DESCRIPTION_TOO_LONG(PaymentField.DESCRIPTION, "Description must be at most ${PaymentValidator.MAX_DESCRIPTION_LENGTH} characters"),
}

sealed interface PaymentValidation {
    data class Valid(val payment: NewPayment) : PaymentValidation
    data class Invalid(val errors: Map<PaymentField, PaymentError>) : PaymentValidation
}

/**
 * Client-side checks that mirror the backend's rules (`PaymentRequestDTO`) so the user gets
 * immediate feedback: a well-formed recipient email, a positive amount with at most two
 * decimals, a supported currency and a short description.
 */
class PaymentValidator {

    fun validate(draft: PaymentDraft): PaymentValidation {
        val errors = listOfNotNull(
            emailError(draft.recipientEmail),
            amountError(draft.amount),
            currencyError(draft.currency),
            descriptionError(draft.description),
        ).associateBy { it.field }

        if (errors.isNotEmpty()) return PaymentValidation.Invalid(errors)
        return PaymentValidation.Valid(
            NewPayment(
                recipientEmail = draft.recipientEmail.trim(),
                amount = Amount.parseOrNull(draft.amount)!!,
                currency = Currency.fromCode(draft.currency)!!,
                description = draft.description.trim().ifEmpty { null },
            ),
        )
    }

    fun emailError(email: String): PaymentError? {
        val value = email.trim()
        return when {
            value.isEmpty() -> PaymentError.EMAIL_REQUIRED
            value.length > MAX_EMAIL_LENGTH -> PaymentError.EMAIL_TOO_LONG
            !EMAIL.matches(value) -> PaymentError.EMAIL_INVALID
            else -> null
        }
    }

    fun amountError(amount: String): PaymentError? {
        val value = amount.trim()
        if (value.isEmpty()) return PaymentError.AMOUNT_REQUIRED
        val parsed = Amount.parseOrNull(value)
        return when {
            parsed == null && TOO_MANY_DECIMALS.matches(value.replace(",", "")) -> PaymentError.AMOUNT_TOO_MANY_DECIMALS
            parsed == null -> PaymentError.AMOUNT_INVALID
            !parsed.isPositive -> PaymentError.AMOUNT_NOT_POSITIVE
            parsed > MAX_AMOUNT -> PaymentError.AMOUNT_TOO_LARGE
            else -> null
        }
    }

    fun currencyError(currency: String): PaymentError? =
        if (Currency.fromCode(currency) == null) PaymentError.CURRENCY_UNSUPPORTED else null

    fun descriptionError(description: String): PaymentError? =
        if (description.trim().length > MAX_DESCRIPTION_LENGTH) PaymentError.DESCRIPTION_TOO_LONG else null

    companion object {
        const val MAX_EMAIL_LENGTH = 254
        const val MAX_DESCRIPTION_LENGTH = 80

        /** Backend allows 12 integer digits. */
        val MAX_AMOUNT = Amount(999_999_999_999_99)

        private val EMAIL = Regex("""^[A-Za-z0-9._%+'-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$""")
        private val TOO_MANY_DECIMALS = Regex("""^-?\d+\.\d{3,}$""")
    }
}
