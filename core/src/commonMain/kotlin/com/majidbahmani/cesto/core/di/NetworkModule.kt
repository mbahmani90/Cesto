package com.majidbahmani.cesto.core.di

import com.majidbahmani.cesto.core.network.createHttpClient
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

/** Engine per platform: OkHttp on Android, Darwin on iOS. */
internal expect val httpEngineModule: Module

val networkModule: Module = module {
    includes(httpEngineModule)

    // One client for the app: it owns the connection pool.
    single<HttpClient> { createHttpClient(engine = get()) } onClose { it?.close() }
}
