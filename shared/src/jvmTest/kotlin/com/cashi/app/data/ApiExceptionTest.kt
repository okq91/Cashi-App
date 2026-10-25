package com.cashi.app.data

import com.cashi.app.core.network.apiExceptionFrom
import com.cashi.app.domain.error.ApiException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class ApiExceptionTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses the backend validation error body`() {
        val body = """{"status":400,"message":"Validation failed","errors":{"amount":"Amount must be greater than zero"}}"""
        val error = apiExceptionFrom(400, body, json)
        assertEquals(400, error.status)
        assertEquals("Validation failed", error.message)
        assertEquals(mapOf("amount" to "Amount must be greater than zero"), error.fieldErrors)
    }

    @Test
    fun `parses an error without field errors`() {
        val error = apiExceptionFrom(401, """{"status":401,"message":"Invalid email or password","errors":null}""", json)
        assertEquals("Invalid email or password", error.message)
        assertTrue(error.fieldErrors.isEmpty())
    }

    @Test
    fun `falls back to the raw body or the status for non-JSON errors`() {
        assertEquals("Bad Gateway", apiExceptionFrom(502, "Bad Gateway", json).message)
        assertEquals("HTTP 500", apiExceptionFrom(500, "", json).message)
        assertEquals("HTTP 500", apiExceptionFrom(500, null, json).message)
    }

    @Test
    fun `network errors are flagged`() {
        assertTrue(ApiException.network().isNetworkError)
        assertFalse(ApiException(500, "boom").isNetworkError)
    }
}
