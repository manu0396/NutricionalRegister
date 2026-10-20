package com.example.nutricionalregister.search

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.RecentSearch
import com.example.nutricionalregister.domain.product.RecentSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class TestProductSearchRepository(
    private val response: (SearchTerm) -> Flow<ProductSearchOutcome>,
) : ProductSearchRepository {
    val requestedTerms = mutableListOf<SearchTerm>()
    private val products = mutableMapOf<String, Product>()

    override fun search(term: SearchTerm): Flow<ProductSearchOutcome> =
        flow {
            requestedTerms += term
            emitAll(response(term))
        }

    override suspend fun getProduct(code: String): Product? = products[code]
}

internal class TestRecentSearchRepository : RecentSearchRepository {
    private val searches = MutableStateFlow<List<RecentSearch>>(emptyList())
    private var sequence = 0L

    override fun observeRecentSearches(): Flow<List<RecentSearch>> = searches

    fun seed(entries: List<RecentSearch>) {
        sequence = entries.maxOfOrNull(RecentSearch::completionSequence) ?: 0L
        searches.value = entries.sortedByDescending(RecentSearch::completionSequence)
    }

    override suspend fun recordCompletedSearch(
        term: SearchTerm,
        completedAtEpochMillis: Long,
    ) {
        sequence += 1
        searches.value =
            (
                listOf(
                    RecentSearch(
                        term.normalizedTerm,
                        term.displayTerm,
                        completedAtEpochMillis,
                        sequence,
                    ),
                ) + searches.value.filterNot { it.normalizedTerm == term.normalizedTerm }
            ).take(10)
    }

    override suspend fun deleteRecentSearch(normalizedTerm: String) {
        searches.value = searches.value.filterNot { it.normalizedTerm == normalizedTerm }
    }
}
