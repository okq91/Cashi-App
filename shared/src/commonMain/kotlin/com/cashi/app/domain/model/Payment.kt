package com.cashi.app.domain.model

import kotlin.time.Instant

/** A payment from the user's history. [currency] is kept as the raw ISO code the server sent. */
data class Payment(
    val id: String,
    val amount: Amount,
    val currency: String,
    val recipientEmail: String,
    val description: String?,
    val createdAt: Instant,
)

/** A validated payment that is ready to be sent. */
data class NewPayment(
    val recipientEmail: String,
    val amount: Amount,
    val currency: Currency,
    val description: String?,
)
