package com.majidbahmani.cesto.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The app's single HTTP client. Generic on purpose: no base URL or auth here, each API
 * (Gmail, Open Food Facts, LLM proxy) sets its own per request. The engine is passed in so
 * tests can use MockEngine.
 */
fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = true // 4xx/5xx throw instead of being parsed as data
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
