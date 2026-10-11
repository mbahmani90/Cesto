package com.majidbahmani.cesto.feature.settings.data.repository

import com.majidbahmani.cesto.feature.settings.domain.model.AiModel
import com.majidbahmani.cesto.llm.GeminiModel
import com.majidbahmani.cesto.llm.data.local.GeminiModelStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GeminiModelRepositoryImplTest {

    private class FakeStore(initial: GeminiModel? = null) : GeminiModelStore {
        override val saved = MutableStateFlow(initial)
        override suspend fun save(model: GeminiModel) {
            saved.value = model
        }
    }

    @Test
    fun nothingSaved_isTheDefaultTheCallsUse() = runTest {
        val repository = GeminiModelRepositoryImpl(FakeStore())

        assertEquals(AiModel.valueOf(GeminiModel.DEFAULT.name), repository.observe().first())
    }

    @Test
    fun everyModel_mapsBothWays() = runTest {
        val store = FakeStore()
        val repository = GeminiModelRepositoryImpl(store)

        AiModel.entries.forEach { model ->
            repository.select(model)

            assertEquals(model.name, store.saved.value?.name)
            assertEquals(model, repository.observe().first())
        }
    }

    @Test
    fun bothEnums_listTheSameModels() {
        assertEquals(GeminiModel.entries.map { it.name }, AiModel.entries.map { it.name })
    }
}
