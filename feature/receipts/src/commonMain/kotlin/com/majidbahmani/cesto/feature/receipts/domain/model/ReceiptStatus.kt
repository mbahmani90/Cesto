package com.majidbahmani.cesto.feature.receipts.domain.model

enum class ReceiptStatus {
    /** Found in Gmail, PDF not downloaded yet. */
    FOUND,
    DOWNLOADED,

    /** Text and items extracted (later). */
    READY,

    /** The PDF couldn't be read from the email. */
    FAILED,
}
