package com.majidbahmani.cesto.llm.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.majidbahmani.cesto.core.di.IoDispatcher
import com.majidbahmani.cesto.llm.data.local.AndroidGeminiKeyStore
import com.majidbahmani.cesto.llm.data.local.GeminiKeyStore
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The encrypted Gemini key, in its own file: never mixed with the plain settings. */
private val SecretsDataStore = named("SecretsDataStore")
private const val SECRETS_FILE = "cesto_secrets.preferences_pb"

internal actual val llmPlatformModule: Module = module {
    single<GeminiKeyStore> { AndroidGeminiKeyStore(dataStore = get(SecretsDataStore), ioDispatcher = get(IoDispatcher)) }
    single<DataStore<Preferences>>(SecretsDataStore) {
        val context = get<Context>()
        preferencesDataStore(migrations = AndroidGeminiKeyStore.migrations(context)) {
            context.filesDir.resolve(SECRETS_FILE).absolutePath
        }
    }
    single<DataStore<Preferences>> {
        val context = get<Context>()
        preferencesDataStore { context.filesDir.resolve(SETTINGS_FILE).absolutePath }
    }
}
