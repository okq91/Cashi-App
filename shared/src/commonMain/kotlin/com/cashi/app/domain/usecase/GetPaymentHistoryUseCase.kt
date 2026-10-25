package com.cashi.app.domain.usecase

import androidx.paging.PagingData
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.repository.PaymentsRepository
import kotlinx.coroutines.flow.Flow

class GetPaymentHistoryUseCase(private val repository: PaymentsRepository) {

    /** The history, newest first, one page at a time. Paging handles refresh, retry and loading more. */
    fun paged(): Flow<PagingData<Payment>> = repository.pagedPayments()
}
