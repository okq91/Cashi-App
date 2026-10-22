package com.cashi.app.data.remote

import com.cashi.app.core.network.apiCall
import com.cashi.app.data.remote.dto.PaymentDto
import com.cashi.app.data.remote.dto.PaymentRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json

class PaymentsApi(private val client: HttpClient, private val json: Json) {

    suspend fun createPayment(request: PaymentRequestDto): PaymentDto =
        apiCall(json) { client.post(PAYMENTS) { setBody(request) } }

    suspend fun getPayments(limit: Int): List<PaymentDto> =
        apiCall(json) { client.get(PAYMENTS) { parameter("limit", limit) } }

    companion object {
        const val PAYMENTS = "api/payments"
    }
}
