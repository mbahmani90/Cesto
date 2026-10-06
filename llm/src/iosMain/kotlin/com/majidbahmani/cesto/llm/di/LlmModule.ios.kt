package com.majidbahmani.cesto.llm.di

import com.majidbahmani.cesto.llm.GeminiKeyStore
import com.majidbahmani.cesto.llm.InMemoryGeminiKeyStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val geminiKeyStoreModule: Module = module {
    single<GeminiKeyStore> { InMemoryGeminiKeyStore() }
}
