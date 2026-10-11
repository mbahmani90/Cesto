package com.majidbahmani.cesto.feature.settings.data.repository

import com.majidbahmani.cesto.feature.settings.domain.model.AiModel
import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiModelRepository
import com.majidbahmani.cesto.llm.GeminiModel
import com.majidbahmani.cesto.llm.data.local.GeminiModelStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GeminiModelRepositoryImpl(private val store: GeminiModelStore) : GeminiModelRepository {

    // Same default as GeminiModelStore.current(), so the screen shows the model the calls really use.
    override fun observe(): Flow<AiModel> = store.saved.map { (it ?: GeminiModel.DEFAULT).toAiModel() }.distinctUntilChanged()

    override suspend fun select(model: AiModel) = store.save(model.toGeminiModel())
}

// Exhaustive `when` both ways: a model added on one side only doesn't compile.
private fun GeminiModel.toAiModel(): AiModel = when (this) {
    GeminiModel.FLASH_3_8 -> AiModel.FLASH_3_8
    GeminiModel.FLASH_LITE_3_5 -> AiModel.FLASH_LITE_3_5
    GeminiModel.FLASH_2_5 -> AiModel.FLASH_2_5
    GeminiModel.FLASH_LITE_2_5 -> AiModel.FLASH_LITE_2_5
}

private fun AiModel.toGeminiModel(): GeminiModel = when (this) {
    AiModel.FLASH_3_8 -> GeminiModel.FLASH_3_8
    AiModel.FLASH_LITE_3_5 -> GeminiModel.FLASH_LITE_3_5
    AiModel.FLASH_2_5 -> GeminiModel.FLASH_2_5
    AiModel.FLASH_LITE_2_5 -> GeminiModel.FLASH_LITE_2_5
}
