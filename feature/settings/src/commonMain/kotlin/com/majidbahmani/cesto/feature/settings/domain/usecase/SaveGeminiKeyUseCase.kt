package com.majidbahmani.cesto.feature.settings.domain.usecase

import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult
import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiKeyRepository

/** Rule: a key is saved only after Gemini accepted it, so a typo can't break extraction later. */
class SaveGeminiKeyUseCase(private val repository: GeminiKeyRepository) {
    suspend operator fun invoke(input: String): SaveKeyResult {
        // Pasting often brings spaces or a line break along.
        val key = input.trim()
        if (key.isEmpty()) return SaveKeyResult.EMPTY
        val result = repository.check(key)
        if (result == SaveKeyResult.SAVED) repository.save(key)
        return result
    }
}
