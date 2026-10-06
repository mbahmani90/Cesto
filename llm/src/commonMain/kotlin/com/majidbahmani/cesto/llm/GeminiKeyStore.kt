package com.majidbahmani.cesto.llm

import kotlinx.coroutines.flow.Flow

/**
 * The user's own Gemini API key, stored on the phone only (Android Keystore, iOS Keychain later).
 * Never logged, never sent anywhere but to Google's Gemini API.
 */
interface GeminiKeyStore {
    /** The saved key, or null; emits again when it's saved or removed. */
    val key: Flow<String?>

    suspend fun save(key: String)

    suspend fun clear()
}
