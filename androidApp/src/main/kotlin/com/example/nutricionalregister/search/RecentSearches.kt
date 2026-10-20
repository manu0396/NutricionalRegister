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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.product.RecentSearch
import com.example.nutricionalregister.ui.SectionHeader
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * Recent search history, most recent first.
 *
 * Presented as an intentional section: a [SectionHeader] followed by one tonal row
 * per term. Each row's term is clickable text that re-runs the search, and a
 * trailing delete control is described as "Delete recent search {term}". Rendering
 * order is taken as given; the ViewModel already sorts by recency.
 */
@Composable
public fun RecentSearches(
    recentSearches: List<RecentSearch>,
    onSelectRecent: (String) -> Unit,
    onDeleteRecent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recentSearches.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
            modifier = modifier.fillMaxWidth(),
        ) {
            SectionHeader(title = "Recent searches")
            recentSearches.forEach { search ->
                RecentSearchRow(
                    search = search,
                    onSelect = { onSelectRecent(search.normalizedTerm) },
                    onDelete = { onDeleteRecent(search.normalizedTerm) },
                )
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    search: RecentSearch,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(start = NutriLogSpacing.large, end = NutriLogSpacing.extraSmall),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onSelect),
            ) {
                Text(
                    text = "⌕",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = search.displayTerm,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier =
                    Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Delete recent search ${search.displayTerm}" },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
