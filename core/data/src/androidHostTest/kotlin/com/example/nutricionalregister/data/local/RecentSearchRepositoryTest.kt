package com.example.nutricionalregister.data.local

import com.example.nutricionalregister.data.ProductSearchRepositoryImpl
import com.example.nutricionalregister.data.RecentSearchRepositoryImpl
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductCatalogue
import com.example.nutricionalregister.domain.product.SearchTerm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentSearchRepositoryTest {
    @Test
    fun boundsHistoryOrdersEqualTimestampsAndPersistsAcrossDatabaseReopen() =
        runTest {
            val firstDatabase = InMemoryProductDatabase.createPersistent()
            val file = requireNotNull(firstDatabase.file)
            val dispatcher = StandardTestDispatcher(testScheduler)
            try {
                val repository = RecentSearchRepositoryImpl(firstDatabase.database, dispatcher)
                repeat(12) { index ->
                    val term = SearchTerm.fromInput("term $index")!!
                    repository.recordCompletedSearch(term, completedAtEpochMillis = 100L)
                }

                val beforeReopen = repository.observeRecentSearches().first()
                assertEquals(10, beforeReopen.size)
                assertEquals("term 11", beforeReopen.first().displayTerm)
                assertEquals("term 2", beforeReopen.last().displayTerm)
                assertTrue(beforeReopen.zipWithNext().all { (newer, older) -> newer.completionSequence > older.completionSequence })

                firstDatabase.close()
                val reopened = InMemoryProductDatabase.reopenPersistent(file)
                try {
                    val reopenedRepository = RecentSearchRepositoryImpl(reopened.database, dispatcher)
                    val afterReopen = reopenedRepository.observeRecentSearches().first()
                    assertEquals(beforeReopen, afterReopen)
                } finally {
                    reopened.close()
                }
            } finally {
                firstDatabase.close()
                file.delete()
                SqliteFileCleanup.delete(file)
            }
        }

    @Test
    fun repeatedTermMovesToFrontAndDeletingHistoryKeepsProducts() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val dispatcher = StandardTestDispatcher(testScheduler)
                val history = RecentSearchRepositoryImpl(inMemory.database, dispatcher)
                val search =
                    ProductSearchRepositoryImpl(
                        database = inMemory.database,
                        catalogue = ProductCatalogue { listOf(Product("0007", "Beans")) },
                        clock = { 100L },
                        databaseDispatcher = dispatcher,
                    )
                val term = SearchTerm.fromInput("Beans")!!
                search.search(term).toList()
                history.recordCompletedSearch(term, 100L)
                history.recordCompletedSearch(SearchTerm.fromInput("Oats")!!, 100L)
                history.recordCompletedSearch(term, 100L)

                val recent = history.observeRecentSearches().first()
                history.deleteRecentSearch(term.normalizedTerm)

                assertEquals(listOf("Beans", "Oats"), recent.map { it.displayTerm })
                assertEquals(Product("0007", "Beans"), search.getProduct("0007"))
                assertTrue(history.observeRecentSearches().first().none { it.normalizedTerm == term.normalizedTerm })
            }
        }
}
