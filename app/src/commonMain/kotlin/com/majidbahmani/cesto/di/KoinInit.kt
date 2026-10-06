package com.majidbahmani.cesto.di

import com.majidbahmani.cesto.core.di.networkModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/** Every Koin module of the app. Only :app knows all of them; features add theirs here. */
internal val appModules = listOf(networkModule)

/** Starts Koin once per process: from the Android Application and the iOS App init. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(appModules)
}
