package com.majidbahmani.cesto.feature.onboarding.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.ErrorReason
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.Status
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingViewModel
import com.majidbahmani.cesto.feature.onboarding.resources.Res
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_connect
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_connect_hint
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_description
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_error_failed
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_error_permission_denied
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_never_change
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_never_title
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_never_upload
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_reads_local
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_reads_receipts
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_reads_title
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_tagline
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_title
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_try_demo
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun OnboardingRoute(onConnect: () -> Unit, onTryDemo: () -> Unit, viewModel: OnboardingViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnConnect by rememberUpdatedState(onConnect)

    LaunchedEffect(uiState.status) {
        if (uiState.status == Status.CONNECTED) currentOnConnect()
    }

    OnboardingScreen(
        uiState = uiState,
        onConnectGmail = viewModel::onConnectGmail,
        onTryDemo = onTryDemo
    )
}

@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onConnectGmail: () -> Unit,
    onTryDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (uiState.status) {
            // Nothing to decide yet; also shown for the moment before navigating on.
            Status.CHECKING, Status.CONNECTED -> Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            Status.READY, Status.CONNECTING -> IntroContent(
                isConnecting = uiState.status == Status.CONNECTING,
                error = uiState.error,
                onConnectGmail = onConnectGmail,
                onTryDemo = onTryDemo
            )
        }
    }
}

@Composable
private fun IntroContent(isConnecting: Boolean, error: ErrorReason?, onConnectGmail: () -> Unit, onTryDemo: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.onboarding_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = stringResource(Res.string.onboarding_tagline), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(Res.string.onboarding_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrivacyCard(
                title = stringResource(Res.string.onboarding_reads_title),
                items = listOf(
                    stringResource(Res.string.onboarding_reads_receipts),
                    stringResource(Res.string.onboarding_reads_local)
                ),
                mark = "✓",
                markColor = MaterialTheme.colorScheme.primary
            )
            PrivacyCard(
                title = stringResource(Res.string.onboarding_never_title),
                items = listOf(
                    stringResource(Res.string.onboarding_never_change),
                    stringResource(Res.string.onboarding_never_upload)
                ),
                mark = "✕",
                markColor = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(8.dp))

            if (error != null) ErrorMessage(error)

            Button(
                onClick = onConnectGmail,
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.onboarding_connect))
                }
            }
            Text(
                text = stringResource(Res.string.onboarding_connect_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            OutlinedButton(
                onClick = onTryDemo,
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(Res.string.onboarding_try_demo))
            }
        }
    }
}

@Composable
private fun PrivacyCard(title: String, items: List<String>, mark: String, markColor: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = mark, color = markColor, fontWeight = FontWeight.Bold)
                    Text(text = item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(error: ErrorReason) {
    val text = when (error) {
        ErrorReason.PERMISSION_DENIED -> stringResource(Res.string.onboarding_error_permission_denied)
        ErrorReason.FAILED -> stringResource(Res.string.onboarding_error_failed)
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}

@Preview
@Composable
private fun OnboardingScreenPreview() {
    CestoTheme {
        OnboardingScreen(uiState = OnboardingUiState(status = Status.READY), onConnectGmail = {}, onTryDemo = {})
    }
}

@Preview
@Composable
private fun OnboardingScreenErrorDarkPreview() {
    CestoTheme(darkTheme = true) {
        OnboardingScreen(
            uiState = OnboardingUiState(status = Status.READY, error = ErrorReason.PERMISSION_DENIED),
            onConnectGmail = {},
            onTryDemo = {}
        )
    }
}
