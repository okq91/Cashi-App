package com.cashi.app.presentation.format

import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Currency
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Display strings for the payments screens, computed against an injectable clock and zone. */
class PaymentFormatter(
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    fun now(): LocalDateTime = clock.now().toLocalDateTime(timeZone)

    /** "Good morning" before noon, "Good afternoon" until 6 PM, then "Good evening". */
    fun greeting(): String = when (now().hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    fun currentMonthName(): String = MONTHS[now().month.number - 1]

    fun isInCurrentMonth(instant: Instant): Boolean {
        val today = now().date
        val date = instant.toLocalDateTime(timeZone).date
        return date.year == today.year && date.month == today.month
    }

    /** "Today, 6:42 PM", "Yesterday, 9:03 PM", "Oct 20, 10:15 AM" or "Oct 20, 2025" for older years. */
    fun date(instant: Instant): String {
        val dateTime = instant.toLocalDateTime(timeZone)
        val today = now().date
        val time = time(dateTime)
        return when (dateTime.date) {
            today -> "Today, $time"
            today.minus(DatePeriod(days = 1)) -> "Yesterday, $time"
            else -> if (dateTime.year == today.year) "${shortDate(dateTime.date)}, $time" else "${shortDate(dateTime.date)}, ${dateTime.year}"
        }
    }

    /** "450.00" */
    fun amount(amount: Amount): String = amount.toDisplayString()

    /** "$320.00" for USD, "JD 42.75" for JOD, "12.00 EUR" for anything else. */
    fun amountWithSymbol(amount: Amount, currencyCode: String): String = when (Currency.fromCode(currencyCode)) {
        Currency.USD -> "$" + amount.toDisplayString()
        Currency.JOD -> "JD " + amount.toDisplayString()
        null -> amount.toDisplayString() + " " + currencyCode
    }

    private fun shortDate(date: LocalDate) = "${MONTHS[date.month.number - 1].take(3)} ${date.day}"

    private fun time(dateTime: LocalDateTime): String {
        val hour12 = (dateTime.hour % 12).let { if (it == 0) 12 else it }
        val minute = dateTime.minute.toString().padStart(2, '0')
        val period = if (dateTime.hour < 12) "AM" else "PM"
        return "$hour12:$minute $period"
    }

    private companion object {
        val MONTHS = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December",
        )
    }
}
