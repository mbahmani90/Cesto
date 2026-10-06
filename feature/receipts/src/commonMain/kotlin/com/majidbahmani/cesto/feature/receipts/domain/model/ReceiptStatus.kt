package com.majidbahmani.cesto.feature.receipts.domain.model

enum class ReceiptStatus {
    /** Found in Gmail, PDF not downloaded yet. */
    FOUND,
    DOWNLOADED,

    /** Text read on the phone; date and total known. */
    TEXT_EXTRACTED,

    /** Items extracted (later). */
    READY,

    /** The PDF couldn't be downloaded or read; retrying won't help. */
    FAILED,
}
