package com.example.nutricionalregister.data.local

import com.example.nutricionalregister.data.DiaryRepositoryImpl
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiaryRepositoryImplTest {
    @Test
    fun anEntryIsStoredAndObservedForItsDay() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                val id = repository.addEntry(entry(day = TODAY, meal = MealType.LUNCH))

                val entries = repository.observeEntriesForDay(TODAY).first()
                assertEquals(1, entries.size)
                assertEquals(id, entries.single().id)
                assertEquals("Oats", entries.single().productName)
                assertEquals(MealType.LUNCH, entries.single().mealType)
            }
        }

    @Test
    fun anEntryOfAnotherDayIsNotObservedForTheDay() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                repository.addEntry(entry(day = TODAY, meal = MealType.LUNCH))
                repository.addEntry(entry(day = TODAY - MILLIS_PER_DAY, meal = MealType.DINNER))

                assertEquals(1, repository.observeEntriesForDay(TODAY).first().size)
                assertEquals(1, repository.observeEntriesForDay(TODAY - MILLIS_PER_DAY).first().size)
            }
        }

    @Test
    fun entriesOfADayAreOrderedByLogTime() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                repository.addEntry(entry(day = TODAY, meal = MealType.DINNER, loggedAt = 300L))
                repository.addEntry(entry(day = TODAY, meal = MealType.BREAKFAST, loggedAt = 100L))
                repository.addEntry(entry(day = TODAY, meal = MealType.LUNCH, loggedAt = 200L))

                val meals = repository.observeEntriesForDay(TODAY).first().map { it.mealType }
                assertEquals(listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER), meals)
            }
        }

    @Test
    fun anUnknownNutrientStaysUnknownRatherThanBecomingZero() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                repository.addEntry(entry(day = TODAY, meal = MealType.LUNCH, fat = null))

                val stored = repository.observeEntriesForDay(TODAY).first().single()
                assertNull(stored.fatGrams)
                assertEquals(150.0, stored.energyKcal)
            }
        }

    @Test
    fun removingAnEntryRemovesItAndRemovingAnUnknownEntryIsANoOp() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                val id = repository.addEntry(entry(day = TODAY, meal = MealType.LUNCH))
                repository.addEntry(entry(day = TODAY, meal = MealType.DINNER))

                repository.removeEntry(id)
                assertEquals(1, repository.observeEntriesForDay(TODAY).first().size)

                repository.removeEntry(9_999L)
                assertEquals(1, repository.observeEntriesForDay(TODAY).first().size)
            }
        }

    @Test
    fun updatingAnEntryReplacesItsStoredValues() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                val id = repository.addEntry(entry(day = TODAY, meal = MealType.LUNCH))

                val stored = repository.observeEntriesForDay(TODAY).first().single()
                repository.updateEntry(stored.copy(mealType = MealType.SNACKS, quantityGrams = 75.0))

                val updated = repository.observeEntriesForDay(TODAY).first().single()
                assertEquals(id, updated.id)
                assertEquals(MealType.SNACKS, updated.mealType)
                assertEquals(75.0, updated.quantityGrams)
            }
        }

    @Test
    fun theWeekObservationReturnsTheLastSevenDays() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                repository.addEntry(entry(day = TODAY, meal = MealType.LUNCH))
                repository.addEntry(entry(day = TODAY - MILLIS_PER_DAY, meal = MealType.LUNCH))
                repository.addEntry(entry(day = TODAY - 6 * MILLIS_PER_DAY, meal = MealType.LUNCH))
                repository.addEntry(entry(day = TODAY - 7 * MILLIS_PER_DAY, meal = MealType.LUNCH))

                val week = repository.observeEntriesForWeek(TODAY).first()
                assertEquals(3, week.size)
                assertTrue(week.all { it.dayEpochMillis >= TODAY - 6 * MILLIS_PER_DAY })
            }
        }

    @Test
    fun entriesSurviveAReopenWithNoNetwork() =
        runTest {
            val firstDatabase = InMemoryProductDatabase.createPersistent()
            val file = requireNotNull(firstDatabase.file)
            try {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val repository = DiaryRepositoryImpl(firstDatabase.database, FakeDayBoundaryProvider(), dispatcher)
                repository.addEntry(entry(day = TODAY, meal = MealType.BREAKFAST))
                val before = repository.observeEntriesForDay(TODAY).first()

                firstDatabase.close()
                val reopened = InMemoryProductDatabase.reopenPersistent(file)
                try {
                    val reopenedRepository = DiaryRepositoryImpl(reopened.database, FakeDayBoundaryProvider(), dispatcher)
                    assertEquals(before, reopenedRepository.observeEntriesForDay(TODAY).first())
                } finally {
                    reopened.close()
                }
            } finally {
                firstDatabase.close()
                file.delete()
                SqliteFileCleanup.delete(file)
            }
        }

    private fun repository(
        inMemory: InMemoryProductDatabase,
        dispatcher: CoroutineDispatcher,
    ): DiaryRepositoryImpl =
        DiaryRepositoryImpl(
            database = inMemory.database,
            dayBoundaryProvider = FakeDayBoundaryProvider(),
            databaseDispatcher = dispatcher,
        )

    private fun entry(
        day: Long,
        meal: MealType,
        loggedAt: Long = 100L,
        fat: Double? = 3.0,
    ): DiaryEntry =
        DiaryEntry(
            productCode = "0001",
            productName = "Oats",
            brands = "Acme",
            mealType = meal,
            quantityGrams = 30.0,
            energyKcal = 150.0,
            proteinGrams = 5.0,
            carbohydrateGrams = 27.0,
            fatGrams = fat,
            loggedAtEpochMillis = loggedAt,
            dayEpochMillis = day,
        )

    private companion object {
        private const val TODAY = 1_700_000_000_000L
    }
}
