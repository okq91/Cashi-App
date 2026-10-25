package com.cashi.app.uitests

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Viewing transaction history")
class TransactionHistoryTest : AppiumTestBase() {

    @Test
    @DisplayName("pull to refresh lists new backend payments with their details, newest first")
    fun showsHistory() {
        val payments = PaymentsPage(driver).waitUntilLoaded()

        val older = uniqueEmail("appium.history.older")
        BackendClient().createPayment("5.25", "JOD", older, "Older history payment")
        val newer = uniqueEmail("appium.history.newer")
        BackendClient().createPayment("40", "USD", newer, "Newer history payment")

        payments.pullToRefresh().waitForFirstRowTo(newer)
        val rows = payments.rows()
        assertTrue(rows.size >= 2, "Expected at least the two seeded payments, got $rows")
        assertEquals(PaymentRow("Newer history payment", newer, rows[0].date, "40.00 USD", "Sent"), rows[0])
        assertEquals(PaymentRow("Older history payment", older, rows[1].date, "5.25 JOD", "Sent"), rows[1])
        assertTrue(rows[0].date.startsWith("Today"), "Expected today's date but was '${rows[0].date}'")
    }
}
