package com.cashi.app.data

import com.cashi.app.testing.TestEnv
import com.cashi.app.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class PaymentsRepositoryTest {

    @Test
    fun `refresh publishes the history newest first`() = runTest {
        val env = TestEnv()
        env.signIn()
        env.backend.addPayment("a", "1", "JOD", "a@a.io", null, Instant.parse("2026-10-10T10:00:00Z"))
        env.backend.addPayment("b", "2", "USD", "b@b.io", "", Instant.parse("2026-10-12T10:00:00Z"))

        val history = env.paymentsRepository.refresh(50)

        assertEquals(listOf("b", "a"), history.map { it.id })
        assertNull(history.first().description) // blank descriptions are dropped
        assertEquals(history, env.paymentsRepository.payments.value)
    }

    @Test
    fun `refresh honours the limit`() = runTest {
        val env = TestEnv()
        env.signIn()
        repeat(60) { i ->
            env.backend.addPayment("p$i", "1", "JOD", "a@a.io", null, Instant.fromEpochSeconds(1_790_000_000L + i))
        }
        assertEquals(50, env.paymentsRepository.refresh(50).size)
    }
}
