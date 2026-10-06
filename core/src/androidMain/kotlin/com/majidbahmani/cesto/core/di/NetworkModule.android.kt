package com.majidbahmani.cesto.core.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val httpEngineModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
}
