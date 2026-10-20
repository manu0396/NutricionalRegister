package com.example.nutricionalregister.feature.search

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchSnapshot
import com.example.nutricionalregister.domain.product.SearchTerm
import com.example.nutricionalregister.feature.search.testing.FakeProductSearchRepository
import com.example.nutricionalregister.feature.search.testing.FakeRecentSearchRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProductSearchViewModelTest {
    @Test
    fun cachedResultsAppearBeforeRefreshFinishes() =
        runTest {
            val term = SearchTerm.fromInput("oat")!!
            val cachedProduct = Product("cached", "Cached oats")
            val refreshedProduct = Product("remote", "Remote oats")
            val releaseRefresh = CompletableDeferred<ProductSearchOutcome>()
            val repository =
                FakeProductSearchRepository {
                    flow {
                        emit(
                            ProductSearchOutcome.Cached(
                                ProductSearchSnapshot(term, listOf(cachedProduct), 1L),
                            ),
                        )
                        emit(releaseRefresh.await())
                    }
                }
            val history = FakeRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 2L }, backgroundScope)

            viewModel.onQueryChanged("oat")
            viewModel.submitSearch()
            runCurrent()

            assertEquals(SearchPhase.REFRESHING, viewModel.state.value.phase)
            assertEquals(listOf(cachedProduct), viewModel.state.value.products)
            assertEquals(true, viewModel.state.value.isRefreshing)

            releaseRefresh.complete(
                ProductSearchOutcome.Success(
                    ProductSearchSnapshot(term, listOf(refreshedProduct), 2L),
                ),
            )
            runCurrent()

            assertEquals(SearchPhase.RESULTS, viewModel.state.value.phase)
            assertEquals(listOf(refreshedProduct), viewModel.state.value.products)
            assertEquals(
                "oat",
                viewModel.state.value.recentSearches
                    .single()
                    .normalizedTerm,
            )
        }

    @Test
    fun blankSubmissionDoesNotCallRepositoryOrCreateHistory() =
        runTest {
            val repository = FakeProductSearchRepository { flow { error("A blank query must not be searched") } }
            val history = FakeRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 2L }, backgroundScope)

            viewModel.onQueryChanged("   ")
            viewModel.submitSearch()
            runCurrent()

            assertEquals(0, repository.requestedTerms.size)
            assertEquals(0, viewModel.state.value.recentSearches.size)
            assertEquals(SearchPhase.INITIAL, viewModel.state.value.phase)
        }

    @Test
    fun latestSubmittedQueryWinsWhenResponsesCompleteOutOfOrder() =
        runTest {
            val first = CompletableDeferred<ProductSearchOutcome>()
            val second = CompletableDeferred<ProductSearchOutcome>()
            val repository =
                FakeProductSearchRepository { term ->
                    flow {
                        emit(ProductSearchOutcome.Cached(null))
                        emit(if (term.normalizedTerm == "first") first.await() else second.await())
                    }
                }
            val history = FakeRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 9L }, backgroundScope)

            viewModel.onQueryChanged("first")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged("second")
            viewModel.submitSearch()
            runCurrent()
            second.complete(
                ProductSearchOutcome.Success(
                    ProductSearchSnapshot(
                        SearchTerm.fromInput("second")!!,
                        listOf(Product("second", "Second product")),
                        2L,
                    ),
                ),
            )
            runCurrent()
            first.complete(
                ProductSearchOutcome.Success(
                    ProductSearchSnapshot(
                        SearchTerm.fromInput("first")!!,
                        listOf(Product("first", "First product")),
                        1L,
                    ),
                ),
            )
            runCurrent()

            assertEquals("second", viewModel.state.value.query)
            assertEquals(
                "second",
                viewModel.state.value.products
                    .single()
                    .code,
            )
        }
}
