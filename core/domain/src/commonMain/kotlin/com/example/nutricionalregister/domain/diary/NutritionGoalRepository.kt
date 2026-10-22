package com.example.nutricionalregister.domain.diary

import kotlinx.coroutines.flow.Flow

/**
 * On-device storage for the single daily goal record.
 */
public interface NutritionGoalRepository {
    /**
     * Emits the stored goal, or the default goal when nothing has been stored
     * yet, so the daily summary is meaningful on the very first launch.
     */
    public fun observeGoal(): Flow<NutritionGoal>

    /** Stores [goal], replacing the previous one. */
    public suspend fun saveGoal(goal: NutritionGoal)
}
