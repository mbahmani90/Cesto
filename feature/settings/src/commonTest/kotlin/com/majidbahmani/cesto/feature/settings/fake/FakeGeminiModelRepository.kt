package com.majidbahmani.cesto.feature.settings.fake

import com.majidbahmani.cesto.feature.settings.domain.model.AiModel
import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiModelRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGeminiModelRepository(initial: AiModel = AiModel.FLASH_LITE_2_5) : GeminiModelRepository {
    val model = MutableStateFlow(initial)

    override fun observe() = model

    override suspend fun select(model: AiModel) {
        this.model.value = model
    }
}
