package com.majidbahmani.cesto.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.dsl.koinApplication
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame

/** Koin resolves at runtime: a missing binding only shows up here, not at compile time. */
class AppModulesTest {

    // Local KoinApplication: tests never touch the global Koin instance.
    private val app = koinApplication { modules(appModules) }

    @AfterTest
    fun tearDown() = app.close()

    @Test
    fun httpClient_isSingleton() {
        assertSame(app.koin.get<HttpClient>(), app.koin.get<HttpClient>())
    }

    @Test
    fun httpEngine_isResolvedForThisPlatform() {
        app.koin.get<HttpClientEngine>()
    }
}
