package com.example.nutricionalregister.ui

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.nutricionalregister.domain.navigation.NutriLogDestination
import com.example.nutricionalregister.domain.navigation.NutriLogRoutes

/**
 * The navigation actions the screens are allowed to perform.
 *
 * The screens never touch the [NavHostController] directly. They receive a
 * navigator and ask for a destination, which keeps the navigation rules in one
 * place, the shell, and keeps the screens free of navigation mechanics.
 *
 * The route strings themselves are shared domain data, so a future native
 * SwiftUI shell speaks the same route names (ADR-0015, ADR-0019).
 */
public class NutriLogNavigator internal constructor(
    private val navController: NavHostController,
) {
    /** Opens the detail of the catalogue product with [code]. */
    public fun openProduct(code: String) {
        navController.navigate(NutriLogRoutes.productDetail(code))
    }

    /**
     * Opens the add-food flow, optionally with a meal and a food already chosen.
     *
     * A meal comes from a meal section of the Today screen and a food from a
     * Library row or from a product detail.
     */
    public fun openAddFood(
        meal: String? = null,
        productCode: String? = null,
    ) {
        navController.navigate(NutriLogRoutes.addFood(meal, productCode))
    }

    /** Moves the add-food flow to the step where the quantity is confirmed. */
    public fun openAddFoodQuantity() {
        navController.navigate(NutriLogRoutes.addFoodQuantity())
    }

    /** Whether an opening of the add-food flow is on the stack right now. */
    public fun isFlowOpen(): Boolean = runCatching { navController.getBackStackEntry(NutriLogRoutes.ADD_FOOD_GRAPH) }.isSuccess

    /**
     * Carries the product the user chose on a detail into the flow that is
     * already open, without opening a second flow (ADR-0021).
     */
    public fun carryProductIntoFlow(mealName: String?) {
        navController.navigate(
            NutriLogRoutes.addFoodQuantity(mealName),
        ) {
            // The detail the flow's row opened is consumed by this carrying:
            // the quantity step sits directly on the flow's source step, so
            // the walk back is one step and not a detour.
            popUpTo(NutriLogRoutes.ADD_FOOD_SOURCE) { inclusive = false }
            launchSingleTop = true
        }
    }

    /**
     * Lands the flow after a food was logged into [mealName].
     *
     * The landing is the flow's own source step, with the meal the food was
     * logged into carried on it, so the same meal's next food is one search
     * away (ADR-0021). The flow's state above it is kept by the view model,
     * so nothing is lost on the way back: the quantity step and the detail a
     * flow row opened are the entries that were consumed by this landing.
     */
    public fun landAfterAddFood(mealName: String?) {
        navController.navigate(
            NutriLogRoutes.addFoodLanding(mealName),
        ) {
            popUpTo(NutriLogRoutes.ADD_FOOD_GRAPH) { inclusive = false }
            launchSingleTop = true
        }
    }

    /** Opens the weekly summary. */
    public fun openWeekly() {
        navController.navigate(NutriLogRoutes.WEEKLY)
    }

    /** Returns to the screen that opened the current one. */
    public fun back() {
        navController.popBackStack()
    }

    /** Switches to [destination], landing on its root. */
    public fun switchTo(destination: NutriLogDestination) {
        navController.switchToDestination(destination)
    }
}

/**
 * Switches to [destination] without growing the back stack.
 *
 * The back stack is popped back to the start destination, so every screen that
 * was pushed on top of it is discarded and the destination is always presented
 * as its own root. This is the rule that fixes the reported navigation defect:
 * switching to an area from anywhere always lands on that area's own screen,
 * never on a detail screen another area had opened.
 *
 * The state of a destination is deliberately NOT saved and restored. Saving it
 * is what made a detail screen survive a tab switch and resurrect itself when
 * the user came back, which is the second half of the reported defect.
 *
 * Re-selecting the destination that is already showing is a no-op, so no
 * duplicate entry is pushed and the screen does not flicker.
 */
internal fun NavHostController.switchToDestination(destination: NutriLogDestination) {
    if (currentDestination?.route == destination.route) {
        return
    }
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { inclusive = false }
        launchSingleTop = true
    }
}
