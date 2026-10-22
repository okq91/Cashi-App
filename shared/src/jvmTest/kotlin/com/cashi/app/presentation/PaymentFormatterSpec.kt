package com.cashi.app.presentation

import com.cashi.app.domain.model.Amount
import com.cashi.app.testing.testFormatter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Instant

class PaymentFormatterSpec : FunSpec({
    val formatter = testFormatter()

    test("dates are relative to today") {
        formatter.date(Instant.parse("2026-10-22T18:42:00Z")) shouldBe "Today, 6:42 PM"
        formatter.date(Instant.parse("2026-10-21T00:05:00Z")) shouldBe "Yesterday, 12:05 AM"
        formatter.date(Instant.parse("2026-10-20T10:15:00Z")) shouldBe "Oct 20, 10:15 AM"
        formatter.date(Instant.parse("2025-12-31T12:00:00Z")) shouldBe "Dec 31, 2025"
    }

    test("greeting follows the time of day") {
        testFormatter("2026-10-22T08:00:00Z").greeting() shouldBe "Good morning"
        testFormatter("2026-10-22T13:00:00Z").greeting() shouldBe "Good afternoon"
        formatter.greeting() shouldBe "Good evening"
        testFormatter("2026-10-22T02:00:00Z").greeting() shouldBe "Good evening"
    }

    test("month and amounts") {
        formatter.currentMonthName() shouldBe "October"
        formatter.isInCurrentMonth(Instant.parse("2026-10-01T00:00:00Z")) shouldBe true
        formatter.isInCurrentMonth(Instant.parse("2026-09-30T23:59:59Z")) shouldBe false
        formatter.amount(Amount(124550)) shouldBe "1,245.50"
        formatter.amountWithSymbol(Amount(32000), "USD") shouldBe "$320.00"
        formatter.amountWithSymbol(Amount(4275), "JOD") shouldBe "JD 42.75"
        formatter.amountWithSymbol(Amount(1200), "EUR") shouldBe "12.00 EUR"
    }
})
