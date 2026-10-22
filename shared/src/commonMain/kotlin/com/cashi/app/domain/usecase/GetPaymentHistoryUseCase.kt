package com.cashi.app.domain.usecase

import com.cashi.app.core.network.AppConfig
import com.cashi.app.core.suspendRunCatching
import com.cashi.app.data.repository.PaymentsRepository
import com.cashi.app.domain.model.Payment
import kotlinx.coroutines.flow.StateFlow

class GetPaymentHistoryUseCase(private val repository: PaymentsRepository) {

    /** Observes the cached history; new payments show up here as soon as they are created. */
    fun observe(): StateFlow<List<Payment>> = repository.payments

    /** Reloads the history from the backend. */
    suspend fun refresh(limit: Int = AppConfig.HISTORY_LIMIT): Result<List<Payment>> =
        suspendRunCatching { repository.refresh(limit) }
}
