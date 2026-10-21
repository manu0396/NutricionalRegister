package com.example.nutricionalregister.domain.summary

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailySummaryTest {
    @Test
    fun `every meal of the day is present even when it has no entries`() {
        val summary = SummaryCalculator.daily(TODAY, listOf(entry(meal = MealType.LUNCH)), NutritionGoal())

        assertEquals(MealType.ordered, summary.entriesByMeal.keys.toList())
        assertEquals(MealType.ordered, summary.mealTotals.keys.toList())
        assertTrue(summary.entriesByMeal.getValue(MealType.BREAKFAST).isEmpty())
    }

    @Test
    fun `the entries are grouped by meal and ordered by log time`() {
        val summary =
            SummaryCalculator.daily(
                TODAY,
                listOf(
                    entry(meal = MealType.DINNER, loggedAt = 300L),
                    entry(meal = MealType.BREAKFAST, loggedAt = 100L),
                    entry(meal = MealType.LUNCH, loggedAt = 200L),
                ),
                NutritionGoal(),
            )

        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
            summary.entries.map { it.mealType },
        )
        assertEquals(1, summary.entriesByMeal.getValue(MealType.LUNCH).size)
    }

    @Test
    fun `the day totals are the sum of the entries`() {
        val summary =
            SummaryCalculator.daily(
                TODAY,
                listOf(
                    entry(energy = 100.0, protein = 10.0, carbohydrate = 5.0, fat = 2.0),
                    entry(energy = 250.0, protein = 20.0, carbohydrate = 15.0, fat = 8.0),
                ),
                NutritionGoal(),
            )

        assertEquals(350.0, summary.totals.energyKcal)
        assertEquals(30.0, summary.totals.proteinGrams)
        assertEquals(20.0, summary.totals.carbohydrateGrams)
        assertEquals(10.0, summary.totals.fatGrams)
        assertEquals(2, summary.entryCount)
    }

    @Test
    fun `the meal totals are the sum of the entries of that meal`() {
        val summary =
            SummaryCalculator.daily(
                TODAY,
                listOf(
                    entry(meal = MealType.LUNCH, energy = 100.0),
                    entry(meal = MealType.DINNER, energy = 250.0),
                ),
                NutritionGoal(),
            )

        assertEquals(100.0, summary.mealTotals.getValue(MealType.LUNCH).energyKcal)
        assertEquals(250.0, summary.mealTotals.getValue(MealType.DINNER).energyKcal)
    }

    @Test
    fun `the remaining amount is the goal minus the totals`() {
        val summary =
            SummaryCalculator.daily(
                TODAY,
                listOf(entry(energy = 500.0)),
                NutritionGoal(energyKcal = 2000),
            )

        assertEquals(1500.0, summary.remaining.energyKcal)
    }

    @Test
    fun `a day with no entries is empty and reports a known zero`() {
        val summary = SummaryCalculator.daily(TODAY, emptyList(), NutritionGoal())

        assertTrue(summary.isEmpty)
        assertEquals(0.0, summary.totals.energyKcal)
        assertEquals(0, summary.entryCount)
    }

    private fun entry(
        meal: MealType = MealType.LUNCH,
        loggedAt: Long = 100L,
        energy: Double = 100.0,
        protein: Double = 10.0,
        carbohydrate: Double = 5.0,
        fat: Double = 2.0,
    ): DiaryEntry =
        DiaryEntry(
            productCode = "0001",
            productName = "Oats",
            mealType = meal,
            quantityGrams = 30.0,
            energyKcal = energy,
            proteinGrams = protein,
            carbohydrateGrams = carbohydrate,
            fatGrams = fat,
            loggedAtEpochMillis = loggedAt,
            dayEpochMillis = TODAY,
        )

    private companion object {
        private const val TODAY = 1_700_000_000_000L
    }
}
