@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * The persistent bottom navigation bar (FR-005, FR-008, FR-017).
 *
 * Renders exactly the three [NutriLogDestination] entries and indicates the
 * current one through the item's selected flag plus its icon *and* label, so the
 * current destination is never conveyed by colour alone and stays understandable
 * with accessibility services (SC-004). Each item keeps Material's 48 dp minimum
 * touch target and carries its [NutriLogDestination.label] as its accessible name.
 *
 * @param current the destination currently displayed.
 * @param onSelect invoked with the tapped destination (including the current one,
 *   so the shell can decide whether to no-op).
 */
@Composable
public fun NutriLogBottomBar(
    current: NutriLogDestination,
    onSelect: (NutriLogDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier.testTag("nutrilog-bottom-bar")) {
        NutriLogDestination.entries.forEach { destination ->
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = null,
                    )
                },
                label = { Text(destination.label) },
                alwaysShowLabel = true,
                colors =
                    NavigationBarItemDefaults.colors(),
                modifier = Modifier.testTag("bottom-bar-${destination.route}"),
            )
        }
    }
}

/** Maps a destination to its Material icon; kept here so the model stays Compose-free. */
private fun NutriLogDestination.icon(): ImageVector =
    when (this) {
        NutriLogDestination.SEARCH -> Icons.Filled.Search
        NutriLogDestination.SAVED -> Icons.AutoMirrored.Filled.List
        NutriLogDestination.DIARY -> Icons.Filled.DateRange
    }

/** Material's minimum interactive size, re-stated so it is not a magic number. */
internal val MinimumTouchTarget = 48.dp
