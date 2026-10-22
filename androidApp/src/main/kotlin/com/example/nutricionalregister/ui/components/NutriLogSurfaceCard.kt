@file:Suppress("ktlint:standard:function-naming")

package com.example.nutricionalregister.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The app's standard content surface.
 *
 * The surface is an outlined card rather than an elevated one: a hairline border
 * separates it from the canvas without casting a shadow, which keeps the screen
 * calm and makes the data inside it the only thing that appears to "float".
 * Elevation is reserved for the one hero element of a screen, the daily summary.
 *
 * @param modifier the modifier applied to the card.
 * @param content the card body.
 */
@Composable
public fun NutriLogSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = { Column(modifier = Modifier.padding(NutriLogSpacing.large), content = content) },
    )
}
