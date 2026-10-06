package com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel

import com.majidbahmani.cesto.feature.onboarding.domain.model.GmailConnectionResult
import com.majidbahmani.cesto.feature.onboarding.domain.repository.GmailConnectionRepository
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckGmailConnectionUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.ConnectGmailUseCase
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.ErrorReason
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.Status
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    /** Suspends until the test answers, so in-between states can be checked. */
    private class FakeGmailConnectionRepository : GmailConnectionRepository {
        val isConnectedAnswer = CompletableDeferred<Boolean>()
        var connectAnswer = CompletableDeferred<GmailConnectionResult>()
        var connectCalls = 0

        override suspend fun isConnected(): Boolean = isConnectedAnswer.await()

        override suspend fun connect(): GmailConnectionResult {
            connectCalls++
            return connectAnswer.await()
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeGmailConnectionRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = OnboardingViewModel(
        checkGmailConnection = CheckGmailConnectionUseCase(repository),
        connectGmail = ConnectGmailUseCase(repository),
    )

    /** A view model that finished the start check with "not connected". */
    private fun TestScope.readyViewModel(): OnboardingViewModel = viewModel().also {
        repository.isConnectedAnswer.complete(false)
        runCurrent()
    }

    @Test
    fun startsChecking_thenConnectedWhenGrantedBefore() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(Status.CHECKING, viewModel.uiState.value.status)

        repository.isConnectedAnswer.complete(true)
        runCurrent()

        assertEquals(Status.CONNECTED, viewModel.uiState.value.status)
    }

    @Test
    fun startCheck_showsIntroWhenNotConnected() = runTest(dispatcher) {
        assertEquals(OnboardingUiState(status = Status.READY), readyViewModel().uiState.value)
    }

    @Test
    fun connect_showsProgressThenConnected() = runTest(dispatcher) {
        val viewModel = readyViewModel()

        viewModel.onConnectGmail()
        assertEquals(Status.CONNECTING, viewModel.uiState.value.status)

        repository.connectAnswer.complete(GmailConnectionResult.CONNECTED)
        runCurrent()
        assertEquals(Status.CONNECTED, viewModel.uiState.value.status)
    }

    @Test
    fun cancel_returnsToIntroWithoutError() = runTest(dispatcher) {
        val viewModel = readyViewModel()

        viewModel.onConnectGmail()
        repository.connectAnswer.complete(GmailConnectionResult.CANCELLED)
        runCurrent()

        assertEquals(OnboardingUiState(status = Status.READY, error = null), viewModel.uiState.value)
    }

    @Test
    fun deniedAndFailed_showReason_andRetryClearsIt() = runTest(dispatcher) {
        val viewModel = readyViewModel()

        viewModel.onConnectGmail()
        repository.connectAnswer.complete(GmailConnectionResult.PERMISSION_DENIED)
        runCurrent()
        assertEquals(OnboardingUiState(status = Status.READY, error = ErrorReason.PERMISSION_DENIED), viewModel.uiState.value)

        repository.connectAnswer = CompletableDeferred()
        viewModel.onConnectGmail()
        assertEquals(OnboardingUiState(status = Status.CONNECTING, error = null), viewModel.uiState.value)

        repository.connectAnswer.complete(GmailConnectionResult.FAILED)
        runCurrent()
        assertEquals(ErrorReason.FAILED, viewModel.uiState.value.error)
    }

    @Test
    fun doubleTap_andTapDuringStartCheck_requestOnlyOnce() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onConnectGmail() // still checking: ignored
        repository.isConnectedAnswer.complete(false)
        runCurrent()

        viewModel.onConnectGmail()
        viewModel.onConnectGmail() // already connecting: ignored
        runCurrent()

        assertEquals(1, repository.connectCalls)
    }
}
