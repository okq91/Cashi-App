package com.cashi.app.domain.repository

import com.cashi.app.domain.model.Credentials
import com.cashi.app.domain.model.User

interface AuthRepository {
    val hasSession: Boolean

    /** Signs in and keeps the session for later authenticated calls. */
    suspend fun login(credentials: Credentials): User?

    fun logout()
}
