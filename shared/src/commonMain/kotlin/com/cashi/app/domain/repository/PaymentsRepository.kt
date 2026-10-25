package com.cashi.app.domain.repository

import androidx.paging.PagingData
import com.cashi.app.domain.model.NewPayment
import com.cashi.app.domain.model.Payment
import kotlinx.coroutines.flow.Flow

interface PaymentsRepository {
    /** The history, newest first, loaded page by page. Reloads from the top after [create]. */
    fun pagedPayments(): Flow<PagingData<Payment>>

    suspend fun create(payment: NewPayment): Payment
}
