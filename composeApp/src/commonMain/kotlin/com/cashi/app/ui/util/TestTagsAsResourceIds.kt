package com.cashi.app.ui.util

import androidx.compose.ui.Modifier

/**
 * Exposes Compose test tags as Android resource ids for UiAutomator/Appium. Popups (dropdown menus)
 * are separate composition roots, so they need it as well as the activity's root. No-op on iOS.
 */
expect fun Modifier.testTagsAsResourceIds(): Modifier
