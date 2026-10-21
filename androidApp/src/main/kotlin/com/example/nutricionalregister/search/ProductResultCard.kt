@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * A single product search result.
 *
 * Shows a fixed-size thumbnail, the product name, a secondary brand/quantity line
 * and the code, with a Nutri-Score badge pinned to the trailing edge when the
 * catalogue supplies a grade. The card uses a lightly elevated surface so result
 * lists read as a set of cards rather than blended into the background.
 *
 * The whole card is clickable and carries the "View {name}, code {code}"
 * description for screen readers; the name and code Text nodes remain individually
 * discoverable because the description is added without merging descendants.
 *
 * @param trailing optional trailing action rendered after the Nutri-Score badge,
 *   such as a remove control. It is placed outside the card's clickable area so
 *   activating it does not open the product.
 */
@Composable
public fun ProductResultCard(
    product: Product,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 2.dp,
            ),
        shape = MaterialTheme.shapes.large,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clickable(onClick = onOpen)
                .semantics { contentDescription = "View ${product.name}, code ${product.code}" },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(NutriLogSpacing.medium),
        ) {
            ProductImage(
                url = product.imageThumbnailUrl,
                contentDescription = null,
                initial = initialFor(product.name),
                seed = product.code,
                modifier = Modifier.size(72.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val secondary = listOfNotNull(product.brands, product.quantity).joinToString(" · ")
                if (secondary.isNotBlank()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "Code ${product.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Fixed trailing slot so every card's right edge aligns whether or not
            // the catalogue supplied a Nutri-Score grade.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp),
            ) {
                NutriScoreBadge(grade = product.nutriScoreGrade)
            }
            trailing?.invoke()
        }
    }
}
