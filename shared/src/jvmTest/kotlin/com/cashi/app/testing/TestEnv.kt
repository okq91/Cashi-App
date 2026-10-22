package com.cashi.app.testing

import com.cashi.app.core.network.createApiClient
import com.cashi.app.core.storage.TokenStorage
import com.cashi.app.data.remote.AuthApi
import com.cashi.app.data.remote.PaymentsApi
import com.cashi.app.data.repository.AuthRepository
import com.cashi.app.data.repository.PaymentsRepositoryImpl
import com.cashi.app.domain.usecase.CreatePaymentUseCase
import com.cashi.app.domain.usecase.GetPaymentHistoryUseCase
import com.cashi.app.domain.usecase.SignInUseCase
import com.cashi.app.domain.validation.PaymentValidator
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.json.Json

/** The real shared stack (client, APIs, repositories, use cases) wired to a [FakeBackend]. */
class TestEnv(val backend: FakeBackend = FakeBackend()) {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }
    val tokens = TokenStorage(MapSettings())
    val client = createApiClient(backend.engine, json, tokens, baseUrl = "http://localhost:8080")
    val authApi = AuthApi(client, json)
    val paymentsApi = PaymentsApi(client, json)
    val authRepository = AuthRepository(authApi, tokens)
    val paymentsRepository = PaymentsRepositoryImpl(paymentsApi)
    val signIn = SignInUseCase(authRepository)
    val getHistory = GetPaymentHistoryUseCase(paymentsRepository)
    val createPayment = CreatePaymentUseCase(PaymentValidator(), paymentsRepository)
}
