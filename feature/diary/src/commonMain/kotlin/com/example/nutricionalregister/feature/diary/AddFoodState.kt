package com.example.nutricionalregister.feature.diary

import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionTotals
import com.example.nutricionalregister.domain.product.Product

/**
 * Where the food of the add-food flow is looked for.
 *
 * The source is the choice the user makes on the first step of the flow, and it
 * decides where the food comes from: the catalogue, the user's own library or
 * the camera.
 */
public enum class AddFoodSource {
    /** The food catalogue, searched by name. */
    SEARCH,

    /** The user's own food library. */
    LIBRARY,

    /** A barcode read with the camera. */
    SCAN,
}

/**
 * The state of the add-food flow.
 *
 * The step of the flow is NOT part of the state: it lives in the route, so the
 * screen and the navigation state can never disagree about it (ADR-0019).
 *
 * @property source where the food is looked for.
 * @property query the text the user typed in the catalogue search.
 * @property mealType the meal the food will be logged into, or null while no
 *   meal has been chosen.
 * @property product the chosen food, or null while none has been chosen.
 * @property quantityGrams the amount in grams.
 * @property preview the nutrition the amount contributes, computed by the
 *   shared domain so the screen never sums a nutrient itself.
 * @property isMealSheetOpen whether the modal meal sheet is open.
 * @property isResolvingProduct whether a product code is being resolved
 *   against the catalogue cache.
 */
public data class AddFoodState(
    val source: AddFoodSource = AddFoodSource.SEARCH,
    val query: String = "",
    val mealType: MealType? = null,
    val product: Product? = null,
    val quantityGrams: Double = 0.0,
    val preview: NutritionTotals = NutritionTotals.ZERO,
    val isMealSheetOpen: Boolean = false,
    val isResolvingProduct: Boolean = false,
) {
    /** Whether the food, the meal and the amount are all chosen. */
    public val canConfirm: Boolean
        get() = product != null && mealType != null && quantityGrams > 0.0
}
