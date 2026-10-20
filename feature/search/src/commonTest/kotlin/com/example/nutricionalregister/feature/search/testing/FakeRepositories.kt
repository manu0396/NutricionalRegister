package com.example.nutricionalregister.feature.search.testing

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

internal class FakeProductSearchRepository(
    var responses: (SearchTerm) -> Flow<ProductSearchOutcome>,
) : ProductSearchRepository {
    val requestedTerms = mutableListOf<SearchTerm>()
    val productsByCode = mutableMapOf<String, Product>()

    override fun search(term: SearchTerm): Flow<ProductSearchOutcome> =
        flow {
            requestedTerms += term
            emitAll(responses(term))
        }

    override suspend fun getProduct(code: String): Product? = productsByCode[code]
}

internal class FakeRecentSearchRepository : RecentSearchRepository {
    private val recent = MutableStateFlow<List<RecentSearch>>(emptyList())
    private var sequence = 0L

    override fun observeRecentSearches(): Flow<List<RecentSearch>> = recent

    fun seed(searches: List<RecentSearch>) {
        sequence = searches.maxOfOrNull(RecentSearch::completionSequence) ?: 0L
        recent.value = searches.sortedByDescending(RecentSearch::completionSequence)
    }

    override suspend fun recordCompletedSearch(
        term: SearchTerm,
        completedAtEpochMillis: Long,
    ) {
        sequence += 1
        val entry =
            RecentSearch(
                normalizedTerm = term.normalizedTerm,
                displayTerm = term.displayTerm,
                lastCompletedAtEpochMillis = completedAtEpochMillis,
                completionSequence = sequence,
            )
        recent.value =
            (listOf(entry) + recent.value.filterNot { it.normalizedTerm == term.normalizedTerm })
                .take(10)
    }

    override suspend fun deleteRecentSearch(normalizedTerm: String) {
        recent.value = recent.value.filterNot { it.normalizedTerm == normalizedTerm }
    }
}
