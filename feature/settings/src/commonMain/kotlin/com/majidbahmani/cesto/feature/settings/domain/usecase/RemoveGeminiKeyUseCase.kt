package com.majidbahmani.cesto.feature.settings.domain.usecase

import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiKeyRepository

class RemoveGeminiKeyUseCase(private val repository: GeminiKeyRepository) {
    suspend operator fun invoke() = repository.remove()
}
