package com.cashi.app.core.network

object AppConfig {
    val baseUrl: String get() = localDevBaseUrl

    // The demo signs in silently on launch with this static account.
    const val LOGIN_EMAIL = "omar@example.com"
    const val LOGIN_PASSWORD = "Password123"

    const val HISTORY_LIMIT = 50
}

/** Android emulators reach the host machine via 10.0.2.2; iOS simulators via localhost. */
expect val localDevBaseUrl: String
