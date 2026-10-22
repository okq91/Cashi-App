package com.cashi.app.testing

import com.cashi.app.presentation.format.PaymentFormatter
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

class FixedClock(var instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/** Formatter pinned to 22 Oct 2026, 20:00 UTC. */
fun testFormatter(now: String = "2026-10-22T20:00:00Z") = PaymentFormatter(FixedClock(Instant.parse(now)), TimeZone.UTC)
