package com.cashi.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import com.cashi.app.ui.App
import com.cashi.app.ui.util.testTagsAsResourceIds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Exposes Compose test tags as Android resource ids so Appium/UiAutomator2 can find them.
        setContent { App(Modifier.testTagsAsResourceIds()) }
    }
}
