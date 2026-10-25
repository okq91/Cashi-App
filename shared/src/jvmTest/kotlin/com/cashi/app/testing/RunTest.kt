package com.cashi.app.testing

import kotlinx.coroutines.runBlocking

/** JUnit test bodies must return Unit; this runs a suspending test body in real time. */
fun runTest(block: suspend () -> Unit) = runBlocking { block() }
