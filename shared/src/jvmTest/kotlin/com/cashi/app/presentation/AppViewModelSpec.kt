package com.cashi.app.presentation

import com.cashi.app.domain.usecase.SignInUseCase
import com.cashi.app.presentation.app.AppIntent
import com.cashi.app.presentation.app.AppViewModel
import com.cashi.app.testing.FakeBackend
import com.cashi.app.testing.MainDispatcherListener
import com.cashi.app.testing.TestEnv
import com.cashi.app.testing.awaitState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class AppViewModelSpec : FunSpec({
    extension(MainDispatcherListener)

    test("signs in silently on launch") {
        val env = TestEnv()
        val state = AppViewModel(env.signIn).state.awaitState { !it.isSigningIn }
        state.isSignedIn shouldBe true
        state.isSigningIn shouldBe false
        env.tokens.accessToken shouldBe "access-1"
    }

    test("shows the error and signs in on retry") {
        val env = TestEnv(FakeBackend())
        env.backend.unreachable = true
        val viewModel = AppViewModel(SignInUseCase(env.authRepository))
        viewModel.state.awaitState { !it.isSigningIn }.isSignedIn shouldBe false
        viewModel.state.value.error shouldBe "Unable to reach the server. Check your connection and try again."

        env.backend.unreachable = false
        viewModel.onIntent(AppIntent.Retry)

        viewModel.state.awaitState { it.isSignedIn }.isSignedIn shouldBe true
        viewModel.state.value.error.shouldBeNull()
    }
})
