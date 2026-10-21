package com.example.nutricionalregister.data.local

import com.example.nutricionalregister.data.NutritionGoalRepositoryImpl
import com.example.nutricionalregister.domain.diary.NutritionGoal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NutritionGoalRepositoryImplTest {
    @Test
    fun theDefaultGoalIsEmittedWhenNothingHasBeenStored() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                assertEquals(NutritionGoal(), repository.observeGoal().first())
            }
        }

    @Test
    fun aStoredGoalIsEmitted() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                val goal = NutritionGoal(energyKcal = 2400, proteinGrams = 150, carbohydrateGrams = 250, fatGrams = 80)

                repository.saveGoal(goal)

                assertEquals(goal, repository.observeGoal().first())
            }
        }

    @Test
    fun savingAGoalTwiceReplacesThePreviousOne() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val repository = repository(inMemory, StandardTestDispatcher(testScheduler))
                repository.saveGoal(NutritionGoal(energyKcal = 2400))
                repository.saveGoal(NutritionGoal(energyKcal = 1800))

                assertEquals(1800, repository.observeGoal().first().energyKcal)
            }
        }

    @Test
    fun theGoalSurvivesAReopen() =
        runTest {
            val firstDatabase = InMemoryProductDatabase.createPersistent()
            val file = requireNotNull(firstDatabase.file)
            try {
                val dispatcher = StandardTestDispatcher(testScheduler)
                val repository = NutritionGoalRepositoryImpl(firstDatabase.database, dispatcher)
                repository.saveGoal(NutritionGoal(energyKcal = 2300))

                firstDatabase.close()
                val reopened = InMemoryProductDatabase.reopenPersistent(file)
                try {
                    val reopenedRepository = NutritionGoalRepositoryImpl(reopened.database, dispatcher)
                    assertEquals(2300, reopenedRepository.observeGoal().first().energyKcal)
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
    ): NutritionGoalRepositoryImpl =
        NutritionGoalRepositoryImpl(
            database = inMemory.database,
            databaseDispatcher = dispatcher,
        )
}
