package com.majidbahmani.cesto.llm.di

import android.content.Context
import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.llm.AndroidGeminiKeyStore
import com.majidbahmani.cesto.llm.GeminiKeyStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val geminiKeyStoreModule: Module = module {
    single<GeminiKeyStore> { AndroidGeminiKeyStore(context = get<Context>(), ioDispatcher = get(IoDispatcher)) }
}
