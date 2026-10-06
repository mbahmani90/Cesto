package com.majidbahmani.cesto.feature.settings.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.majidbahmani.cesto.feature.settings.presentation.ui.SettingsRoute as SettingsRouteContent
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

/** The feature's only entry point; :app puts it in the bottom bar. [contentPadding]: space the bar covers. */
fun NavGraphBuilder.settingsScreen(contentPadding: PaddingValues = PaddingValues()) {
    composable<SettingsRoute> { SettingsRouteContent(contentPadding = contentPadding) }
}
