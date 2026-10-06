package com.majidbahmani.cesto.feature.receipts.data.embedding

/** A product and the text its vector is made from. */
data class ProductText(val productId: Long, val text: String)

/**
 * What's embedded for a product: only its full name and category, never prices or receipt data.
 * Changing this format re-embeds every product on the next sync (the stored text no longer matches).
 */
fun productEmbedText(normalizedName: String, category: String?): String =
    if (category.isNullOrBlank()) normalizedName else "$normalizedName. Categoria: $category"
