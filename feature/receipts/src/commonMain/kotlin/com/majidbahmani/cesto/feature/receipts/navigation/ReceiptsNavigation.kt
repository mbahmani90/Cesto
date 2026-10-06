package com.majidbahmani.cesto.feature.receipts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.majidbahmani.cesto.feature.receipts.presentation.ui.DemoReceiptsScreen
import com.majidbahmani.cesto.feature.receipts.presentation.ui.ReceiptsRoute as ReceiptsRouteContent
import kotlinx.serialization.Serializable

/** @param demo true: sample receipts instead of Gmail ("Try demo"). */
@Serializable
data class ReceiptsRoute(val demo: Boolean = false)

fun NavGraphBuilder.receiptsScreen() {
    composable<ReceiptsRoute> { entry ->
        if (entry.toRoute<ReceiptsRoute>().demo) DemoReceiptsScreen() else ReceiptsRouteContent()
    }
}
