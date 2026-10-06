package com.majidbahmani.cesto.feature.chat.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.majidbahmani.cesto.feature.chat.presentation.ui.ChatRoute as ChatRouteContent
import kotlinx.serialization.Serializable

@Serializable
data object ChatRoute

/**
 * The feature's only entry point; :app puts it in the bottom bar. [contentPadding]: space the bar covers.
 * [onOpenSettings]: where the Gemini key is set up (the feature doesn't know the Settings route).
 */
fun NavGraphBuilder.chatScreen(onOpenSettings: () -> Unit, contentPadding: PaddingValues = PaddingValues()) {
    composable<ChatRoute> { ChatRouteContent(onOpenSettings = onOpenSettings, contentPadding = contentPadding) }
}
