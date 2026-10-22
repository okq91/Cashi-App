package com.cashi.app.uitests

import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import java.net.URI
import java.time.Duration

/**
 * Starts a fresh UiAutomator2 session per test: the debug APK is (re)installed with its data
 * cleared, so every test begins at launch with the silent sign-in.
 */
abstract class AppiumTestBase {
    protected lateinit var driver: AndroidDriver

    @BeforeEach
    fun startSession() {
        assumeTrue(UiTestConfig.isAppiumRunning(), "Appium server not reachable at ${UiTestConfig.appiumUrl}; skipping UI tests")
        check(UiTestConfig.apk.exists()) {
            "APK not found at ${UiTestConfig.apk}. Build it first: ./gradlew :androidApp:assembleDebug"
        }
        val options = UiAutomator2Options()
            .setApp(UiTestConfig.apk.absolutePath)
            .setAppPackage(UiTestConfig.APP_PACKAGE)
            .setAppActivity(UiTestConfig.APP_ACTIVITY)
            .setNoReset(false)
            .setAutoGrantPermissions(true)
            .setNewCommandTimeout(Duration.ofSeconds(120))
            .apply { UiTestConfig.deviceUdid?.let { setUdid(it) } }
        driver = AndroidDriver(URI(UiTestConfig.appiumUrl).toURL(), options)
    }

    @AfterEach
    fun stopSession() {
        if (::driver.isInitialized) driver.quit()
    }

    /** Unique per run so assertions never match payments left over from earlier runs. */
    protected fun uniqueEmail(prefix: String) = "$prefix.${System.currentTimeMillis()}@example.com"
}
