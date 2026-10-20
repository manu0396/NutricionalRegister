package com.example.nutricionalregister.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * NutriLog shape scale.
 *
 * Slightly rounded corners throughout: cards land on the medium/large radii so
 * result cards and the detail header feel soft and modern without looking bubbly.
 */
internal val NutriLogShapes: Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )
