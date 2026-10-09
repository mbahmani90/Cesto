package com.majidbahmani.cesto.feature.settings.presentation.viewmodel

import com.majidbahmani.cesto.feature.settings.domain.repository.AccountSessionRepository
import com.majidbahmani.cesto.feature.settings.domain.usecase.ObserveSignedInAccountUseCase
import com.majidbahmani.cesto.feature.settings.domain.usecase.SignOutUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    /** Sign-out waits for [signOutAnswer], then clears the account. */
    private class FakeAccountSessionRepository : AccountSessionRepository {
        val email = MutableStateFlow<String?>("ana@example.com")
        val signOutAnswer = CompletableDeferred<Unit>()
        var signOutCalls = 0

        override fun observeSignedInEmail() = email

        override suspend fun signOut() {
            signOutCalls++
            signOutAnswer.await()
            email.value = null
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeAccountSessionRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = AccountViewModel(ObserveSignedInAccountUseCase(repository), SignOutUseCase(repository)).also {
        backgroundScope.launch { it.uiState.collect() }
        runCurrent()
    }

    @Test
    fun showsTheSignedInEmail() = runTest(dispatcher) {
        val state = viewModel().uiState.value

        assertEquals("ana@example.com", state.email)
        assertFalse(state.isSignedOut)
    }

    @Test
    fun signOut_asksFirst_cancelKeepsTheAccount() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onSignOut()
        runCurrent()
        assertTrue(viewModel.uiState.value.isConfirmingSignOut)

        viewModel.onDismissSignOut()
        runCurrent()
        assertFalse(viewModel.uiState.value.isConfirmingSignOut)
        assertEquals(0, repository.signOutCalls)
    }

    @Test
    fun confirm_signsOutOnce_thenSignedOut() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onSignOut()
        viewModel.onConfirmSignOut()
        viewModel.onConfirmSignOut() // double tap: ignored
        runCurrent()
        assertTrue(viewModel.uiState.value.isSigningOut)
        assertFalse(viewModel.uiState.value.isConfirmingSignOut)

        repository.signOutAnswer.complete(Unit)
        runCurrent()
        assertTrue(viewModel.uiState.value.isSignedOut)
        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun loading_isNotSignedOut() {
        assertFalse(AccountUiState().isSignedOut) // no navigation before the store answered
    }
}
