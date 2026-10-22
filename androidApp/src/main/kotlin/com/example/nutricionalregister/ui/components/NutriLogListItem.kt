@file:Suppress("ktlint:standard:function-naming")

package com.example.nutricionalregister.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * A row in one of the app's lists.
 *
 * The row is a single surface with a title, an optional subtitle and optional
 * leading and trailing slots, so every list in the app has the same rhythm. The
 * whole row is the touch target when [onClick] is given, which is what a thumb
 * expects on a phone.
 *
 * @param title the primary text.
 * @param subtitle the secondary text, or null when there is none.
 * @param onClick invoked when the row is tapped, or null when it is not tappable.
 * @param leading the optional leading slot, usually a thumbnail.
 * @param trailing the optional trailing slot, usually an action.
 * @param modifier the modifier applied to the row.
 */
@Composable
public fun NutriLogListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = NutriLogSpacing.small),
    ) {
        leading?.invoke()
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * A hairline rule drawn between two list rows.
 *
 * The rule belongs between rows only, never above the first one, so a list never
 * starts with a stray line under its heading.
 */
@Composable
public fun NutriLogListDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier.testTag("nutrilog-list-divider"),
    )
}
