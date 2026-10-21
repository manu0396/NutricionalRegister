package com.example.nutricionalregister.savedproducts

import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import com.example.nutricionalregister.feature.savedproducts.SavedProductsViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * Composition root for the Saved products destination (mirrors
 * `ProductSearchDependencies`).
 *
 * It takes the already-built [savedProductRepository] and shared [clock] from the
 * app's single runtime, so the destination reads and writes the same on-device
 * store as the rest of the app.
 */
public class SavedProductsDependencies(
    savedProductRepository: SavedProductRepository,
    clock: EpochClock,
    scope: CoroutineScope,
) : AutoCloseable {
    public val viewModel: SavedProductsViewModel =
        SavedProductsViewModel(
            savedProductRepository = savedProductRepository,
            clock = clock,
            scope = scope,
        )

    override fun close() {
        viewModel.clear()
    }
}
