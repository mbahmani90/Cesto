package com.majidbahmani.cesto.feature.settings.domain.usecase

import com.majidbahmani.cesto.feature.settings.domain.model.AiModel
import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiModelRepository
import kotlinx.coroutines.flow.Flow

class ObserveAiModelUseCase(private val repository: GeminiModelRepository) {
    operator fun invoke(): Flow<AiModel> = repository.observe()
}

/** Used from the next Gemini call on: chat and receipt extraction read it each time. */
class SelectAiModelUseCase(private val repository: GeminiModelRepository) {
    suspend operator fun invoke(model: AiModel) = repository.select(model)
}
