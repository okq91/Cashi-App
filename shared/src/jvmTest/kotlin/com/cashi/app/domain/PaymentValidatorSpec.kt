package com.cashi.app.domain

import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.model.NewPayment
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentError
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.domain.validation.PaymentValidation
import com.cashi.app.domain.validation.PaymentValidator
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PaymentValidatorSpec : BehaviorSpec({
    val validator = PaymentValidator()

    Given("a complete payment draft") {
        val draft = PaymentDraft(" ahmad@gmail.com ", "30", "usd", "  Money transfer ")

        When("it is validated") {
            val result = validator.validate(draft)

            Then("it becomes a normalized payment") {
                result shouldBe PaymentValidation.Valid(
                    NewPayment("ahmad@gmail.com", Amount(3000), Currency.USD, "Money transfer"),
                )
            }
        }
    }

    Given("a draft with a blank description") {
        Then("the description is omitted") {
            val result = validator.validate(PaymentDraft("a@b.co", "1", "JOD", "   "))
            result.shouldBeInstanceOf<PaymentValidation.Valid>().payment.description.shouldBeNull()
        }
    }

    Given("an empty draft") {
        Then("every required field reports an error") {
            val result = validator.validate(PaymentDraft("", "", ""))
            result.shouldBeInstanceOf<PaymentValidation.Invalid>().errors shouldContainExactly mapOf(
                PaymentField.RECIPIENT_EMAIL to PaymentError.EMAIL_REQUIRED,
                PaymentField.AMOUNT to PaymentError.AMOUNT_REQUIRED,
                PaymentField.CURRENCY to PaymentError.CURRENCY_UNSUPPORTED,
            )
        }
    }

    Given("recipient emails") {
        When("they are well formed") {
            withData("ahmad@gmail.com", "sara.k@example.com", "first+tag@sub.domain.io") { email ->
                validator.emailError(email).shouldBeNull()
            }
        }
        When("they are malformed") {
            withData("ahmad", "ahmad@", "@gmail.com", "ahmad@gmail", "a b@c.com", "ahmad@@gmail.com") { email ->
                validator.emailError(email) shouldBe PaymentError.EMAIL_INVALID
            }
        }
        When("they are longer than the backend allows") {
            Then("they are rejected") {
                validator.emailError("a".repeat(250) + "@b.com") shouldBe PaymentError.EMAIL_TOO_LONG
            }
        }
    }

    Given("amounts") {
        When("they are not positive") {
            withData("0", "0.00", "-1", "-0.01") { amount ->
                validator.amountError(amount) shouldBe PaymentError.AMOUNT_NOT_POSITIVE
            }
        }
        When("they have more than two decimals") {
            withData("1.001", "10.999") { amount ->
                validator.amountError(amount) shouldBe PaymentError.AMOUNT_TOO_MANY_DECIMALS
            }
        }
        When("they are not numbers") {
            withData("abc", "1.2.3", "$5") { amount ->
                validator.amountError(amount) shouldBe PaymentError.AMOUNT_INVALID
            }
        }
        When("they exceed 12 integer digits") {
            Then("they are too large") {
                validator.amountError("1000000000000") shouldBe PaymentError.AMOUNT_TOO_LARGE
            }
        }
        When("they are valid") {
            withData("0.01", "30", "1,245.50", "999999999999.99") { amount ->
                validator.amountError(amount).shouldBeNull()
            }
        }
    }

    Given("currencies") {
        Then("JOD and USD are supported in any case") {
            validator.currencyError("JOD").shouldBeNull()
            validator.currencyError("usd").shouldBeNull()
        }
        Then("anything else is rejected") {
            validator.currencyError("EUR") shouldBe PaymentError.CURRENCY_UNSUPPORTED
            validator.currencyError("") shouldBe PaymentError.CURRENCY_UNSUPPORTED
        }
    }

    Given("a description") {
        Then("80 characters are allowed and 81 are not") {
            validator.descriptionError("x".repeat(80)).shouldBeNull()
            validator.descriptionError("x".repeat(81)) shouldBe PaymentError.DESCRIPTION_TOO_LONG
        }
    }
})
