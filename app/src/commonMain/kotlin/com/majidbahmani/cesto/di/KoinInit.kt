package com.majidbahmani.cesto.di

import com.majidbahmani.cesto.account.di.IdentityPlatformConfig
import com.majidbahmani.cesto.account.di.accountModule
import com.majidbahmani.cesto.core.di.dispatchersModule
import com.majidbahmani.cesto.core.di.networkModule
import com.majidbahmani.cesto.database.di.databaseModule
import com.majidbahmani.cesto.feature.chat.di.chatModule
import com.majidbahmani.cesto.feature.onboarding.di.onboardingModule
import com.majidbahmani.cesto.feature.receipts.di.receiptsModule
import com.majidbahmani.cesto.feature.settings.di.settingsModule
import com.majidbahmani.cesto.gmailauth.GmailAuthorizer
import com.majidbahmani.cesto.gmailauth.GoogleIdTokenProvider
import com.majidbahmani.cesto.llm.di.llmModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Every Koin module of the app. Only :app knows all of them; features add theirs here. */
internal val appModules = listOf(
    networkModule,
    dispatchersModule,
    databaseModule,
    llmModule,
    accountModule,
    onboardingModule,
    receiptsModule,
    settingsModule,
    chatModule
)

/** What the platform apps create before Koin starts: Google SDKs, and build config (the API key). */
class PlatformServices(
    val gmailAuthorizer: GmailAuthorizer,
    val googleIdTokenProvider: GoogleIdTokenProvider,
    /** From local.properties / Secrets.xcconfig; empty when not set up (sign-in then fails, the app still runs). */
    val identityPlatformApiKey: String
)

internal fun platformServicesModule(services: PlatformServices): Module = module {
    single<GmailAuthorizer> { services.gmailAuthorizer }
    single<GoogleIdTokenProvider> { services.googleIdTokenProvider }
    single { IdentityPlatformConfig(apiKey = services.identityPlatformApiKey) }
}

/** Starts Koin once per process: from the Android Application and the iOS App init. */
fun initKoin(services: PlatformServices, appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(listOf(platformServicesModule(services)) + appModules)
}
