package com.example.nutricionalregister.domain.product

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ProductSearchOutcomeTest {
    @Test
    fun snapshotRetainsTermProductsAndFetchTimestamp() {
        // GIVEN a search term and a product returned for it
        val term = SearchTerm.fromInput("Oat drink")!!
        val product = Product(code = "0001", name = "Oat drink")

        // WHEN a snapshot is captured for them
        val snapshot =
            ProductSearchSnapshot(
                term = term,
                products = listOf(product),
                fetchedAtEpochMillis = 1_000L,
            )

        // THEN it exposes the term, the products, and the fetch timestamp
        assertEquals(term, snapshot.term)
        assertEquals(listOf(product), snapshot.products)
        assertEquals(1_000L, snapshot.fetchedAtEpochMillis)
        assertEquals(snapshot, snapshot.copy(fetchedAtEpochMillis = 1_000L))
        assertNotEquals(snapshot, snapshot.copy(fetchedAtEpochMillis = 2_000L))
    }

    @Test
    fun cachedOutcomeCarriesNullableSnapshot() {
        // GIVEN a snapshot and a cache-less cached outcome
        val snapshot =
            ProductSearchSnapshot(
                term = SearchTerm.fromInput("Oat drink")!!,
                products = emptyList(),
                fetchedAtEpochMillis = 10L,
            )

        // WHEN both cached outcomes are constructed
        val withCache = ProductSearchOutcome.Cached(snapshot)
        val withoutCache = ProductSearchOutcome.Cached(null)

        // THEN each exposes its snapshot
        assertEquals(snapshot, withCache.snapshot)
        assertNull(withoutCache.snapshot)
        assertEquals(withCache, ProductSearchOutcome.Cached(snapshot))
        assertNotEquals(withCache, withoutCache)
    }

    @Test
    fun successOutcomeExposesTheFetchedSnapshot() {
        // GIVEN a fetched snapshot
        val snapshot =
            ProductSearchSnapshot(
                term = SearchTerm.fromInput("Oat drink")!!,
                products = emptyList(),
                fetchedAtEpochMillis = 10L,
            )

        // WHEN the success outcome wraps it
        val outcome = ProductSearchOutcome.Success(snapshot)

        // THEN the snapshot is retained and distinguishable from other outcomes
        assertEquals(snapshot, outcome.snapshot)
        assertEquals(ProductSearchOutcome.Success(snapshot), outcome)
        assertNotEquals<ProductSearchOutcome>(ProductSearchOutcome.Cached(snapshot), outcome)
    }

    @Test
    fun failureOutcomeExposesReasonAndCachedSnapshot() {
        // GIVEN a previously cached snapshot
        val snapshot =
            ProductSearchSnapshot(
                term = SearchTerm.fromInput("Oat drink")!!,
                products = emptyList(),
                fetchedAtEpochMillis = 10L,
            )

        // WHEN failures are constructed with and without cache
        val offline = ProductSearchOutcome.Failure(ProductSearchFailure.OFFLINE, snapshot)
        val service = ProductSearchOutcome.Failure(ProductSearchFailure.SERVICE, null)

        // THEN the reason and the optional cached snapshot are retained
        assertEquals(ProductSearchFailure.OFFLINE, offline.reason)
        assertEquals(snapshot, offline.cachedSnapshot)
        assertEquals(ProductSearchFailure.SERVICE, service.reason)
        assertNull(service.cachedSnapshot)
    }

    @Test
    fun searchFailureReasonsCoverOfflineServiceAndInvalidResponse() {
        // WHEN every declared failure reason is read
        // THEN offline, service, and invalid response are available in declaration order
        assertEquals(3, ProductSearchFailure.entries.size)
        assertEquals(ProductSearchFailure.OFFLINE, ProductSearchFailure.entries[0])
        assertEquals(ProductSearchFailure.SERVICE, ProductSearchFailure.entries[1])
        assertEquals(ProductSearchFailure.INVALID_RESPONSE, ProductSearchFailure.entries[2])
    }
}
