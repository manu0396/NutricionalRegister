package com.example.nutricionalregister.domain.summary

import com.example.nutricionalregister.domain.diary.NutritionTotals

/**
 * One day of the weekly summary.
 *
 * An empty day reports zero totals **and** [isEmpty] is true, so the UI can
 * show it as "no entries" rather than as "0 kcal eaten". The two are different
 * statements: the first means nothing was logged, the second means nothing was
 * eaten.
 *
 * @property dayEpochMillis the local midnight of the day.
 * @property entryCount the number of entries logged in the day.
 * @property totals the nutrition of the day.
 * @property isEmpty whether the day has no entries at all.
 */
public data class DaySummary(
    val dayEpochMillis: Long,
    val entryCount: Int,
    val totals: NutritionTotals,
    val isEmpty: Boolean,
)
