package com.cashi.app.domain.error

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
    }
}
