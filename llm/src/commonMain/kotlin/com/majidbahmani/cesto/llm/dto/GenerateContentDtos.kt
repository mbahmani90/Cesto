package com.majidbahmani.cesto.llm.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** `models.generateContent` request (only what Cesto uses). */
@Serializable
data class GenerateContentRequestDto(
    val contents: List<ContentDto>,
    val systemInstruction: ContentDto? = null,
    val generationConfig: GenerationConfigDto? = null
)

@Serializable
data class ContentDto(val parts: List<PartDto>, val role: String? = null)

@Serializable
data class PartDto(val text: String? = null)

/** Structured output: `responseMimeType = "application/json"` + a `responseSchema` (Gemini's Schema object). */
@Serializable
data class GenerationConfigDto(
    val temperature: Double? = null,
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null
)

@Serializable
data class GenerateContentResponseDto(val candidates: List<CandidateDto> = emptyList())

@Serializable
data class CandidateDto(val content: ContentDto? = null, val finishReason: String? = null)
