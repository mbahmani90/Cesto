package com.majidbahmani.cesto.systemdesign.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Every role used by the app is set in both schemes; unset roles fall back to Material's purple baseline.
private val LightColors = lightColorScheme(
    primary = CestoColors.Green,
    onPrimary = Color.White,
    primaryContainer = CestoColors.GreenContainer,
    onPrimaryContainer = CestoColors.OnGreenContainer,
    secondary = CestoColors.Green,
    onSecondary = Color.White,
    secondaryContainer = CestoColors.GreenContainer,
    onSecondaryContainer = CestoColors.OnGreenContainer,
    background = CestoColors.Surface,
    onBackground = CestoColors.Ink,
    surface = CestoColors.Surface,
    onSurface = CestoColors.Ink,
    surfaceVariant = CestoColors.SurfaceVariant,
    onSurfaceVariant = CestoColors.OnSurfaceVariant,
    surfaceTint = CestoColors.Green,
    surfaceContainerLowest = CestoColors.SurfaceContainerLowest,
    surfaceContainerLow = CestoColors.SurfaceContainerLow,
    surfaceContainer = CestoColors.SurfaceContainer,
    surfaceContainerHigh = CestoColors.SurfaceContainerHigh,
    surfaceContainerHighest = CestoColors.SurfaceContainerHighest,
    surfaceDim = CestoColors.SurfaceDim,
    surfaceBright = CestoColors.Surface,
    outline = CestoColors.Outline,
    outlineVariant = CestoColors.OutlineVariant,
)

private val DarkColors = darkColorScheme(
    primary = CestoDarkColors.Green,
    onPrimary = CestoDarkColors.OnGreen,
    primaryContainer = CestoDarkColors.GreenContainer,
    onPrimaryContainer = CestoDarkColors.OnGreenContainer,
    secondary = CestoDarkColors.Green,
    onSecondary = CestoDarkColors.OnGreen,
    secondaryContainer = CestoDarkColors.GreenContainer,
    onSecondaryContainer = CestoDarkColors.OnGreenContainer,
    background = CestoDarkColors.Surface,
    onBackground = CestoDarkColors.OnSurface,
    surface = CestoDarkColors.Surface,
    onSurface = CestoDarkColors.OnSurface,
    surfaceVariant = CestoDarkColors.SurfaceVariant,
    onSurfaceVariant = CestoDarkColors.OnSurfaceVariant,
    surfaceTint = CestoDarkColors.Green,
    surfaceContainerLowest = CestoDarkColors.SurfaceContainerLowest,
    surfaceContainerLow = CestoDarkColors.SurfaceContainerLow,
    surfaceContainer = CestoDarkColors.SurfaceContainer,
    surfaceContainerHigh = CestoDarkColors.SurfaceContainerHigh,
    surfaceContainerHighest = CestoDarkColors.SurfaceContainerHighest,
    surfaceDim = CestoDarkColors.Surface,
    surfaceBright = CestoDarkColors.SurfaceBright,
    outline = CestoDarkColors.Outline,
    outlineVariant = CestoDarkColors.OutlineVariant,
)

/**
 * The app's theme; follows the system light/dark setting. [darkTheme] is a parameter so previews,
 * tests and a later in-app setting can force a mode.
 */
@Composable
fun CestoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
