package com.majidbahmani.cesto.feature.receipts.fake

import com.majidbahmani.cesto.llm.GeminiModel
import com.majidbahmani.cesto.llm.data.local.GeminiModelStore
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGeminiModelStore(initial: GeminiModel? = null) : GeminiModelStore {
    override val saved = MutableStateFlow(initial)
    override suspend fun save(model: GeminiModel) {
        saved.value = model
    }
}
