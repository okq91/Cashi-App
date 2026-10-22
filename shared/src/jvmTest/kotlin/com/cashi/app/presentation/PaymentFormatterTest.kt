package com.cashi.app.presentation

import com.cashi.app.domain.model.Amount
import com.cashi.app.testing.testFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class PaymentFormatterTest {
    private val formatter = testFormatter()

    @Test
    fun `dates are relative to today`() {
        assertEquals("Today, 6:42 PM", formatter.date(Instant.parse("2026-10-22T18:42:00Z")))
        assertEquals("Yesterday, 12:05 AM", formatter.date(Instant.parse("2026-10-21T00:05:00Z")))
        assertEquals("Oct 20, 10:15 AM", formatter.date(Instant.parse("2026-10-20T10:15:00Z")))
        assertEquals("Dec 31, 2025", formatter.date(Instant.parse("2025-12-31T12:00:00Z")))
    }

    @Test
    fun `greeting follows the time of day`() {
        assertEquals("Good morning", testFormatter("2026-10-22T08:00:00Z").greeting())
        assertEquals("Good afternoon", testFormatter("2026-10-22T13:00:00Z").greeting())
        assertEquals("Good evening", formatter.greeting())
        assertEquals("Good evening", testFormatter("2026-10-22T02:00:00Z").greeting())
    }

    @Test
    fun `month and amounts`() {
        assertEquals("October", formatter.currentMonthName())
        assertTrue(formatter.isInCurrentMonth(Instant.parse("2026-10-01T00:00:00Z")))
        assertFalse(formatter.isInCurrentMonth(Instant.parse("2026-09-30T23:59:59Z")))
        assertEquals("1,245.50", formatter.amount(Amount(124550)))
        assertEquals("$320.00", formatter.amountWithSymbol(Amount(32000), "USD"))
        assertEquals("JD 42.75", formatter.amountWithSymbol(Amount(4275), "JOD"))
        assertEquals("12.00 EUR", formatter.amountWithSymbol(Amount(1200), "EUR"))
    }
}
