package com.majidbahmani.cesto.llm.di

import com.majidbahmani.cesto.llm.GeminiApi
import org.koin.core.module.Module
import org.koin.dsl.module

/** GeminiKeyStore per platform (Android needs the Koin Android context). */
internal expect val geminiKeyStoreModule: Module

/** Needs the HttpClient from :core. */
val llmModule = module {
    includes(geminiKeyStoreModule)
    single { GeminiApi(client = get()) }
}
