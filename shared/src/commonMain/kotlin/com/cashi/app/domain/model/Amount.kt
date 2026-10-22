package com.cashi.app.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A money amount stored as minor units (cents) so it never goes through floating point
 * on its way to the backend, which expects at most 2 decimal places.
 */
data class Amount(val minorUnits: Long) : Comparable<Amount> {

    val isPositive: Boolean get() = minorUnits > 0

    operator fun plus(other: Amount) = Amount(minorUnits + other.minorUnits)

    override fun compareTo(other: Amount) = minorUnits.compareTo(other.minorUnits)

    /** Plain decimal with exactly two fraction digits, e.g. `1245.50`. */
    fun toPlainString(): String {
        val sign = if (minorUnits < 0) "-" else ""
        val units = abs(minorUnits)
        return "$sign${units / 100}.${(units % 100).toString().padStart(2, '0')}"
    }

    /** Grouped decimal for display, e.g. `1,245.50`. */
    fun toDisplayString(): String {
        val plain = toPlainString()
        val sign = if (plain.startsWith("-")) "-" else ""
        val (whole, fraction) = plain.removePrefix("-").split('.')
        val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
        return "$sign$grouped.$fraction"
    }

    override fun toString() = toPlainString()

    companion object {
        val ZERO = Amount(0)
        const val MAX_FRACTION_DIGITS = 2
        private val DECIMAL = Regex("""^-?\d+(\.\d+)?$""")

        /**
         * Parses user input such as `30`, `30.5` or `1,245.50` exactly. Returns null when the
         * text is not a number or has more than [MAX_FRACTION_DIGITS] decimals.
         */
        fun parseOrNull(text: String): Amount? {
            val normalized = text.trim().replace(",", "")
            if (!DECIMAL.matches(normalized)) return null
            val negative = normalized.startsWith("-")
            val (whole, fraction) = normalized.removePrefix("-").split('.').let { it[0] to it.getOrElse(1) { "" } }
            if (fraction.length > MAX_FRACTION_DIGITS) return null
            if (whole.trimStart('0').length > 13) return null // keeps minor units well inside Long
            val minor = (whole.toLong() * 100) + fraction.padEnd(MAX_FRACTION_DIGITS, '0').toLong()
            return Amount(if (negative) -minor else minor)
        }

        /** Server values are trusted numbers; anything finer than cents is rounded. */
        fun fromServer(text: String): Amount =
            parseOrNull(text) ?: Amount((text.toDouble() * 100).roundToLong())
    }
}

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
