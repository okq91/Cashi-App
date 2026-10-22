package com.cashi.app.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual val localDevBaseUrl: String = "http://localhost:8080"

actual fun createHttpEngine(): HttpClientEngine = Darwin.create()
