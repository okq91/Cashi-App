package com.cashi.app.testing

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** Requests run on the MockEngine's own dispatcher, so wait for the state the test expects. */
suspend fun <S> StateFlow<S>.awaitState(predicate: (S) -> Boolean): S = withTimeout(5_000) { first(predicate) }
