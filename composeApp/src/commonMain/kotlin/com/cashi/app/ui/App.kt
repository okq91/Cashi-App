package com.cashi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.cashi.app.presentation.app.AppIntent
import com.cashi.app.presentation.app.AppViewModel
import com.cashi.app.ui.navigation.AppNavHost
import com.cashi.app.ui.screens.splash.SplashScreen
import com.cashi.app.ui.theme.CashiColors
import com.cashi.app.ui.theme.CashiTheme
import com.cashi.app.ui.util.TestTags
import org.koin.compose.viewmodel.koinViewModel

/** App-wide snackbar, so a message can outlive the screen that sent it (e.g. "Payment sent"). */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("SnackbarHostState not provided") }

@Composable
fun App(modifier: Modifier = Modifier) {
    CashiTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val appViewModel = koinViewModel<AppViewModel>()
        val state by appViewModel.state.collectAsStateWithLifecycle()

        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            Box(modifier.fillMaxSize().background(CashiColors.Background)) {
                if (state.isSignedIn) {
                    AppNavHost(rememberNavController())
                } else {
                    SplashScreen(state, onRetry = { appViewModel.onIntent(AppIntent.Retry) })
                }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
                ) { data ->
                    Snackbar(containerColor = CashiColors.TextPrimary, contentColor = CashiColors.Surface) {
                        Text(data.visuals.message, modifier = Modifier.testTag(TestTags.SNACKBAR))
                    }
                }
            }
        }
    }
}
