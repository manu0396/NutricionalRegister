@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.feature.search.SearchPhase
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * Renders the current search-phase message and its actions.
 *
 * Text is unchanged from the pre-refresh behaviour (and asserted by tests):
 * only the presentation is themed. Retryable phases show "Retry" and
 * "Edit search"; NO_RESULTS never shows "Retry". A leading icon signals the kind
 * of outcome at a glance (a hint, a refresh in progress, or a warning) without
 * replacing the wording.
 */
@Composable
public fun SearchOutcomeMessage(
    phase: SearchPhase,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message =
        when (phase) {
            SearchPhase.INITIAL -> "Search for a food by name to inspect its catalogue details."
            SearchPhase.LOADING -> "Searching the catalogue…"
            SearchPhase.REFRESHING -> "Refreshing catalogue results…"
            SearchPhase.NO_RESULTS -> "No products found. Try a different search term."
            SearchPhase.OFFLINE_WITH_CACHE -> "You’re offline. Showing saved catalogue results."
            SearchPhase.OFFLINE_WITHOUT_CACHE -> "You’re offline and no saved results are available."
            SearchPhase.ERROR_WITH_CACHE -> "Couldn’t refresh. Showing saved catalogue results."
            SearchPhase.ERROR_WITHOUT_CACHE -> "The search could not be completed."
            SearchPhase.RESULTS -> return
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
        modifier = modifier.fillMaxWidth().padding(vertical = NutriLogSpacing.extraSmall),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = phase.outcomeIcon(),
                contentDescription = null,
                tint = phase.outcomeIconTint(),
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (phase in RETRYABLE_PHASES) {
            Row(horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small)) {
                Button(
                    onClick = onRetry,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Retry")
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Edit search")
                }
            }
        }
    }
}

/** The icon that signals the kind of outcome for this search phase. */
private fun SearchPhase.outcomeIcon(): ImageVector =
    when (this) {
        SearchPhase.INITIAL, SearchPhase.NO_RESULTS -> Icons.Outlined.Search
        SearchPhase.LOADING, SearchPhase.REFRESHING -> Icons.Filled.Refresh
        SearchPhase.OFFLINE_WITH_CACHE, SearchPhase.OFFLINE_WITHOUT_CACHE -> Icons.Filled.Info
        SearchPhase.ERROR_WITH_CACHE, SearchPhase.ERROR_WITHOUT_CACHE -> Icons.Filled.Warning
        SearchPhase.RESULTS -> Icons.Outlined.Search
    }

/** Warning phases use the error colour; the rest stay neutral. */
@Composable
private fun SearchPhase.outcomeIconTint() =
    when (this) {
        SearchPhase.ERROR_WITH_CACHE, SearchPhase.ERROR_WITHOUT_CACHE ->
            MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private val RETRYABLE_PHASES =
    setOf(
        SearchPhase.OFFLINE_WITH_CACHE,
        SearchPhase.OFFLINE_WITHOUT_CACHE,
        SearchPhase.ERROR_WITH_CACHE,
        SearchPhase.ERROR_WITHOUT_CACHE,
    )
