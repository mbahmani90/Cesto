package com.majidbahmani.cesto.feature.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.majidbahmani.cesto.feature.onboarding.presentation.ui.OnboardingRoute
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

/** The feature's only entry point; where to go next is decided by :app through the lambdas. */
fun NavGraphBuilder.onboardingScreen(onConnect: () -> Unit, onTryDemo: () -> Unit) {
    composable<OnboardingRoute> {
        OnboardingRoute(onConnect = onConnect, onTryDemo = onTryDemo)
    }
}
