package com.cashi.app.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Error raised for every failed API call. Mirrors the backend's error body:
 * `{ "status": 400, "message": "Validation failed", "errors": { "amount": "..." } }`.
 */
class ApiException(
    val status: Int,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    cause: Throwable? = null,
) : Exception(message, cause) {

    val isNetworkError: Boolean get() = status == NETWORK_ERROR

    companion object {
        /** Status used for local failures (no connection, timeout, unreadable response). */
        const val NETWORK_ERROR = 0

        fun network(cause: Throwable? = null) = ApiException(
            status = NETWORK_ERROR,
            message = "Unable to reach the server. Check your connection and try again.",
            cause = cause,
        )

        fun fromResponse(status: Int, body: String?, json: Json): ApiException {
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
    }
}
