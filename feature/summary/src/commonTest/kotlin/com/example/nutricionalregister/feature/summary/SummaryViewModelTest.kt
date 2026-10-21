package com.example.nutricionalregister.feature.summary

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.feature.summary.testing.FakeDayBoundaryProvider
import com.example.nutricionalregister.feature.summary.testing.FakeDiaryRepository
import com.example.nutricionalregister.feature.summary.testing.MILLIS_PER_DAY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {
    @Test
    fun theSummaryIsNullUntilTheFirstWeekHasBeenRead() =
        runTest {
            val viewModel = viewModel(FakeDiaryRepository(), scope(testScheduler))

            assertNull(viewModel.weeklySummary.value)
            advanceUntilIdle()

            assertNotNull(viewModel.weeklySummary.value)
            viewModel.clear()
        }

    @Test
    fun theSummaryCoversSevenDaysEndingWithToday() =
        runTest {
            val viewModel = viewModel(FakeDiaryRepository(), scope(testScheduler))

            advanceUntilIdle()

            val summary = assertNotNull(viewModel.weeklySummary.value)
            assertEquals(7, summary.days.size)
            assertEquals(0L, summary.days.last().dayEpochMillis)
            viewModel.clear()
        }

    @Test
    fun theAverageIsTakenOverTheDaysThatHaveEntriesOnly() =
        runTest {
            val repository = FakeDiaryRepository()
            repository.addEntry(entry(day = 0L, energy = 400.0))
            repository.addEntry(entry(day = -MILLIS_PER_DAY, energy = 600.0))
            val viewModel = viewModel(repository, scope(testScheduler))

            advanceUntilIdle()

            val summary = assertNotNull(viewModel.weeklySummary.value)
            assertEquals(2, summary.daysWithEntries)
            assertEquals(500.0, summary.averageTotals.energyKcal)
            viewModel.clear()
        }

    @Test
    fun aDayWithoutEntriesIsEmptyRatherThanZeroIntake() =
        runTest {
            val repository = FakeDiaryRepository()
            repository.addEntry(entry(day = 0L, energy = 400.0))
            val viewModel = viewModel(repository, scope(testScheduler))

            advanceUntilIdle()

            val summary = assertNotNull(viewModel.weeklySummary.value)
            val emptyDay = summary.days.first { it.isEmpty }
            assertEquals(0, emptyDay.entryCount)
            assertEquals(0.0, emptyDay.totals.energyKcal)
            assertTrue(summary.loggedDays.all { !it.isEmpty })
            viewModel.clear()
        }

    @Test
    fun refreshPicksUpADayBoundaryCrossedWhileTheAppWasOpen() =
        runTest {
            val repository = FakeDiaryRepository()
            repository.addEntry(entry(day = 0L, energy = 400.0))
            var now = 100L
            val viewModel = viewModel(repository, scope(testScheduler)) { now }
            advanceUntilIdle()

            now = MILLIS_PER_DAY + 100L
            viewModel.refresh()
            advanceUntilIdle()

            val summary = assertNotNull(viewModel.weeklySummary.value)
            assertEquals(MILLIS_PER_DAY, summary.days.last().dayEpochMillis)
            assertEquals(1, summary.daysWithEntries)
            viewModel.clear()
        }

    private fun scope(scheduler: TestCoroutineScheduler): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(scheduler))

    private fun viewModel(
        repository: FakeDiaryRepository,
        scope: CoroutineScope,
        clock: EpochClock = EpochClock { 100L },
    ): SummaryViewModel =
        SummaryViewModel(
            diaryRepository = repository,
            dayBoundaryProvider = FakeDayBoundaryProvider(),
            clock = clock,
            scope = scope,
        )

    private fun entry(
        day: Long,
        energy: Double,
    ): DiaryEntry =
        DiaryEntry(
            productCode = "0001",
            productName = "Oats",
            mealType = MealType.LUNCH,
            quantityGrams = 30.0,
            energyKcal = energy,
            proteinGrams = 5.0,
            carbohydrateGrams = 10.0,
            fatGrams = 2.0,
            loggedAtEpochMillis = day + 1_000L,
            dayEpochMillis = day,
        )
}
