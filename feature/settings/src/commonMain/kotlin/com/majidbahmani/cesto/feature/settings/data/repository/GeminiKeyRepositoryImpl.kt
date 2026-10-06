package com.majidbahmani.cesto.feature.settings.data.repository

import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult
import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiKeyRepository
import com.majidbahmani.cesto.llm.GeminiApi
import com.majidbahmani.cesto.llm.GeminiKeyStore
import com.majidbahmani.cesto.llm.KeyCheck
import kotlinx.coroutines.flow.Flow

class GeminiKeyRepositoryImpl(
    private val store: GeminiKeyStore,
    private val gemini: GeminiApi,
) : GeminiKeyRepository {

    override fun observeKey(): Flow<String?> = store.key

    override suspend fun check(key: String): SaveKeyResult = when (gemini.checkKey(key)) {
        KeyCheck.VALID -> SaveKeyResult.SAVED
        KeyCheck.INVALID_KEY -> SaveKeyResult.INVALID_KEY
        KeyCheck.NOT_ALLOWED -> SaveKeyResult.NOT_ALLOWED
        KeyCheck.NO_CONNECTION -> SaveKeyResult.NO_CONNECTION
        KeyCheck.FAILED -> SaveKeyResult.FAILED
    }

    override suspend fun save(key: String) = store.save(key)

    override suspend fun remove() = store.clear()
}
