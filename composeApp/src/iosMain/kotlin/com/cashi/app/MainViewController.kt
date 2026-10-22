package com.cashi.app

import androidx.compose.ui.window.ComposeUIViewController
import com.cashi.app.di.initKoin
import com.cashi.app.ui.App
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused") // called from Swift
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
