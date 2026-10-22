package com.cashi.app.data

import com.cashi.app.testing.TestEnv
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.time.Instant

class PaymentsRepositorySpec : FunSpec({

    test("refresh publishes the history newest first") {
        val env = TestEnv()
        env.signIn()
        env.backend.addPayment("a", "1", "JOD", "a@a.io", null, Instant.parse("2026-10-10T10:00:00Z"))
        env.backend.addPayment("b", "2", "USD", "b@b.io", "", Instant.parse("2026-10-12T10:00:00Z"))

        val history = env.paymentsRepository.refresh(50)

        history.map { it.id } shouldBe listOf("b", "a")
        history.first().description shouldBe null // blank descriptions are dropped
        env.paymentsRepository.payments.value shouldBe history
    }

    test("refresh honours the limit") {
        val env = TestEnv()
        env.signIn()
        repeat(60) { i ->
            env.backend.addPayment("p$i", "1", "JOD", "a@a.io", null, Instant.fromEpochSeconds(1_790_000_000L + i))
        }
        env.paymentsRepository.refresh(50) shouldHaveSize 50
    }
})
