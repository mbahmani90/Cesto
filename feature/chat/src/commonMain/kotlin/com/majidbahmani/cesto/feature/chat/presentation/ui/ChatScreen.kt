package com.majidbahmani.cesto.feature.chat.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
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
import com.majidbahmani.cesto.feature.chat.resources.Res
import com.majidbahmani.cesto.feature.chat.resources.chat_placeholder
import com.majidbahmani.cesto.feature.chat.resources.chat_title
import com.majidbahmani.cesto.systemdesign.component.CestoScreenTitle
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.stringResource

/** Placeholder until this tab is built. */
@Composable
internal fun ChatScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp + contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CestoScreenTitle(stringResource(Res.string.chat_title))
            Text(
                text = stringResource(Res.string.chat_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun ChatScreenPreview() {
    CestoTheme { ChatScreen() }
}
