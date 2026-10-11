package com.majidbahmani.cesto.feature.settings.presentation.viewmodel

import com.majidbahmani.cesto.feature.settings.domain.model.AiModel
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveAiModelUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SelectAiModelUseCase
import com.majidbahmani.cesto.feature.settings.fake.FakeGeminiModelRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
class ModelViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGeminiModelRepository(initial = AiModel.FLASH_LITE_2_5)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = ModelViewModel(ObserveAiModelUseCase(repository), SelectAiModelUseCase(repository)).also {
        backgroundScope.launch { it.uiState.collect() }
        runCurrent()
    }

    @Test
    fun showsTheModelInUse() = runTest(dispatcher) {
        val state = viewModel().uiState.value

        assertEquals(AiModel.FLASH_LITE_2_5, state.selected)
        assertFalse(state.isPicking)
    }

    @Test
    fun openAndDismissThePicker() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onOpenPicker()
        runCurrent()
        assertTrue(viewModel.uiState.value.isPicking)

        viewModel.onDismissPicker()
        runCurrent()
        assertFalse(viewModel.uiState.value.isPicking)
        assertEquals(AiModel.FLASH_LITE_2_5, repository.model.value)
    }

    @Test
    fun select_savesTheModel_andClosesThePicker() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onOpenPicker()

        viewModel.onSelect(AiModel.FLASH_3_8)
        runCurrent()

        assertEquals(AiModel.FLASH_3_8, repository.model.value)
        assertEquals(ModelUiState(selected = AiModel.FLASH_3_8, isPicking = false), viewModel.uiState.value)
    }
}
