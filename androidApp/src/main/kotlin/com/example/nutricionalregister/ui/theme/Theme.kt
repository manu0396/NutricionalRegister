package com.example.nutricionalregister.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Light Material 3 colour scheme for NutriLog.
 *
 * Surfaces are white and near-white, the canvas is a very light grey and the
 * accent is the only saturated colour in the scheme. Outline roles are set
 * explicitly because several surfaces are separated by a hairline rule rather
 * than by a shadow.
 */
private val LightColorScheme =
    lightColorScheme(
        primary = Evergreen40,
        onPrimary = Color.White,
        primaryContainer = Evergreen90,
        onPrimaryContainer = Evergreen10,
        secondary = Sage40,
        onSecondary = Color.White,
        secondaryContainer = Sage90,
        onSecondaryContainer = Sage10,
        tertiary = Clay40,
        onTertiary = Color.White,
        tertiaryContainer = Clay90,
        onTertiaryContainer = Clay10,
        error = Red40,
        onError = Color.White,
        errorContainer = Red90,
        onErrorContainer = Red10,
        background = NeutralLightBackground,
        onBackground = NeutralLightOnSurface,
        surface = NeutralLightSurface,
        onSurface = NeutralLightOnSurface,
        surfaceVariant = NeutralLightSurfaceVariant,
        onSurfaceVariant = NeutralLightOnSurfaceVariant,
        surfaceContainer = NeutralLightSurfaceContainer,
        surfaceContainerHigh = NeutralLightSurfaceVariant,
        surfaceContainerHighest = NeutralLightSurfaceVariant,
        surfaceContainerLow = NeutralLightSurface,
        surfaceContainerLowest = NeutralLightSurface,
        surfaceBright = NeutralLightSurface,
        surfaceDim = NeutralLightSurfaceVariant,
        outline = NeutralLightOutline,
        outlineVariant = NeutralLightOutlineVariant,
        inverseSurface = NeutralLightOnSurface,
        inverseOnSurface = NeutralLightSurface,
        inversePrimary = Evergreen80,
        scrim = Color.Black,
    )

/**
 * Dark Material 3 colour scheme for NutriLog.
 *
 * The dark scheme is a true dark theme rather than a dimmed light theme: the
 * canvas is near-black, surfaces are separated by hairline outlines and the
 * accent is lightened so it keeps its contrast on a dark background.
 */
private val DarkColorScheme =
    darkColorScheme(
        primary = Evergreen80,
        onPrimary = Evergreen20,
        primaryContainer = Evergreen30,
        onPrimaryContainer = Evergreen90,
        secondary = Sage80,
        onSecondary = Sage20,
        secondaryContainer = Sage30,
        onSecondaryContainer = Sage90,
        tertiary = Clay80,
        onTertiary = Clay20,
        tertiaryContainer = Clay30,
        onTertiaryContainer = Clay90,
        error = Red80,
        onError = Red20,
        errorContainer = Red30,
        onErrorContainer = Red90,
        background = NeutralDarkBackground,
        onBackground = NeutralDarkOnSurface,
        surface = NeutralDarkSurface,
        onSurface = NeutralDarkOnSurface,
        surfaceVariant = NeutralDarkSurfaceVariant,
        onSurfaceVariant = NeutralDarkOnSurfaceVariant,
        surfaceContainer = NeutralDarkSurfaceContainer,
        surfaceContainerHigh = NeutralDarkSurfaceVariant,
        surfaceContainerHighest = NeutralDarkSurfaceVariant,
        surfaceContainerLow = NeutralDarkSurface,
        surfaceContainerLowest = NeutralDarkBackground,
        surfaceBright = NeutralDarkSurfaceVariant,
        surfaceDim = NeutralDarkBackground,
        outline = NeutralDarkOutline,
        outlineVariant = NeutralDarkOutlineVariant,
        inverseSurface = NeutralDarkOnSurface,
        inverseOnSurface = NeutralDarkSurface,
        inversePrimary = Evergreen40,
        scrim = Color.Black,
    )

/**
 * Applies the NutriLog Material 3 theme (colour, typography, shape) to [content].
 *
 * Screens must also render correctly under a stock [MaterialTheme]; this theme
 * only supplies a brand palette and never changes the semantics screens rely on.
 */
@Composable
public fun NutriLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = NutriLogTypography,
        shapes = NutriLogShapes,
        content = content,
    )
}
