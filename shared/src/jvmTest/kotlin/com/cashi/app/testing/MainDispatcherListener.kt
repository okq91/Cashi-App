package com.cashi.app.testing

import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** ViewModels launch on Dispatchers.Main; run them eagerly on the test thread instead. */
@OptIn(ExperimentalCoroutinesApi::class)
object MainDispatcherListener : BeforeSpecListener, AfterSpecListener {
    override suspend fun beforeSpec(spec: Spec) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override suspend fun afterSpec(spec: Spec) = Dispatchers.resetMain()
}
