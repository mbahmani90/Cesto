package com.majidbahmani.cesto.feature.settings.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.cesto.feature.settings.domain.model.AiModel
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.ModelUiState
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.ModelViewModel
import com.majidbahmani.cesto.feature.settings.resources.Res
import com.majidbahmani.cesto.feature.settings.resources.settings_cancel
import com.majidbahmani.cesto.feature.settings.resources.settings_model_change
import com.majidbahmani.cesto.feature.settings.resources.settings_model_flash_2_5
import com.majidbahmani.cesto.feature.settings.resources.settings_model_flash_2_5_text
import com.majidbahmani.cesto.feature.settings.resources.settings_model_flash_3_8
import com.majidbahmani.cesto.feature.settings.resources.settings_model_flash_3_8_text
import com.majidbahmani.cesto.feature.settings.resources.settings_model_flash_lite_2_5
import com.majidbahmani.cesto.feature.settings.resources.settings_model_flash_lite_2_5_text
import com.majidbahmani.cesto.feature.settings.resources.settings_model_flash_lite_3_5
import com.majidbahmani.cesto.feature.settings.resources.settings_model_flash_lite_3_5_text
import com.majidbahmani.cesto.feature.settings.resources.settings_model_title
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The Gemini model for chat and receipt extraction: a card with the current one, a dialog to change it. */
@Composable
internal fun ModelSectionRoute(viewModel: ModelViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModelSection(
        uiState = uiState,
        onOpenPicker = viewModel::onOpenPicker,
        onDismissPicker = viewModel::onDismissPicker,
        onSelect = viewModel::onSelect
    )
}

@Composable
internal fun ModelSection(
    uiState: ModelUiState,
    onOpenPicker: () -> Unit,
    onDismissPicker: () -> Unit,
    onSelect: (AiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = uiState.selected ?: return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onOpenPicker).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(selected.title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(selected.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenPicker) { Text(stringResource(Res.string.settings_model_change)) }
            }
        }

        if (uiState.isPicking) {
            ModelPicker(selected = selected, onSelect = onSelect, onDismiss = onDismissPicker)
        }
    }
}

@Composable
private fun ModelPicker(selected: AiModel, onSelect: (AiModel) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_model_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AiModel.entries.forEach { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // The whole row is the touch target; the radio button only shows the state.
                            .selectable(selected = model == selected, role = Role.RadioButton, onClick = { onSelect(model) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = model == selected, onClick = null)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(stringResource(model.title), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(model.description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.settings_cancel)) } }
    )
}

private val AiModel.title: StringResource
    get() = when (this) {
        AiModel.FLASH_3_8 -> Res.string.settings_model_flash_3_8
        AiModel.FLASH_LITE_3_5 -> Res.string.settings_model_flash_lite_3_5
        AiModel.FLASH_2_5 -> Res.string.settings_model_flash_2_5
        AiModel.FLASH_LITE_2_5 -> Res.string.settings_model_flash_lite_2_5
    }

private val AiModel.description: StringResource
    get() = when (this) {
        AiModel.FLASH_3_8 -> Res.string.settings_model_flash_3_8_text
        AiModel.FLASH_LITE_3_5 -> Res.string.settings_model_flash_lite_3_5_text
        AiModel.FLASH_2_5 -> Res.string.settings_model_flash_2_5_text
        AiModel.FLASH_LITE_2_5 -> Res.string.settings_model_flash_lite_2_5_text
    }

@Preview
@Composable
private fun ModelSectionPreview() {
    CestoTheme {
        Surface {
            ModelSection(ModelUiState(selected = AiModel.FLASH_LITE_2_5), {}, {}, {}, Modifier.padding(24.dp))
        }
    }
}

@Preview
@Composable
private fun ModelPickerPreview() {
    CestoTheme {
        ModelPicker(selected = AiModel.FLASH_LITE_2_5, onSelect = {}, onDismiss = {})
    }
}
