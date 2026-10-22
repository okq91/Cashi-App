package com.cashi.app.ui.util

/**
 * Stable test tags for UI automation. On Android they are exposed as resource ids
 * (`testTagsAsResourceId`), so Appium finds them with `By.id("<tag>")`.
 */
object TestTags {
    const val SPLASH_LOADING = "splash_loading"
    const val SPLASH_ERROR = "splash_error"
    const val SPLASH_RETRY = "splash_retry"

    const val PAYMENTS_SCREEN = "payments_screen"
    const val SUMMARY_TOTAL = "summary_total"
    const val SUMMARY_COUNT = "summary_count"
    const val SUMMARY_USD = "summary_usd"
    const val HISTORY_LIST = "history_list"
    const val HISTORY_LOADING = "history_loading"
    const val HISTORY_EMPTY = "history_empty"
    const val HISTORY_ERROR = "history_error"
    const val HISTORY_RETRY = "history_retry"
    const val SEE_ALL = "see_all_button"
    const val PAYMENT_ITEM = "payment_item"
    const val PAYMENT_TITLE = "payment_item_title"
    const val PAYMENT_EMAIL = "payment_item_email"
    const val PAYMENT_DATE = "payment_item_date"
    const val PAYMENT_AMOUNT = "payment_item_amount"
    const val PAYMENT_STATUS = "payment_item_status"
    const val ADD_PAYMENT_BUTTON = "add_payment_button"

    const val ADD_PAYMENT_SCREEN = "add_payment_screen"
    const val BACK_BUTTON = "back_button"
    const val RECIPIENT_EMAIL_INPUT = "recipient_email_input"
    const val RECIPIENT_EMAIL_ERROR = "recipient_email_error"
    const val AMOUNT_INPUT = "amount_input"
    const val AMOUNT_ERROR = "amount_error"
    const val CURRENCY_DROPDOWN = "currency_dropdown"
    const val CURRENCY_ERROR = "currency_error"
    const val DESCRIPTION_INPUT = "description_input"
    const val DESCRIPTION_ERROR = "description_error"
    const val SEND_BUTTON = "send_button"

    const val SNACKBAR = "snackbar_message"

    fun currencyOption(code: String) = "currency_option_$code"
}
