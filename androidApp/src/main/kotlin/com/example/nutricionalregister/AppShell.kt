@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nutricionalregister.ui.NutriLogBottomBar
import com.example.nutricionalregister.ui.NutriLogDestination

/**
 * The app shell: a [Scaffold] whose `bottomBar` is the shared [NutriLogBottomBar]
 * and whose body is a [NavHost] over the three [NutriLogDestination] routes
 * (FR-005, FR-007, FR-008).
 *
 * Placing the bar in the Scaffold means the Scaffold consumes the system and
 * navigation bar insets for us, so the bar never obscures content or the system
 * navigation area, including under gesture navigation (FR-008). Each destination
 * keeps its own back stack; re-selecting the current destination is a no-op rather
 * than pushing a duplicate entry (navigation contract rule 2).
 *
 * Screen content is supplied as slots so the shell carries no product-search or
 * saved-products wiring and can be driven directly by tests. [searchContent] and
 * [savedContent] receive the [PaddingValues] the Scaffold provides so their bodies
 * clear the bars; the Diary slot defaults to a caller-supplied placeholder.
 */
@Composable
public fun AppShell(
    searchContent: @Composable (PaddingValues) -> Unit,
    savedContent: @Composable (PaddingValues, onNavigateToSearch: () -> Unit) -> Unit,
    diaryContent: @Composable (PaddingValues) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry = navController.currentBackStackEntryAsState().value
    val current =
        NutriLogDestination.fromRoute(backStackEntry?.destination?.route)
            ?: NutriLogDestination.start

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NutriLogBottomBar(
                current = current,
                onSelect = { destination -> navController.navigateToDestination(destination) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NutriLogDestination.start.route,
        ) {
            composable(NutriLogDestination.SEARCH.route) {
                searchContent(innerPadding)
            }
            composable(NutriLogDestination.SAVED.route) {
                savedContent(innerPadding) {
                    navController.navigateToDestination(NutriLogDestination.SEARCH)
                }
            }
            composable(NutriLogDestination.DIARY.route) {
                diaryContent(innerPadding)
            }
        }
    }
}

/**
 * Switches to [destination] without growing the back stack.
 *
 * Navigating with a fixed start destination and `launchSingleTop` keeps the bar's
 * indicator and the back stack consistent with a normal destination switch, so a
 * repeated selection of the current destination does not push a duplicate entry
 * (navigation contract rules 2 and 6).
 */
private fun NavHostController.navigateToDestination(destination: NutriLogDestination) {
    if (currentDestination?.hierarchy?.any { it.route == destination.route } == true) {
        return
    }
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
