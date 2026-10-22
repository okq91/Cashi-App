package com.cashi.app.di

import com.cashi.app.core.network.createApiClient
import com.cashi.app.core.network.createHttpEngine
import com.cashi.app.core.storage.TokenStorage
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/** Settings implementation (SharedPreferences vs NSUserDefaults). */
expect fun platformModule(): Module

val coreModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
    single { TokenStorage(get()) }
    single { createApiClient(createHttpEngine(), get(), get()) }
}

fun sharedModules(): List<Module> = listOf(platformModule(), coreModule)
