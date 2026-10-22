package com.cashi.app.presentation.app

import androidx.lifecycle.viewModelScope
import com.cashi.app.core.mvi.MviViewModel
import com.cashi.app.core.mvi.UiEffect
import com.cashi.app.core.mvi.UiIntent
import com.cashi.app.core.mvi.UiState
import com.cashi.app.domain.usecase.SignInUseCase
import kotlinx.coroutines.launch

/** Launch gate: signs in silently with the demo account before the payments UI is shown. */
data class AppState(
    val isSigningIn: Boolean = true,
    val isSignedIn: Boolean = false,
    val error: String? = null,
) : UiState

sealed interface AppIntent : UiIntent {
    data object Retry : AppIntent
}

sealed interface AppEffect : UiEffect

class AppViewModel(private val signIn: SignInUseCase) : MviViewModel<AppState, AppIntent, AppEffect>(AppState()) {

    init {
        signIn()
    }

    override fun handleIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.Retry -> signIn()
        }
    }

    private fun signIn() {
        if (currentState.isSignedIn) return
        setState { copy(isSigningIn = true, error = null) }
        viewModelScope.launch {
            signIn.invoke()
                .onSuccess { setState { copy(isSigningIn = false, isSignedIn = true) } }
                .onFailure { e -> setState { copy(isSigningIn = false, error = e.message ?: "Could not sign in") } }
        }
    }
}
