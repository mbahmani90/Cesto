package com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel

import com.majidbahmani.cesto.feature.onboarding.domain.model.GmailConnectionResult
import com.majidbahmani.cesto.feature.onboarding.domain.model.SignInResult
import com.majidbahmani.cesto.feature.onboarding.domain.repository.GmailConnectionRepository
import com.majidbahmani.cesto.feature.onboarding.domain.repository.SignInRepository
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckGmailConnectionUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.CheckSignInUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.ConnectGmailUseCase
import com.majidbahmani.cesto.feature.onboarding.domain.usecase.SignInUseCase
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.ErrorReason
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.Status
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    /** Suspend until the test answers, so in-between states can be checked. */
    private class FakeSignInRepository : SignInRepository {
        val isSignedInAnswer = CompletableDeferred<Boolean>()
        var signInAnswer = CompletableDeferred<SignInResult>()
        var signInCalls = 0

        override suspend fun isSignedIn(): Boolean = isSignedInAnswer.await()

        override suspend fun signIn(): SignInResult {
            signInCalls++
            return signInAnswer.await()
        }
    }

    private class FakeGmailConnectionRepository : GmailConnectionRepository {
        val isConnectedAnswer = CompletableDeferred<Boolean>()
        var connectAnswer = CompletableDeferred<GmailConnectionResult>()
        var isConnectedCalls = 0
        var connectCalls = 0

        override suspend fun isConnected(): Boolean {
            isConnectedCalls++
            return isConnectedAnswer.await()
        }

        override suspend fun connect(): GmailConnectionResult {
            connectCalls++
            return connectAnswer.await()
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val accounts = FakeSignInRepository()
    private val gmail = FakeGmailConnectionRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = OnboardingViewModel(
        checkSignIn = CheckSignInUseCase(accounts),
        signIn = SignInUseCase(accounts),
        checkGmailConnection = CheckGmailConnectionUseCase(gmail),
        connectGmail = ConnectGmailUseCase(gmail)
    )

    /** A view model that finished the start check: not signed in, or signed in without Gmail. */
    private fun TestScope.readyViewModel(signedIn: Boolean = false): OnboardingViewModel = viewModel().also {
        accounts.isSignedInAnswer.complete(signedIn)
        gmail.isConnectedAnswer.complete(false)
        runCurrent()
    }

    @Test
    fun startsChecking_thenConnectedWhenSignedInAndGrantedBefore() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(Status.CHECKING, viewModel.uiState.value.status)

        accounts.isSignedInAnswer.complete(true)
        gmail.isConnectedAnswer.complete(true)
        runCurrent()

        assertEquals(Status.CONNECTED, viewModel.uiState.value.status)
    }

    @Test
    fun notSignedIn_showsIntro_withoutAskingGmail() = runTest(dispatcher) {
        assertEquals(OnboardingUiState(status = Status.READY, signedIn = false), readyViewModel().uiState.value)
        assertEquals(0, gmail.isConnectedCalls)
    }

    @Test
    fun signedInWithoutGmail_showsIntroForGmailOnly() = runTest(dispatcher) {
        assertEquals(OnboardingUiState(status = Status.READY, signedIn = true), readyViewModel(signedIn = true).uiState.value)
    }

    @Test
    fun continue_signsInThenConnectsGmail() = runTest(dispatcher) {
        val viewModel = readyViewModel()

        viewModel.onContinue()
        runCurrent()
        assertEquals(Status.SIGNING_IN, viewModel.uiState.value.status)

        accounts.signInAnswer.complete(SignInResult.SIGNED_IN)
        runCurrent()
        assertEquals(OnboardingUiState(status = Status.CONNECTING, signedIn = true), viewModel.uiState.value)

        gmail.connectAnswer.complete(GmailConnectionResult.CONNECTED)
        runCurrent()
        assertEquals(Status.CONNECTED, viewModel.uiState.value.status)
    }

    @Test
    fun signedIn_continueOnlyAsksGmail() = runTest(dispatcher) {
        val viewModel = readyViewModel(signedIn = true)

        viewModel.onContinue()
        gmail.connectAnswer.complete(GmailConnectionResult.CONNECTED)
        runCurrent()

        assertEquals(0, accounts.signInCalls)
        assertEquals(Status.CONNECTED, viewModel.uiState.value.status)
    }

    @Test
    fun signInCancelled_returnsToIntroWithoutError_andNoGmailDialog() = runTest(dispatcher) {
        val viewModel = readyViewModel()

        viewModel.onContinue()
        accounts.signInAnswer.complete(SignInResult.CANCELLED)
        runCurrent()

        assertEquals(OnboardingUiState(status = Status.READY), viewModel.uiState.value)
        assertEquals(0, gmail.connectCalls)
    }

    @Test
    fun signInErrors_showTheReason() = runTest(dispatcher) {
        val viewModel = readyViewModel()
        val expected = mapOf(
            SignInResult.NO_GOOGLE_ACCOUNT to ErrorReason.NO_GOOGLE_ACCOUNT,
            SignInResult.REJECTED to ErrorReason.SIGN_IN_REJECTED,
            SignInResult.FAILED to ErrorReason.FAILED
        )
        expected.forEach { (result, reason) ->
            accounts.signInAnswer = CompletableDeferred()
            viewModel.onContinue()
            runCurrent()
            assertEquals(null, viewModel.uiState.value.error) // a new attempt clears the last error
            accounts.signInAnswer.complete(result)
            runCurrent()
            assertEquals(OnboardingUiState(status = Status.READY, error = reason), viewModel.uiState.value)
        }
    }

    @Test
    fun gmailDeniedAfterSignIn_staysSignedIn_retryOnlyAsksGmail() = runTest(dispatcher) {
        val viewModel = readyViewModel()

        viewModel.onContinue()
        accounts.signInAnswer.complete(SignInResult.SIGNED_IN)
        gmail.connectAnswer.complete(GmailConnectionResult.PERMISSION_DENIED)
        runCurrent()
        assertEquals(
            OnboardingUiState(status = Status.READY, signedIn = true, error = ErrorReason.PERMISSION_DENIED),
            viewModel.uiState.value
        )

        gmail.connectAnswer = CompletableDeferred()
        viewModel.onContinue()
        gmail.connectAnswer.complete(GmailConnectionResult.FAILED)
        runCurrent()
        assertEquals(ErrorReason.FAILED, viewModel.uiState.value.error)
        assertEquals(1, accounts.signInCalls)
    }

    @Test
    fun gmailCancelled_returnsToIntroWithoutError() = runTest(dispatcher) {
        val viewModel = readyViewModel(signedIn = true)

        viewModel.onContinue()
        gmail.connectAnswer.complete(GmailConnectionResult.CANCELLED)
        runCurrent()

        assertEquals(OnboardingUiState(status = Status.READY, signedIn = true), viewModel.uiState.value)
    }

    @Test
    fun doubleTap_andTapDuringStartCheck_requestOnlyOnce() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onContinue() // still checking: ignored
        accounts.isSignedInAnswer.complete(false)
        runCurrent()

        viewModel.onContinue()
        runCurrent()
        viewModel.onContinue() // already signing in: ignored
        runCurrent()

        assertEquals(1, accounts.signInCalls)
    }
}
