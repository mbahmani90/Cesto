package com.majidbahmani.cesto.llm.embedding

/**
 * Turns text into vectors for semantic search. Behind an interface so an on-device model (EmbeddingGemma)
 * can replace Gemini later; vectors from different models can't be compared, so each is stored with [modelId].
 */
interface EmbeddingProvider {
    /** Stored next to every vector, e.g. "gemini-embedding-2@768": a different id means "embed again". */
    val modelId: String

    /** One vector per text (product name + category), same order. @throws EmbeddingUnavailableException */
    suspend fun embedDocuments(texts: List<String>): List<FloatArray>

    /** The vector of search words like "dairy". @throws EmbeddingUnavailableException */
    suspend fun embedQuery(text: String): FloatArray
}

/** No vectors now (no key, quota, offline…): callers pause or fall back to keyword search. */
class EmbeddingUnavailableException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason { NO_KEY, KEY_REJECTED, QUOTA, BUSY, NO_CONNECTION, FAILED }
}
