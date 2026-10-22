package com.cashi.app.core.network

import com.cashi.app.core.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Builds the API client: JSON, timeouts, logging and the backend base URL. */
fun createApiClient(
    engine: HttpClientEngine,
    json: Json,
    tokens: TokenStorage,
    baseUrl: String = AppConfig.baseUrl,
): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
        requestTimeoutMillis = 60_000
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.INFO
        sanitizeHeader { it == HttpHeaders.Authorization }
    }
    defaultRequest {
        url(baseUrl.trimEnd('/') + "/")
        contentType(ContentType.Application.Json)
    }
}
