package com.example.nutricionalregister.domain.summary

import com.example.nutricionalregister.domain.diary.NutritionTotals

/**
 * The week the user is living in.
 *
 * The week is the one the device's calendar starts, Monday to Sunday: the days
 * before today carry what was logged, and the days after today are still to
 * come, so the weekly view reads as the week the user is living and not as a
 * window the app invented.
 *
 * @property days exactly seven days, oldest first, and [today] the last of the
 *   days that are not in the future.
 * @property averageTotals the average over the days **that have entries**; a
 *   day without entries is never counted as a day of zero intake.
 * @property daysWithEntries the number of days with at least one entry.
 */
public data class WeeklySummary(
    val days: List<DaySummary>,
    val averageTotals: NutritionTotals,
    val daysWithEntries: Int,
) {
    /** The most recent day that is not in the future, which is today. */
    public val today: DaySummary?
        get() = days.lastOrNull { day -> !day.isFuture }

    /** The days of the week that are still to come. */
    public val futureDays: List<DaySummary>
        get() = days.filter { it.isFuture }

    /** The days that have at least one entry. */
    public val loggedDays: List<DaySummary>
        get() = days.filterNot { it.isEmpty }
}
