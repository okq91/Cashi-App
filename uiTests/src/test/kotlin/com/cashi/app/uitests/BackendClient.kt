package com.cashi.app.uitests

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/** Seeds data straight through the backend API so history tests don't depend on UI steps. */
class BackendClient(private val baseUrl: String = UiTestConfig.backendUrl) {
    private val http = HttpClient.newHttpClient()

    fun createPayment(amount: String, currency: String, recipientEmail: String, description: String) {
        val token = login()
        val body = """{"amount":$amount,"currency":"$currency","recipientEmail":"$recipientEmail","description":"$description"}"""
        val response = post("/api/payments", body, token)
        check(response.statusCode() == 201) { "Creating a payment failed: ${response.statusCode()} ${response.body()}" }
    }

    private fun login(): String {
        val response = post("/api/auth/login", """{"email":"omar@example.com","password":"Password123"}""")
        check(response.statusCode() == 200) { "Login failed: ${response.statusCode()} ${response.body()}" }
        return Regex("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").find(response.body())!!.groupValues[1]
    }

    private fun post(path: String, json: String, token: String? = null): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI(baseUrl.trimEnd('/') + path))
            .header("Content-Type", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build()
        return http.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
