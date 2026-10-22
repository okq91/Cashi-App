package com.cashi.app.data.remote

import com.cashi.app.core.network.apiCall
import com.cashi.app.data.remote.dto.AuthResponseDto
import com.cashi.app.data.remote.dto.LoginRequestDto
import com.cashi.app.data.remote.dto.RefreshRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json

class AuthApi(private val client: HttpClient, private val json: Json) {

    suspend fun login(email: String, password: String, configure: HttpRequestBuilder.() -> Unit = {}): AuthResponseDto =
        apiCall(json) {
            client.post(LOGIN) {
                configure()
                setBody(LoginRequestDto(email, password))
            }
        }

    suspend fun refresh(refreshToken: String, configure: HttpRequestBuilder.() -> Unit = {}): AuthResponseDto =
        apiCall(json) {
            client.post(REFRESH) {
                configure()
                setBody(RefreshRequestDto(refreshToken))
            }
        }

    /** Drops the bearer tokens cached by Ktor's Auth plugin so the next request reloads them from storage. */
    fun clearCachedTokens() {
        if (client.pluginOrNull(Auth) != null) client.authProvider<BearerAuthProvider>()?.clearToken()
    }

    companion object {
        const val LOGIN = "api/auth/login"
        const val REFRESH = "api/auth/refresh"
    }
}
