package com.cashi.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.cashi.app.data.mapper.toDomain
import com.cashi.app.data.mapper.toRequest
import com.cashi.app.data.paging.PaymentsPagingSource
import com.cashi.app.data.remote.PaymentsApi
import com.cashi.app.domain.model.NewPayment
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.repository.PaymentsRepository
import kotlinx.coroutines.flow.Flow

class PaymentsRepositoryImpl(private val api: PaymentsApi) : PaymentsRepository {

    // The source Paging is currently reading from; the network gives no change signal, so we invalidate it ourselves.
    private var currentSource: PaymentsPagingSource? = null

    private val pager = Pager(
        PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false
        )
    ) {
        PaymentsPagingSource(api).also { currentSource = it }
    }

    override fun pagedPayments(): Flow<PagingData<Payment>> = pager.flow

    override suspend fun create(payment: NewPayment): Payment {
        val created = api.createPayment(payment.toRequest()).toDomain()
        // Reload from the top so the list matches the server (the new payment is first).
        currentSource?.invalidate()
        return created
    }

    private companion object {
        // The server decides how many items a page has; these only tune when Paging asks for the next one.
        const val PAGE_SIZE = 20
        const val PREFETCH_DISTANCE = 5
    }
}
