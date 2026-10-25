package com.cashi.app.domain

import com.cashi.app.domain.error.ApiException
import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.model.NewPayment
import com.cashi.app.domain.model.Payment
import com.cashi.app.domain.repository.PaymentsRepository
import com.cashi.app.domain.usecase.CreatePaymentResult
import com.cashi.app.domain.usecase.CreatePaymentUseCase
import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.domain.validation.PaymentField
import com.cashi.app.domain.validation.PaymentValidator
import com.cashi.app.testing.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Instant

class CreatePaymentUseCaseTest {
    private val repository = mock<PaymentsRepository>()
    private val useCase = CreatePaymentUseCase(PaymentValidator(), repository)

    private val created = Payment("p1", Amount(3000), "USD", "ahmad@gmail.com", "Money transfer", Instant.parse("2026-10-22T20:00:00Z"))

    @Test
    fun `a valid draft is normalized and sent to the repository`() = runTest {
        whenever(repository.create(any())).thenReturn(created)

        val result = useCase(PaymentDraft(" ahmad@gmail.com ", "30", "usd", "Money transfer"))

        assertEquals(created, assertInstanceOf<CreatePaymentResult.Success>(result).payment)
        verify(repository).create(NewPayment("ahmad@gmail.com", Amount(3000), Currency.USD, "Money transfer"))
    }

    @Test
    fun `an invalid draft never reaches the repository`() = runTest {
        val result = useCase(PaymentDraft("ahmad", "0", "GBP"))

        assertEquals(
            mapOf(
                PaymentField.RECIPIENT_EMAIL to "Enter a valid email address",
                PaymentField.AMOUNT to "Amount must be greater than zero",
                PaymentField.CURRENCY to "Choose a supported currency",
            ),
            assertInstanceOf<CreatePaymentResult.Invalid>(result).errors,
        )
        verifyNoInteractions(repository)
    }

    @Test
    fun `backend field errors are mapped onto form fields`() = runTest {
        val error = ApiException(400, "Validation failed", mapOf("recipientEmail" to "Recipient cannot receive payments"))
        whenever(repository.create(any())).thenAnswer { throw error }

        val result = useCase(PaymentDraft("blocked@example.com", "5", "JOD"))

        val invalid = assertInstanceOf<CreatePaymentResult.Invalid>(result)
        assertEquals(mapOf(PaymentField.RECIPIENT_EMAIL to "Recipient cannot receive payments"), invalid.errors)
        assertEquals("Validation failed", invalid.message)
    }

    @Test
    fun `an API error without known fields is a failure`() = runTest {
        whenever(repository.create(any())).thenAnswer { throw ApiException(500, "boom", mapOf("unknown" to "x")) }

        val failure = assertInstanceOf<CreatePaymentResult.Failure>(useCase(PaymentDraft("ahmad@gmail.com", "30", "USD")))

        assertEquals("boom", failure.message)
        assertEquals(false, failure.isNetworkError)
    }

    @Test
    fun `connection problems are reported as network failures`() = runTest {
        whenever(repository.create(any())).thenAnswer { throw ApiException.network() }

        val failure = assertInstanceOf<CreatePaymentResult.Failure>(useCase(PaymentDraft("ahmad@gmail.com", "30", "USD")))

        assertTrue(failure.isNetworkError)
    }

    @Test
    fun `unexpected errors are reported with their message`() = runTest {
        whenever(repository.create(any())).thenAnswer { throw IllegalStateException("oops") }

        val failure = assertInstanceOf<CreatePaymentResult.Failure>(useCase(PaymentDraft("ahmad@gmail.com", "30", "USD")))

        assertEquals("oops", failure.message)
        assertEquals(false, failure.isNetworkError)
    }
}
