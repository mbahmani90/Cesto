package com.majidbahmani.cesto.feature.settings.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.AccountUiState
import com.majidbahmani.cesto.feature.settings.presentation.viewmodel.AccountViewModel
import com.majidbahmani.cesto.feature.settings.resources.Res
import com.majidbahmani.cesto.feature.settings.resources.settings_account_signed_in_as
import com.majidbahmani.cesto.feature.settings.resources.settings_account_title
import com.majidbahmani.cesto.feature.settings.resources.settings_cancel
import com.majidbahmani.cesto.feature.settings.resources.settings_sign_out
import com.majidbahmani.cesto.feature.settings.resources.settings_sign_out_confirm_text
import com.majidbahmani.cesto.feature.settings.resources.settings_sign_out_confirm_title
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The signed-in account and "Sign out"; calls [onSignedOut] once no account is signed in. */
@Composable
internal fun AccountSectionRoute(onSignedOut: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSignedOut by rememberUpdatedState(onSignedOut)

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) currentOnSignedOut()
    }

    AccountSection(
        uiState = uiState,
        onSignOut = viewModel::onSignOut,
        onDismissSignOut = viewModel::onDismissSignOut,
        onConfirmSignOut = viewModel::onConfirmSignOut
    )
}

@Composable
internal fun AccountSection(
    uiState: AccountUiState,
    onSignOut: () -> Unit,
    onDismissSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val email = uiState.email ?: return
    // Same spacing as the rest of Settings, so the section reads as part of the screen.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(Res.string.settings_account_title), style = MaterialTheme.typography.titleLarge)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.settings_account_signed_in_as),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(email, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onSignOut, enabled = !uiState.isSigningOut) {
                    if (uiState.isSigningOut) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.settings_sign_out), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (uiState.isConfirmingSignOut) {
            AlertDialog(
                onDismissRequest = onDismissSignOut,
                title = { Text(stringResource(Res.string.settings_sign_out_confirm_title)) },
                text = { Text(stringResource(Res.string.settings_sign_out_confirm_text)) },
                confirmButton = {
                    TextButton(onClick = onConfirmSignOut) {
                        Text(stringResource(Res.string.settings_sign_out), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissSignOut) { Text(stringResource(Res.string.settings_cancel)) }
                }
            )
        }
    }
}

@Preview
@Composable
private fun AccountSectionPreview() {
    CestoTheme {
        Surface {
            AccountSection(AccountUiState(isLoading = false, email = "ana@example.com"), {}, {}, {}, Modifier.padding(24.dp))
        }
    }
}
