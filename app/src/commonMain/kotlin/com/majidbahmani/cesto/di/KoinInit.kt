package com.majidbahmani.cesto.di

import com.majidbahmani.cesto.core.di.networkModule
import com.majidbahmani.cesto.feature.onboarding.di.onboardingModule
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Every Koin module of the app. Only :app knows all of them; features add theirs here. */
internal val appModules = listOf(networkModule, onboardingModule)

/** Bindings created by the platform apps before Koin starts (they need the platform SDKs). */
internal fun platformServicesModule(gmailAuthorizer: GmailAuthorizer): Module = module {
    single<GmailAuthorizer> { gmailAuthorizer }
}

/** Starts Koin once per process: from the Android Application and the iOS App init. */
fun initKoin(
    gmailAuthorizer: GmailAuthorizer,
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = startKoin {
    appDeclaration()
    modules(listOf(platformServicesModule(gmailAuthorizer)) + appModules)
}
