package com.majidbahmani.cesto.feature.onboarding.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.ErrorReason
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingUiState.Status
import com.majidbahmani.cesto.feature.onboarding.presentation.viewmodel.OnboardingViewModel
import com.majidbahmani.cesto.feature.onboarding.resources.Res
import com.majidbahmani.cesto.feature.onboarding.resources.cesto_logo
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_connect
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_continue_google
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_error_failed
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_error_no_google_account
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_error_permission_denied
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_error_sign_in_rejected
import com.majidbahmani.cesto.feature.onboarding.resources.onboarding_title
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun OnboardingRoute(onConnect: () -> Unit, viewModel: OnboardingViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnConnect by rememberUpdatedState(onConnect)

    LaunchedEffect(uiState.status) {
        if (uiState.status == Status.CONNECTED) currentOnConnect()
    }

    OnboardingScreen(uiState = uiState, onContinue = viewModel::onContinue)
}

@Composable
internal fun OnboardingScreen(uiState: OnboardingUiState, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (uiState.status) {
            // Nothing to decide yet; also shown for the moment before navigating on.
            Status.CHECKING, Status.CONNECTED -> Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            Status.READY, Status.SIGNING_IN, Status.CONNECTING -> IntroContent(
                isBusy = uiState.status != Status.READY,
                signedIn = uiState.signedIn,
                error = uiState.error,
                onContinue = onContinue
            )
        }
    }
}

/** The name at the top, then one card in the middle: logo and the button. What Cesto may read is shown by Google's own consent dialog. */
@Composable
private fun IntroContent(isBusy: Boolean, signedIn: Boolean, error: ErrorReason?, onContinue: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()) // small screens, large fonts, a long error message
                .heightIn(min = maxHeight) // at least the screen, so the name stays at the top and the card in the middle
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.onboarding_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary
            )
            val cardShape = MaterialTheme.shapes.extraLarge
            val glow = MaterialTheme.colorScheme.primary
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    // A green shadow instead of the Card's own black one, which can't be seen on the dark background.
                    .shadow(elevation = 12.dp, shape = cardShape, ambientColor = glow, spotColor = glow)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The app icon's drawing; the name above says it, so no description for screen readers.
                    Image(painter = painterResource(Res.drawable.cesto_logo), contentDescription = null, modifier = Modifier.size(96.dp))
                    if (error != null) ErrorMessage(error)
                    Button(
                        onClick = onContinue,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(if (signedIn) Res.string.onboarding_connect else Res.string.onboarding_continue_google))
                        }
                    }
                }
            }
            // Empty last child: with SpaceBetween it keeps the card centred in the space below the name.
            Spacer(Modifier)
        }
    }
}

@Composable
private fun ErrorMessage(error: ErrorReason) {
    val text = when (error) {
        ErrorReason.NO_GOOGLE_ACCOUNT -> stringResource(Res.string.onboarding_error_no_google_account)
        ErrorReason.SIGN_IN_REJECTED -> stringResource(Res.string.onboarding_error_sign_in_rejected)
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
        OnboardingScreen(uiState = OnboardingUiState(status = Status.READY), onContinue = {})
    }
}

@Preview
@Composable
private fun OnboardingScreenErrorDarkPreview() {
    CestoTheme(darkTheme = true) {
        OnboardingScreen(
            uiState = OnboardingUiState(status = Status.READY, signedIn = true, error = ErrorReason.PERMISSION_DENIED),
            onContinue = {}
        )
    }
}
