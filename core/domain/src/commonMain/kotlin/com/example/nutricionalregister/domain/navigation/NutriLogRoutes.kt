package com.example.nutricionalregister.domain.navigation

/**
 * The route strings of the app's navigation graph.
 *
 * The route names are a shared contract: the Android Compose graph and a
 * future native SwiftUI shell both read them from here, so the two shells
 * cannot silently drift apart (ADR-0015, ADR-0019). Renaming a route is
 * therefore a cross-platform change, which is the point.
 *
 * The three top-level routes are the roots of the bottom-navigation
 * destinations. Every other route is a child that is pushed on the stack of
 * the screen that opened it, so leaving a destination always returns to its
 * root (FR-010, FR-011).
 */
public object NutriLogRoutes {
    /** The diary home: the root of the first bottom-navigation destination. */
    public const val TODAY: String = "today"

    /** The personal food library: the root of the second destination. */
    public const val LIBRARY: String = "library"

    /** Goals, coach, premium, privacy and about: the root of the third destination. */
    public const val MORE: String = "more"

    /** The nested graph that holds the add-food flow. */
    public const val ADD_FOOD_GRAPH: String = "addfood"

    /** The step where the food is chosen: search, library or barcode. */
    public const val ADD_FOOD_SOURCE: String = "addfood/source"

    /** The step where the quantity and the meal are confirmed. */
    public const val ADD_FOOD_QUANTITY: String = "addfood/quantity"

    /** The argument that carries the preselected meal. */
    public const val ARG_MEAL: String = "meal"

    /** The argument that carries the preselected product code. */
    public const val ARG_PRODUCT: String = "product"

    /**
     * The declared route pattern of the add-food source step.
     *
     * The two arguments are optional query parameters, so the step can be
     * opened with a meal already chosen, with a food already chosen, or with
     * neither. A step that carries a food opens on the quantity step instead,
     * because there is nothing left to choose.
     */
    public const val ADD_FOOD_SOURCE_ROUTE: String =
        "$ADD_FOOD_SOURCE?$ARG_MEAL={$ARG_MEAL}&$ARG_PRODUCT={$ARG_PRODUCT}"

    /** The declared route pattern of the add-food quantity step. */
    public const val ADD_FOOD_QUANTITY_ROUTE: String =
        "$ADD_FOOD_QUANTITY?$ARG_MEAL={$ARG_MEAL}&$ARG_PRODUCT={$ARG_PRODUCT}"

    /** The product detail, scoped to the screen that opened it. */
    public const val PRODUCT_DETAIL: String = "product/{code}"

    /** The conversational assistant. */
    public const val COACH: String = "coach"

    /** The seven-day summary. */
    public const val WEEKLY: String = "weekly"

    /**
     * The product detail route for [code].
     *
     * The code is trimmed because it comes from an external catalogue, and a
     * blank code is rejected because a detail route without a product would
     * open a screen that cannot show anything.
     */
    public fun productDetail(code: String): String {
        val trimmed = code.trim()
        require(trimmed.isNotEmpty()) { "A product detail route needs a product code" }
        return "product/$trimmed"
    }

    /**
     * The add-food route, optionally starting on a meal and with a food already
     * chosen.
     *
     * A food that is already chosen means the flow opens on the quantity step:
     * there is nothing left to look for. Otherwise it opens on the source step,
     * where the food is chosen.
     *
     * Both arguments are optional: the flow is opened from the Today screen
     * with a meal preselected, from the Library or from a product detail with
     * a food preselected, or on its own.
     */
    public fun addFood(
        meal: String? = null,
        productCode: String? = null,
    ): String {
        val step = if (productCode.isNullOrBlank()) ADD_FOOD_SOURCE else ADD_FOOD_QUANTITY
        return addFoodStep(step, meal, productCode)
    }

    /**
     * The add-food route of the quantity step.
     *
     * The step is part of the route, so the screen always renders the step the
     * navigation state says it is on, and the two can never disagree.
     */
    public fun addFoodQuantity(
        meal: String? = null,
        productCode: String? = null,
    ): String = addFoodStep(ADD_FOOD_QUANTITY, meal, productCode)

    /**
     * The add-food route of the source step.
     *
     * The step is part of the route, so the screen always renders the step the
     * navigation state says it is on, and the two can never disagree.
     */
    public fun addFoodSource(
        meal: String? = null,
        productCode: String? = null,
    ): String = addFoodStep(ADD_FOOD_SOURCE, meal, productCode)

    /**
     * The route the flow's confirm control lands on.
     *
     * Logging a food does not return the user to a screen they already left:
     * it lands on the step where the food is looked for, with the meal the
     * food was logged into kept, because the same meal's next food is one
     * search away (ADR-0021). The landing is the domain's own rule, so a shell
     * that carries the navigation out asks for it and never works it out from
     * the stack's shape.
     */
    public fun addFoodLanding(meal: String? = null): String = addFoodSource(meal)

    /**
     * The route of [step] with the two optional arguments.
     *
     * Both arguments are always written, even when they are empty, because the
     * declared route pattern names them: a route that leaves one of them out
     * does not match the pattern and the navigation cannot resolve it.
     */
    private fun addFoodStep(
        step: String,
        meal: String?,
        productCode: String?,
    ): String {
        val mealValue = meal?.takeIf { it.isNotBlank() }.orEmpty()
        val productValue = productCode?.takeIf { it.isNotBlank() }.orEmpty()
        return "$step?$ARG_MEAL=$mealValue&$ARG_PRODUCT=$productValue"
    }
}
