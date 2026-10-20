package com.example.nutricionalregister.feature.search

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchSnapshot
import com.example.nutricionalregister.domain.product.SearchTerm
import com.example.nutricionalregister.feature.search.testing.FakeProductSearchRepository
import com.example.nutricionalregister.feature.search.testing.FakeRecentSearchRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchOutcomeTest {
    @Test
    fun distinguishesOfflineWithAndWithoutCachedProducts() =
        runTest {
            val product = Product("0001", "Oats")
            val term = SearchTerm.fromInput("oats")!!
            val cached = ProductSearchSnapshot(term, listOf(product), 1L)
            val viewModelWithCache =
                ProductSearchViewModel(
                    FakeProductSearchRepository {
                        flowOf(
                            ProductSearchOutcome.Cached(cached),
                            ProductSearchOutcome.Failure(ProductSearchFailure.OFFLINE, cached),
                        )
                    },
                    FakeRecentSearchRepository(),
                    { 2L },
                    backgroundScope,
                )
            viewModelWithCache.onQueryChanged("oats")
            viewModelWithCache.submitSearch()
            runCurrent()

            val viewModelWithoutCache =
                ProductSearchViewModel(
                    FakeProductSearchRepository {
                        flowOf(
                            ProductSearchOutcome.Cached(null),
                            ProductSearchOutcome.Failure(ProductSearchFailure.OFFLINE, null),
                        )
                    },
                    FakeRecentSearchRepository(),
                    { 2L },
                    backgroundScope,
                )
            viewModelWithoutCache.onQueryChanged("oats")
            viewModelWithoutCache.submitSearch()
            runCurrent()

            assertEquals(SearchPhase.OFFLINE_WITH_CACHE, viewModelWithCache.state.value.phase)
            assertEquals(listOf(product), viewModelWithCache.state.value.products)
            assertEquals(SearchPhase.OFFLINE_WITHOUT_CACHE, viewModelWithoutCache.state.value.phase)
        }

    @Test
    fun distinguishesServiceFailureAndMalformedResponseFromNoResults() =
        runTest {
            val serviceFailure = viewModelForFailure(ProductSearchFailure.SERVICE)
            val malformedResponse = viewModelForFailure(ProductSearchFailure.INVALID_RESPONSE)
            val noResults =
                ProductSearchViewModel(
                    FakeProductSearchRepository {
                        flowOf(
                            ProductSearchOutcome.Cached(null),
                            ProductSearchOutcome.Success(
                                ProductSearchSnapshot(SearchTerm.fromInput("none")!!, emptyList(), 1L),
                            ),
                        )
                    },
                    FakeRecentSearchRepository(),
                    { 1L },
                    backgroundScope,
                )

            listOf(serviceFailure, malformedResponse, noResults).forEach { viewModel ->
                viewModel.onQueryChanged("none")
                viewModel.submitSearch()
            }
            runCurrent()

            assertEquals(SearchPhase.ERROR_WITHOUT_CACHE, serviceFailure.state.value.phase)
            assertEquals(SearchPhase.ERROR_WITHOUT_CACHE, malformedResponse.state.value.phase)
            assertEquals(SearchPhase.NO_RESULTS, noResults.state.value.phase)
        }

    @Test
    fun editingQueryReturnsToInitialGuidance() =
        runTest {
            val viewModel = viewModelForFailure(ProductSearchFailure.SERVICE)
            viewModel.onQueryChanged("bad")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged("updated")

            assertEquals(SearchPhase.INITIAL, viewModel.state.value.phase)
            assertEquals("updated", viewModel.state.value.query)
            assertEquals(emptyList(), viewModel.state.value.products)
        }
}

private fun kotlinx.coroutines.test.TestScope.viewModelForFailure(reason: ProductSearchFailure): ProductSearchViewModel =
    ProductSearchViewModel(
        FakeProductSearchRepository {
            flowOf(
                ProductSearchOutcome.Cached(null),
                ProductSearchOutcome.Failure(reason, null),
            )
        },
        FakeRecentSearchRepository(),
        { 1L },
        backgroundScope,
    )
