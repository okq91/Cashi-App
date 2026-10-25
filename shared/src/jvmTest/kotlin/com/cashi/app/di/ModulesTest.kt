package com.cashi.app.di

import com.cashi.app.presentation.addpayment.AddPaymentViewModel
import com.cashi.app.presentation.app.AppViewModel
import com.cashi.app.presentation.payments.PaymentsViewModel
import com.cashi.app.testing.MainDispatcherExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.dsl.koinApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull

@ExtendWith(MainDispatcherExtension::class)
class ModulesTest {

    @Test
    fun `the Koin graph provides every screen's view model`() {
        val koin = koinApplication { modules(sharedModules()) }.koin
        assertNotNull(koin.get<AppViewModel>())
        assertNotNull(koin.get<PaymentsViewModel>())
        assertNotNull(koin.get<AddPaymentViewModel>())
        koin.close()
    }
}
