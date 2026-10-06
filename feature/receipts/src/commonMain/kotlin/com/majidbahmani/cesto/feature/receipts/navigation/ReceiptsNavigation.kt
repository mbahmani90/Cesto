package com.majidbahmani.cesto.feature.receipts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.majidbahmani.cesto.feature.receipts.presentation.ui.ReceiptsScreen
import kotlinx.serialization.Serializable

/** @param demo true: sample receipts instead of Gmail ("Try demo"). */
@Serializable
data class ReceiptsRoute(val demo: Boolean = false)

fun NavGraphBuilder.receiptsScreen() {
    composable<ReceiptsRoute> { entry ->
        ReceiptsScreen(demo = entry.toRoute<ReceiptsRoute>().demo)
    }
}
