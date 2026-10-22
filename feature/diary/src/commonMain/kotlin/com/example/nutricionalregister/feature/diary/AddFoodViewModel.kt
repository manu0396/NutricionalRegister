package com.example.nutricionalregister.feature.diary

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionCalculator
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.purchase.SavedProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation logic for the add-food flow: meal, food and quantity.
 *
 * The flow owns the three choices the user makes and nothing else: the food
 * catalogue it searches comes from the search feature and the food library from
 * the saved-products feature, so this view model never holds a copy of either.
 *
 * The nutrition the chosen quantity produces is computed by [NutritionCalculator]
 * in the shared domain and handed to the screen ready to render, so the UI never
 * computes a nutrition value itself (NFR-005).
 *
 * @param scope the lifecycle scope that owns the writes; the caller clears it on
 *   dispose.
 */
public class AddFoodViewModel(
    private val diaryRepository: DiaryRepository,
    private val productSearchRepository: ProductSearchRepository,
    private val dayBoundaryProvider: DayBoundaryProvider,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(AddFoodState())
    public val state: StateFlow<AddFoodState> = mutableState.asStateFlow()

    /**
     * Starts the flow.
     *
     * A meal chosen by the caller skips the meal step and a food chosen by the
     * caller skips the food step. A flow opened with only a food still asks for
     * the meal, because the meal is the decision the food belongs to and it
     * cannot be guessed.
     */
    public fun start(
        meal: MealType? = null,
        product: Product? = null,
    ) {
        mutableState.value = AddFoodState()
        if (meal != null) {
            mutableState.update { current -> current.copy(mealType = meal) }
        }
        if (product != null) {
            mutableState.update { current ->
                current.copy(
                    product = product,
                    quantityGrams = NutritionCalculator.defaultQuantityGrams(product),
                    preview = NutritionCalculator.contribution(product, NutritionCalculator.defaultQuantityGrams(product)),
                )
            }
        }
        mutableState.update { current ->
            current.copy(
                step =
                    when {
                        product != null && meal != null -> AddFoodStep.QUANTITY
                        product != null -> AddFoodStep.MEAL
                        meal != null -> AddFoodStep.FOOD
                        else -> AddFoodStep.MEAL
                    },
            )
        }
    }

    /** Chooses the meal the food will be logged into. */
    public fun selectMeal(mealType: MealType) {
        mutableState.update { current ->
            current.copy(
                mealType = mealType,
                step = if (current.product != null) AddFoodStep.QUANTITY else AddFoodStep.FOOD,
            )
        }
    }

    /** Chooses the food and moves to the quantity step. */
    public fun selectProduct(product: Product) {
        val quantity = NutritionCalculator.defaultQuantityGrams(product)
        mutableState.update { current ->
            current.copy(
                step = AddFoodStep.QUANTITY,
                product = product,
                quantityGrams = quantity,
                preview = NutritionCalculator.contribution(product, quantity),
            )
        }
    }

    /**
     * Chooses a food from the user's library.
     *
     * The catalogue copy is used when the device has one, so the preview shows
     * the real nutrition. When the catalogue copy is not on the device the saved
     * record is used, and the nutrients it does not carry are previewed as
     * unknown rather than as zero.
     */
    public fun selectSavedProduct(saved: SavedProduct) {
        scope.launch {
            val product = productSearchRepository.getProduct(saved.code) ?: saved.toProduct()
            selectProduct(product)
        }
    }

    /** Sets the quantity in grams and updates the nutrition preview. */
    public fun setQuantity(grams: Double) {
        val product = mutableState.value.product ?: return
        if (!grams.isFinite() || grams <= 0.0) return
        mutableState.update { current ->
            current.copy(
                quantityGrams = grams,
                preview = NutritionCalculator.contribution(product, grams),
            )
        }
    }

    /**
     * Logs the chosen food and quantity into the chosen meal.
     *
     * Nothing happens when the flow is not ready to be confirmed.
     */
    public fun confirm() {
        val current = mutableState.value
        val product = current.product ?: return
        if (!current.canConfirm) return
        val loggedAt = clock.nowEpochMillis()
        val day = dayBoundaryProvider.startOfDay(loggedAt)
        val entry =
            NutritionCalculator.entryFor(
                product = product,
                mealType = current.mealType,
                quantityGrams = current.quantityGrams,
                loggedAtEpochMillis = loggedAt,
                dayEpochMillis = day,
            )
        scope.launch {
            diaryRepository.addEntry(entry)
        }
    }

    /**
     * Handles a back gesture inside the flow.
     *
     * Returns true when the flow moved back one step and the screen should stay
     * open, and false when the flow was already on its first step and the screen
     * should be closed.
     */
    public fun back(): Boolean {
        val current = mutableState.value
        return when (current.step) {
            AddFoodStep.QUANTITY -> {
                mutableState.update { state -> state.copy(step = AddFoodStep.FOOD) }
                true
            }
            AddFoodStep.FOOD -> {
                mutableState.update { state -> state.copy(step = AddFoodStep.MEAL) }
                true
            }
            AddFoodStep.MEAL -> false
        }
    }
}
