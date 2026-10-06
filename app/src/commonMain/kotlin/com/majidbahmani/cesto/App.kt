package com.majidbahmani.cesto

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.majidbahmani.cesto.feature.onboarding.navigation.OnboardingRoute
import com.majidbahmani.cesto.feature.onboarding.navigation.onboardingScreen
import com.majidbahmani.cesto.feature.receipts.navigation.ReceiptsRoute
import com.majidbahmani.cesto.feature.receipts.navigation.receiptsScreen
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme

/** Composition root: the theme and the navigation graph; features only know their own routes. */
@Composable
fun App() {
    CestoTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = OnboardingRoute) {
            onboardingScreen(
                // Connected: onboarding is done, so back from receipts closes the app.
                onConnected = {
                    navController.navigate(ReceiptsRoute(demo = false)) {
                        popUpTo<OnboardingRoute> { inclusive = true }
                    }
                },
                // Demo: back returns to onboarding to connect Gmail for real.
                onTryDemo = { navController.navigate(ReceiptsRoute(demo = true)) },
            )
            receiptsScreen()
        }
    }
}
