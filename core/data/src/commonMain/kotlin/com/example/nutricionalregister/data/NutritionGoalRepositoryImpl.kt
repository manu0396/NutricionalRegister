package com.example.nutricionalregister.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.diary.NutritionGoalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed [NutritionGoalRepository] over the singleton
 * `NutritionGoal` table.
 *
 * The table holds at most one row, so the goal is a single user-wide record.
 * When nothing has been stored yet the default goal is emitted, so the daily
 * summary is meaningful on the very first launch.
 */
public class NutritionGoalRepositoryImpl(
    private val database: ProductDatabase,
    private val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : NutritionGoalRepository {
    override fun observeGoal(): Flow<NutritionGoal> =
        database.diaryEntryQueries
            .selectNutritionGoal()
            .asFlow()
            .mapToOneOrNull(databaseDispatcher)
            .map { row -> row?.toNutritionGoal() ?: NutritionGoal() }

    override suspend fun saveGoal(goal: NutritionGoal) {
        withContext(databaseDispatcher) {
            database.diaryEntryQueries.upsertNutritionGoal(
                energy_kcal = goal.energyKcal.toLong(),
                protein_grams = goal.proteinGrams.toLong(),
                carbohydrate_grams = goal.carbohydrateGrams.toLong(),
                fat_grams = goal.fatGrams.toLong(),
            )
        }
    }
}

/**
 * Maps the generated row to the domain entity.
 *
 * The generated row type lives in this package and clashes with the domain
 * type of the same name, so it is referenced by its fully qualified name.
 */
private fun com.example.nutricionalregister.data.NutritionGoal.toNutritionGoal(): NutritionGoal =
    NutritionGoal(
        energyKcal = energy_kcal.toInt(),
        proteinGrams = protein_grams.toInt(),
        carbohydrateGrams = carbohydrate_grams.toInt(),
        fatGrams = fat_grams.toInt(),
    )
