package com.cashi.app.domain.model

import com.cashi.app.core.network.AppConfig

data class Credentials(val email: String, val password: String) {
    companion object {
        val Default = Credentials(AppConfig.LOGIN_EMAIL, AppConfig.LOGIN_PASSWORD)
    }
}
