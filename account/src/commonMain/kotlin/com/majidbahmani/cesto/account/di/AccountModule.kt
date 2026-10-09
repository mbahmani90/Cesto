package com.majidbahmani.cesto.account.di

import com.majidbahmani.cesto.account.AccountRepository
import com.majidbahmani.cesto.account.AccountRepositoryImpl
import com.majidbahmani.cesto.account.IdentityPlatformApi
import kotlin.time.Clock
import org.koin.core.module.Module
import org.koin.dsl.module

/** The Identity Platform Web API key; comes from local.properties / Secrets.xcconfig through the platform apps. */
data class IdentityPlatformConfig(val apiKey: String)

/** SessionStore per platform (Android needs the Koin Android context). */
internal expect val sessionStoreModule: Module

/** Needs the HttpClient from :core and an [IdentityPlatformConfig]. */
val accountModule = module {
    includes(sessionStoreModule)
    single { IdentityPlatformApi(client = get(), apiKey = get<IdentityPlatformConfig>().apiKey) }
    single<AccountRepository> {
        AccountRepositoryImpl(api = get(), store = get(), currentTimeMillis = { Clock.System.now().toEpochMilliseconds() })
    }
}
