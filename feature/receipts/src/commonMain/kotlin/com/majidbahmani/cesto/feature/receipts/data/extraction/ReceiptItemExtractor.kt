package com.majidbahmani.cesto.feature.receipts.data.extraction

/** Item section of a receipt → its lines. Implemented with Gemini; a fake in tests. */
interface ReceiptItemExtractor {
    /**
     * @throws ExtractionUnavailableException when no receipt can be extracted now (key rejected,
     *   quota reached): the sync stops extracting and tries again next time.
     */
    suspend fun extract(key: String, itemSection: String): List<ExtractedLine>
}

class ExtractionUnavailableException(reason: String, cause: Throwable? = null) : Exception(reason, cause)
