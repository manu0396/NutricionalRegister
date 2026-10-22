package com.example.nutricionalregister.feature.diary.testing

import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.diary.NutritionGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An in-memory [NutritionGoalRepository] that starts from the default goal and
 * records every goal the tests store.
 */
internal class FakeNutritionGoalRepository(
    initialGoal: NutritionGoal = NutritionGoal(),
) : NutritionGoalRepository {
    private val stored = MutableStateFlow(initialGoal)

    /** The goals the repository has been asked to store, in order. */
    val savedGoals: MutableList<NutritionGoal> = mutableListOf()

    override fun observeGoal(): Flow<NutritionGoal> = stored

    override suspend fun saveGoal(goal: NutritionGoal) {
        savedGoals += goal
        stored.value = goal
    }
}
