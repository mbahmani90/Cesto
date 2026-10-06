package com.majidbahmani.cesto.feature.receipts.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.majidbahmani.cesto.feature.receipts.presentation.ui.DemoReceiptsScreen
import com.majidbahmani.cesto.feature.receipts.presentation.ui.ReceiptsRoute as ReceiptsRouteContent
import kotlinx.serialization.Serializable

/** @param demo true: sample receipts instead of Gmail ("Try demo"). */
@Serializable
data class ReceiptsRoute(val demo: Boolean = false)

/** @param contentPadding space covered by :app's floating bottom bar. */
fun NavGraphBuilder.receiptsScreen(contentPadding: PaddingValues = PaddingValues()) {
    composable<ReceiptsRoute> { entry ->
        if (entry.toRoute<ReceiptsRoute>().demo) DemoReceiptsScreen() else ReceiptsRouteContent(contentPadding)
    }
}
