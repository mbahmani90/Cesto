package com.majidbahmani.cesto.feature.settings.domain.repository

import com.majidbahmani.cesto.feature.settings.domain.model.AiModel
import kotlinx.coroutines.flow.Flow

interface GeminiModelRepository {
    /** The model in use: the chosen one, or the default when nothing is chosen. */
    fun observe(): Flow<AiModel>

    suspend fun select(model: AiModel)
}
