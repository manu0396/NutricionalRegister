package com.example.nutricionalregister.data

import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.RecentSearchRepository

public class ProductSearchRuntime internal constructor(
    public val productSearchRepository: ProductSearchRepository,
    public val recentSearchRepository: RecentSearchRepository,
    private val closeResources: () -> Unit,
) : AutoCloseable {
    override fun close() {
        closeResources()
    }
}
