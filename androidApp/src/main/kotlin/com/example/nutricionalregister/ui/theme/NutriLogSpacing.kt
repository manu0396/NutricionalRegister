package com.example.nutricionalregister.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * NutriLog spacing scale.
 *
 * A single 4 dp-based rhythm shared by every screen. Using named steps instead of
 * ad-hoc literals keeps vertical rhythm and padding consistent as screens evolve,
 * and makes the intent of each gap explicit (tight within a group, roomy between
 * sections).
 *
 * The scale is intentionally small; screens compose gaps from these steps rather
 * than inventing new values.
 */
public object NutriLogSpacing {
    /** 4 dp — hairline gaps between tightly related elements (e.g. icon + label). */
    public val extraSmall: Dp = 4.dp

    /** 8 dp — gaps within a group (rows inside a card, badge to its label). */
    public val small: Dp = 8.dp

    /** 12 dp — gaps between related groups inside a section. */
    public val medium: Dp = 12.dp

    /** 16 dp — default screen padding and content insets. */
    public val large: Dp = 16.dp

    /** 24 dp — separation between major sections. */
    public val extraLarge: Dp = 24.dp

    /** 32 dp — reserved for hero-level breathing room. */
    public val extraExtraLarge: Dp = 32.dp
}
