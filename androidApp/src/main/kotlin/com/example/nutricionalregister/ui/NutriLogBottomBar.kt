@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.navigation.NutriLogDestination

/**
 * The persistent bottom navigation bar (FR-009, FR-031).
 *
 * Renders exactly the three [NutriLogDestination] entries. Search is not one
 * of them: a catalogue is an input to the diary, not a peer of it, so the
 * search lives in the add-food flow and in the Library empty state.
 *
 * The current destination is conveyed by the item's selected flag plus its icon
 * *and* label, so it is never conveyed by colour alone and stays understandable
 * with accessibility services (SC-004). Each item keeps Material's 48 dp
 * minimum touch target and carries its [NutriLogDestination.label] as its
 * accessible name.
 *
 * @param current the destination currently displayed.
 * @param onSelect invoked with the tapped destination, including the current
 *   one, so the shell can decide whether to treat it as a no-op.
 */
@Composable
public fun NutriLogBottomBar(
    current: NutriLogDestination,
    onSelect: (NutriLogDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.testTag("nutrilog-bottom-bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        NutriLogDestination.entries.forEach { destination ->
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon() else destination.unselectedIcon(),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
                alwaysShowLabel = true,
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                modifier = Modifier.testTag("bottom-bar-${destination.route}"),
            )
        }
    }
}

/** Maps a destination to its selected (filled) Material icon. */
private fun NutriLogDestination.selectedIcon(): ImageVector =
    when (this) {
        NutriLogDestination.TODAY -> Icons.Filled.DateRange
        NutriLogDestination.LIBRARY -> Icons.AutoMirrored.Filled.List
        NutriLogDestination.MORE -> Icons.Filled.MoreVert
    }

/** Maps a destination to its unselected (outlined) Material icon. */
private fun NutriLogDestination.unselectedIcon(): ImageVector =
    when (this) {
        NutriLogDestination.TODAY -> Icons.Outlined.DateRange
        NutriLogDestination.LIBRARY -> Icons.AutoMirrored.Outlined.List
        NutriLogDestination.MORE -> Icons.Outlined.MoreVert
    }

/** Material's minimum interactive size, re-stated so it is not a magic number. */
internal val MinimumTouchTarget = 48.dp
