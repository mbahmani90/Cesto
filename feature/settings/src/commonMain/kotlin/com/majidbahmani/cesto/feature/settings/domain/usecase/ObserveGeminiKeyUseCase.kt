package com.majidbahmani.cesto.feature.settings.domain.usecase

import com.majidbahmani.cesto.feature.settings.domain.repository.GeminiKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The saved key, masked ("AIza…x9Q2"), or null: the full key never leaves the data layer. */
class ObserveGeminiKeyUseCase(private val repository: GeminiKeyRepository) {
    operator fun invoke(): Flow<String?> = repository.observeKey().map { it?.let(::maskKey) }
}

/** First and last 4 characters; short keys are fully hidden. */
fun maskKey(key: String): String = if (key.length <= 8) "••••" else "${key.take(4)}…${key.takeLast(4)}"
