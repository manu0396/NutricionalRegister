package com.example.nutricionalregister.feature.search

import com.example.nutricionalregister.domain.product.ConnectivityStatus
import com.example.nutricionalregister.domain.product.ConnectivityStatusProvider
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchSnapshot
import com.example.nutricionalregister.domain.product.RecentSearch
import com.example.nutricionalregister.domain.product.SearchTerm
import com.example.nutricionalregister.feature.search.testing.FakeProductSearchRepository
import com.example.nutricionalregister.feature.search.testing.FakeRecentSearchRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecentSearchViewModelTest {
    @Test
    fun historyIsVisibleOfflineAndSelectingCachedTermReusesCache() =
        runTest {
            val product = Product("0001", "Oat drink")
            val term = SearchTerm.fromInput("oat drink")!!
            val cached = ProductSearchSnapshot(term, listOf(product), 10L)
            val history = FakeRecentSearchRepository()
            history.seed(listOf(RecentSearch("oat drink", "Oat Drink", 10L, 1L)))
            val repository =
                FakeProductSearchRepository {
                    flowOf(
                        ProductSearchOutcome.Cached(cached),
                        ProductSearchOutcome.Failure(ProductSearchFailure.OFFLINE, cached),
                    )
                }
            val viewModel =
                ProductSearchViewModel(
                    repository,
                    history,
                    { 20L },
                    backgroundScope,
                    ConnectivityStatusProvider { ConnectivityStatus.OFFLINE },
                )
            runCurrent()

            viewModel.selectRecentSearch("oat drink")
            runCurrent()

            assertEquals("Oat Drink", viewModel.state.value.query)
            assertEquals(SearchPhase.OFFLINE_WITH_CACHE, viewModel.state.value.phase)
            assertEquals(listOf(product), viewModel.state.value.products)
            assertEquals(1, repository.requestedTerms.size)
        }

    @Test
    fun uncachedRecentTermShowsOfflineGuidanceAndCanBeDeletedIndividually() =
        runTest {
            val history = FakeRecentSearchRepository()
            history.seed(listOf(RecentSearch("uncached", "Uncached", 10L, 1L)))
            val repository =
                FakeProductSearchRepository {
                    flowOf(
                        ProductSearchOutcome.Cached(null),
                        ProductSearchOutcome.Failure(ProductSearchFailure.OFFLINE, null),
                    )
                }
            val viewModel =
                ProductSearchViewModel(
                    repository,
                    history,
                    { 20L },
                    backgroundScope,
                    ConnectivityStatusProvider { ConnectivityStatus.OFFLINE },
                )
            runCurrent()

            viewModel.selectRecentSearch("uncached")
            runCurrent()
            assertEquals(SearchPhase.OFFLINE_WITHOUT_CACHE, viewModel.state.value.phase)

            viewModel.deleteRecentSearch("uncached")
            runCurrent()

            assertEquals(emptyList(), viewModel.state.value.recentSearches)
        }

    @Test
    fun completedRepeatedTermIsPromotedWithoutDuplication() =
        runTest {
            val history = FakeRecentSearchRepository()
            val repository =
                FakeProductSearchRepository { term ->
                    flowOf(
                        ProductSearchOutcome.Cached(null),
                        ProductSearchOutcome.Success(
                            ProductSearchSnapshot(term, emptyList(), 10L),
                        ),
                    )
                }
            val viewModel = ProductSearchViewModel(repository, history, { 20L }, backgroundScope)

            viewModel.onQueryChanged("oats")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged("beans")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged(" oats ")
            viewModel.submitSearch()
            runCurrent()

            assertEquals(
                listOf("oats", "beans"),
                viewModel.state.value.recentSearches
                    .map { it.normalizedTerm },
            )
        }
}
