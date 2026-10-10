package com.majidbahmani.cesto.feature.receipts.presentation.viewmodel

import com.majidbahmani.cesto.feature.receipts.domain.model.DailySpending
import com.majidbahmani.cesto.feature.receipts.domain.model.Receipt
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncFailure
import com.majidbahmani.cesto.feature.receipts.domain.model.SyncResult
import com.majidbahmani.cesto.feature.receipts.domain.repository.ReceiptRepository
import com.majidbahmani.cesto.feature.receipts.domain.usecase.ObserveReceiptsUseCase
import com.majidbahmani.cesto.feature.receipts.domain.usecase.SyncReceiptsUseCase
import com.majidbahmani.cesto.feature.receipts.presentation.viewmodel.ReceiptsUiState.SyncProblem
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
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptsViewModelTest {

    /** The database as a StateFlow; each sync waits until the test answers it. */
    private class FakeReceiptRepository : ReceiptRepository {
        val receipts = MutableStateFlow<List<Receipt>>(emptyList())
        var syncAnswer = CompletableDeferred<SyncResult>()
        var syncCalls = 0

        override fun observeReceipts() = receipts

        var lookBackMonths: Int? = null

        override suspend fun sync(lookBackMonths: Int): SyncResult {
            this.lookBackMonths = lookBackMonths
            syncCalls++
            return syncAnswer.await()
        }

        override suspend fun spendingBetween(fromMillis: Long, toMillis: Long) = DailySpending(totalCents = 0, receiptCount = 0)
    }

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeReceiptRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** A view model with its state collected, like the screen does (stateIn is lazy). */
    private fun TestScope.viewModel() = ReceiptsViewModel(
        observeReceipts = ObserveReceiptsUseCase(repository),
        syncReceipts = SyncReceiptsUseCase(repository),
        timeZone = TimeZone.UTC
    ).also {
        backgroundScope.launch { it.uiState.collect() }
        runCurrent()
    }

    private fun receipt(id: Long) = Receipt(id, "f$id.pdf", receivedAtMillis = id, status = ReceiptStatus.DOWNLOADED)

    @Test
    fun opening_syncsOnce_andShowsSavedReceiptsMeanwhile() = runTest(dispatcher) {
        repository.receipts.value = listOf(receipt(1))
        val viewModel = viewModel()

        val state = viewModel.uiState.value
        assertEquals(1, repository.syncCalls)
        assertEquals(3, repository.lookBackMonths) // the domain's rule, via the use case
        assertTrue(state.isSyncing)
        assertFalse(state.isLoading)
        assertEquals(listOf(1L), state.receipts.map { it.id })
    }

    @Test
    fun syncWritingToTheDatabase_updatesTheList() = runTest(dispatcher) {
        val viewModel = viewModel()

        repository.receipts.value = listOf(receipt(2), receipt(1))
        repository.syncAnswer.complete(SyncResult.Success(downloaded = 2))
        runCurrent()

        assertEquals(listOf(2L, 1L), viewModel.uiState.value.receipts.map { it.id })
        assertFalse(viewModel.uiState.value.isSyncing)
    }

    @Test
    fun failures_showReason_andRefreshClearsIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        repository.syncAnswer.complete(SyncResult.Failure(SyncFailure.NOT_AUTHORIZED))
        runCurrent()
        assertEquals(SyncProblem.NotAuthorized, viewModel.uiState.value.syncProblem)

        repository.syncAnswer = CompletableDeferred()
        viewModel.onRefresh()
        runCurrent()
        assertEquals(null, viewModel.uiState.value.syncProblem)
        assertTrue(viewModel.uiState.value.isSyncing)

        repository.syncAnswer.complete(SyncResult.Failure(SyncFailure.FAILED))
        runCurrent()
        assertEquals(SyncProblem.Failed, viewModel.uiState.value.syncProblem)
    }

    @Test
    fun partlyLoaded_showsHowManyAreMissing_completeShowsNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        repository.syncAnswer.complete(SyncResult.Success(downloaded = 3, incomplete = 2))
        runCurrent()
        assertEquals(SyncProblem.Incomplete(2), viewModel.uiState.value.syncProblem)

        repository.syncAnswer = CompletableDeferred()
        viewModel.onRefresh()
        repository.syncAnswer.complete(SyncResult.Success(downloaded = 2))
        runCurrent()
        assertEquals(null, viewModel.uiState.value.syncProblem)
    }

    @Test
    fun refreshWhileSyncing_doesNotStartASecondSync() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onRefresh()
        viewModel.onRefresh()
        runCurrent()

        assertEquals(1, repository.syncCalls)
    }
}
