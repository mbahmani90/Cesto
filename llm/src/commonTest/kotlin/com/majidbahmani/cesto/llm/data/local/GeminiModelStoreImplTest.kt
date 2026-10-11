package com.majidbahmani.cesto.llm.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.majidbahmani.cesto.llm.GeminiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okio.IOException

class GeminiModelStoreImplTest {

    @Test
    fun nothingSaved_isNull() = runTest {
        assertNull(GeminiModelStoreImpl(InMemoryDataStore()).saved.first())
    }

    @Test
    fun savedModel_isReadBack() = runTest {
        val store = GeminiModelStoreImpl(InMemoryDataStore())

        store.save(GeminiModel.FLASH_2_5)

        assertEquals(GeminiModel.FLASH_2_5, store.saved.first())
    }

    @Test
    fun savedByName() = runTest {
        val dataStore = InMemoryDataStore()

        GeminiModelStoreImpl(dataStore).save(GeminiModel.FLASH_LITE_2_5)

        assertEquals("FLASH_LITE_2_5", dataStore.data.first()[stringPreferencesKey("gemini_model")])
    }

    @Test
    fun unknownName_isNull() = runTest {
        val dataStore = InMemoryDataStore()
        dataStore.edit { it[stringPreferencesKey("gemini_model")] = "REMOVED_MODEL" }

        assertNull(GeminiModelStoreImpl(dataStore).saved.first())
    }

    @Test
    fun unreadableFile_isNull() = runTest {
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) = error("not used")
        }

        assertNull(GeminiModelStoreImpl(broken).saved.first())
    }

    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
