package com.majidbahmani.cesto.feature.receipts.fake

import com.majidbahmani.cesto.llm.embedding.EmbeddingProvider
import com.majidbahmani.cesto.llm.embedding.EmbeddingUnavailableException

/** Records the texts sent; every vector is [1, text length]. */
class FakeEmbeddingProvider(override var modelId: String = "fake@2") : EmbeddingProvider {
    val sentTexts = mutableListOf<String>()
    var unavailable: EmbeddingUnavailableException.Reason? = null

    override suspend fun embedDocuments(texts: List<String>): List<FloatArray> {
        unavailable?.let { throw EmbeddingUnavailableException(it) }
        sentTexts += texts
        return texts.map { floatArrayOf(1f, it.length.toFloat()) }
    }

    override suspend fun embedQuery(text: String): FloatArray = error("not used by the sync")
}
