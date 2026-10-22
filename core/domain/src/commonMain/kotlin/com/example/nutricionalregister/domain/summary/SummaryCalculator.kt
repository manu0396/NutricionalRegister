package com.example.nutricionalregister.domain.summary

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryDay
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.diary.NutritionTotals

/**
 * Builds the daily and the weekly summaries.
 *
 * The calculator is pure: the same entries and the same goal always produce the
 * same summary, and nothing is written to storage. That keeps the Today and
 * Weekly screens consistent with each other and with the diary itself.
 *
 * The calculator lives in the shared domain because it is domain logic, not
 * presentation logic: the Android UI renders the result and never sums a
 * nutrient itself.
 */
public object SummaryCalculator {
    /** The number of days a weekly summary covers. */
    public const val DAYS_IN_WEEK: Int = 7

    /**
     * The summary of the day that starts at [dayEpochMillis].
     *
     * Every meal of the day is present in [DailySummary.entriesByMeal] and
     * [DailySummary.mealTotals], including the meals with no entries, so the
     * Today screen can render its four sections without special-casing them.
     */
    public fun daily(
        dayEpochMillis: Long,
        entries: List<DiaryEntry>,
        goal: NutritionGoal,
    ): DailySummary {
        val ordered = entries.sortedWith(compareBy({ it.loggedAtEpochMillis }, { it.id }))
        val entriesByMeal =
            MealType.ordered.associateWith { meal -> ordered.filter { it.mealType == meal } }
        val mealTotals =
            entriesByMeal.mapValues { (_, mealEntries) -> NutritionTotals.fromEntries(mealEntries) }
        return DailySummary(
            dayEpochMillis = dayEpochMillis,
            entries = ordered,
            entriesByMeal = entriesByMeal,
            totals = NutritionTotals.fromEntries(ordered),
            mealTotals = mealTotals,
            goal = goal,
        )
    }

    /**
     * The summary of the week the user is living in.
     *
     * The week is the one the device's calendar starts and is exactly seven
     * days: the days before today carry what was logged and the days after
     * today are marked as still to come, which is what "still to come"
     * means: the user cannot have logged into the future yet.
     *
     * The seven days are always returned, even the ones without entries, and
     * the average is taken over the days that have entries only: a day without
     * entries is a day the user did not log, not a day they ate nothing.
     */
    public fun weekly(
        dayBoundaryProvider: DayBoundaryProvider,
        todayEpochMillis: Long,
        entries: List<DiaryEntry>,
    ): WeeklySummary {
        val weekStart = dayBoundaryProvider.startOfWeek(todayEpochMillis)
        val days = DiaryDay.weekDays(dayBoundaryProvider, weekStart, DAYS_IN_WEEK)
        val today = dayBoundaryProvider.startOfDay(todayEpochMillis)
        val entriesByDay = entries.groupBy { it.dayEpochMillis }
        val daySummaries =
            days.map { day ->
                val dayEntries = entriesByDay[day].orEmpty()
                DaySummary(
                    dayEpochMillis = day,
                    entryCount = dayEntries.size,
                    totals = NutritionTotals.fromEntries(dayEntries),
                    isEmpty = dayEntries.isEmpty(),
                    isFuture = day > today,
                )
            }
        val daysWithEntries = daySummaries.filterNot { it.isEmpty }
        return WeeklySummary(
            days = daySummaries,
            averageTotals = averageOf(daysWithEntries.map { it.totals }),
            daysWithEntries = daysWithEntries.size,
        )
    }

    private fun averageOf(totals: List<NutritionTotals>): NutritionTotals =
        if (totals.isEmpty()) {
            NutritionTotals.ZERO
        } else {
            totals.reduce { accumulator, value -> accumulator + value }.scaledBy(1.0 / totals.size)
        }
}
