package com.majidbahmani.cesto.llm.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.majidbahmani.cesto.llm.GeminiApi
import com.majidbahmani.cesto.llm.data.local.DataStoreGeminiModelStore
import com.majidbahmani.cesto.llm.data.local.GeminiModelStore
import com.majidbahmani.cesto.llm.embedding.EmbeddingProvider
import com.majidbahmani.cesto.llm.embedding.GeminiEmbeddingProvider
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

/** Per platform: the GeminiKeyStore and the settings DataStore (Android needs the Koin Android context). */
internal expect val llmPlatformModule: Module

/** Must end with .preferences_pb. */
internal const val SETTINGS_FILE = "cesto_settings.preferences_pb"

/** One DataStore per file: only create it inside a Koin `single`. [path] is read lazily, off the main thread. */
internal fun settingsDataStore(path: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path().toPath() })

/** Needs the HttpClient from :core. */
val llmModule = module {
    includes(llmPlatformModule)
    single { GeminiApi(client = get()) }
    single<EmbeddingProvider> { GeminiEmbeddingProvider(gemini = get(), keys = get()) }
    single<GeminiModelStore> { DataStoreGeminiModelStore(dataStore = get()) }
}
