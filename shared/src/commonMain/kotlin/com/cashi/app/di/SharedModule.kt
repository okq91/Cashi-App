package com.cashi.app.di

import com.cashi.app.core.network.createApiClient
import com.cashi.app.core.network.createHttpEngine
import com.cashi.app.core.storage.TokenStorage
import com.cashi.app.data.remote.AuthApi
import com.cashi.app.data.remote.PaymentsApi
import com.cashi.app.data.repository.AuthRepository
import com.cashi.app.data.repository.PaymentsRepository
import com.cashi.app.data.repository.PaymentsRepositoryImpl
import com.cashi.app.domain.usecase.CreatePaymentUseCase
import com.cashi.app.domain.usecase.GetPaymentHistoryUseCase
import com.cashi.app.domain.usecase.SignInUseCase
import com.cashi.app.domain.validation.PaymentValidator
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
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

val dataModule = module {
    singleOf(::AuthApi)
    singleOf(::PaymentsApi)
    singleOf(::AuthRepository)
    singleOf(::PaymentsRepositoryImpl) bind PaymentsRepository::class
}

val domainModule = module {
    factoryOf(::PaymentValidator)
    factory { SignInUseCase(get()) }
    factoryOf(::GetPaymentHistoryUseCase)
    factoryOf(::CreatePaymentUseCase)
}

fun sharedModules(): List<Module> = listOf(platformModule(), coreModule, dataModule, domainModule)
