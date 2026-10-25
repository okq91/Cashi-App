package com.cashi.app.uitests

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Sending a payment")
class SendPaymentTest : AppiumTestBase() {

    @Test
    @DisplayName("a valid payment is sent and shows up at the top of the history")
    fun sendsPayment() {
        val recipient = uniqueEmail("test")
        val payments = PaymentsPage(driver).waitUntilLoaded()

        val form = payments.openAddPayment()
            .enterRecipient(recipient)
            .enterAmount("12.34")
            .selectCurrency("USD")
            .enterDescription("Appium payment")
        assertEquals("USD — US Dollar", form.selectedCurrencyLabel())
        form.send()

        // Back on the history: confirmation, then the new payment on top.
        assertEquals("Sent $12.34 to $recipient", payments.waitForSnackbar())
        val row = payments.waitForFirstRowTo(recipient)
        assertEquals("Appium payment", row.title)
        assertEquals("12.34 USD", row.amount)
        assertEquals("Sent", row.status)
        assertTrue(row.date.startsWith("Today"), "Expected today's date but was '${row.date}'")
    }

    @Test
    @DisplayName("an invalid form shows errors and sends nothing")
    fun rejectsInvalidPayment() {
        val payments = PaymentsPage(driver).waitUntilLoaded()
        // The history may be empty on a fresh backend, so compare "no row" as well.
        val topBefore = payments.rows().firstOrNull()

        val form = payments.openAddPayment()
            .enterRecipient("ahmad@")
            .enterAmount("0")
            .send()

        assertEquals("Enter a valid email address", form.recipientError())
        assertEquals("Amount must be greater than zero", form.amountError())
        assertTrue(form.isShown(TestTags.ADD_PAYMENT_SCREEN), "The form should stay open")

        val history = form.back().waitUntilLoaded()
        assertEquals(topBefore, history.rows().firstOrNull())
    }
}
