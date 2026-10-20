package com.example.nutricionalregister.search

import android.content.Context
import com.example.nutricionalregister.data.createProductSearchRuntime
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import kotlinx.coroutines.CoroutineScope

public class ProductSearchDependencies(
    context: Context,
    scope: CoroutineScope,
) : AutoCloseable {
    private val clock = EpochClock { System.currentTimeMillis() }
    private val runtime = createProductSearchRuntime(context, clock)

    public val viewModel: ProductSearchViewModel =
        ProductSearchViewModel(
            productSearchRepository = runtime.productSearchRepository,
            recentSearchRepository = runtime.recentSearchRepository,
            clock = clock,
            scope = scope,
            connectivityStatusProvider = AndroidConnectivityStatusProvider(context),
        )

    override fun close() {
        viewModel.clear()
        runtime.close()
    }
}
