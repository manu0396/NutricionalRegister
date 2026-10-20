package com.example.nutricionalregister.feature.search

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.ProductSearchSnapshot
import com.example.nutricionalregister.domain.product.RecentSearch
import com.example.nutricionalregister.domain.product.RecentSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import com.example.nutricionalregister.feature.search.testing.FakeProductSearchRepository
import com.example.nutricionalregister.feature.search.testing.FakeRecentSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchPrivacyTest {
    @Test
    fun submittedTermsReachOnlyTheApprovedSearchContract() =
        runTest {
            val repository =
                RecordingProductSearchRepository(
                    FakeProductSearchRepository { term ->
                        flowOf(
                            ProductSearchOutcome.Cached(null),
                            if (term.normalizedTerm == "broken") {
                                ProductSearchOutcome.Failure(ProductSearchFailure.SERVICE, null)
                            } else {
                                ProductSearchOutcome.Success(
                                    ProductSearchSnapshot(
                                        term,
                                        listOf(Product("0001", "${term.displayTerm} product")),
                                        1L,
                                    ),
                                )
                            },
                        )
                    },
                )
            val history = RecordingRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 5L }, backgroundScope)

            viewModel.onQueryChanged("oats")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged("   ")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged("broken")
            viewModel.submitSearch()
            runCurrent()

            assertEquals(SearchPhase.ERROR_WITHOUT_CACHE, viewModel.state.value.phase)
            assertEquals(
                listOf("search:oats", "search:broken"),
                repository.interactions,
            )
            assertEquals(
                listOf("observeRecentSearches", "recordCompletedSearch:oats", "recordCompletedSearch:broken"),
                history.interactions,
            )
            assertEquals(
                listOf("broken", "oats"),
                viewModel.state.value.recentSearches
                    .map { it.normalizedTerm },
            )
        }

    @Test
    fun blankQueriesNeverLeaveTheViewModel() =
        runTest {
            val repository =
                RecordingProductSearchRepository(
                    FakeProductSearchRepository { flow { error("A blank query must not be searched") } },
                )
            val history = RecordingRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 5L }, backgroundScope)

            viewModel.onQueryChanged("   ")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged("")
            viewModel.submitSearch()
            runCurrent()

            assertEquals(emptyList(), repository.interactions)
            assertEquals(listOf("observeRecentSearches"), history.interactions)
            assertEquals(SearchPhase.INITIAL, viewModel.state.value.phase)
        }

    @Test
    fun observableStateEmissionsCarryOnlySearchData() =
        runTest {
            val repository =
                RecordingProductSearchRepository(
                    FakeProductSearchRepository { term ->
                        flowOf(
                            ProductSearchOutcome.Cached(null),
                            ProductSearchOutcome.Success(
                                ProductSearchSnapshot(
                                    term,
                                    listOf(Product("0001", "Oats")),
                                    1L,
                                ),
                            ),
                        )
                    },
                )
            val history = RecordingRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 5L }, backgroundScope)
            val emissions = mutableListOf<ProductSearchState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.state.toList(emissions)
            }

            viewModel.onQueryChanged("oats")
            viewModel.submitSearch()
            runCurrent()

            assertEquals(
                listOf(
                    SearchPhase.INITIAL,
                    SearchPhase.INITIAL,
                    SearchPhase.LOADING,
                    SearchPhase.REFRESHING,
                    SearchPhase.RESULTS,
                    SearchPhase.RESULTS,
                ),
                emissions.map { it.phase },
            )
            assertEquals(
                listOf("search:oats"),
                repository.interactions,
            )
            assertEquals(
                listOf("observeRecentSearches", "recordCompletedSearch:oats"),
                history.interactions,
            )
        }
}

private class RecordingProductSearchRepository(
    private val delegate: FakeProductSearchRepository,
) : ProductSearchRepository by delegate {
    val interactions = mutableListOf<String>()

    override fun search(term: SearchTerm): Flow<ProductSearchOutcome> {
        interactions += "search:${term.displayTerm}"
        return delegate.search(term)
    }

    override suspend fun getProduct(code: String): Product? {
        interactions += "getProduct:$code"
        return delegate.getProduct(code)
    }
}

private class RecordingRecentSearchRepository(
    private val delegate: FakeRecentSearchRepository = FakeRecentSearchRepository(),
) : RecentSearchRepository by delegate {
    val interactions = mutableListOf<String>()

    override fun observeRecentSearches(): Flow<List<RecentSearch>> {
        interactions += "observeRecentSearches"
        return delegate.observeRecentSearches()
    }

    override suspend fun recordCompletedSearch(
        term: SearchTerm,
        completedAtEpochMillis: Long,
    ) {
        interactions += "recordCompletedSearch:${term.normalizedTerm}"
        delegate.recordCompletedSearch(term, completedAtEpochMillis)
    }

    override suspend fun deleteRecentSearch(normalizedTerm: String) {
        interactions += "deleteRecentSearch:$normalizedTerm"
        delegate.deleteRecentSearch(normalizedTerm)
    }
}
