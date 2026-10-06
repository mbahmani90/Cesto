package com.majidbahmani.cesto.feature.chat.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.majidbahmani.cesto.feature.chat.presentation.ui.ChatScreen
import kotlinx.serialization.Serializable

@Serializable
data object ChatRoute

/** The feature's only entry point; :app puts it in the bottom bar. [contentPadding]: space the bar covers. */
fun NavGraphBuilder.chatScreen(contentPadding: PaddingValues = PaddingValues()) {
    composable<ChatRoute> { ChatScreen(contentPadding = contentPadding) }
}
