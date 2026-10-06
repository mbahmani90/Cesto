package com.majidbahmani.cesto.feature.chat.data.tools

import kotlin.math.sqrt

/** How close two vectors point (1 = same direction, 0 = unrelated). 0 for vectors of different models' sizes. */
internal fun cosine(a: FloatArray, b: FloatArray): Float {
    if (a.size != b.size || a.isEmpty()) return 0f
    var dot = 0f
    var normA = 0f
    var normB = 0f
    for (i in a.indices) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    return if (normA == 0f || normB == 0f) 0f else dot / (sqrt(normA) * sqrt(normB))
}
