@file:Suppress("ktlint:standard:function-naming")

package com.example.nutricionalregister.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.example.nutricionalregister.ui.formatNutrient
import com.example.nutricionalregister.ui.progressOf
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * One macro line: the name, the amount against its goal, and a progress bar.
 *
 * The bar is the measurement; the numbers are the reading. When the amount is
 * unknown the row shows a dash and an empty bar rather than a fabricated figure,
 * so a food that does not report fat never looks like a food with no fat.
 *
 * @param label the macro name.
 * @param value the amount consumed, or null when it is unknown.
 * @param goal the daily target for the macro.
 * @param unit the unit the amount is expressed in.
 * @param lowerBound whether the value is only a lower bound.
 * @param modifier the modifier applied to the row.
 */
@Composable
public fun NutriLogMacroRow(
    label: String,
    value: Double?,
    goal: Int,
    unit: String,
    lowerBound: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val amount = formatNutrient(value, unit, lowerBound)
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$amount / $goal $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "$label: $amount of $goal $unit" },
            )
        }
        NutriLogProgressBar(
            progress = progressOf(value, goal),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
