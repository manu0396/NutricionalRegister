package com.example.nutricionalregister.data.local

import com.example.nutricionalregister.data.ProductSearchRepositoryImpl
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductCatalogue
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.SearchTerm
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProductSearchRepositoryTest {
    @Test
    fun emitsCachedSnapshotBeforeBlockedRefreshAndReplacesItOnSuccess() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val firstProduct = Product("0001", "Oat drink")
                val refreshedProduct = Product("0002", "Oat beverage")
                val response = CompletableDeferred<List<Product>>()
                var calls = 0
                val repository =
                    ProductSearchRepositoryImpl(
                        database = inMemory.database,
                        catalogue =
                            ProductCatalogue {
                                calls += 1
                                if (calls == 1) listOf(firstProduct) else response.await()
                            },
                        clock = { 1_000L + calls },
                        databaseDispatcher = StandardTestDispatcher(testScheduler),
                    )
                val term = SearchTerm.fromInput("oat")!!
                repository.search(term).toList()

                val updates = mutableListOf<ProductSearchOutcome>()
                val refresh =
                    backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
                        repository.search(term).toList(updates)
                    }
                runCurrent()
                assertIs<ProductSearchOutcome.Cached>(updates.first())
                assertEquals(listOf(firstProduct), updates.first().let { (it as ProductSearchOutcome.Cached).snapshot?.products })

                response.complete(listOf(refreshedProduct))
                refresh.await()

                val completed = updates.filterIsInstance<ProductSearchOutcome.Success>().single().snapshot
                assertEquals(listOf(refreshedProduct), completed.products)
                assertEquals(refreshedProduct, repository.getProduct("0002"))
                assertEquals(firstProduct, repository.getProduct("0001"))
            }
        }

    @Test
    fun olderSameTermRefreshCannotOverwriteNewerSnapshot() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val firstResponse = CompletableDeferred<List<Product>>()
                var calls = 0
                val repository =
                    ProductSearchRepositoryImpl(
                        database = inMemory.database,
                        catalogue =
                            ProductCatalogue {
                                calls += 1
                                if (calls == 1) firstResponse.await() else listOf(Product("new", "Newer"))
                            },
                        clock = { 5_000L },
                        databaseDispatcher = StandardTestDispatcher(testScheduler),
                    )
                val term = SearchTerm.fromInput("same term")!!
                val older =
                    backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
                        repository.search(term).toList()
                    }
                val newer = repository.search(term).toList()
                firstResponse.complete(listOf(Product("old", "Older")))
                older.await()

                val cached = newer.filterIsInstance<ProductSearchOutcome.Success>().single().snapshot
                assertEquals("new", cached.products.single().code)
                assertEquals(Product("new", "Newer"), repository.getProduct("new"))
                assertNull(repository.getProduct("old"))
            }
        }

    @Test
    fun snapshotPreservesRemoteOrderAndEmptySuccess() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val ordered = listOf(Product("b", "Second"), Product("a", "First"))
                val repository =
                    ProductSearchRepositoryImpl(
                        database = inMemory.database,
                        catalogue = ProductCatalogue { ordered },
                        clock = { 123L },
                        databaseDispatcher = StandardTestDispatcher(testScheduler),
                    )

                val snapshot =
                    repository
                        .search(SearchTerm.fromInput("order")!!)
                        .toList()
                        .filterIsInstance<ProductSearchOutcome.Success>()
                        .single()
                        .snapshot
                val cached =
                    repository
                        .search(snapshot.term)
                        .toList()
                        .filterIsInstance<ProductSearchOutcome.Cached>()
                        .single()
                        .snapshot

                assertEquals(ordered, snapshot.products)
                assertEquals(ordered, cached?.products)
                assertEquals(123L, cached?.fetchedAtEpochMillis)
            }
        }

    @Test
    fun failedRefreshPreservesCachedSnapshotAndReportsOffline() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                var calls = 0
                val product = Product("cached", "Saved product")
                val repository =
                    ProductSearchRepositoryImpl(
                        database = inMemory.database,
                        catalogue =
                            ProductCatalogue {
                                calls += 1
                                if (calls == 1) listOf(product) else throw IOException("offline")
                            },
                        clock = { 10L },
                        databaseDispatcher = StandardTestDispatcher(testScheduler),
                    )
                val term = SearchTerm.fromInput("saved")!!
                repository.search(term).toList()

                val outcomes = repository.search(term).toList()

                assertEquals(listOf(product), (outcomes.first() as ProductSearchOutcome.Cached).snapshot?.products)
                val failure = outcomes.filterIsInstance<ProductSearchOutcome.Failure>().single()
                assertEquals(ProductSearchFailure.OFFLINE, failure.reason)
                assertEquals(listOf(product), failure.cachedSnapshot?.products)
            }
        }
}
