package com.cashi.app.domain.usecase

import com.cashi.app.core.suspendRunCatching
import com.cashi.app.data.repository.AuthRepository
import com.cashi.app.domain.model.Credentials
import com.cashi.app.domain.model.User

/** Silent sign-in on launch with the demo account. */
class SignInUseCase(
    private val repository: AuthRepository,
    private val credentials: Credentials = Credentials.Default,
) {
    suspend operator fun invoke(): Result<User?> = suspendRunCatching { repository.login(credentials) }
}
