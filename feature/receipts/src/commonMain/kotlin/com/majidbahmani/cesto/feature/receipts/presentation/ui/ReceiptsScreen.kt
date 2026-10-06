package com.majidbahmani.cesto.feature.receipts.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.majidbahmani.cesto.feature.receipts.resources.Res
import com.majidbahmani.cesto.feature.receipts.resources.receipts_connected
import com.majidbahmani.cesto.feature.receipts.resources.receipts_demo
import com.majidbahmani.cesto.feature.receipts.resources.receipts_title
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.stringResource

/** Placeholder until the Gmail search and receipt list are built. */
@Composable
internal fun ReceiptsScreen(
    demo: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(Res.string.receipts_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(if (demo) Res.string.receipts_demo else Res.string.receipts_connected),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun ReceiptsScreenPreview() {
    CestoTheme { ReceiptsScreen(demo = false) }
}
