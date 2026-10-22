@file:Suppress("ktlint:standard:function-naming")

package com.example.nutricionalregister.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The shared empty state: an icon, a heading, a body and an optional call to
 * action.
 *
 * Every empty state in the app uses this component, so an empty screen always
 * looks the same and always tells the user what to do next. The icon is
 * decorative and hidden from accessibility services, because the heading already
 * carries the meaning.
 *
 * @param icon the decorative glyph.
 * @param title the heading.
 * @param body the supporting sentence.
 * @param action the optional call to action, usually a button.
 * @param modifier the modifier applied to the column.
 */
@Composable
public fun NutriLogEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        modifier = modifier.fillMaxWidth().padding(NutriLogSpacing.large),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}
