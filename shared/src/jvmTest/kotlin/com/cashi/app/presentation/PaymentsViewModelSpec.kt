package com.cashi.app.presentation

import com.cashi.app.domain.validation.PaymentDraft
import com.cashi.app.presentation.payments.MonthSummary
import com.cashi.app.presentation.payments.PaymentsEffect
import com.cashi.app.presentation.payments.PaymentsIntent
import com.cashi.app.presentation.payments.PaymentsState
import com.cashi.app.presentation.payments.PaymentsViewModel
import com.cashi.app.testing.MainDispatcherListener
import com.cashi.app.testing.TestEnv
import com.cashi.app.testing.awaitState
import com.cashi.app.testing.testFormatter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlin.time.Instant

class PaymentsViewModelSpec : FunSpec({
    extension(MainDispatcherListener)

    suspend fun signedInEnv() = TestEnv().also { it.signIn() }

    test("loads the history and the monthly summary") {
        val env = signedInEnv()
        env.backend.addPayment("p1", "42.75", "JOD", "billing@jepco.example", "Electricity bill", Instant.parse("2026-10-22T18:42:00Z"))
        env.backend.addPayment("p2", "450", "JOD", "landlord@example.com", "Rent", Instant.parse("2026-10-20T10:15:00Z"))
        env.backend.addPayment("p3", "120", "USD", "academy@example.com", null, Instant.parse("2026-10-18T21:03:00Z"))
        env.backend.addPayment("old", "999", "JOD", "old@example.com", "September", Instant.parse("2026-09-30T10:00:00Z"))

        val state = PaymentsViewModel(env.getHistory, testFormatter()).state.awaitState { !it.isLoading }

        state.isLoading shouldBe false
        state.error.shouldBeNull()
        state.greeting shouldBe "Good evening"
        state.payments.map { it.id } shouldBe listOf("p1", "p2", "p3", "old")
        with(state.payments.first()) {
            title shouldBe "Electricity bill"
            date shouldBe "Today, 6:42 PM"
            amount shouldBe "42.75"
            currency shouldBe "JOD"
            status shouldBe "Sent"
        }
        state.payments[2].title shouldBe "Payment"
        state.summary shouldBe MonthSummary(month = "October", total = "492.75", totalCurrency = "JOD", count = 3, usdTotal = "$120.00")
    }

    test("shows the empty state when there are no payments") {
        val state = PaymentsViewModel(signedInEnv().getHistory, testFormatter()).state.awaitState { !it.isLoading }
        state.isEmpty shouldBe true
        state.summary?.count shouldBe 0
    }

    test("shows an error and recovers on retry") {
        val env = signedInEnv()
        env.backend.unreachable = true
        val viewModel = PaymentsViewModel(env.getHistory, testFormatter())
        viewModel.state.awaitState { !it.isLoading }.error.shouldNotBeNull()

        env.backend.unreachable = false
        env.backend.addPayment("p1", "1", "JOD", "a@b.io", null, Instant.parse("2026-10-22T10:00:00Z"))
        viewModel.onIntent(PaymentsIntent.Retry)

        val state = viewModel.state.awaitState { it.payments.isNotEmpty() }
        state.error.shouldBeNull()
        state.payments shouldHaveSize 1
    }

    test("a failed refresh keeps the list and reports a message") {
        val env = signedInEnv()
        env.backend.addPayment("p1", "1", "JOD", "a@b.io", null, Instant.parse("2026-10-22T10:00:00Z"))
        val viewModel = PaymentsViewModel(env.getHistory, testFormatter())
        viewModel.state.awaitState { it.payments.isNotEmpty() }
        env.backend.unreachable = true

        viewModel.onIntent(PaymentsIntent.Refresh)

        (viewModel.effects.first() as PaymentsEffect.ShowMessage).message shouldBe
            "Unable to reach the server. Check your connection and try again."
        viewModel.state.value.payments shouldHaveSize 1
        viewModel.state.value.isRefreshing shouldBe false
    }

    test("new payments appear at the top without reloading") {
        val env = signedInEnv()
        val viewModel = PaymentsViewModel(env.getHistory, testFormatter())
        viewModel.state.awaitState { !it.isLoading }

        env.createPayment(PaymentDraft("ahmad@gmail.com", "30", "USD", "Money transfer"))

        val state = viewModel.state.awaitState { it.payments.isNotEmpty() }
        state.payments.first().recipientEmail shouldBe "ahmad@gmail.com"
        state.summary?.usdTotal shouldBe "$30.00"
    }

    test("long histories are collapsed until See all is tapped") {
        val env = signedInEnv()
        repeat(12) { i -> env.backend.addPayment("p$i", "1", "JOD", "a@b.io", null, Instant.parse("2026-10-0${i % 9 + 1}T10:00:00Z")) }
        val viewModel = PaymentsViewModel(env.getHistory, testFormatter())
        viewModel.state.awaitState { !it.isLoading }

        viewModel.state.value.visiblePayments shouldHaveSize PaymentsState.COLLAPSED_COUNT
        viewModel.state.value.canExpand shouldBe true
        viewModel.onIntent(PaymentsIntent.ToggleShowAll)
        viewModel.state.value.visiblePayments shouldHaveSize 12
    }

    test("tapping Add new payment opens the form") {
        val viewModel = PaymentsViewModel(signedInEnv().getHistory, testFormatter())
        viewModel.onIntent(PaymentsIntent.AddPaymentClicked)
        viewModel.effects.first() shouldBe PaymentsEffect.OpenAddPayment
    }
})
