package com.cashi.app.core.network

import com.cashi.app.domain.error.ApiException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Builds an [ApiException] from a failed response and its (possibly missing or non-JSON) body. */
fun apiExceptionFrom(status: Int, body: String?, json: Json): ApiException {
    val obj = body?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
    val message = obj?.get("message")?.jsonPrimitive?.contentOrNull
    if (obj == null || message == null) {
        return ApiException(status, body?.takeIf { it.isNotBlank() } ?: "HTTP $status")
    }
    val errors = (obj["errors"] as? JsonObject)
        ?.mapValues { (_, value) -> (value as? JsonPrimitive)?.contentOrNull ?: value.toString() }
        .orEmpty()
    return ApiException(
        status = obj["status"]?.jsonPrimitive?.intOrNull ?: status,
        message = message,
        fieldErrors = errors,
    )
}
