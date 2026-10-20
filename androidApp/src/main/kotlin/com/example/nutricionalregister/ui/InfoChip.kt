@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * A small pill pairing a muted label with a piece of content.
 *
 * Used to give standalone values context (for example the Nutri-Score badge
 * alongside an explicit "Nutri-Score" label) so a glyph never floats unexplained.
 * The chip uses a low tonal surface so it reads as metadata rather than as a
 * primary action, and its content slot keeps whatever semantics the caller passes.
 */
@Composable
public fun InfoChip(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(
                    horizontal = NutriLogSpacing.medium,
                    vertical = NutriLogSpacing.small,
                ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
