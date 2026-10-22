package com.example.nutricionalregister.feature.diary

import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionTotals
import com.example.nutricionalregister.domain.product.Product

/**
 * The steps of the add-food flow.
 *
 * The flow is deliberately short: pick the meal, pick the food, pick how much.
 * A user who opens the flow from a meal section has already chosen the meal,
 * so the flow starts on the food step.
 */
public enum class AddFoodStep {
    MEAL,
    FOOD,
    QUANTITY,
}

/**
 * The state of the add-food flow.
 *
 * @property step the step the flow is on.
 * @property mealType the meal the food will be logged into.
 * @property product the chosen food, or null while none has been chosen.
 * @property quantityGrams the chosen quantity in grams.
 * @property preview the nutrition the chosen quantity contributes, computed by
 *   the shared domain so the screen never sums a nutrient itself.
 */
public data class AddFoodState(
    val step: AddFoodStep = AddFoodStep.MEAL,
    val mealType: MealType = MealType.BREAKFAST,
    val product: Product? = null,
    val quantityGrams: Double = 0.0,
    val preview: NutritionTotals = NutritionTotals.ZERO,
) {
    /** Whether a food has been chosen and the flow can be completed. */
    public val canConfirm: Boolean
        get() = product != null && quantityGrams > 0.0
}
