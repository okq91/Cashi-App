package com.cashi.app.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual val localDevBaseUrl: String = "http://10.0.2.2:8080"

actual fun createHttpEngine(): HttpClientEngine = OkHttp.create()
