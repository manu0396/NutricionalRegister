package com.example.nutricionalregister.domain.summary

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.testing.FakeDayBoundaryProvider
import com.example.nutricionalregister.domain.testing.MILLIS_PER_DAY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WeeklySummaryTest {
    private val provider = FakeDayBoundaryProvider()
    private val today = provider.startOfDay(NOW)
    private val yesterday = today - MILLIS_PER_DAY

    @Test
    fun `the summary always covers exactly seven days - oldest first and ending with today`() {
        val summary = SummaryCalculator.weekly(provider, NOW, emptyList())

        assertEquals(SummaryCalculator.DAYS_IN_WEEK, summary.days.size)
        assertEquals(today, summary.days.last().dayEpochMillis)
        assertEquals(summary.days.map { it.dayEpochMillis }.sorted(), summary.days.map { it.dayEpochMillis })
    }

    @Test
    fun `a day without entries is empty and reports a known zero`() {
        val summary = SummaryCalculator.weekly(provider, NOW, emptyList())

        assertTrue(summary.days.all { it.isEmpty })
        assertTrue(summary.days.all { it.entryCount == 0 })
        assertEquals(
            0.0,
            summary.days
                .first()
                .totals.energyKcal,
        )
    }

    @Test
    fun `an entry belongs to the day it was logged in`() {
        val summary = SummaryCalculator.weekly(provider, NOW, listOf(entry(day = yesterday)))

        val yesterdaySummary = summary.days.first { it.dayEpochMillis == yesterday }
        assertEquals(1, yesterdaySummary.entryCount)
        assertTrue(!yesterdaySummary.isEmpty)
        assertEquals(100.0, yesterdaySummary.totals.energyKcal)
    }

    @Test
    fun `the average is taken over the days that have entries only`() {
        val summary =
            SummaryCalculator.weekly(
                provider,
                NOW,
                listOf(
                    entry(day = today, energy = 400.0),
                    entry(day = yesterday, energy = 600.0),
                ),
            )

        assertEquals(2, summary.daysWithEntries)
        assertEquals(500.0, summary.averageTotals.energyKcal)
    }

    @Test
    fun `a day without entries does not drag the average down`() {
        val summary =
            SummaryCalculator.weekly(
                provider,
                NOW,
                listOf(
                    entry(day = today, energy = 400.0),
                    entry(day = yesterday - 3 * MILLIS_PER_DAY, energy = 600.0),
                ),
            )

        assertEquals(2, summary.daysWithEntries)
        assertEquals(500.0, summary.averageTotals.energyKcal)
    }

    @Test
    fun `the average of a week without entries is a known zero`() {
        val summary = SummaryCalculator.weekly(provider, NOW, emptyList())

        assertEquals(0, summary.daysWithEntries)
        assertEquals(0.0, summary.averageTotals.energyKcal)
    }

    @Test
    fun `an entry of a day outside the window is not counted`() {
        val summary =
            SummaryCalculator.weekly(
                provider,
                NOW,
                listOf(entry(day = today - 8 * MILLIS_PER_DAY, energy = 900.0)),
            )

        assertEquals(0, summary.daysWithEntries)
        assertEquals(0.0, summary.averageTotals.energyKcal)
    }

    @Test
    fun `the logged days are the days that have entries`() {
        val summary =
            SummaryCalculator.weekly(
                provider,
                NOW,
                listOf(
                    entry(day = today, energy = 400.0),
                    entry(day = yesterday, energy = 600.0),
                ),
            )

        assertEquals(2, summary.loggedDays.size)
        assertEquals(today, assertNotNull(summary.today).dayEpochMillis)
        assertTrue(summary.days.first().isEmpty)
    }

    private fun entry(
        day: Long,
        energy: Double = 100.0,
    ): DiaryEntry =
        DiaryEntry(
            productCode = "0001",
            productName = "Oats",
            mealType = MealType.LUNCH,
            quantityGrams = 30.0,
            energyKcal = energy,
            proteinGrams = 5.0,
            carbohydrateGrams = 10.0,
            fatGrams = 2.0,
            loggedAtEpochMillis = day + 1_000L,
            dayEpochMillis = day,
        )

    private companion object {
        private const val NOW = 1_700_000_000_000L
    }
}
