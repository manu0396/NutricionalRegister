package com.example.nutricionalregister.feature.diary

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionCalculator
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation logic for the add-food flow: the food, the amount and the meal.
 *
 * The view model owns the three choices the user makes in the flow and nothing
 * else. The step of the flow is NOT held here: it lives in the route, so the
 * screen and the navigation state can never disagree about where the user is
 * (ADR-0019).
 *
 * The meal is chosen from a modal sheet and not from a step of its own, because
 * a sheet is not navigation and must not create a back-stack entry (FR-030).
 *
 * The nutrition the chosen amount produces is computed by [NutritionCalculator]
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

    private var currentSessionId: String? = null
    private var resolvingCode: String? = null

    /**
     * Starts the flow.
     *
     * [sessionId] identifies the opening of the flow. When it differs from the
     * one of the last call the flow is reset, so reopening the flow never shows
     * the previous food. When it is the same the call only fills in what is
     * still missing, so moving between the two steps of the flow never loses
     * what the user already chose.
     *
     * [meal] is the meal the flow was opened with and [productCode] the food it
     * was opened with. A food that is not in the catalogue cache resolves to
     * nothing and leaves the flow empty; it must not fail.
     */
    public fun start(
        sessionId: String,
        meal: MealType? = null,
        productCode: String? = null,
    ) {
        if (currentSessionId != sessionId) {
            currentSessionId = sessionId
            resolvingCode = null
            mutableState.value = AddFoodState()
        }
        if (meal != null && mutableState.value.mealType == null) {
            mutableState.update { current -> current.copy(mealType = meal) }
        }
        if (productCode != null && needsResolution(productCode)) {
            resolveProduct(productCode)
        }
    }

    /** Chooses which source the food is looked for in. */
    public fun selectSource(source: AddFoodSource) {
        mutableState.update { current -> current.copy(source = source) }
    }

    /** Records the text the user typed. */
    public fun setQuery(query: String) {
        mutableState.update { current -> current.copy(query = query) }
    }

    /** Chooses the meal the food will be logged into and closes the meal sheet. */
    public fun selectMeal(mealType: MealType) {
        mutableState.update { current ->
            current.copy(
                mealType = mealType,
                isMealSheetOpen = false,
            )
        }
    }

    /** Opens the meal sheet. */
    public fun openMealSheet() {
        mutableState.update { current -> current.copy(isMealSheetOpen = true) }
    }

    /** Closes the meal sheet without choosing a meal. */
    public fun closeMealSheet() {
        mutableState.update { current -> current.copy(isMealSheetOpen = false) }
    }

    /** Chooses the food and sets the default quantity for it. */
    public fun selectProduct(product: Product) {
        val quantity = NutritionCalculator.defaultQuantityGrams(product)
        mutableState.update { current ->
            current.copy(
                product = product,
                quantityGrams = quantity,
                preview = NutritionCalculator.contribution(product, quantity),
            )
        }
    }

    /** Sets the amount in grams; a value that is not a positive number is ignored. */
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

    /** Logs the chosen food into the chosen meal. Nothing happens when the flow is not ready. */
    public fun confirm() {
        val current = mutableState.value
        if (!current.canConfirm) return
        val product = current.product ?: return
        val mealType = current.mealType ?: return
        val loggedAt = clock.nowEpochMillis()
        val day = dayBoundaryProvider.startOfDay(loggedAt)
        val entry =
            NutritionCalculator.entryFor(
                product = product,
                mealType = mealType,
                quantityGrams = current.quantityGrams,
                loggedAtEpochMillis = loggedAt,
                dayEpochMillis = day,
            )
        scope.launch {
            diaryRepository.addEntry(entry)
        }
    }

    /**
     * Whether the code still has to be looked up in the catalogue.
     *
     * A code that is already the chosen food needs no lookup, and neither does
     * a code whose lookup is already in flight.
     */
    private fun needsResolution(code: String): Boolean = mutableState.value.product?.code != code && resolvingCode != code

    /**
     * Resolves [code] against the catalogue cache.
     *
     * The lookup runs in the injected scope, and the state says that it is in
     * flight while it runs. A code the catalogue does not know leaves the flow
     * empty instead of failing: the user is simply told that the food is not
     * available.
     */
    private fun resolveProduct(code: String) {
        val sessionId = currentSessionId
        resolvingCode = code
        mutableState.update { current -> current.copy(isResolvingProduct = true) }
        scope.launch {
            val product = productSearchRepository.getProduct(code)
            if (currentSessionId != sessionId) return@launch
            if (resolvingCode == code) resolvingCode = null
            mutableState.update { current -> current.withResolvedProduct(product) }
        }
    }

    /**
     * The state after a lookup finished.
     *
     * A food that was chosen by hand while the lookup was in flight wins: the
     * user is never overruled by a lookup that finished late.
     */
    private fun AddFoodState.withResolvedProduct(product: Product?): AddFoodState {
        if (product == null || this.product != null) {
            return copy(isResolvingProduct = false)
        }
        val quantity = NutritionCalculator.defaultQuantityGrams(product)
        return copy(
            product = product,
            quantityGrams = quantity,
            preview = NutritionCalculator.contribution(product, quantity),
            isResolvingProduct = false,
        )
    }
}
