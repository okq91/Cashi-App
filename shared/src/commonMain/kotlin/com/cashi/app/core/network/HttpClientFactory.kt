package com.cashi.app.core.network

import com.cashi.app.core.storage.TokenStorage
import com.cashi.app.core.suspendRunCatching
import com.cashi.app.data.remote.AuthApi
import com.cashi.app.domain.model.Credentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
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

/**
 * Builds the API client:
 * - JSON content negotiation, timeouts, logging (Authorization header redacted) and the base URL;
 * - Ktor's bearer [Auth] plugin attaches the stored access token to every non-auth request;
 * - on a 401 it refreshes the tokens with `/api/auth/refresh` and retries the request once;
 *   if the refresh token is missing, expired or revoked it signs in again with [credentials].
 */
fun createApiClient(
    engine: HttpClientEngine,
    json: Json,
    tokens: TokenStorage,
    baseUrl: String = AppConfig.baseUrl,
    credentials: Credentials = AppConfig.demoCredentials,
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
    install(Auth) {
        bearer {
            loadTokens {
                tokens.accessToken?.let { BearerTokens(it, tokens.refreshToken) }
            }
            refreshTokens {
                val authApi = AuthApi(client, json)
                val refreshToken = oldTokens?.refreshToken ?: tokens.refreshToken
                val refreshed = refreshToken?.let { token ->
                    suspendRunCatching { authApi.refresh(token) { markAsRefreshTokenRequest() } }.getOrNull()
                }
                val response = refreshed ?: suspendRunCatching {
                    authApi.login(credentials.email, credentials.password) { markAsRefreshTokenRequest() }
                }.getOrNull()

                if (response == null) {
                    tokens.clear()
                    null
                } else {
                    tokens.save(response.accessToken, response.refreshToken)
                    BearerTokens(response.accessToken, response.refreshToken)
                }
            }
            // Auth endpoints are public; everything else gets the token up front (no 401 round-trip).
            sendWithoutRequest { request -> !request.url.encodedPathSegments.joinToString("/").contains(AUTH_PATH) }
        }
    }
    defaultRequest {
        url(baseUrl.trimEnd('/') + "/")
        contentType(ContentType.Application.Json)
    }
}

private const val AUTH_PATH = "api/auth/"
