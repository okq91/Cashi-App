package com.cashi.app.data

import androidx.paging.testing.asSnapshot
import com.cashi.app.data.remote.PaymentsApi
import com.cashi.app.data.remote.dto.PaymentDto
import com.cashi.app.data.remote.dto.PaymentRequestDto
import com.cashi.app.data.remote.dto.PaymentsPageDto
import com.cashi.app.data.repository.PaymentsRepositoryImpl
import com.cashi.app.domain.error.ApiException
import com.cashi.app.domain.model.Amount
import com.cashi.app.domain.model.Currency
import com.cashi.app.domain.model.NewPayment
import com.cashi.app.testing.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Instant

class PaymentsRepositoryTest {
    private val api = mock<PaymentsApi>()
    private val repository = PaymentsRepositoryImpl(api)

    private fun dto(id: String, description: String? = null, createdAt: String = "2026-10-10T10:00:00Z") =
        PaymentDto(id, Amount(100), "JOD", "a@a.io", description, Instant.parse(createdAt))

    private fun page(range: IntRange, nextCursor: String?) =
        PaymentsPageDto(range.reversed().map { dto("p$it") }, nextCursor)

    @Test
    fun `the first page keeps the server order and drops blank descriptions`() = runTest {
        whenever(api.getPayments(isNull())).thenReturn(PaymentsPageDto(listOf(dto("b", ""), dto("a", "Rent")), nextCursor = null))

        val history = repository.pagedPayments().asSnapshot()

        assertEquals(listOf("b", "a"), history.map { it.id })
        assertNull(history.first().description)
        assertEquals("Rent", history.last().description)
    }

    @Test
    fun `only the first page is requested until the list is scrolled`() = runTest {
        whenever(api.getPayments(isNull())).thenReturn(page(25..44, nextCursor = "c20"))

        val history = repository.pagedPayments().asSnapshot()

        assertEquals(20, history.size)
        verify(api).getPayments(null)
        verifyNoMoreInteractions(api)
    }

    @Test
    fun `scrolling to the end loads every page with the server's cursors`() = runTest {
        whenever(api.getPayments(isNull())).thenReturn(page(25..44, nextCursor = "c20"))
        whenever(api.getPayments(eq("c20"))).thenReturn(page(5..24, nextCursor = "c40"))
        whenever(api.getPayments(eq("c40"))).thenReturn(page(0..4, nextCursor = null))

        val history = repository.pagedPayments().asSnapshot { appendScrollWhile { true } }

        assertEquals((44 downTo 0).map { "p$it" }, history.map { it.id })
        inOrder(api) {
            verify(api).getPayments(null)
            verify(api).getPayments("c20")
            verify(api).getPayments("c40")
        }
    }

    @Test
    fun `a failed load surfaces the API error`() = runTest {
        whenever(api.getPayments(isNull())).thenAnswer { throw ApiException.network() }

        val error = assertThrows<ApiException> { repository.pagedPayments().asSnapshot() }

        assertEquals(true, error.isNetworkError)
    }

    @Test
    fun `creating a payment sends the mapped request and returns the domain payment`() = runTest {
        whenever(api.createPayment(any())).thenReturn(dto("new", "Transfer"))

        val created = repository.create(NewPayment("new@example.com", Amount(3000), Currency.USD, "Transfer"))

        assertEquals("new", created.id)
        assertEquals("Transfer", created.description)
        verify(api).createPayment(PaymentRequestDto(Amount(3000), "USD", "new@example.com", "Transfer"))
    }

    @Test
    fun `a created payment reloads the history from the top`() = runTest {
        whenever(api.getPayments(isNull()))
            .thenReturn(PaymentsPageDto(listOf(dto("old")), null))
            .thenReturn(PaymentsPageDto(listOf(dto("new"), dto("old")), null))
        whenever(api.createPayment(any())).thenReturn(dto("new"))

        val history = repository.pagedPayments().asSnapshot {
            repository.create(NewPayment("new@example.com", Amount(3000), Currency.USD, "Transfer"))
        }

        assertEquals(listOf("new", "old"), history.map { it.id })
        verify(api, times(2)).getPayments(null)
    }

    @Test
    fun `a failed create leaves the history alone`() = runTest {
        whenever(api.getPayments(isNull())).thenReturn(PaymentsPageDto(listOf(dto("old")), null))
        whenever(api.createPayment(any())).thenAnswer { throw ApiException(400, "Validation failed") }

        val history = repository.pagedPayments().asSnapshot {
            assertThrows<ApiException> { repository.create(NewPayment("x@y.io", Amount(100), Currency.JOD, null)) }
        }

        assertEquals(listOf("old"), history.map { it.id })
        verify(api, times(1)).getPayments(null)
    }
}
