package com.cashi.app.uitests

import java.io.File
import java.net.HttpURLConnection
import java.net.URI

/** Settings passed in by `uiTests/build.gradle.kts` (override with -PappiumUrl, -Papk, -PbackendUrl, -Pudid). */
object UiTestConfig {
    val appiumUrl: String = System.getProperty("appium.url", "http://127.0.0.1:4723")
    val apk: File = File(System.getProperty("app.apk", "androidApp/build/outputs/apk/debug/androidApp-debug.apk"))
    val backendUrl: String = System.getProperty("backend.url", "http://localhost:8080")
    val deviceUdid: String? = System.getProperty("device.udid")

    const val APP_PACKAGE = "com.cashi.app"
    const val APP_ACTIVITY = "com.cashi.app.android.MainActivity"

    fun isAppiumRunning(): Boolean = runCatching {
        val connection = URI("${appiumUrl.trimEnd('/')}/status").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 2_000
        connection.readTimeout = 2_000
        connection.responseCode == 200
    }.getOrDefault(false)
}
