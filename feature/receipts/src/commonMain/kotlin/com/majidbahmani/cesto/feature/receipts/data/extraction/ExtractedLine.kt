package com.majidbahmani.cesto.feature.receipts.data.extraction

/** One line of a receipt as extracted by Gemini; amounts already in cents. */
data class ExtractedLine(
    val kind: Kind,
    val rawName: String,
    val normalizedName: String,
    val category: String?,
    /** Units (6) or kilograms (0.760). */
    val quantity: Double,
    val unit: Unit,
    /** "4X125G" → 4; otherwise 1. */
    val unitsPerPack: Int,
    val unitPriceCents: Long?,
    val lineTotalCents: Long
) {
    enum class Kind { ITEM, DEPOSIT, DISCOUNT }

    enum class Unit { UNIT, KG }
}
