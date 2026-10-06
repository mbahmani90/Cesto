package com.majidbahmani.cesto.llm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** iOS for now: kept only while the app runs. TODO: store in the iOS Keychain. */
class InMemoryGeminiKeyStore : GeminiKeyStore {
    private val state = MutableStateFlow<String?>(null)

    override val key: StateFlow<String?> = state

    override suspend fun save(key: String) {
        state.value = key
    }

    override suspend fun clear() {
        state.value = null
    }
}
