package com.cashi.app.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Immutable snapshot rendered by a screen. */
interface UiState

/** Something the user (or the screen) asks the ViewModel to do. */
interface UiIntent

/** One-shot side effect (navigation, dialogs, snackbars) consumed exactly once by the UI. */
interface UiEffect

/**
 * Base class for every screen's store in the MVI loop:
 *
 *   UI --intent--> [onIntent] --reduce--> [state] --render--> UI
 *                        \--------------> [effects] --> UI (one-shot)
 *
 * State is exposed as a hot [StateFlow]; effects go through a buffered [Channel] so
 * each is delivered once even if the UI is briefly not collecting (e.g. rotation).
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    fun onIntent(intent: I) = handleIntent(intent)

    protected abstract fun handleIntent(intent: I)

    protected fun setState(reducer: S.() -> S) = _state.update(reducer)

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
