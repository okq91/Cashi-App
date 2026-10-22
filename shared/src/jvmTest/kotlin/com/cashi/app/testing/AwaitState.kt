package com.cashi.app.testing

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** Requests run on the MockEngine's own dispatcher, so wait (in real time) for the state the test expects. */
suspend fun <S> StateFlow<S>.awaitState(predicate: (S) -> Boolean): S = withTimeout(5_000) { first(predicate) }

/** JUnit test bodies must return Unit; this runs a suspending test body in real time. */
fun runTest(block: suspend () -> Unit) = runBlocking { block() }
