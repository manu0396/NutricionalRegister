package com.example.nutricionalregister.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * NutriLog shape scale.
 *
 * Restrained radii, in the 4 to 18 dp range. The previous scale reached 24 dp
 * and every card, button and chip inherited it, which is the single strongest
 * signal that made the app read as a game. The scale still softens the corners
 * enough to feel modern, and the extraLarge step is reserved for sheets and
 * dialogs where a larger radius is expected.
 */
internal val NutriLogShapes: Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(18.dp),
    )
