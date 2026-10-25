package com.cashi.app.domain

import com.cashi.app.data.remote.dto.PaymentRequestDto
import com.cashi.app.domain.model.Amount
import com.cashi.app.data.remote.dto.AmountSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class AmountTest {

    @ParameterizedTest(name = "\"{0}\" is {1} cents")
    @CsvSource(
        "30,3000",
        "30.5,3050",
        "30.50,3050",
        "0.01,1",
        "'1,245.50',124550",
        "' 7 ',700",
        "-5,-500",
        ignoreLeadingAndTrailingWhitespace = false,
    )
    fun `parses user input exactly`(text: String, minorUnits: Long) {
        assertEquals(minorUnits, Amount.parseOrNull(text)?.minorUnits)
    }

    @ParameterizedTest(name = "\"{0}\" is not an amount")
    @ValueSource(strings = ["", "abc", "1.2.3", "10.999", ".5", "5.", "1e3"])
    fun `rejects input that is not an amount`(text: String) {
        assertNull(Amount.parseOrNull(text))
    }

    @Test
    fun `formats with two decimals and thousands separators`() {
        assertEquals("1245.50", Amount(124550).toPlainString())
        assertEquals("1,245.50", Amount(124550).toDisplayString())
        assertEquals("1,000,000.00", Amount(100_000_000).toDisplayString())
        assertEquals("0.05", Amount(5).toDisplayString())
        assertEquals("-42.75", Amount(-4275).toDisplayString())
    }

    @Test
    fun `adds and compares`() {
        assertEquals(Amount(350), Amount(100) + Amount(250))
        assertTrue(Amount(1) > Amount.ZERO)
    }

    @Test
    fun `is sent to the backend as a bare JSON number with two decimals`() {
        val body = Json.encodeToString(PaymentRequestDto.serializer(), PaymentRequestDto(Amount(3050), "USD", "a@b.co"))
        assertEquals("""{"amount":30.50,"currency":"USD","recipientEmail":"a@b.co"}""", body)
    }

    @Test
    fun `reads integer, decimal and string numbers from the backend`() {
        assertEquals(Amount(3000), Json.decodeFromString(AmountSerializer, "30"))
        assertEquals(Amount(4275), Json.decodeFromString(AmountSerializer, "42.75"))
        assertEquals(Amount(12000), Json.decodeFromString(AmountSerializer, "\"120.00\""))
        assertEquals(Amount(13), Json.decodeFromString(AmountSerializer, "0.125"))
    }
}
