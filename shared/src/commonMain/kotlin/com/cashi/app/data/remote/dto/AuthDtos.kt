package com.cashi.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class UserDto(val id: String, val email: String, val fullName: String? = null)

/** Response of `/api/auth/login` and `/api/auth/refresh`. */
@Serializable
data class AuthResponseDto(
    val user: UserDto? = null,
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 0,
    val refreshToken: String,
)
