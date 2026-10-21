package com.example.nutricionalregister.search

import android.content.Context
import com.example.nutricionalregister.data.createProductSearchRuntime
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Composition root for the shared runtime. The platform [PurchaseClient] is passed
 * in by [com.example.nutricionalregister.App] so this class stays free of the Play
 * Billing types and can be constructed from a test with a fake client.
 */
public class ProductSearchDependencies(
    context: Context,
    scope: CoroutineScope,
    purchaseClient: PurchaseClient,
) : AutoCloseable {
    public val clock: EpochClock = EpochClock { System.currentTimeMillis() }
    private val runtime = createProductSearchRuntime(context, clock, purchaseClient)

    public val viewModel: ProductSearchViewModel =
        ProductSearchViewModel(
            productSearchRepository = runtime.productSearchRepository,
            recentSearchRepository = runtime.recentSearchRepository,
            clock = clock,
            scope = scope,
            connectivityStatusProvider = AndroidConnectivityStatusProvider(context),
        )

    public val savedProductRepository = runtime.savedProductRepository

    public val purchaseRepository = runtime.purchaseRepository

    override fun close() {
        viewModel.clear()
        runtime.close()
    }
}
