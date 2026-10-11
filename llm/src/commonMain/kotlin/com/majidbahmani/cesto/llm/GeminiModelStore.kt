package com.majidbahmani.cesto.llm

import kotlinx.coroutines.flow.Flow

/** The Gemini model chosen in Settings. Storage only: the caller applies [GeminiModel.DEFAULT]. */
interface GeminiModelStore {
    /** As stored; null when nothing (valid) is saved. Emits again when it's saved. */
    val saved: Flow<GeminiModel?>

    suspend fun save(model: GeminiModel)
}
