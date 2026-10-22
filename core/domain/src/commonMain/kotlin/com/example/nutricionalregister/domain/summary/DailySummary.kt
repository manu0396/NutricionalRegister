package com.example.nutricionalregister.domain.summary

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.diary.NutritionTotals

/**
 * The day as the Today screen sees it.
 *
 * The summary is **computed, never stored**: a stored copy would have to be
 * invalidated on every entry change, every goal change and every time-zone
 * change. The cost of computing it is bounded by the number of entries in the
 * day, which is small.
 *
 * @property dayEpochMillis the local midnight of the day this summary covers.
 * @property entries every entry of the day, oldest first.
 * @property entriesByMeal the entries grouped by meal; every meal of the day is
 *   present, even when it has no entries.
 * @property totals the nutrition of the whole day.
 * @property mealTotals the nutrition of each meal; every meal of the day is
 *   present, even when it has no entries.
 * @property goal the daily targets the totals are measured against.
 */
public data class DailySummary(
    val dayEpochMillis: Long,
    val entries: List<DiaryEntry>,
    val entriesByMeal: Map<MealType, List<DiaryEntry>>,
    val totals: NutritionTotals,
    val mealTotals: Map<MealType, NutritionTotals>,
    val goal: NutritionGoal,
) {
    /** The amount still available before the goal is reached. */
    public val remaining: NutritionTotals
        get() = totals.remainingAgainst(goal)

    /** The number of entries logged in the day. */
    public val entryCount: Int
        get() = entries.size

    /** Whether nothing has been logged yet. */
    public val isEmpty: Boolean
        get() = entries.isEmpty()
}
