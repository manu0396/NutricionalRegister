package com.example.nutricionalregister.domain.summary

import com.example.nutricionalregister.domain.diary.NutritionTotals

/**
 * The last seven days.
 *
 * @property days exactly seven days, oldest first, ending with today.
 * @property averageTotals the average over the days **that have entries**; a
 *   day without entries is never counted as a day of zero intake.
 * @property daysWithEntries the number of days with at least one entry.
 */
public data class WeeklySummary(
    val days: List<DaySummary>,
    val averageTotals: NutritionTotals,
    val daysWithEntries: Int,
) {
    /** The most recent day, which is today. */
    public val today: DaySummary?
        get() = days.lastOrNull()

    /** The days that have at least one entry. */
    public val loggedDays: List<DaySummary>
        get() = days.filterNot { it.isEmpty }
}
