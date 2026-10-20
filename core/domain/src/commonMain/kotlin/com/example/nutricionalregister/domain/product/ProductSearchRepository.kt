package com.example.nutricionalregister.domain.product

import kotlinx.coroutines.flow.Flow

public interface ProductSearchRepository {
    public fun search(term: SearchTerm): Flow<ProductSearchOutcome>

    public suspend fun getProduct(code: String): Product?
}

public fun interface ProductCatalogue {
    public suspend fun search(term: SearchTerm): List<Product>
}

public fun interface EpochClock {
    public fun nowEpochMillis(): Long
}
