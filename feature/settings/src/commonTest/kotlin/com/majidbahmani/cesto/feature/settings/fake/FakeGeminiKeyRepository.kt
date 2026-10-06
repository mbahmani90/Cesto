package com.majidbahmani.cesto.feature.settings.fake

import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult
import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiKeyRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

/** The saved key as a StateFlow; [checkAnswer] decides what Gemini says (waits until the test answers). */
class FakeGeminiKeyRepository(saved: String? = null) : GeminiKeyRepository {
    val savedKey = MutableStateFlow(saved)
    var checkAnswer = CompletableDeferred(SaveKeyResult.SAVED)
    val checkedKeys = mutableListOf<String>()

    override fun observeKey() = savedKey

    override suspend fun check(key: String): SaveKeyResult {
        checkedKeys += key
        return checkAnswer.await()
    }

    override suspend fun save(key: String) {
        savedKey.value = key
    }

    override suspend fun remove() {
        savedKey.value = null
    }
}
