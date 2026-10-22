package com.cashi.app.data

import com.cashi.app.core.network.ApiException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ApiExceptionSpec : FunSpec({
    val json = Json { ignoreUnknownKeys = true }

    test("parses the backend validation error body") {
        val body = """{"status":400,"message":"Validation failed","errors":{"amount":"Amount must be greater than zero"}}"""
        val error = ApiException.fromResponse(400, body, json)
        error.status shouldBe 400
        error.message shouldBe "Validation failed"
        error.fieldErrors shouldContainExactly mapOf("amount" to "Amount must be greater than zero")
    }

    test("parses an error without field errors") {
        val error = ApiException.fromResponse(401, """{"status":401,"message":"Invalid email or password","errors":null}""", json)
        error.message shouldBe "Invalid email or password"
        error.fieldErrors.shouldBeEmpty()
    }

    test("falls back to the raw body or the status for non-JSON errors") {
        ApiException.fromResponse(502, "Bad Gateway", json).message shouldBe "Bad Gateway"
        ApiException.fromResponse(500, "", json).message shouldBe "HTTP 500"
        ApiException.fromResponse(500, null, json).message shouldBe "HTTP 500"
    }

    test("network errors are flagged") {
        ApiException.network().isNetworkError shouldBe true
        ApiException(500, "boom").isNetworkError shouldBe false
    }
})
