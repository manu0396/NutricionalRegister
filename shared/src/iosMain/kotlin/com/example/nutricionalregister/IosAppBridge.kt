package com.example.nutricionalregister

import com.example.nutricionalregister.data.ProductSearchRuntime
import com.example.nutricionalregister.data.createProductSearchRuntime
import com.example.nutricionalregister.domain.assistant.AssistantActionExecutor
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.domain.summary.WeeklySummary
import com.example.nutricionalregister.feature.assistant.AssistantState
import com.example.nutricionalregister.feature.assistant.CoachViewModel
import com.example.nutricionalregister.feature.diary.AddFoodSource
import com.example.nutricionalregister.feature.diary.AddFoodState
import com.example.nutricionalregister.feature.diary.AddFoodViewModel
import com.example.nutricionalregister.feature.diary.DiaryState
import com.example.nutricionalregister.feature.diary.DiaryViewModel
import com.example.nutricionalregister.feature.savedproducts.PremiumPurchaseState
import com.example.nutricionalregister.feature.savedproducts.PremiumPurchaseViewModel
import com.example.nutricionalregister.feature.savedproducts.SavedProductsState
import com.example.nutricionalregister.feature.savedproducts.SavedProductsViewModel
import com.example.nutricionalregister.feature.scan.BarcodeResolution
import com.example.nutricionalregister.feature.scan.BarcodeResolver
import com.example.nutricionalregister.feature.search.ProductDetailState
import com.example.nutricionalregister.feature.search.ProductDetailViewModel
import com.example.nutricionalregister.feature.search.ProductSearchState
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import com.example.nutricionalregister.feature.summary.SummaryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS-facing bridge over the full shared runtime.
 *
 * This replaces the narrow feature-004 bridge. It owns the runtime and every
 * view model, exposes their state through closure subscriptions and forwards
 * every action, so the SwiftUI shell never duplicates business logic
 * (ADR-0015, ADR-0019).
 */
public class IosAppBridge internal constructor(
    private val runtime: ProductSearchRuntime,
    private val purchaseClient: PurchaseClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val diaryViewModel: DiaryViewModel
    private val addFoodViewModel: AddFoodViewModel
    private val summaryViewModel: SummaryViewModel
    private val libraryViewModel: SavedProductsViewModel
    private val searchViewModel: ProductSearchViewModel
    private val coachViewModel: CoachViewModel
    private val premiumPurchaseViewModel: PremiumPurchaseViewModel
    private val barcodeResolver: BarcodeResolver

    private val jobs = mutableMapOf<Long, Job>()
    private var nextSubscriptionId: Long = 0L

    init {
        val clock = EpochClock { (NSDate().timeIntervalSince1970 * 1000.0).toLong() }
        diaryViewModel =
            DiaryViewModel(
                diaryRepository = runtime.diaryRepository,
                goalRepository = runtime.nutritionGoalRepository,
                dayBoundaryProvider = runtime.dayBoundaryProvider,
                clock = clock,
                scope = scope,
            )
        addFoodViewModel =
            AddFoodViewModel(
                diaryRepository = runtime.diaryRepository,
                productSearchRepository = runtime.productSearchRepository,
                dayBoundaryProvider = runtime.dayBoundaryProvider,
                clock = clock,
                scope = scope,
            )
        summaryViewModel =
            SummaryViewModel(
                diaryRepository = runtime.diaryRepository,
                dayBoundaryProvider = runtime.dayBoundaryProvider,
                clock = clock,
                scope = scope,
            )
        libraryViewModel =
            SavedProductsViewModel(
                savedProductRepository = runtime.savedProductRepository,
                clock = clock,
                scope = scope,
            )
        searchViewModel =
            ProductSearchViewModel(
                productSearchRepository = runtime.productSearchRepository,
                recentSearchRepository = runtime.recentSearchRepository,
                clock = clock,
                scope = scope,
            )
        coachViewModel =
            CoachViewModel(
                assistantClient = runtime.assistantClient,
                actionExecutor =
                    AssistantActionExecutor(
                        diaryRepository = runtime.diaryRepository,
                        productRepository = runtime.productSearchRepository,
                        savedProductRepository = runtime.savedProductRepository,
                        clock = clock,
                        dayBoundaryProvider = runtime.dayBoundaryProvider,
                    ),
                diaryRepository = runtime.diaryRepository,
                goalRepository = runtime.nutritionGoalRepository,
                savedProductRepository = runtime.savedProductRepository,
                dayBoundaryProvider = runtime.dayBoundaryProvider,
                clock = clock,
                scope = scope,
                isConfigured = runtime.isAssistantConfigured,
                connectivityStatusProvider = null,
            )
        premiumPurchaseViewModel =
            PremiumPurchaseViewModel(
                purchaseRepository = runtime.purchaseRepository,
                scope = scope,
                queryProduct = { code -> purchaseClient.queryProduct(code)?.amountLabel },
            )
        barcodeResolver = BarcodeResolver(runtime.productSearchRepository)
    }

    /** Whether the AI gateway is configured for this build. */
    public val isAssistantConfigured: Boolean
        get() = runtime.isAssistantConfigured

    /** The single premium product code. */
    public val premiumProductCode: String
        get() = com.example.nutricionalregister.domain.premium.PremiumProduct.CODE

    // region Diary
    public fun observeDiaryState(onChange: (DiaryState) -> Unit): Long = subscribe(diaryViewModel.state) { state -> onChange(state) }

    public fun refreshDiary() {
        diaryViewModel.refresh()
    }

    public fun removeDiaryEntry(id: Long) {
        diaryViewModel.removeEntry(id)
    }

    public fun updateGoal(goal: NutritionGoal) {
        diaryViewModel.updateGoal(goal)
    }
    // endregion

    // region Add food
    public fun observeAddFoodState(onChange: (AddFoodState) -> Unit): Long = subscribe(addFoodViewModel.state) { state -> onChange(state) }

    public fun startAddFood(
        sessionId: String,
        meal: String? = null,
        productCode: String? = null,
    ) {
        val mealType = meal?.let { runCatching { MealType.fromStorage(it) }.getOrNull() }
        addFoodViewModel.start(
            sessionId = sessionId,
            meal = mealType,
            productCode = productCode,
        )
    }

    public fun selectAddFoodSource(source: String) {
        val resolved = runCatching { AddFoodSource.valueOf(source.uppercase()) }.getOrNull() ?: return
        addFoodViewModel.selectSource(resolved)
    }

    public fun setAddFoodQuery(query: String) {
        addFoodViewModel.setQuery(query)
    }

    public fun selectAddFoodProduct(product: com.example.nutricionalregister.domain.product.Product) {
        addFoodViewModel.selectProduct(product)
    }

    public fun setAddFoodQuantity(grams: Double) {
        addFoodViewModel.setQuantity(grams)
    }

    public fun openAddFoodMealSheet() {
        addFoodViewModel.openMealSheet()
    }

    public fun closeAddFoodMealSheet() {
        addFoodViewModel.closeMealSheet()
    }

    public fun selectAddFoodMeal(meal: String) {
        val resolved = runCatching { MealType.fromStorage(meal) }.getOrNull() ?: return
        addFoodViewModel.selectMeal(resolved)
    }

    public fun confirmAddFood() {
        addFoodViewModel.confirm()
    }
    // endregion

    // region Search
    public fun observeSearchState(onChange: (ProductSearchState) -> Unit): Long =
        subscribe(searchViewModel.state) { state -> onChange(state) }

    public fun setSearchQuery(query: String) {
        searchViewModel.onQueryChanged(query)
    }

    public fun submitSearch() {
        searchViewModel.submitSearch()
    }

    public fun retrySearch() {
        searchViewModel.retrySearch()
    }

    public fun selectRecentSearch(normalizedTerm: String) {
        searchViewModel.selectRecentSearch(normalizedTerm)
    }

    public fun deleteRecentSearch(normalizedTerm: String) {
        searchViewModel.deleteRecentSearch(normalizedTerm)
    }

    public fun openSearchProduct(code: String) {
        searchViewModel.openProduct(code)
    }

    public fun closeSearchProductDetails() {
        searchViewModel.closeProductDetails()
    }
    // endregion

    // region Library
    public fun observeLibraryState(onChange: (SavedProductsState) -> Unit): Long =
        subscribe(libraryViewModel.state) { state -> onChange(state) }

    public fun saveProduct(product: com.example.nutricionalregister.domain.product.Product) {
        libraryViewModel.save(product)
    }

    public fun removeSavedProduct(code: String) {
        libraryViewModel.remove(code)
    }
    // endregion

    // region Product detail
    public fun createProductDetailHandle(code: String): ProductDetailHandle {
        val handleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val viewModel =
            ProductDetailViewModel(
                productSearchRepository = runtime.productSearchRepository,
                savedProductRepository = runtime.savedProductRepository,
                scope = handleScope,
            ).apply { load(code) }
        return ProductDetailHandle(viewModel, handleScope)
    }
    // endregion

    // region Summary
    public fun observeWeeklySummary(onChange: (WeeklySummary?) -> Unit): Long =
        subscribe(summaryViewModel.weeklySummary) { summary -> onChange(summary) }

    public fun refreshWeekly() {
        summaryViewModel.refresh()
    }
    // endregion

    // region Coach
    public fun observeCoachState(onChange: (AssistantState) -> Unit): Long = subscribe(coachViewModel.state) { state -> onChange(state) }

    public fun sendCoachMessage(text: String) {
        coachViewModel.send(text)
    }

    public fun grantCoachConsent() {
        coachViewModel.grantConsent()
    }

    public fun confirmCoachProposal() {
        coachViewModel.confirmProposal()
    }

    public fun dismissCoachProposal() {
        coachViewModel.dismissProposal()
    }
    // endregion

    // region Premium
    public fun observePremiumState(onChange: (PremiumPurchaseState) -> Unit): Long =
        subscribe(premiumPurchaseViewModel.state) { state -> onChange(state) }

    public fun refreshPremium() {
        premiumPurchaseViewModel.refresh()
    }

    public fun startPremiumPurchase() {
        premiumPurchaseViewModel.startPurchase()
    }

    public fun restorePremiumPurchases() {
        premiumPurchaseViewModel.restore()
    }
    // endregion

    // region Barcode
    public fun resolveBarcode(
        code: String,
        onResolved: (BarcodeResolution) -> Unit,
    ) {
        scope.launch { onResolved(barcodeResolver.resolve(code)) }
    }
    // endregion

    // region Utilities
    public fun getProduct(
        code: String,
        onResult: (com.example.nutricionalregister.domain.product.Product?) -> Unit,
    ) {
        scope.launch { onResult(runtime.productSearchRepository.getProduct(code)) }
    }
    // endregion

    // region Lifecycle
    public fun cancelSubscription(subscriptionId: Long) {
        jobs.remove(subscriptionId)?.cancel()
    }

    public fun close() {
        searchViewModel.clear()
        premiumPurchaseViewModel.clear()
        libraryViewModel.clear()
        summaryViewModel.clear()
        diaryViewModel.clear()
        runtime.close()
        scope.cancel()
        jobs.values.forEach(Job::cancel)
        jobs.clear()
    }
    // endregion

    private fun <T> subscribe(
        flow: Flow<T>,
        onEach: (T) -> Unit,
    ): Long {
        val subscriptionId = ++nextSubscriptionId
        jobs[subscriptionId] =
            scope.launch {
                flow.collect { value -> onEach(value) }
            }
        return subscriptionId
    }
}

public class ProductDetailHandle internal constructor(
    private val viewModel: ProductDetailViewModel,
    private val scope: CoroutineScope,
) {
    private val jobs = mutableMapOf<Long, Job>()
    private var nextSubscriptionId: Long = 0L

    public fun observeState(onChange: (ProductDetailState) -> Unit): Long {
        val subscriptionId = ++nextSubscriptionId
        jobs[subscriptionId] =
            scope.launch {
                viewModel.state.collect { state -> onChange(state) }
            }
        return subscriptionId
    }

    public fun load(code: String) {
        viewModel.load(code)
    }

    public fun clear() {
        viewModel.clear()
        jobs.values.forEach(Job::cancel)
        jobs.clear()
    }

    public fun cancelSubscription(subscriptionId: Long) {
        jobs.remove(subscriptionId)?.cancel()
    }
}

public fun createIosAppBridge(
    purchaseClient: PurchaseClient,
    aiGatewayBaseUrl: String = "",
): IosAppBridge {
    val clock = EpochClock { (NSDate().timeIntervalSince1970 * 1000.0).toLong() }
    return IosAppBridge(
        runtime =
            createProductSearchRuntime(
                clock = clock,
                purchaseClient = purchaseClient,
                aiGatewayBaseUrl = aiGatewayBaseUrl,
            ),
        purchaseClient = purchaseClient,
    )
}
