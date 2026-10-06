package com.majidbahmani.cesto.feature.receipts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.cesto.feature.receipts.domain.model.ReceiptStatus
import com.majidbahmani.cesto.feature.receipts.presentation.model.ReceiptItemUi
import com.majidbahmani.cesto.feature.receipts.presentation.viewmodel.ReceiptsUiState
import com.majidbahmani.cesto.feature.receipts.presentation.viewmodel.ReceiptsUiState.SyncProblem
import com.majidbahmani.cesto.feature.receipts.presentation.viewmodel.ReceiptsViewModel
import com.majidbahmani.cesto.feature.receipts.resources.Res
import com.majidbahmani.cesto.feature.receipts.resources.receipts_demo
import com.majidbahmani.cesto.feature.receipts.resources.receipts_empty
import com.majidbahmani.cesto.feature.receipts.resources.receipts_error_failed
import com.majidbahmani.cesto.feature.receipts.resources.receipts_error_incomplete
import com.majidbahmani.cesto.feature.receipts.resources.receipts_error_not_authorized
import com.majidbahmani.cesto.feature.receipts.resources.receipts_retry
import com.majidbahmani.cesto.feature.receipts.resources.receipts_status_downloaded
import com.majidbahmani.cesto.feature.receipts.resources.receipts_status_failed
import com.majidbahmani.cesto.feature.receipts.resources.receipts_status_found
import com.majidbahmani.cesto.feature.receipts.resources.receipts_status_ready
import com.majidbahmani.cesto.feature.receipts.resources.receipts_syncing
import com.majidbahmani.cesto.feature.receipts.resources.receipts_title
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ReceiptsRoute(viewModel: ReceiptsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReceiptsScreen(uiState = uiState, onRefresh = viewModel::onRefresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceiptsScreen(
    uiState: ReceiptsUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            Text(
                text = stringResource(Res.string.receipts_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
            )
            uiState.syncProblem?.let { SyncProblemBanner(it, onRetry = onRefresh) }

            PullToRefreshBox(
                isRefreshing = uiState.isSyncing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    uiState.receipts.isEmpty() -> EmptyContent(isSyncing = uiState.isSyncing)
                    else -> ReceiptList(uiState.receipts)
                }
            }
        }
    }
}

@Composable
private fun ReceiptList(receipts: List<ReceiptItemUi>) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = receipts, key = { it.id }) { receipt -> ReceiptRow(receipt) }
    }
}

@Composable
private fun ReceiptRow(receipt: ReceiptItemUi) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = receipt.dateText, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = receipt.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(receipt.status)
        }
    }
}

@Composable
private fun StatusChip(status: ReceiptStatus) {
    val colors = MaterialTheme.colorScheme
    val (text, container, content) = when (status) {
        ReceiptStatus.FOUND -> Triple(stringResource(Res.string.receipts_status_found), colors.surfaceVariant, colors.onSurfaceVariant)
        ReceiptStatus.DOWNLOADED -> Triple(stringResource(Res.string.receipts_status_downloaded), colors.secondaryContainer, colors.onSecondaryContainer)
        ReceiptStatus.READY -> Triple(stringResource(Res.string.receipts_status_ready), colors.primary, colors.onPrimary)
        ReceiptStatus.FAILED -> Triple(stringResource(Res.string.receipts_status_failed), colors.errorContainer, colors.onErrorContainer)
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun EmptyContent(isSyncing: Boolean) {
    // Scrollable so pull-to-refresh also works on an empty list.
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            Text(
                text = stringResource(if (isSyncing) Res.string.receipts_syncing else Res.string.receipts_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncProblemBanner(problem: SyncProblem, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (problem) {
                    SyncProblem.NotAuthorized -> stringResource(Res.string.receipts_error_not_authorized)
                    SyncProblem.Failed -> stringResource(Res.string.receipts_error_failed)
                    is SyncProblem.Incomplete ->
                        pluralStringResource(Res.plurals.receipts_error_incomplete, problem.count, problem.count)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            )
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.receipts_retry)) }
        }
    }
}

/** "Try demo": sample receipts come with demo mode later. */
@Composable
internal fun DemoReceiptsScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(Res.string.receipts_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(Res.string.receipts_demo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val previewReceipts = listOf(
    ReceiptItemUi(3, "05/10/2026 21:22", "Fatura_Cartao_Continente_20261005_2122.pdf", ReceiptStatus.DOWNLOADED),
    ReceiptItemUi(2, "28/09/2026 18:04", "Fatura_Cartao_Continente_20260928_1804.pdf", ReceiptStatus.FOUND),
    ReceiptItemUi(1, "21/09/2026 10:47", "Fatura_Cartao_Continente_20260921_1047.pdf", ReceiptStatus.FAILED),
)

@Preview
@Composable
private fun ReceiptsScreenPreview() {
    CestoTheme {
        ReceiptsScreen(uiState = ReceiptsUiState(isLoading = false, receipts = previewReceipts), onRefresh = {})
    }
}

@Preview
@Composable
private fun ReceiptsScreenErrorDarkPreview() {
    CestoTheme(darkTheme = true) {
        ReceiptsScreen(
            uiState = ReceiptsUiState(isLoading = false, receipts = previewReceipts, syncProblem = SyncProblem.Incomplete(2)),
            onRefresh = {},
        )
    }
}
