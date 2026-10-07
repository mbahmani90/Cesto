package com.majidbahmani.cesto.feature.settings.presentation.viewmodel

import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.RemoveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SaveGeminiKeyUseCase
import com.majidbahmani.cesto.feature.settings.fake.FakeGeminiKeyRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var repository = FakeGeminiKeyRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = SettingsViewModel(
        observeGeminiKey = ObserveGeminiKeyUseCase(repository),
        saveGeminiKey = SaveGeminiKeyUseCase(repository),
        removeGeminiKey = RemoveGeminiKeyUseCase(repository)
    ).also {
        backgroundScope.launch { it.uiState.collect() }
        runCurrent()
    }

    private val SettingsViewModel.state get() = uiState.value

    @Test
    fun withoutKey_showsTheSetupSteps() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertFalse(viewModel.state.isLoading)
        assertTrue(viewModel.state.isEditing)
        assertNull(viewModel.state.savedKeyMasked)
    }

    @Test
    fun withKey_showsItMasked_notTheSteps() = runTest(dispatcher) {
        repository = FakeGeminiKeyRepository(saved = "AIzaSyExampleKeyx9Q2")
        val viewModel = viewModel()

        assertEquals("AIza…x9Q2", viewModel.state.savedKeyMasked)
        assertFalse(viewModel.state.isEditing)
    }

    @Test
    fun testAndSave_showsProgress_thenSavedAndClearsTheField() = runTest(dispatcher) {
        repository.checkAnswer = CompletableDeferred()
        val viewModel = viewModel()
        viewModel.onKeyInputChange("AIzaSyExampleKeyx9Q2")

        viewModel.onTestAndSave()
        runCurrent()
        assertTrue(viewModel.state.isTesting)

        repository.checkAnswer.complete(SaveKeyResult.SAVED)
        runCurrent()
        assertEquals(SettingsUiState(isLoading = false, savedKeyMasked = "AIza…x9Q2", feedback = SaveKeyResult.SAVED), viewModel.state)
    }

    @Test
    fun rejectedKey_keepsTheInput_andShowsWhy_typingClearsIt() = runTest(dispatcher) {
        repository.checkAnswer = CompletableDeferred(SaveKeyResult.NOT_ALLOWED)
        val viewModel = viewModel()
        viewModel.onKeyInputChange("AIzaSyWrongProject")

        viewModel.onTestAndSave()
        runCurrent()
        assertEquals(SaveKeyResult.NOT_ALLOWED, viewModel.state.feedback)
        assertEquals("AIzaSyWrongProject", viewModel.state.keyInput)
        assertFalse(viewModel.state.isTesting)

        viewModel.onKeyInputChange("AIzaSyWrongProject2")
        runCurrent()
        assertNull(viewModel.state.feedback)
    }

    @Test
    fun change_opensTheSteps_cancelKeepsTheOldKey() = runTest(dispatcher) {
        repository = FakeGeminiKeyRepository(saved = "AIzaSyExampleKeyx9Q2")
        val viewModel = viewModel()

        viewModel.onChangeKey()
        runCurrent()
        assertTrue(viewModel.state.isEditing)

        viewModel.onCancelChange()
        runCurrent()
        assertFalse(viewModel.state.isEditing)
        assertEquals("AIzaSyExampleKeyx9Q2", repository.savedKey.value)
    }

    @Test
    fun remove_showsTheSetupStepsAgain() = runTest(dispatcher) {
        repository = FakeGeminiKeyRepository(saved = "AIzaSyExampleKeyx9Q2")
        val viewModel = viewModel()

        viewModel.onRemoveKey()
        runCurrent()

        assertNull(viewModel.state.savedKeyMasked)
        assertTrue(viewModel.state.isEditing)
    }

    @Test
    fun accountEmail_isKeptInTheScreenState_andNotSaved() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onAccountEmailChange("other.account@example.com")
        runCurrent()

        assertEquals("other.account@example.com", viewModel.state.accountEmail)
        assertNull(repository.savedKey.value)
    }

    @Test
    fun keyVisibility_toggles() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onToggleKeyVisibility()
        runCurrent()
        assertTrue(viewModel.state.isKeyVisible)
        viewModel.onToggleKeyVisibility()
        runCurrent()
        assertFalse(viewModel.state.isKeyVisible)
    }
}
