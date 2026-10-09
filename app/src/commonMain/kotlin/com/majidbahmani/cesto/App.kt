package com.majidbahmani.cesto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.majidbahmani.cesto.feature.chat.navigation.ChatRoute
import com.majidbahmani.cesto.feature.chat.navigation.chatScreen
import com.majidbahmani.cesto.feature.onboarding.navigation.OnboardingRoute
import com.majidbahmani.cesto.feature.onboarding.navigation.onboardingScreen
import com.majidbahmani.cesto.feature.receipts.navigation.ReceiptsRoute
import com.majidbahmani.cesto.feature.receipts.navigation.receiptsScreen
import com.majidbahmani.cesto.feature.settings.navigation.settingsScreen
import com.majidbahmani.cesto.navigation.TopLevelDestination
import com.majidbahmani.cesto.systemdesign.theme.CestoTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Composition root: the theme, the bottom bar and the navigation graph; features only know their own routes. */
@Composable
fun App(modifier: Modifier = Modifier) {
    CestoTheme {
        val navController = rememberNavController()
        val currentEntry by navController.currentBackStackEntryAsState()
        val currentDestination = currentEntry?.destination
        // Onboarding and demo mode (no data yet) are outside the tabs.
        val isDemo = currentDestination?.hasRoute(ReceiptsRoute::class) == true &&
            currentEntry?.toRoute<ReceiptsRoute>()?.demo == true
        val showBottomBar = !isDemo && TopLevelDestination.entries.any { currentDestination.isInTab(it) }

        Scaffold(
            modifier = modifier,
            bottomBar = {
                if (showBottomBar) FloatingPillBar(navController, currentDestination)
            },
            // Screens handle the system bars themselves; tabs draw behind the floating pill.
            contentWindowInsets = WindowInsets(0)
        ) { innerPadding ->
            // How much the pill covers (incl. the system navigation bar); tabs use it for their last item.
            val contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            NavHost(navController = navController, startDestination = OnboardingRoute) {
                onboardingScreen(
                    // Connected: onboarding is done; Ask becomes the root, so back from it closes the app.
                    onConnect = {
                        navController.navigate(TopLevelDestination.START.route) {
                            popUpTo<OnboardingRoute> { inclusive = true }
                        }
                    }
                )
                chatScreen(onOpenSettings = { navController.navigateToTab(TopLevelDestination.SETTINGS) }, contentPadding = contentPadding)
                receiptsScreen(contentPadding)
                settingsScreen(
                    // Signed out: back to onboarding, with nothing behind it.
                    onSignedOut = {
                        navController.navigate(OnboardingRoute) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    contentPadding = contentPadding
                )
            }
        }
    }
}

/** Floating pill (doc 35, style B): rounded, translucent, away from the screen edges. */
@Composable
private fun FloatingPillBar(navController: NavHostController, currentDestination: NavDestination?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars) // above the gesture bar / 3 buttons
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            // Alpha on the color, not Modifier.alpha: icons and labels stay fully opaque.
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.selectableGroup().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TopLevelDestination.entries.forEach { destination ->
                    BottomBarItem(
                        destination = destination,
                        // Derived from the back stack, not remembered: stays right after back, deep links, process death.
                        selected = currentDestination.isInTab(destination),
                        onClick = { navController.navigateToTab(destination) }
                    )
                }
            }
        }
    }
}

/** Compact item: Material's NavigationBarItem has a minimum 80 dp bar height that can't shrink. */
@Composable
private fun BottomBarItem(destination: TopLevelDestination, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(CircleShape) // ripple follows the pill
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 26.dp)
                .background(if (selected) colors.secondaryContainer else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(destination.icon),
                contentDescription = null, // the label names the tab; no double reading
                modifier = Modifier.size(18.dp),
                tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(destination.label),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant
        )
    }
}

private fun NavDestination?.isInTab(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

/** Each tab keeps its state (scroll, typed text) when you switch; the back stack stays [Ask, tab]. */
private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo<ChatRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
