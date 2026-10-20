@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Light Material 3 colour scheme for NutriLog. */
private val LightColorScheme =
    lightColorScheme(
        primary = Green40,
        onPrimary = Color.White,
        primaryContainer = Green90,
        onPrimaryContainer = Green10,
        secondary = Amber40,
        onSecondary = Color.White,
        secondaryContainer = Amber90,
        onSecondaryContainer = Amber10,
        tertiary = Teal40,
        onTertiary = Color.White,
        tertiaryContainer = Teal90,
        onTertiaryContainer = Teal10,
        error = Red40,
        onError = Color.White,
        errorContainer = Red90,
        onErrorContainer = Red10,
        background = NeutralLightSurface,
        onBackground = NeutralLightOnSurface,
        surface = NeutralLightSurface,
        onSurface = NeutralLightOnSurface,
        surfaceVariant = NeutralLightSurfaceVariant,
        onSurfaceVariant = NeutralLightOnSurfaceVariant,
        outline = NeutralLightOutline,
    )

/** Dark Material 3 colour scheme for NutriLog. */
private val DarkColorScheme =
    darkColorScheme(
        primary = Green80,
        onPrimary = Green20,
        primaryContainer = Green30,
        onPrimaryContainer = Green100,
        secondary = Amber80,
        onSecondary = Amber20,
        secondaryContainer = Amber30,
        onSecondaryContainer = Amber90,
        tertiary = Teal80,
        onTertiary = Teal20,
        tertiaryContainer = Teal30,
        onTertiaryContainer = Teal90,
        error = Red80,
        onError = Red20,
        errorContainer = Red30,
        onErrorContainer = Red90,
        background = NeutralDarkSurface,
        onBackground = NeutralDarkOnSurface,
        surface = NeutralDarkSurface,
        onSurface = NeutralDarkOnSurface,
        surfaceVariant = NeutralDarkSurfaceVariant,
        onSurfaceVariant = NeutralDarkOnSurfaceVariant,
        outline = NeutralDarkOutline,
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
