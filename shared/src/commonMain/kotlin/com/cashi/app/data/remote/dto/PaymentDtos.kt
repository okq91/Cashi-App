package com.cashi.app.data.remote.dto

import com.cashi.app.domain.model.Amount
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

@Serializable
data class PaymentRequestDto(
    @Serializable(with = AmountSerializer::class) val amount: Amount,
    val currency: String,
    val recipientEmail: String,
    val description: String? = null,
)

@Serializable
data class PaymentDto(
    val id: String,
    @Serializable(with = AmountSerializer::class) val amount: Amount,
    val currency: String,
    val recipientEmail: String,
    val description: String? = null,
    @Serializable(with = IsoInstantSerializer::class) val createdAt: Instant,
)

/** One page of `GET /api/payments`; [nextCursor] is null on the last page. */
@Serializable
data class PaymentsPageDto(
    val items: List<PaymentDto>,
    val nextCursor: String? = null,
)

/** ISO-8601 instants such as `2026-10-22T20:06:25.221845Z`. */
object IsoInstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("IsoInstant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
