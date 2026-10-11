package com.majidbahmani.cesto.llm

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

class DataStoreGeminiModelStore(private val dataStore: DataStore<Preferences>) : GeminiModelStore {

    override val saved: Flow<GeminiModel?> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e } // unreadable → nothing saved
        .map { prefs -> prefs[MODEL]?.let { saved -> GeminiModel.entries.firstOrNull { it.name == saved } } } // unknown → null
        .distinctUntilChanged()

    override suspend fun save(model: GeminiModel) {
        dataStore.edit { it[MODEL] = model.name } // by name: enum order can change
    }

    private companion object {
        val MODEL = stringPreferencesKey("gemini_model")
    }
}
