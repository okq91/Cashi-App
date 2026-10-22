package com.cashi.app.presentation

import com.cashi.app.presentation.addpayment.AddPaymentViewModel
import com.cashi.app.presentation.app.AppViewModel
import com.cashi.app.presentation.format.PaymentFormatter
import com.cashi.app.presentation.payments.PaymentsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    factory { PaymentFormatter() }
    viewModelOf(::AppViewModel)
    viewModelOf(::PaymentsViewModel)
    viewModelOf(::AddPaymentViewModel)
}
