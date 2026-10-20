@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * A single label/value line for the detail screen's info and nutrition lists.
 *
 * Dividers are the caller's responsibility: [StatRowDivider] is placed only
 * *between* rows, never above the first one, so a heading is never separated from
 * its first row by a stray rule.
 *
 * The value sits in a bounded trailing column and is right-aligned, so a stack of
 * rows reads as a clean value column with the numbers flush to the same edge
 * instead of jittering with each label's length.
 *
 * [leadingDot] optionally paints a small coloured disc before the label, used by
 * the nutrition list to give each nutrient a visual anchor. [valueColor] lets a
 * caller de-emphasise an unavailable value.
 */
@Composable
public fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    leadingDot: Color? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = NutriLogSpacing.small),
    ) {
        if (leadingDot != null) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(leadingDot, CircleShape),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Hairline rule drawn *between* [StatRow]s.
 *
 * Callers insert this before every row except the first, which fixes the previous
 * pattern where a divider sat between a section heading and its first row and none
 * closed the list. It is deliberately a plain divider rather than a list wrapper so
 * the placement decision stays visible at the call site.
 */
@Composable
public fun StatRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier,
    )
}
