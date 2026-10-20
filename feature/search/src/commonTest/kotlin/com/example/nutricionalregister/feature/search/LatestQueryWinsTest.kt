package com.example.nutricionalregister.feature.search

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchSnapshot
import com.example.nutricionalregister.domain.product.SearchTerm
import com.example.nutricionalregister.feature.search.testing.FakeProductSearchRepository
import com.example.nutricionalregister.feature.search.testing.FakeRecentSearchRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LatestQueryWinsTest {
    @Test
    fun lateResponseForOlderQueryCannotOverwriteNewerQueryResults() =
        runTest {
            val firstResponse = CompletableDeferred<ProductSearchOutcome>()
            val secondResponse = CompletableDeferred<ProductSearchOutcome>()
            val repository =
                FakeProductSearchRepository { term ->
                    flow {
                        emit(ProductSearchOutcome.Cached(null))
                        emit(
                            if (term.normalizedTerm == "first") {
                                firstResponse.await()
                            } else {
                                secondResponse.await()
                            },
                        )
                    }
                }
            val history = FakeRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 2L }, backgroundScope)

            viewModel.onQueryChanged("first")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged("second")
            viewModel.submitSearch()
            runCurrent()
            secondResponse.complete(
                ProductSearchOutcome.Success(
                    ProductSearchSnapshot(
                        SearchTerm.fromInput("second")!!,
                        listOf(Product("second", "Second product")),
                        2L,
                    ),
                ),
            )
            runCurrent()
            val stateWithLatestResults = viewModel.state.value

            assertEquals(SearchPhase.RESULTS, stateWithLatestResults.phase)
            assertEquals("second", stateWithLatestResults.query)
            assertEquals(listOf("second"), stateWithLatestResults.products.map(Product::code))

            firstResponse.complete(
                ProductSearchOutcome.Success(
                    ProductSearchSnapshot(
                        SearchTerm.fromInput("first")!!,
                        listOf(Product("first", "First product")),
                        1L,
                    ),
                ),
            )
            runCurrent()

            assertEquals(stateWithLatestResults, viewModel.state.value)
            assertEquals(
                listOf("second"),
                viewModel.state.value.recentSearches
                    .map { it.normalizedTerm },
            )
        }

    @Test
    fun lateFailureForOlderQueryCannotOverwriteNewerQuerySuccess() =
        runTest {
            val firstResponse = CompletableDeferred<ProductSearchOutcome>()
            val secondResponse = CompletableDeferred<ProductSearchOutcome>()
            val repository =
                FakeProductSearchRepository { term ->
                    flow {
                        emit(ProductSearchOutcome.Cached(null))
                        emit(
                            if (term.normalizedTerm == "first") {
                                firstResponse.await()
                            } else {
                                secondResponse.await()
                            },
                        )
                    }
                }
            val history = FakeRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 2L }, backgroundScope)

            viewModel.onQueryChanged("first")
            viewModel.submitSearch()
            runCurrent()
            viewModel.onQueryChanged("second")
            viewModel.submitSearch()
            runCurrent()
            secondResponse.complete(
                ProductSearchOutcome.Success(
                    ProductSearchSnapshot(
                        SearchTerm.fromInput("second")!!,
                        listOf(Product("second", "Second product")),
                        2L,
                    ),
                ),
            )
            runCurrent()

            firstResponse.complete(ProductSearchOutcome.Failure(ProductSearchFailure.SERVICE, null))
            runCurrent()

            assertEquals(SearchPhase.RESULTS, viewModel.state.value.phase)
            assertEquals("second", viewModel.state.value.query)
            assertEquals(
                listOf("second"),
                viewModel.state.value.products
                    .map(Product::code),
            )
            assertEquals(
                listOf("second"),
                viewModel.state.value.recentSearches
                    .map { it.normalizedTerm },
            )
        }

    @Test
    fun laterRequestForSameNormalizedTermWinsWhenResponsesCompleteOutOfOrder() =
        runTest {
            val firstAttempt = CompletableDeferred<ProductSearchOutcome>()
            val secondAttempt = CompletableDeferred<ProductSearchOutcome>()
            var attempt = 0
            val repository =
                FakeProductSearchRepository {
                    attempt += 1
                    val response = if (attempt == 1) firstAttempt else secondAttempt
                    flow {
                        emit(ProductSearchOutcome.Cached(null))
                        emit(response.await())
                    }
                }
            val history = FakeRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 3L }, backgroundScope)

            viewModel.onQueryChanged("oat")
            viewModel.submitSearch()
            runCurrent()
            viewModel.retrySearch()
            runCurrent()

            assertEquals(
                listOf("oat", "oat"),
                repository.requestedTerms.map { it.normalizedTerm },
            )

            secondAttempt.complete(
                ProductSearchOutcome.Success(
                    ProductSearchSnapshot(
                        SearchTerm.fromInput("oat")!!,
                        listOf(Product("newer", "Newer oats")),
                        2L,
                    ),
                ),
            )
            runCurrent()

            assertEquals(
                listOf("newer"),
                viewModel.state.value.products
                    .map(Product::code),
            )

            firstAttempt.complete(
                ProductSearchOutcome.Success(
                    ProductSearchSnapshot(
                        SearchTerm.fromInput("oat")!!,
                        listOf(Product("older", "Older oats")),
                        1L,
                    ),
                ),
            )
            runCurrent()

            assertEquals(SearchPhase.RESULTS, viewModel.state.value.phase)
            assertEquals("oat", viewModel.state.value.query)
            assertEquals(
                listOf("newer"),
                viewModel.state.value.products
                    .map(Product::code),
            )
        }

    @Test
    fun lateProductLookupForSupersededQueryDoesNotChangeSelection() =
        runTest {
            val repository =
                FakeProductSearchRepository { term ->
                    flowOf(
                        ProductSearchOutcome.Success(
                            ProductSearchSnapshot(term, emptyList(), 1L),
                        ),
                    )
                }
            repository.productsByCode["late"] = Product("late", "Late product")
            val history = FakeRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 4L }, backgroundScope)

            viewModel.onQueryChanged("first")
            viewModel.submitSearch()
            runCurrent()

            viewModel.openProduct("late")
            viewModel.onQueryChanged("second")
            runCurrent()

            assertEquals("second", viewModel.state.value.query)
            assertEquals(null, viewModel.state.value.selectedProduct)
        }

    @Test
    fun productLookupForCurrentQueryStillAppliesSelection() =
        runTest {
            val repository =
                FakeProductSearchRepository { term ->
                    flowOf(
                        ProductSearchOutcome.Success(
                            ProductSearchSnapshot(term, emptyList(), 1L),
                        ),
                    )
                }
            repository.productsByCode["late"] = Product("late", "Late product")
            val history = FakeRecentSearchRepository()
            val viewModel = ProductSearchViewModel(repository, history, { 4L }, backgroundScope)

            viewModel.onQueryChanged("current")
            viewModel.submitSearch()
            runCurrent()

            viewModel.openProduct("late")
            runCurrent()

            assertEquals(Product("late", "Late product"), viewModel.state.value.selectedProduct)
        }
}
