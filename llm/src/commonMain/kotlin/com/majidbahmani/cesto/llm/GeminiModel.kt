package com.majidbahmani.cesto.llm

/** The Gemini models the user can choose in Settings, for chat and receipt extraction. [id] goes in the API URL. */
enum class GeminiModel(val id: String) {
    FLASH_3_8("gemini-3.8-flash"),
    FLASH_LITE_3_5("gemini-3.5-flash-lite"),
    FLASH_2_5("gemini-2.5-flash"),
    FLASH_LITE_2_5("gemini-2.5-flash-lite");

    companion object {
        /** Used when nothing (valid) is saved. */
        val DEFAULT = FLASH_LITE_2_5
    }
}
