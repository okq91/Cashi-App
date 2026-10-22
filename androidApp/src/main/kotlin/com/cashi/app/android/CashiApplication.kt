package com.cashi.app.android

import android.app.Application
import com.cashi.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class CashiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@CashiApplication) }
    }
}
