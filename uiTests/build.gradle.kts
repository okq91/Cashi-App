plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.appium.java.client)
    testImplementation(libs.selenium.support)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Appium end-to-end tests for the Android app. They need a running Appium server with the
// UiAutomator2 driver, a booted emulator, the debug APK and the backend; see README.md.
// Every test is skipped (not failed) when the Appium server is not reachable.
tasks.test {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
    val apk = rootProject.layout.projectDirectory.file("androidApp/build/outputs/apk/debug/androidApp-debug.apk").asFile
    systemProperty("appium.url", providers.gradleProperty("appiumUrl").getOrElse("http://127.0.0.1:4723"))
    systemProperty("app.apk", providers.gradleProperty("apk").getOrElse(apk.absolutePath))
    systemProperty("backend.url", providers.gradleProperty("backendUrl").getOrElse("http://localhost:8080"))
    providers.gradleProperty("udid").orNull?.let { systemProperty("device.udid", it) }
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
