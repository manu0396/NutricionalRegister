@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * A section title with an optional trailing affordance.
 *
 * The title renders with [MaterialTheme.typography.titleMedium] and an
 * [onSurfaceVariant] accent so sections read as structure rather than as another
 * line of content. [trailing] hosts an optional action such as "Clear"; it is
 * aligned to the end of the row at the title's visual baseline.
 */
@Composable
public fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}
