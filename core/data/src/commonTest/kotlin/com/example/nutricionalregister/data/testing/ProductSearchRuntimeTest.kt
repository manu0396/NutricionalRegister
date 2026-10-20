package com.example.nutricionalregister.data.testing

import com.example.nutricionalregister.data.ProductSearchRuntime
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.RecentSearch
import com.example.nutricionalregister.domain.product.RecentSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ProductSearchRuntimeTest {
    @Test
    fun closeDelegatesToCloseResourcesAndKeepsRepositoriesExposed() {
        // GIVEN a runtime wired to stub repositories and a close callback
        val productRepository = StubProductSearchRepository
        val recentRepository = StubRecentSearchRepository
        var closeCalls = 0
        val runtime =
            ProductSearchRuntime(
                productSearchRepository = productRepository,
                recentSearchRepository = recentRepository,
                closeResources = { closeCalls += 1 },
            )

        // THEN both repositories stay exposed and nothing has been closed yet
        assertSame(productRepository, runtime.productSearchRepository)
        assertSame(recentRepository, runtime.recentSearchRepository)
        assertEquals(0, closeCalls)

        // WHEN the runtime is closed twice
        runtime.close()
        runtime.close()

        // THEN the close callback runs once per close
        assertEquals(2, closeCalls)
    }
}

private object StubProductSearchRepository : ProductSearchRepository {
    override fun search(term: SearchTerm): Flow<ProductSearchOutcome> = flowOf()

    override suspend fun getProduct(code: String): Product? = null
}

private object StubRecentSearchRepository : RecentSearchRepository {
    override fun observeRecentSearches(): Flow<List<RecentSearch>> = flowOf(emptyList())

    override suspend fun recordCompletedSearch(
        term: SearchTerm,
        completedAtEpochMillis: Long,
    ) = Unit

    override suspend fun deleteRecentSearch(normalizedTerm: String) = Unit
}
