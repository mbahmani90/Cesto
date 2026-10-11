package com.majidbahmani.cesto.llm.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.llm.AndroidGeminiKeyStore
import com.majidbahmani.cesto.llm.GeminiKeyStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val llmPlatformModule: Module = module {
    single<GeminiKeyStore> { AndroidGeminiKeyStore(context = get<Context>(), ioDispatcher = get(IoDispatcher)) }
    single<DataStore<Preferences>> {
        val context = get<Context>()
        settingsDataStore { context.filesDir.resolve(SETTINGS_FILE).absolutePath }
    }
}
