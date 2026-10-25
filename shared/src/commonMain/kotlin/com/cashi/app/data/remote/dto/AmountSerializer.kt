package com.cashi.app.data.remote.dto

import com.cashi.app.domain.model.Amount
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

/** Writes amounts as bare JSON numbers (`30.50`) and reads any JSON number or numeric string. */
object AmountSerializer : KSerializer<Amount> {
    override val descriptor = PrimitiveSerialDescriptor("Amount", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Amount) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
        } else {
            encoder.encodeString(value.toPlainString())
        }
    }

    override fun deserialize(decoder: Decoder): Amount {
        val text = if (decoder is JsonDecoder) decoder.decodeJsonElement().jsonPrimitive.content else decoder.decodeString()
        return Amount.fromServer(text)
    }
}
