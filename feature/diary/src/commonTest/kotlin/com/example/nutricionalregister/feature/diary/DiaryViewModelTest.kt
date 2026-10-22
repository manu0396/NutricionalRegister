package com.example.nutricionalregister.feature.diary

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.feature.diary.testing.FakeDayBoundaryProvider
import com.example.nutricionalregister.feature.diary.testing.FakeDiaryRepository
import com.example.nutricionalregister.feature.diary.testing.FakeNutritionGoalRepository
import com.example.nutricionalregister.feature.diary.testing.MILLIS_PER_DAY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {
    @Test
    fun theStateLoadsTheEntriesOfTheCurrentDayGroupedByMeal() =
        runTest {
            val repository = FakeDiaryRepository()
            repository.addEntry(entry(meal = MealType.LUNCH, loggedAt = 100L))
            repository.addEntry(entry(meal = MealType.BREAKFAST, loggedAt = 200L))
            val viewModel = viewModel(repository, scope(testScheduler))

            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(2, state.entryCount)
            assertEquals(1, state.entriesByMeal.getValue(MealType.LUNCH).size)
            assertEquals(1, state.entriesByMeal.getValue(MealType.BREAKFAST).size)
            assertFalse(state.isLoading)
            viewModel.clear()
        }

    @Test
    fun theTotalsAreTheSumOfTheEntries() =
        runTest {
            val repository = FakeDiaryRepository()
            repository.addEntry(entry(energy = 100.0, loggedAt = 100L))
            repository.addEntry(entry(energy = 250.0, loggedAt = 200L))
            val viewModel = viewModel(repository, scope(testScheduler))

            advanceUntilIdle()

            assertEquals(350.0, viewModel.state.value.totals.energyKcal)
            viewModel.clear()
        }

    @Test
    fun loggingAnEntryStoresASnapshotWithTheMealQuantityAndNutrition() =
        runTest {
            val repository = FakeDiaryRepository()
            val viewModel = viewModel(repository, scope(testScheduler))

            viewModel.log(product(), MealType.DINNER, quantityGrams = 50.0)
            advanceUntilIdle()

            val logged = repository.addedEntries.single()
            assertEquals(MealType.DINNER, logged.mealType)
            assertEquals(50.0, logged.quantityGrams)
            assertEquals("Oats", logged.productName)
            assertEquals(190.0, logged.energyKcal)
            assertEquals(5.0, logged.proteinGrams)
            assertEquals(1, viewModel.state.value.entryCount)
            viewModel.clear()
        }

    @Test
    fun removingAnEntryRemovesItFromTheState() =
        runTest {
            val repository = FakeDiaryRepository()
            val id = repository.addEntry(entry(loggedAt = 100L))
            val viewModel = viewModel(repository, scope(testScheduler))
            advanceUntilIdle()

            viewModel.removeEntry(id)
            advanceUntilIdle()

            assertEquals(listOf(id), repository.removedIds)
            assertEquals(0, viewModel.state.value.entryCount)
            viewModel.clear()
        }

    @Test
    fun updatingTheGoalStoresItAndTheStateReflectsIt() =
        runTest {
            val goalRepository = FakeNutritionGoalRepository()
            val viewModel =
                viewModel(diaryRepository = FakeDiaryRepository(), scope = scope(testScheduler), goalRepository = goalRepository)

            viewModel.updateGoal(NutritionGoal(energyKcal = 2400))
            advanceUntilIdle()

            assertEquals(2400, goalRepository.savedGoals.single().energyKcal)
            assertEquals(2400, viewModel.state.value.goal.energyKcal)
            viewModel.clear()
        }

    @Test
    fun theRemainingAmountIsTheGoalMinusTheTotals() =
        runTest {
            val repository = FakeDiaryRepository()
            repository.addEntry(entry(energy = 500.0, loggedAt = 100L))
            val viewModel = viewModel(repository, scope(testScheduler))
            advanceUntilIdle()

            assertEquals(1500.0, viewModel.state.value.remaining.energyKcal)
            viewModel.clear()
        }

    @Test
    fun refreshPicksUpADayBoundaryCrossedWhileTheAppWasOpen() =
        runTest {
            val repository = FakeDiaryRepository()
            repository.addEntry(entry(energy = 100.0, loggedAt = 100L))
            var now = 100L
            val viewModel = viewModel(repository, scope(testScheduler)) { now }
            advanceUntilIdle()
            assertEquals(1, viewModel.state.value.entryCount)

            now = MILLIS_PER_DAY + 100L
            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(MILLIS_PER_DAY, state.dayEpochMillis)
            assertTrue(state.isEmpty)
            viewModel.clear()
        }

    private fun scope(scheduler: TestCoroutineScheduler): CoroutineScope = CoroutineScope(StandardTestDispatcher(scheduler))

    private fun viewModel(
        diaryRepository: FakeDiaryRepository,
        scope: CoroutineScope,
        goalRepository: FakeNutritionGoalRepository = FakeNutritionGoalRepository(),
        clock: EpochClock = EpochClock { 100L },
    ): DiaryViewModel =
        DiaryViewModel(
            diaryRepository = diaryRepository,
            goalRepository = goalRepository,
            dayBoundaryProvider = FakeDayBoundaryProvider(),
            clock = clock,
            scope = scope,
        )

    private fun entry(
        meal: MealType = MealType.LUNCH,
        loggedAt: Long,
        energy: Double = 100.0,
    ): DiaryEntry =
        DiaryEntry(
            productCode = "0001",
            productName = "Oats",
            mealType = meal,
            quantityGrams = 30.0,
            energyKcal = energy,
            proteinGrams = 5.0,
            carbohydrateGrams = 10.0,
            fatGrams = 2.0,
            loggedAtEpochMillis = loggedAt,
            dayEpochMillis = 0L,
        )

    private fun product(): Product =
        Product(
            code = "0001",
            name = "Oats",
            nutritionValues =
                listOf(
                    NutritionValue("energy", 380.0, null, NutritionBasis.PER_100_G),
                    NutritionValue("protein", 10.0, null, NutritionBasis.PER_100_G),
                ),
        )
}
