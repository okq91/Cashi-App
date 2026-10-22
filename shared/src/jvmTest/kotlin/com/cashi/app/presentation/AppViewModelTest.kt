package com.cashi.app.presentation

import com.cashi.app.presentation.app.AppIntent
import com.cashi.app.presentation.app.AppViewModel
import com.cashi.app.testing.MainDispatcherExtension
import com.cashi.app.testing.TestEnv
import com.cashi.app.testing.awaitState
import com.cashi.app.testing.runTest
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MainDispatcherExtension::class)
class AppViewModelTest {

    @Test
    fun `signs in silently on launch`() = runTest {
        val env = TestEnv()
        val state = AppViewModel(env.signIn).state.awaitState { !it.isSigningIn }
        assertTrue(state.isSignedIn)
        assertEquals("access-1", env.tokens.accessToken)
    }

    @Test
    fun `shows the error and signs in on retry`() = runTest {
        val env = TestEnv()
        env.backend.unreachable = true
        val viewModel = AppViewModel(env.signIn)
        assertFalse(viewModel.state.awaitState { !it.isSigningIn }.isSignedIn)
        assertEquals("Unable to reach the server. Check your connection and try again.", viewModel.state.value.error)

        env.backend.unreachable = false
        viewModel.onIntent(AppIntent.Retry)

        assertTrue(viewModel.state.awaitState { it.isSignedIn }.isSignedIn)
        assertNull(viewModel.state.value.error)
    }
}
