package com.cashi.app.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cashi.app.presentation.app.AppState
import com.cashi.app.ui.components.PrimaryButton
import com.cashi.app.ui.theme.CashiColors
import com.cashi.app.ui.theme.CashiText
import com.cashi.app.ui.theme.CashiTheme
import com.cashi.app.ui.util.TestTags
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Shown while the app signs in silently on launch, or when that sign-in failed. */
@Composable
fun SplashScreen(state: AppState, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                Modifier.size(72.dp).background(CashiColors.Accent, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("C", style = CashiText.ScreenTitle.copy(color = CashiColors.Yellow, fontSize = 36.sp))
            }
            Text("Cashi", style = CashiText.ScreenTitle)
            val error = state.error
            if (error == null) {
                CircularProgressIndicator(color = CashiColors.Accent, modifier = Modifier.testTag(TestTags.SPLASH_LOADING))
                Text("Signing you in…", style = CashiText.Body)
            } else {
                Text(
                    "We couldn't sign you in.\n$error",
                    style = CashiText.Body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(TestTags.SPLASH_ERROR),
                )
                PrimaryButton("Try again", onClick = onRetry, modifier = Modifier.testTag(TestTags.SPLASH_RETRY))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SplashLoadingPreview() = CashiTheme { SplashScreen(AppState(), onRetry = {}) }

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SplashErrorPreview() = CashiTheme { SplashScreen(AppState(isSigningIn = false, error = "No internet connection"), onRetry = {}) }
