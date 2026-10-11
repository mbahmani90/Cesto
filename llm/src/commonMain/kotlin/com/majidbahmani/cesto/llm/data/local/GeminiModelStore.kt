package com.majidbahmani.cesto.llm.data.local

import com.majidbahmani.cesto.llm.GeminiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** The Gemini model chosen in Settings. Storage only: [current] applies [GeminiModel.DEFAULT]. */
interface GeminiModelStore {
    /** As stored; null when nothing (valid) is saved. Emits again when it's saved. */
    val saved: Flow<GeminiModel?>

    suspend fun save(model: GeminiModel)
}

/** The model to call now: the saved one, or [GeminiModel.DEFAULT]. */
suspend fun GeminiModelStore.current(): GeminiModel = saved.first() ?: GeminiModel.DEFAULT
