package com.cashi.app.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

private var koinApplication: KoinApplication? = null

/** Starts Koin once per process (Android Application / iOS MainViewController). */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    koinApplication ?: startKoin {
        appDeclaration()
        modules(sharedModules())
    }.also { koinApplication = it }
