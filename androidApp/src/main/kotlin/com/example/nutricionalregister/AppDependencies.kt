package com.example.nutricionalregister

import android.content.Context
import com.example.nutricionalregister.data.createProductSearchRuntime
import com.example.nutricionalregister.domain.assistant.AssistantActionExecutor
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.feature.assistant.CoachViewModel
import com.example.nutricionalregister.feature.diary.AddFoodViewModel
import com.example.nutricionalregister.feature.diary.DiaryViewModel
import com.example.nutricionalregister.feature.savedproducts.PremiumPurchaseViewModel
import com.example.nutricionalregister.feature.savedproducts.SavedProductsViewModel
import com.example.nutricionalregister.feature.scan.BarcodeResolution
import com.example.nutricionalregister.feature.scan.BarcodeResolver
import com.example.nutricionalregister.feature.search.ProductDetailViewModel
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import com.example.nutricionalregister.feature.summary.SummaryViewModel
import com.example.nutricionalregister.search.AndroidConnectivityStatusProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The composition root of the Android app.
 *
 * Everything the app needs is built once, here, and handed to the screens as
 * plain state and plain callbacks. The screens never build a repository, never
 * hold a view model and never navigate on their own, so the whole app can be
 * reasoned about from this one place (ADR-0015).
 *
 * The class owns the lifetime of everything it creates: the shared runtime, the
 * HTTP client, the database and the flow of every view model. The caller closes
 * it when the app leaves the composition.
 *
 * @param context the Android context the platform pieces need.
 * @param scope the scope that owns the collections of the view models.
 * @param purchaseClient the platform purchase client, so this class never
 *   depends on the Play Billing types.
 * @param aiGatewayBaseUrl the address of the first-party AI gateway. An empty
 *   address disables the assistant for this build (ADR-0016, FR-025).
 */
public class AppDependencies(
    context: Context,
    private val scope: CoroutineScope,
    purchaseClient: PurchaseClient,
    aiGatewayBaseUrl: String,
) : AutoCloseable {
    /** The clock every view model reads, so the whole app agrees on the time. */
    public val clock: EpochClock = EpochClock { System.currentTimeMillis() }

    private val runtime =
        createProductSearchRuntime(
            context = context,
            clock = clock,
            purchaseClient = purchaseClient,
            aiGatewayBaseUrl = aiGatewayBaseUrl,
        )

    private val barcodeResolver = BarcodeResolver(runtime.productSearchRepository)

    /** The diary of the day and the goal it is measured against. */
    public val diaryViewModel: DiaryViewModel =
        DiaryViewModel(
            diaryRepository = runtime.diaryRepository,
            goalRepository = runtime.nutritionGoalRepository,
            dayBoundaryProvider = runtime.dayBoundaryProvider,
            clock = clock,
            scope = scope,
        )

    /** The add-food flow. It outlives the two steps of the flow, so it lives here. */
    public val addFoodViewModel: AddFoodViewModel =
        AddFoodViewModel(
            diaryRepository = runtime.diaryRepository,
            productSearchRepository = runtime.productSearchRepository,
            dayBoundaryProvider = runtime.dayBoundaryProvider,
            clock = clock,
            scope = scope,
        )

    /** The seven-day summary. */
    public val summaryViewModel: SummaryViewModel =
        SummaryViewModel(
            diaryRepository = runtime.diaryRepository,
            dayBoundaryProvider = runtime.dayBoundaryProvider,
            clock = clock,
            scope = scope,
        )

    /** The personal food library. */
    public val libraryViewModel: SavedProductsViewModel =
        SavedProductsViewModel(
            savedProductRepository = runtime.savedProductRepository,
            clock = clock,
            scope = scope,
        )

    /** The catalogue search the add-food flow uses to find a food. */
    public val searchViewModel: ProductSearchViewModel =
        ProductSearchViewModel(
            productSearchRepository = runtime.productSearchRepository,
            recentSearchRepository = runtime.recentSearchRepository,
            clock = clock,
            scope = scope,
            connectivityStatusProvider = AndroidConnectivityStatusProvider(context),
        )

    /** The conversational assistant. */
    public val coachViewModel: CoachViewModel =
        CoachViewModel(
            assistantClient = runtime.assistantClient,
            actionExecutor =
                AssistantActionExecutor(
                    diaryRepository = runtime.diaryRepository,
                    productRepository = runtime.productSearchRepository,
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
            connectivityStatusProvider = AndroidConnectivityStatusProvider(context),
        )

    /** The single premium unlock, sold in the More screen and nowhere else. */
    public val premiumPurchaseViewModel: PremiumPurchaseViewModel =
        PremiumPurchaseViewModel(
            purchaseRepository = runtime.purchaseRepository,
            scope = scope,
            queryProduct = { code -> purchaseClient.queryProduct(code)?.amountLabel },
        )

    /**
     * Resolves a scanned barcode and reports the product it belongs to.
     *
     * A code the offline catalogue does not know is reported as null, and the
     * caller falls back to a search by name, which is the honest outcome: the
     * app never invents a food from a barcode it cannot resolve.
     */
    public fun resolveBarcode(
        code: String,
        onResolved: (BarcodeResolution) -> Unit,
    ) {
        scope.launch { onResolved(barcodeResolver.resolve(code)) }
    }

    /** The product with [code], or null when the catalogue does not know it. */
    public suspend fun product(code: String): Product? = runtime.productSearchRepository.getProduct(code)

    /**
     * The view model of the product detail of [code].
     *
     * The detail is a route, so the view model belongs to the route and not to
     * the app: it is created for one code and discarded with the route, so no
     * stale detail can survive a change of destination.
     *
     * @param code the product the detail shows.
     * @param scope the scope that owns the load; the caller clears it on
     *   dispose.
     */
    public fun productDetailViewModel(
        code: String,
        scope: CoroutineScope,
    ): ProductDetailViewModel =
        ProductDetailViewModel(
            productSearchRepository = runtime.productSearchRepository,
            savedProductRepository = runtime.savedProductRepository,
            scope = scope,
        ).apply { load(code) }

    override fun close() {
        searchViewModel.clear()
        premiumPurchaseViewModel.clear()
        libraryViewModel.clear()
        summaryViewModel.clear()
        diaryViewModel.clear()
        runtime.close()
    }
}
