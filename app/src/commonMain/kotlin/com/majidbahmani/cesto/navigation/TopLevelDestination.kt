package com.majidbahmani.cesto.navigation

import com.majidbahmani.cesto.feature.chat.navigation.ChatRoute
import com.majidbahmani.cesto.feature.receipts.navigation.ReceiptsRoute
import com.majidbahmani.cesto.feature.settings.navigation.SettingsRoute
import com.majidbahmani.cesto.resources.Res
import com.majidbahmani.cesto.resources.ic_ask
import com.majidbahmani.cesto.resources.ic_receipts
import com.majidbahmani.cesto.resources.ic_settings
import com.majidbahmani.cesto.resources.nav_ask
import com.majidbahmani.cesto.resources.nav_receipts
import com.majidbahmani.cesto.resources.nav_settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import kotlin.reflect.KClass

/** The bottom bar's tabs; the order of the entries is the order in the bar. Only :app knows all tabs. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: StringResource,
    val icon: DrawableResource,
) {
    ASK(ChatRoute, ChatRoute::class, Res.string.nav_ask, Res.drawable.ic_ask),
    RECEIPTS(ReceiptsRoute(demo = false), ReceiptsRoute::class, Res.string.nav_receipts, Res.drawable.ic_receipts),
    SETTINGS(SettingsRoute, SettingsRoute::class, Res.string.nav_settings, Res.drawable.ic_settings),
    ;

    companion object {
        /** Opened after onboarding; back from another tab returns here, back again closes the app. */
        val START = ASK
    }
}
