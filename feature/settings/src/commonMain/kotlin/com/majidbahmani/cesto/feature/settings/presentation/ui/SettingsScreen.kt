package com.majidbahmani.cesto.feature.settings.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.cesto.feature.settings.domain.model.SaveKeyResult
import com.majidbahmani.cesto.feature.settings.presentation.mapper.AI_STUDIO_KEYS_URL
import com.majidbahmani.cesto.feature.settings.presentation.mapper.CLOUD_PROJECT_URL
import com.majidbahmani.cesto.feature.settings.presentation.mapper.looksLikeEmail
import com.majidbahmani.cesto.feature.settings.presentation.mapper.withGoogleAccount
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.SettingsUiState
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.SettingsViewModel
import com.majidbahmani.cesto.feature.settings.resources.Res
import com.majidbahmani.cesto.feature.settings.resources.settings_account_hint
import com.majidbahmani.cesto.feature.settings.resources.settings_account_invalid
import com.majidbahmani.cesto.feature.settings.resources.settings_account_label
import com.majidbahmani.cesto.feature.settings.resources.settings_cancel
import com.majidbahmani.cesto.feature.settings.resources.settings_change
import com.majidbahmani.cesto.feature.settings.resources.settings_gemini_intro
import com.majidbahmani.cesto.feature.settings.resources.settings_gemini_title
import com.majidbahmani.cesto.feature.settings.resources.settings_key_hide
import com.majidbahmani.cesto.feature.settings.resources.settings_key_label
import com.majidbahmani.cesto.feature.settings.resources.settings_key_show
import com.majidbahmani.cesto.feature.settings.resources.settings_remove
import com.majidbahmani.cesto.feature.settings.resources.settings_result_empty
import com.majidbahmani.cesto.feature.settings.resources.settings_result_failed
import com.majidbahmani.cesto.feature.settings.resources.settings_result_invalid
import com.majidbahmani.cesto.feature.settings.resources.settings_result_no_connection
import com.majidbahmani.cesto.feature.settings.resources.settings_result_not_allowed
import com.majidbahmani.cesto.feature.settings.resources.settings_result_saved
import com.majidbahmani.cesto.feature.settings.resources.settings_saved_key
import com.majidbahmani.cesto.feature.settings.resources.settings_saved_key_working
import com.majidbahmani.cesto.feature.settings.resources.settings_step1_button
import com.majidbahmani.cesto.feature.settings.resources.settings_step1_text
import com.majidbahmani.cesto.feature.settings.resources.settings_step1_title
import com.majidbahmani.cesto.feature.settings.resources.settings_step2_button
import com.majidbahmani.cesto.feature.settings.resources.settings_step2_text
import com.majidbahmani.cesto.feature.settings.resources.settings_step2_title
import com.majidbahmani.cesto.feature.settings.resources.settings_test_and_save
import com.majidbahmani.cesto.feature.settings.resources.settings_title
import com.majidbahmani.cesto.systemdesign.component.CestoScreenTitle
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsRoute(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    SettingsScreen(
        uiState = uiState,
        onOpenCloudConsole = { uriHandler.openUri(withGoogleAccount(CLOUD_PROJECT_URL, uiState.accountEmail)) },
        onOpenAiStudio = { uriHandler.openUri(withGoogleAccount(AI_STUDIO_KEYS_URL, uiState.accountEmail)) },
        onAccountEmailChange = viewModel::onAccountEmailChange,
        onKeyInputChange = viewModel::onKeyInputChange,
        onToggleKeyVisibility = viewModel::onToggleKeyVisibility,
        onTestAndSave = viewModel::onTestAndSave,
        onChangeKey = viewModel::onChangeKey,
        onCancelChange = viewModel::onCancelChange,
        onRemoveKey = viewModel::onRemoveKey,
        contentPadding = contentPadding,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onOpenCloudConsole: () -> Unit,
    onOpenAiStudio: () -> Unit,
    onAccountEmailChange: (String) -> Unit,
    onKeyInputChange: (String) -> Unit,
    onToggleKeyVisibility: () -> Unit,
    onTestAndSave: () -> Unit,
    onChangeKey: () -> Unit,
    onCancelChange: () -> Unit,
    onRemoveKey: () -> Unit,
    modifier: Modifier = Modifier,
    /** Space the floating bottom bar covers. */
    contentPadding: PaddingValues = PaddingValues(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp + contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CestoScreenTitle(stringResource(Res.string.settings_title))

            Text(stringResource(Res.string.settings_gemini_title), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(Res.string.settings_gemini_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                uiState.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                !uiState.isEditing && uiState.savedKeyMasked != null -> SavedKeyCard(
                    masked = uiState.savedKeyMasked,
                    onChangeKey = onChangeKey,
                    onRemoveKey = onRemoveKey,
                )
                else -> {
                    AccountField(email = uiState.accountEmail, onEmailChange = onAccountEmailChange)
                    StepCard(
                        number = 1,
                        title = Res.string.settings_step1_title,
                        text = Res.string.settings_step1_text,
                        button = Res.string.settings_step1_button,
                        onClick = onOpenCloudConsole,
                    )
                    StepCard(
                        number = 2,
                        title = Res.string.settings_step2_title,
                        text = Res.string.settings_step2_text,
                        button = Res.string.settings_step2_button,
                        onClick = onOpenAiStudio,
                    )
                    KeyEntry(
                        uiState = uiState,
                        onKeyInputChange = onKeyInputChange,
                        onToggleKeyVisibility = onToggleKeyVisibility,
                        onTestAndSave = onTestAndSave,
                        onCancelChange = onCancelChange,
                    )
                }
            }

            uiState.feedback?.let { Feedback(it) }
        }
    }
}

@Composable
private fun AccountField(email: String, onEmailChange: (String) -> Unit) {
    val invalid = email.isNotBlank() && !looksLikeEmail(email)
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(Res.string.settings_account_label)) },
        placeholder = { Text("name@gmail.com") },
        singleLine = true,
        isError = invalid,
        supportingText = {
            Text(stringResource(if (invalid) Res.string.settings_account_invalid else Res.string.settings_account_hint))
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StepCard(number: Int, title: StringResource, text: StringResource, button: StringResource, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = number.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onClick) { Text(stringResource(button)) }
        }
    }
}

@Composable
private fun KeyEntry(
    uiState: SettingsUiState,
    onKeyInputChange: (String) -> Unit,
    onToggleKeyVisibility: () -> Unit,
    onTestAndSave: () -> Unit,
    onCancelChange: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = uiState.keyInput,
            onValueChange = onKeyInputChange,
            label = { Text(stringResource(Res.string.settings_key_label)) },
            singleLine = true,
            enabled = !uiState.isTesting,
            visualTransformation = if (uiState.isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            trailingIcon = {
                TextButton(onClick = onToggleKeyVisibility) {
                    Text(stringResource(if (uiState.isKeyVisible) Res.string.settings_key_hide else Res.string.settings_key_show))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onTestAndSave, enabled = !uiState.isTesting, modifier = Modifier.height(48.dp)) {
                if (uiState.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.settings_test_and_save))
                }
            }
            // Only when replacing a saved key: without one there's nothing to go back to.
            if (uiState.savedKeyMasked != null) {
                TextButton(onClick = onCancelChange, enabled = !uiState.isTesting) {
                    Text(stringResource(Res.string.settings_cancel))
                }
            }
        }
    }
}

@Composable
private fun SavedKeyCard(masked: String, onChangeKey: () -> Unit, onRemoveKey: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.settings_saved_key), style = MaterialTheme.typography.titleMedium)
            Text(masked, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(Res.string.settings_saved_key_working),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChangeKey) { Text(stringResource(Res.string.settings_change)) }
                TextButton(onClick = onRemoveKey) {
                    Text(stringResource(Res.string.settings_remove), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun Feedback(result: SaveKeyResult) {
    val isSuccess = result == SaveKeyResult.SAVED
    val text = when (result) {
        SaveKeyResult.SAVED -> Res.string.settings_result_saved
        SaveKeyResult.EMPTY -> Res.string.settings_result_empty
        SaveKeyResult.INVALID_KEY -> Res.string.settings_result_invalid
        SaveKeyResult.NOT_ALLOWED -> Res.string.settings_result_not_allowed
        SaveKeyResult.NO_CONNECTION -> Res.string.settings_result_no_connection
        SaveKeyResult.FAILED -> Res.string.settings_result_failed
    }
    Surface(
        color = if (isSuccess) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (isSuccess) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}

@Preview
@Composable
private fun SettingsSetupPreview() {
    CestoTheme {
        SettingsScreen(
            uiState = SettingsUiState(isLoading = false, isEditing = true, keyInput = "AIzaSy…", feedback = SaveKeyResult.INVALID_KEY),
            onOpenCloudConsole = {}, onOpenAiStudio = {}, onAccountEmailChange = {}, onKeyInputChange = {}, onToggleKeyVisibility = {},
            onTestAndSave = {}, onChangeKey = {}, onCancelChange = {}, onRemoveKey = {},
        )
    }
}

@Preview
@Composable
private fun SettingsSavedDarkPreview() {
    CestoTheme(darkTheme = true) {
        SettingsScreen(
            uiState = SettingsUiState(isLoading = false, savedKeyMasked = "AIza…x9Q2", feedback = SaveKeyResult.SAVED),
            onOpenCloudConsole = {}, onOpenAiStudio = {}, onAccountEmailChange = {}, onKeyInputChange = {}, onToggleKeyVisibility = {},
            onTestAndSave = {}, onChangeKey = {}, onCancelChange = {}, onRemoveKey = {},
        )
    }
}
