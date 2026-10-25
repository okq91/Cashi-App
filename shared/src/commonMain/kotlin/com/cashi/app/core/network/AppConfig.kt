package com.cashi.app.core.network

import com.cashi.app.domain.model.Credentials

object AppConfig {
    val baseUrl: String get() = localDevBaseUrl

    // The demo signs in silently on launch with this static account.
    //no login screen added for the demo
    const val LOGIN_EMAIL = "omar@example.com"
    const val LOGIN_PASSWORD = "Password123"
    val demoCredentials: Credentials get() = Credentials(LOGIN_EMAIL, LOGIN_PASSWORD)
}

/** Android emulators reach the host machine via 10.0.2.2; iOS simulators via localhost. */
expect val localDevBaseUrl: String
