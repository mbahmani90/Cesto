package com.majidbahmani.cesto.systemdesign.theme

import androidx.compose.ui.graphics.Color

// The only place with hex values. `internal`: features use MaterialTheme.colorScheme roles instead.

/** Light: fresh grocery green as the brand accent, neutral (untinted) greys for surfaces. */
internal object CestoColors {
    val Green = Color(0xFF1E7A4F) // brand: primary actions
    val GreenContainer = Color(0xFFCDEFD9)
    val OnGreenContainer = Color(0xFF00210F)
    val Ink = Color(0xFF1C1C1C) // near-black text
    val Surface = Color(0xFFFFFFFF)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF7F7F7)
    val SurfaceContainer = Color(0xFFF2F2F2)
    val SurfaceContainerHigh = Color(0xFFECECEC)
    val SurfaceContainerHighest = Color(0xFFE6E6E6)
    val SurfaceVariant = Color(0xFFE6E6E6)
    val OnSurfaceVariant = Color(0xFF4A4A4A)
    val SurfaceDim = Color(0xFFDADADA)
    val Outline = Color(0xFF7A7A7A)
    val OutlineVariant = Color(0xFFCACACA)
}

/** Dark: same roles, other values. Off-black / off-white; higher containers are lighter. */
internal object CestoDarkColors {
    val Green = Color(0xFF7FD8A6) // lighter green: readable on a dark surface
    val OnGreen = Color(0xFF003920)
    val GreenContainer = Color(0xFF0E5233)
    val OnGreenContainer = Color(0xFFCDEFD9)
    val Surface = Color(0xFF121212)
    val SurfaceContainerLowest = Color(0xFF0D0D0D)
    val SurfaceContainerLow = Color(0xFF1A1A1A)
    val SurfaceContainer = Color(0xFF1F1F1F)
    val SurfaceContainerHigh = Color(0xFF262626)
    val SurfaceContainerHighest = Color(0xFF2E2E2E)
    val SurfaceVariant = Color(0xFF2E2E2E)
    val OnSurface = Color(0xFFECECEC)
    val OnSurfaceVariant = Color(0xFFB8B8B8)
    val SurfaceBright = Color(0xFF383838)
    val Outline = Color(0xFF8C8C8C)
    val OutlineVariant = Color(0xFF444444)
}
