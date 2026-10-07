package com.majidbahmani.cesto.feature.receipts.fake

import com.majidbahmani.cesto.feature.receipts.data.extraction.ExtractedLine
import com.majidbahmani.cesto.feature.receipts.data.extraction.ExtractionUnavailableException
import com.majidbahmani.cesto.feature.receipts.data.extraction.ReceiptItemExtractor

/** Returns [lines] for every section; records what was sent. */
class FakeReceiptItemExtractor(var lines: List<ExtractedLine> = listOf(milk, banana)) : ReceiptItemExtractor {
    val sentSections = mutableListOf<String>()
    var unavailable = false
    var failFor: String? = null

    override suspend fun extract(key: String, itemSection: String): List<ExtractedLine> {
        if (unavailable) throw ExtractionUnavailableException("quota")
        if (failFor != null && itemSection.contains(failFor!!)) throw IllegalStateException("bad JSON")
        sentSections += itemSection
        return lines
    }

    companion object {
        val milk = ExtractedLine(
            ExtractedLine.Kind.ITEM, "LEITE PAST GORDO 1L", "Leite pasteurizado gordo 1 L", "Laticinios",
            1.0, ExtractedLine.Unit.UNIT, 1, 119, 119
        )
        val banana = ExtractedLine(ExtractedLine.Kind.ITEM, "BANANA", "Banana", "Frutas e Legumes", 0.76, ExtractedLine.Unit.KG, 1, 119, 90)
    }
}

class FakeGeminiKeyStore(initial: String? = "AIza-test") : com.majidbahmani.cesto.llm.GeminiKeyStore {
    override val key = kotlinx.coroutines.flow.MutableStateFlow(initial)
    override suspend fun save(key: String) {
        this.key.value = key
    }
    override suspend fun clear() {
        key.value = null
    }
}
