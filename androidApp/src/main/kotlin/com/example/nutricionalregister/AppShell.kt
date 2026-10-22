@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nutricionalregister.domain.navigation.AddFoodStep
import com.example.nutricionalregister.domain.navigation.NutriLogDestination
import com.example.nutricionalregister.domain.navigation.NutriLogRoutes
import com.example.nutricionalregister.ui.AddFoodDestination
import com.example.nutricionalregister.ui.NutriLogBottomBar
import com.example.nutricionalregister.ui.NutriLogNavigator
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The app shell: the tracker frame every screen is shown in.
 *
 * The shell owns the navigation graph and the frame around it. The screens
 * themselves are passed in as slots, so the shell decides where a screen lives
 * in the graph and the screen decides what it shows (ADR-0015, ADR-0019).
 *
 * The Coach is not one of them: it opens over the screen the user is on, which
 * is why [onOpenCoach] is an action and not a slot, and the shell offers it
 * wherever the user is (ADR-0019).
 *
 * The frame is derived from the **route hierarchy**, never from a lookup that
 * can fail for a nested route. The current destination is the first entry of
 * the hierarchy, read from the deepest side, that is one of the three
 * top-level destinations. A route inside the add-food graph or the product
 * detail resolves to no destination at all, and the bar is hidden, which is
 * exactly what should happen there (FR-031). The earlier rule, which looked
 * the current route up in an enum and blanked the bar when the lookup
 * returned nothing, is the defect this replaces.
 *
 * The floating action button opens the add-food flow and is present on the two
 * destinations where adding food makes sense: the diary and the library.
 *
 * @param todayContent the diary home.
 * @param libraryContent the personal food library.
 * @param moreContent goals, coach, premium and the rest.
 * @param addFoodContent the add-food flow, on the step the route names.
 * @param productContent the product detail, with the product's code.
 * @param weeklyContent the seven-day summary.
 */
@Composable
public fun AppShell(
    todayContent: @Composable (PaddingValues, NutriLogNavigator) -> Unit,
    libraryContent: @Composable (PaddingValues, NutriLogNavigator) -> Unit,
    moreContent: @Composable (PaddingValues, NutriLogNavigator) -> Unit,
    addFoodContent: @Composable (PaddingValues, AddFoodDestination, NutriLogNavigator) -> Unit,
    productContent: @Composable (PaddingValues, String, NutriLogNavigator) -> Unit,
    weeklyContent: @Composable (PaddingValues, NutriLogNavigator) -> Unit,
    onOpenCoach: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navigator = remember(navController) { NutriLogNavigator(navController) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.currentTopLevelDestination()
    val showBottomBar = currentDestination != null
    val showAddAction =
        currentDestination == NutriLogDestination.TODAY ||
            currentDestination == NutriLogDestination.LIBRARY

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar && currentDestination != null) {
                NutriLogBottomBar(
                    current = currentDestination,
                    onSelect = navigator::switchTo,
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            ) {
                if (showAddAction) {
                    FloatingActionButton(
                        onClick = { navigator.openAddFood() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.testTag("nutrilog-add-food-fab"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add food",
                        )
                    }
                }
                SmallFloatingActionButton(
                    onClick = onOpenCoach,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.testTag("nutrilog-coach-fab"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Face,
                        contentDescription = "Ask the Coach",
                    )
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = NutriLogRoutes.TODAY,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(NutriLogRoutes.TODAY) {
                todayContent(contentPadding, navigator)
            }
            composable(NutriLogRoutes.LIBRARY) {
                libraryContent(contentPadding, navigator)
            }
            composable(NutriLogRoutes.MORE) {
                moreContent(contentPadding, navigator)
            }
            navigation(
                startDestination = NutriLogRoutes.ADD_FOOD_SOURCE_ROUTE,
                route = NutriLogRoutes.ADD_FOOD_GRAPH,
            ) {
                composable(
                    route = NutriLogRoutes.ADD_FOOD_SOURCE_ROUTE,
                    arguments = addFoodArguments(),
                ) { entry ->
                    addFoodContent(
                        contentPadding,
                        entry.addFoodDestination(navController, AddFoodStep.SOURCE),
                        navigator,
                    )
                }
                composable(
                    route = NutriLogRoutes.ADD_FOOD_QUANTITY_ROUTE,
                    arguments = addFoodArguments(),
                ) { entry ->
                    addFoodContent(
                        contentPadding,
                        entry.addFoodDestination(navController, AddFoodStep.QUANTITY),
                        navigator,
                    )
                }
            }
            composable(
                route = NutriLogRoutes.PRODUCT_DETAIL,
                arguments = listOf(navArgument(PRODUCT_CODE_ARGUMENT) { type = NavType.StringType }),
            ) { entry ->
                productContent(
                    contentPadding,
                    entry.argument(PRODUCT_CODE_ARGUMENT).orEmpty(),
                    navigator,
                )
            }
            composable(NutriLogRoutes.WEEKLY) {
                weeklyContent(contentPadding, navigator)
            }
        }
    }
}

/** The path parameter that carries the product code in the product detail route. */
private const val PRODUCT_CODE_ARGUMENT = "code"

/** The two optional arguments the add-food steps carry. */
private fun addFoodArguments(): List<NamedNavArgument> =
    listOf(
        navArgument(NutriLogRoutes.ARG_MEAL) {
            type = NavType.StringType
            defaultValue = ""
        },
        navArgument(NutriLogRoutes.ARG_PRODUCT) {
            type = NavType.StringType
            defaultValue = ""
        },
    )

/**
 * The top-level destination the current entry belongs to, or null when the
 * current entry is a child of one.
 *
 * The hierarchy is read from the deepest entry upwards, so the answer does not
 * depend on where the root graph sits in the hierarchy and a nested route can
 * never be mistaken for a top-level one.
 */
private fun NavBackStackEntry?.currentTopLevelDestination(): NutriLogDestination? =
    this
        ?.destination
        ?.hierarchy
        ?.toList()
        ?.asReversed()
        ?.firstNotNullOfOrNull { destination ->
            NutriLogDestination.fromRoute(destination.route)
        }

/** The value of the argument [name] for this entry, or an empty string. */
private fun NavBackStackEntry.argument(name: String): String = arguments?.getString(name).orEmpty()

/**
 * The id of the add-food graph's entry, which identifies one opening of the flow.
 *
 * A new open pushes a new graph entry, so the id changes and the flow is reset;
 * moving between the two steps of one open keeps the same graph entry, so the id
 * is the same and the choice is kept. The graph entry is always on the stack
 * while one of its steps is composed; the fallback exists so a hook that reads
 * the destination outside that window degrades to an unusable session id instead
 * of throwing.
 */
private fun NavHostController.addFoodSessionId(): String? =
    runCatching { getBackStackEntry(NutriLogRoutes.ADD_FOOD_GRAPH).id.toString() }.getOrNull()

/** The add-food destination this entry describes. */
private fun NavBackStackEntry.addFoodDestination(
    navController: NavHostController,
    step: AddFoodStep,
): AddFoodDestination =
    AddFoodDestination(
        sessionId = navController.addFoodSessionId().orEmpty(),
        step = step,
        meal = argument(NutriLogRoutes.ARG_MEAL).takeIf { it.isNotBlank() },
        productCode = argument(NutriLogRoutes.ARG_PRODUCT).takeIf { it.isNotBlank() },
    )
