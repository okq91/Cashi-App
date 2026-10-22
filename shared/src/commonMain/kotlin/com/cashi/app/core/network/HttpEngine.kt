package com.cashi.app.core.network

import io.ktor.client.engine.HttpClientEngine

/** OkHttp on Android/JVM, Darwin (NSURLSession) on iOS. */
expect fun createHttpEngine(): HttpClientEngine
