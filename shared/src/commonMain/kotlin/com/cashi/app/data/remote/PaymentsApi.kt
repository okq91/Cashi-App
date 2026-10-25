package com.cashi.app.data.remote

import com.cashi.app.core.network.apiCall
import com.cashi.app.data.remote.dto.PaymentDto
import com.cashi.app.data.remote.dto.PaymentRequestDto
import com.cashi.app.data.remote.dto.PaymentsPageDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json

class PaymentsApi(private val client: HttpClient, private val json: Json) {

    suspend fun createPayment(request: PaymentRequestDto): PaymentDto =
        apiCall(json) { client.post(PAYMENTS) { setBody(request) } }

    /** One page of the history, newest first. Pass the previous page's `nextCursor`, or null for the first page. */
    suspend fun getPayments(cursor: String? = null): PaymentsPageDto =
        apiCall(json) { client.get(PAYMENTS) { cursor?.let { parameter("cursor", it) } } }

    companion object {
        const val PAYMENTS = "api/payments"
    }
}
