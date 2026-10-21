package com.example.nutricionalregister.data

import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.RecentSearchRepository
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.domain.purchase.SavedProductRepository

public class ProductSearchRuntime internal constructor(
    public val productSearchRepository: ProductSearchRepository,
    public val recentSearchRepository: RecentSearchRepository,
    public val savedProductRepository: SavedProductRepository,
    public val purchaseRepository: PurchaseRepository,
    private val closeResources: () -> Unit,
) : AutoCloseable {
    override fun close() {
        closeResources()
    }
}
