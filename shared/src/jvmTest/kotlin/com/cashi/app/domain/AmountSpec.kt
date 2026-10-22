package com.cashi.app.domain

import com.cashi.app.data.remote.dto.PaymentRequestDto
import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.AmountSerializer
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class AmountSpec : FunSpec({

    context("parsing user input exactly") {
        withData(
            "30" to 3000L,
            "30.5" to 3050L,
            "30.50" to 3050L,
            "0.01" to 1L,
            "1,245.50" to 124550L,
            " 7 " to 700L,
            "-5" to -500L,
        ) { (text, minor) -> Amount.parseOrNull(text)?.minorUnits shouldBe minor }
    }

    context("rejecting input that is not an amount") {
        withData(nameFn = { "\"$it\"" }, "", "abc", "1.2.3", "10.999", ".5", "5.", "1e3") { text ->
            Amount.parseOrNull(text).shouldBeNull()
        }
    }

    test("formats with two decimals and thousands separators") {
        Amount(124550).toPlainString() shouldBe "1245.50"
        Amount(124550).toDisplayString() shouldBe "1,245.50"
        Amount(100_000_000).toDisplayString() shouldBe "1,000,000.00"
        Amount(5).toDisplayString() shouldBe "0.05"
        Amount(-4275).toDisplayString() shouldBe "-42.75"
    }

    test("adds and compares") {
        (Amount(100) + Amount(250)) shouldBe Amount(350)
        (Amount(1) > Amount.ZERO) shouldBe true
    }

    test("is sent to the backend as a bare JSON number with two decimals") {
        val body = Json.encodeToString(PaymentRequestDto.serializer(), PaymentRequestDto(Amount(3050), "USD", "a@b.co"))
        body shouldBe """{"amount":30.50,"currency":"USD","recipientEmail":"a@b.co"}"""
    }

    test("reads integer, decimal and string numbers from the backend") {
        Json.decodeFromString(AmountSerializer, "30") shouldBe Amount(3000)
        Json.decodeFromString(AmountSerializer, "42.75") shouldBe Amount(4275)
        Json.decodeFromString(AmountSerializer, "\"120.00\"") shouldBe Amount(12000)
        Json.decodeFromString(AmountSerializer, "0.125") shouldBe Amount(13)
    }
})
