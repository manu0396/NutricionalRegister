@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The app's outer shell: a Material 3 [Scaffold] with a real [TopAppBar].
 *
 * This is the fix for the app drawing under the status bar. `MainActivity` calls
 * `enableEdgeToEdge()`, so without a [Scaffold] the content lays out from the very
 * top of the window and its title collides with the system clock. The Scaffold
 * consumes the system-bar insets, pushes the [TopAppBar] below the status bar, and
 * exposes [PaddingValues] that callers apply so content clears both the bar and the
 * navigation bar.
 *
 * The shell is *additive*: screens are also rendered directly by instrumented tests
 * inside a bare `MaterialTheme {}`, so nothing screen-essential lives here. The
 * search root gets its app bar from this shell; the detail screen keeps its own
 * back control and handles its own top inset (see `ProductDetailScreen`), so it is
 * never rendered through this shell.
 *
 * @param title the text shown in the top app bar.
 * @param subtitle optional secondary line rendered under [title] inside the bar.
 * @param content the screen body; receives the Scaffold [PaddingValues] the caller
 *   applies to its scroll container so content is not clipped by the bars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NutriLogScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
            )
        },
        content = content,
    )
}
