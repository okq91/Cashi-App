package com.cashi.app.uitests

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** Compose test tags are exposed as resource ids without a package prefix. */
fun byTag(tag: String): By = AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"$tag\")")

abstract class Page(protected val driver: AndroidDriver) {
    protected fun waitFor(tag: String, seconds: Long = 20): WebElement =
        WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.visibilityOfElementLocated(byTag(tag)))

    protected fun find(tag: String): WebElement = driver.findElement(byTag(tag))

    fun isShown(tag: String): Boolean = driver.findElements(byTag(tag)).any { it.isDisplayed }
}

data class PaymentRow(val title: String, val email: String, val date: String, val amount: String, val status: String)

/** The history screen (start destination once the app has signed in). */
class PaymentsPage(driver: AndroidDriver) : Page(driver) {

    /** Waits for the silent sign-in and the first history load. */
    fun waitUntilLoaded(): PaymentsPage = apply {
        waitFor(TestTags.PAYMENTS_SCREEN, seconds = 40)
        WebDriverWait(driver, Duration.ofSeconds(20)).until {
            isShown(TestTags.PAYMENT_ITEM) || isShown(TestTags.HISTORY_EMPTY)
        }
    }


    /** Rows currently on screen. A row cut off at the bottom edge has no readable children and is skipped. */
    fun rows(): List<PaymentRow> = driver.findElements(byTag(TestTags.PAYMENT_ITEM)).mapNotNull { row ->
        runCatching {
            PaymentRow(
                title = row.findElement(byTag(TestTags.PAYMENT_TITLE)).text,
                email = row.findElement(byTag(TestTags.PAYMENT_EMAIL)).text,
                date = row.findElement(byTag(TestTags.PAYMENT_DATE)).text,
                amount = row.findElement(byTag(TestTags.PAYMENT_AMOUNT)).text,
                status = row.findElement(byTag(TestTags.PAYMENT_STATUS)).text,
            )
        }.getOrNull()
    }

    fun firstRow(): PaymentRow = rows().first()

    /** Swipes down on the history to reload it from the backend. */
    fun pullToRefresh(): PaymentsPage = apply {
        val list = find(TestTags.HISTORY_LIST).rect
        driver.executeScript(
            "mobile: swipeGesture",
            mapOf(
                "left" to list.x + list.width / 4,
                "top" to list.y + list.height / 4,
                "width" to list.width / 2,
                "height" to list.height / 2,
                "direction" to "down",
                "percent" to 0.9,
            ),
        )
    }

    /** Waits until the top of the history shows a payment to [email]. */
    fun waitForFirstRowTo(email: String): PaymentRow {
        WebDriverWait(driver, Duration.ofSeconds(20)).until { runCatching { firstRow().email == email }.getOrDefault(false) }
        return firstRow()
    }

    fun openAddPayment(): AddPaymentPage {
        waitFor(TestTags.ADD_PAYMENT_BUTTON).click()
        return AddPaymentPage(driver).also { it.waitUntilShown() }
    }

    fun waitForSnackbar(): String = waitFor(TestTags.SNACKBAR).text
}

/** The "New payment" form. */
class AddPaymentPage(driver: AndroidDriver) : Page(driver) {

    fun waitUntilShown(): AddPaymentPage = apply { waitFor(TestTags.ADD_PAYMENT_SCREEN) }

    fun enterRecipient(email: String) = apply { type(TestTags.RECIPIENT_EMAIL_INPUT, email) }
    fun enterAmount(amount: String) = apply { type(TestTags.AMOUNT_INPUT, amount) }
    fun enterDescription(text: String) = apply { type(TestTags.DESCRIPTION_INPUT, text) }

    fun selectCurrency(code: String) = apply {
        find(TestTags.CURRENCY_DROPDOWN).click()
        waitFor(TestTags.currencyOption(code)).click()
    }

    fun selectedCurrencyLabel(): String =
        find(TestTags.CURRENCY_DROPDOWN).findElement(By.className("android.widget.TextView")).text

    fun send() = apply { hideKeyboard(); find(TestTags.SEND_BUTTON).click() }

    fun recipientError(): String = waitFor(TestTags.RECIPIENT_EMAIL_ERROR).text
    fun amountError(): String = waitFor(TestTags.AMOUNT_ERROR).text

    fun back(): PaymentsPage {
        hideKeyboard()
        find(TestTags.BACK_BUTTON).click()
        return PaymentsPage(driver)
    }

    private fun type(tag: String, text: String) {
        val field = waitFor(tag)
        field.click()
        field.clear()
        field.sendKeys(text)
    }

    private fun hideKeyboard() {
        if (driver.isKeyboardShown) driver.hideKeyboard()
    }
}
