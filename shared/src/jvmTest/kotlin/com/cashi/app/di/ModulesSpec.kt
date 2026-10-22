package com.cashi.app.di

import com.cashi.app.presentation.addpayment.AddPaymentViewModel
import com.cashi.app.presentation.app.AppViewModel
import com.cashi.app.presentation.payments.PaymentsViewModel
import com.cashi.app.testing.MainDispatcherListener
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import org.koin.dsl.koinApplication

class ModulesSpec : FunSpec({
    extension(MainDispatcherListener)

    test("the Koin graph provides every screen's view model") {
        val koin = koinApplication { modules(sharedModules()) }.koin
        koin.get<AppViewModel>().shouldNotBeNull()
        koin.get<PaymentsViewModel>().shouldNotBeNull()
        koin.get<AddPaymentViewModel>().shouldNotBeNull()
        koin.close()
    }
})
