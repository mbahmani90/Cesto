package com.majidbahmani.cesto.feature.chat.domain.usecase

import com.majidbahmani.cesto.feature.chat.domain.model.AgentModel
import kotlinx.coroutines.flow.Flow

/** Ask works only with a Gemini key: without it, the screen points to Settings. */
class ObserveAssistantReadyUseCase(private val model: AgentModel) {
    operator fun invoke(): Flow<Boolean> = model.isReady
}
