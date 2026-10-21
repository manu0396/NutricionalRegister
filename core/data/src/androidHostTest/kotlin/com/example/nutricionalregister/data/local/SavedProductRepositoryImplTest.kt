package com.example.nutricionalregister.data.local

import com.example.nutricionalregister.data.SavedProductRepositoryImpl
import com.example.nutricionalregister.domain.purchase.SavedProduct
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedProductRepositoryImplTest {
    @Test
    fun savingPersistsListsMostRecentFirstAndSurvivesReopenWithoutNetwork() =
        runTest {
            val firstDatabase = InMemoryProductDatabase.createPersistent()
            val file = requireNotNull(firstDatabase.file)
            val dispatcher = StandardTestDispatcher(testScheduler)
            try {
                val repository = SavedProductRepositoryImpl(firstDatabase.database, dispatcher)
                repository.save(SavedProduct(code = "0001", name = "Oats", savedAtEpochMillis = 100L))
                repository.save(SavedProduct(code = "0002", name = "Beans", savedAtEpochMillis = 200L))

                val beforeReopen = repository.observeSaved().first()
                assertEquals(listOf("0002", "0001"), beforeReopen.map { it.code })
                assertTrue(repository.isSaved("0001").first())

                // saved products survive a database reopen with no network (SC-002)
                firstDatabase.close()
                val reopened = InMemoryProductDatabase.reopenPersistent(file)
                try {
                    val reopenedRepository = SavedProductRepositoryImpl(reopened.database, dispatcher)
                    assertEquals(beforeReopen, reopenedRepository.observeSaved().first())
                    assertTrue(reopenedRepository.isSaved("0002").first())
                } finally {
                    reopened.close()
                }
            } finally {
                firstDatabase.close()
                file.delete()
                SqliteSidecars.delete(file)
            }
        }

    @Test
    fun savingTheSameCodeTwiceDoesNotDuplicateAndRemoveTouchesOnlyTheSavedRow() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val dispatcher = StandardTestDispatcher(testScheduler)
                val saved = SavedProductRepositoryImpl(inMemory.database, dispatcher)

                saved.save(SavedProduct(code = "0001", name = "Oats", savedAtEpochMillis = 100L))
                saved.save(SavedProduct(code = "0001", name = "Oats renamed", savedAtEpochMillis = 200L))

                val rows = saved.observeSaved().first()
                assertEquals(1, rows.size)
                assertEquals("Oats renamed", rows.single().name)
                assertFalse(saved.isSaved("9999").first())

                saved.remove("0001")
                assertTrue(saved.observeSaved().first().isEmpty())
                assertFalse(saved.isSaved("0001").first())
            }
        }
}

private object SqliteSidecars {
    fun delete(file: java.io.File) {
        java.io.File("${file.path}-journal").delete()
        java.io.File("${file.path}-shm").delete()
        java.io.File("${file.path}-wal").delete()
    }
}
