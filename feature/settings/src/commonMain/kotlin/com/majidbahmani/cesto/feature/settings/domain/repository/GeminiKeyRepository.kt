package com.majidbahmani.cesto.feature.settings.domain.repository

import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult
import kotlinx.coroutines.flow.Flow

interface GeminiKeyRepository {
    /** The saved key, or null. Only for masking: the key itself never reaches the UI. */
    fun observeKey(): Flow<String?>

    /** One small Gemini request with [key]: SAVED means valid (nothing is saved here). */
    suspend fun check(key: String): SaveKeyResult

    suspend fun save(key: String)

    suspend fun remove()
}
