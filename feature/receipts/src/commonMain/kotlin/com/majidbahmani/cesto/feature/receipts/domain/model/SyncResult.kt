package com.majidbahmani.cesto.feature.receipts.domain.model

sealed interface SyncResult {
    /**
     * Gmail was searched. [incomplete] counts emails that couldn't be read and receipts that couldn't
     * be downloaded this time (network hiccups); they are retried by the next sync.
     */
    data class Success(val downloaded: Int, val incomplete: Int = 0) : SyncResult

    /** Gmail couldn't be searched at all. */
    data class Failure(val reason: SyncFailure) : SyncResult
}

enum class SyncFailure {
    /** Gmail access was revoked or expired without refresh: the user has to connect again. */
    NOT_AUTHORIZED,

    /** Network or Gmail error; what was already saved is kept and the next sync continues. */
    FAILED
}
