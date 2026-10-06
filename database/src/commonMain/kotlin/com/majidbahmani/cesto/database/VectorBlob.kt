package com.majidbahmani.cesto.database

/** A vector as the `product_embedding.vector` BLOB: 4 bytes per number, float32 little-endian. */
fun FloatArray.toBlob(): ByteArray {
    val bytes = ByteArray(size * 4)
    forEachIndexed { i, value ->
        val bits = value.toRawBits()
        for (b in 0 until 4) bytes[i * 4 + b] = (bits ushr (8 * b)).toByte()
    }
    return bytes
}

fun ByteArray.toVector(): FloatArray {
    require(size % 4 == 0) { "Not a float32 vector: $size bytes" }
    return FloatArray(size / 4) { i ->
        var bits = 0
        for (b in 0 until 4) bits = bits or ((this[i * 4 + b].toInt() and 0xFF) shl (8 * b))
        Float.fromBits(bits)
    }
}
