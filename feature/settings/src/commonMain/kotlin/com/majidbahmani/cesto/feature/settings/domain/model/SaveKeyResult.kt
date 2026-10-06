package com.majidbahmani.cesto.feature.settings.domain.model

/** Outcome of "Test and save": the key is only saved when Gemini accepted it. */
enum class SaveKeyResult {
    SAVED,
    EMPTY,

    /** Gemini says this isn't a valid API key. */
    INVALID_KEY,

    /** Valid key, but its project hasn't enabled the Gemini API (or the key is restricted). */
    NOT_ALLOWED,
    NO_CONNECTION,
    FAILED,
}
