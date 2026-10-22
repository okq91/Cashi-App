package com.cashi.app.data.repository

import com.cashi.app.data.mapper.toDomain
import com.cashi.app.data.mapper.toRequest
import com.cashi.app.data.remote.PaymentsApi
import com.cashi.app.domain.model.NewPayment
import com.cashi.app.domain.model.Payment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface PaymentsRepository {
    /** Latest known history, newest first. Updated by [refresh] and by every [create]. */
    val payments: StateFlow<List<Payment>>

    suspend fun refresh(limit: Int): List<Payment>

    suspend fun create(payment: NewPayment): Payment
}

class PaymentsRepositoryImpl(private val api: PaymentsApi) : PaymentsRepository {

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    override val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

    override suspend fun refresh(limit: Int): List<Payment> {
        val history = api.getPayments(limit).map { it.toDomain() }.newestFirst()
        _payments.value = history
        return history
    }

    override suspend fun create(payment: NewPayment): Payment {
        val created = api.createPayment(payment.toRequest()).toDomain()
        _payments.update { current -> (listOf(created) + current.filterNot { it.id == created.id }).newestFirst() }
        return created
    }

    private fun List<Payment>.newestFirst() = sortedByDescending { it.createdAt }
}
