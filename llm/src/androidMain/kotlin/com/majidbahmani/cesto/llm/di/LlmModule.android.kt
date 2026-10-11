package com.majidbahmani.cesto.llm.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.llm.data.local.AndroidGeminiKeyStore
import com.majidbahmani.cesto.llm.data.local.GeminiKeyStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val llmPlatformModule: Module = module {
    single<GeminiKeyStore> { AndroidGeminiKeyStore(dataStore = get(SecretsDataStore), ioDispatcher = get(IoDispatcher)) }
    single<DataStore<Preferences>>(SecretsDataStore) {
        val context = get<Context>()
        preferencesDataStore {
            context.filesDir.resolve(SECRETS_FILE).absolutePath
        }
    }
    single<DataStore<Preferences>>(SettingsDataStore) {
        val context = get<Context>()
        preferencesDataStore { context.filesDir.resolve(SETTINGS_FILE).absolutePath }
    }
}
