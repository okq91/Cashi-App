package com.cashi.app.core.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a request and decodes a successful body as [T]. Non-2xx responses become an
 * [ApiException] parsed from the error body; transport/decoding failures become
 * [ApiException.network].
 */
suspend inline fun <reified T> apiCall(json: Json, request: () -> HttpResponse): T {
    val response = try {
        request()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        throw e
    } catch (e: Throwable) {
        throw ApiException.network(e)
    }
    if (!response.status.isSuccess()) {
        val body = runCatching { response.bodyAsText() }.getOrNull()
        throw ApiException.fromResponse(response.status.value, body, json)
    }
    return try {
        response.body()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ApiException.network(e)
    }
}
