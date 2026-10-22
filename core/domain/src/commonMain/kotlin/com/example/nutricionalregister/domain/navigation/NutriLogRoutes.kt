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

    /** The product detail, scoped to the screen that opened it. */
    public const val PRODUCT_DETAIL: String = "product/{code}"

    /** The conversational assistant. */
    public const val COACH: String = "coach"

    /** The seven-day summary. */
    public const val WEEKLY: String = "weekly"

    /** The argument that carries the preselected meal. */
    public const val ARG_MEAL: String = "meal"

    /** The argument that carries the preselected product code. */
    public const val ARG_PRODUCT: String = "product"

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
     * Both arguments are optional: the flow is opened from the Today screen
     * with a meal preselected, from the Library or from a product detail with
     * a food preselected, or on its own.
     */
    public fun addFood(
        meal: String? = null,
        productCode: String? = null,
    ): String {
        val mealValue = meal?.takeIf { it.isNotBlank() }.orEmpty()
        val productValue = productCode?.takeIf { it.isNotBlank() }.orEmpty()
        return "$ADD_FOOD_GRAPH?$ARG_MEAL=$mealValue&$ARG_PRODUCT=$productValue"
    }
}
