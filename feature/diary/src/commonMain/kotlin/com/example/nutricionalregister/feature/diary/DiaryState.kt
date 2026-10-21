package com.example.nutricionalregister.feature.diary

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.diary.NutritionTotals

/**
 * The state of the diary for the day that is currently shown.
 *
 * The state is a single immutable value, so a screen renders one consistent
 * snapshot instead of piecing several streams together. The totals in the state
 * are always the sum of the entries in the state.
 *
 * @property dayEpochMillis the local midnight of the day the entries belong to.
 * @property entriesByMeal the entries grouped by meal; every meal of the day is
 *   present, even when it has no entries.
 * @property totals the nutrition of the whole day.
 * @property mealTotals the nutrition of each meal.
 * @property goal the daily targets the totals are measured against.
 * @property isLoading whether the day's entries are still being read.
 */
public data class DiaryState(
    val dayEpochMillis: Long = 0,
    val entriesByMeal: Map<MealType, List<DiaryEntry>> = emptyMap(),
    val totals: NutritionTotals = NutritionTotals.ZERO,
    val mealTotals: Map<MealType, NutritionTotals> = emptyMap(),
    val goal: NutritionGoal = NutritionGoal(),
    val isLoading: Boolean = true,
) {
    /** The number of entries logged in the day. */
    public val entryCount: Int
        get() = entriesByMeal.values.sumOf { entries -> entries.size }

    /** The amount still available before the goal is reached. */
    public val remaining: NutritionTotals
        get() = totals.remainingAgainst(goal)

    /** Whether nothing has been logged yet. */
    public val isEmpty: Boolean
        get() = entryCount == 0
}
