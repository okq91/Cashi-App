package com.cashi.app.data.repository

import com.cashi.app.core.storage.TokenStorage
import com.cashi.app.data.mapper.toDomain
import com.cashi.app.data.remote.AuthApi
import com.cashi.app.domain.model.Credentials
import com.cashi.app.domain.model.User
import com.cashi.app.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokens: TokenStorage,
) : AuthRepository {
    override val hasSession: Boolean get() = !tokens.accessToken.isNullOrEmpty()

    override suspend fun login(credentials: Credentials): User? {
        val response = api.login(credentials.email, credentials.password)
        tokens.save(response.accessToken, response.refreshToken)
        api.clearCachedTokens()
        return response.user?.toDomain()
    }

    override fun logout() {
        tokens.clear()
        api.clearCachedTokens()
    }
}
