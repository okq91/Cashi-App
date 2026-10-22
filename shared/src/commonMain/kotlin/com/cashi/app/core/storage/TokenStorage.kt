package com.cashi.app.core.storage

import com.russhwolf.settings.Settings

/** Access/refresh tokens, backed by platform settings (SharedPreferences / NSUserDefaults). */
class TokenStorage(private val settings: Settings) {
    var accessToken: String?
        get() = settings.getStringOrNull(KEY_ACCESS)
        set(value) = if (value == null) settings.remove(KEY_ACCESS) else settings.putString(KEY_ACCESS, value)

    var refreshToken: String?
        get() = settings.getStringOrNull(KEY_REFRESH)
        set(value) = if (value == null) settings.remove(KEY_REFRESH) else settings.putString(KEY_REFRESH, value)

    fun save(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    fun clear() {
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
