package com.cashi.app.domain

import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.model.NewPayment
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentError
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.domain.validation.PaymentValidation
import com.cashi.app.domain.validation.PaymentValidator
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull

class PaymentValidatorTest {
    private val validator = PaymentValidator()

    @Test
    fun `a complete draft becomes a normalized payment`() {
        val result = validator.validate(PaymentDraft(" ahmad@gmail.com ", "30", "usd", "  Money transfer "))
        assertEquals(
            PaymentValidation.Valid(NewPayment("ahmad@gmail.com", Amount(3000), Currency.USD, "Money transfer")),
            result,
        )
    }

    @Test
    fun `a blank description is omitted`() {
        val result = validator.validate(PaymentDraft("a@b.co", "1", "JOD", "   "))
        assertNull(assertInstanceOf<PaymentValidation.Valid>(result).payment.description)
    }

    @Test
    fun `an empty draft reports every required field`() {
        val result = validator.validate(PaymentDraft("", "", ""))
        assertEquals(
            mapOf(
                PaymentField.RECIPIENT_EMAIL to PaymentError.EMAIL_REQUIRED,
                PaymentField.AMOUNT to PaymentError.AMOUNT_REQUIRED,
                PaymentField.CURRENCY to PaymentError.CURRENCY_UNSUPPORTED,
            ),
            assertInstanceOf<PaymentValidation.Invalid>(result).errors,
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["ahmad@gmail.com", "sara.k@example.com", "first+tag@sub.domain.io"])
    fun `well formed emails are accepted`(email: String) {
        assertNull(validator.emailError(email))
    }

    @ParameterizedTest
    @ValueSource(strings = ["ahmad", "ahmad@", "@gmail.com", "ahmad@gmail", "a b@c.com", "ahmad@@gmail.com"])
    fun `malformed emails are rejected`(email: String) {
        assertEquals(PaymentError.EMAIL_INVALID, validator.emailError(email))
    }

    @Test
    fun `emails longer than the backend allows are rejected`() {
        assertEquals(PaymentError.EMAIL_TOO_LONG, validator.emailError("a".repeat(250) + "@b.com"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "0.00", "-1", "-0.01"])
    fun `amounts must be positive`(amount: String) {
        assertEquals(PaymentError.AMOUNT_NOT_POSITIVE, validator.amountError(amount))
    }

    @ParameterizedTest
    @ValueSource(strings = ["1.001", "10.999"])
    fun `amounts allow at most two decimals`(amount: String) {
        assertEquals(PaymentError.AMOUNT_TOO_MANY_DECIMALS, validator.amountError(amount))
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc", "1.2.3", "\$5"])
    fun `amounts must be numbers`(amount: String) {
        assertEquals(PaymentError.AMOUNT_INVALID, validator.amountError(amount))
    }

    @Test
    fun `amounts above 12 integer digits are too large`() {
        assertEquals(PaymentError.AMOUNT_TOO_LARGE, validator.amountError("1000000000000"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["0.01", "30", "1,245.50", "999999999999.99"])
    fun `valid amounts are accepted`(amount: String) {
        assertNull(validator.amountError(amount))
    }

    @Test
    fun `JOD and USD are supported in any case`() {
        assertNull(validator.currencyError("JOD"))
        assertNull(validator.currencyError("usd"))
    }

    @Test
    fun `other currencies are rejected`() {
        assertEquals(PaymentError.CURRENCY_UNSUPPORTED, validator.currencyError("GBP"))
        assertEquals(PaymentError.CURRENCY_UNSUPPORTED, validator.currencyError(""))
    }

    @Test
    fun `descriptions allow 80 characters`() {
        assertNull(validator.descriptionError("x".repeat(80)))
        assertEquals(PaymentError.DESCRIPTION_TOO_LONG, validator.descriptionError("x".repeat(81)))
    }
}
