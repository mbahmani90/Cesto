package com.majidbahmani.cesto.feature.settings.domain.usecase

import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult
import com.majidbahmani.cesto.feature.settings.fake.FakeGeminiKeyRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GeminiKeyUseCasesTest {

    private val repository = FakeGeminiKeyRepository()
    private val save = SaveGeminiKeyUseCase(repository)

    @Test
    fun validKey_isTrimmed_checked_andSaved() = runTest {
        assertEquals(SaveKeyResult.SAVED, save("  AIzaSyExample1234  \n"))

        assertEquals(listOf("AIzaSyExample1234"), repository.checkedKeys)
        assertEquals("AIzaSyExample1234", repository.savedKey.value)
    }

    @Test
    fun emptyInput_isNotChecked() = runTest {
        assertEquals(SaveKeyResult.EMPTY, save("   "))

        assertTrue(repository.checkedKeys.isEmpty())
    }

    @Test
    fun rejectedKey_isNotSaved_andTheOldKeyStays() = runTest {
        repository.savedKey.value = "AIzaSyOldWorkingKey"
        repository.checkAnswer = CompletableDeferred(SaveKeyResult.INVALID_KEY)

        assertEquals(SaveKeyResult.INVALID_KEY, save("AIzaSyTypo"))
        assertEquals("AIzaSyOldWorkingKey", repository.savedKey.value)
    }

    @Test
    fun observe_isMasked_neverTheFullKey() = runTest {
        val observe = ObserveGeminiKeyUseCase(repository)
        assertNull(observe().first())

        repository.savedKey.value = "AIzaSyExampleKeyx9Q2"
        assertEquals("AIza…x9Q2", observe().first())
    }

    @Test
    fun maskKey_hidesShortKeysCompletely() {
        assertEquals("••••", maskKey("short"))
        assertEquals("••••", maskKey("12345678"))
        assertEquals("1234…6789", maskKey("123456789"))
    }

    @Test
    fun remove_clearsTheKey() = runTest {
        repository.savedKey.value = "AIzaSyExampleKeyx9Q2"

        RemoveGeminiKeyUseCase(repository)()

        assertNull(repository.savedKey.value)
    }
}
