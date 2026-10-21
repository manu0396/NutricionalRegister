package com.example.nutricionalregister.domain.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NutritionTotalsTest {
    @Test
    fun `an empty day is a known zero`() {
        val totals = NutritionTotals.fromEntries(emptyList())
        assertEquals(0.0, totals.energyKcal)
        assertEquals(0.0, totals.proteinGrams)
        assertEquals(0.0, totals.carbohydrateGrams)
        assertEquals(0.0, totals.fatGrams)
        assertTrue(totals.isComplete)
    }

    @Test
    fun `totals are the sum of the entries`() {
        val totals =
            NutritionTotals.fromEntries(
                listOf(
                    entry(energy = 100.0, protein = 10.0, carbohydrate = 5.0, fat = 2.0),
                    entry(energy = 250.0, protein = 20.0, carbohydrate = 15.0, fat = 8.0),
                ),
            )
        assertEquals(350.0, totals.energyKcal)
        assertEquals(30.0, totals.proteinGrams)
        assertEquals(20.0, totals.carbohydrateGrams)
        assertEquals(10.0, totals.fatGrams)
    }

    @Test
    fun `a nutrient no entry reports is unknown rather than zero`() {
        val totals =
            NutritionTotals.fromEntries(
                listOf(
                    entry(energy = 100.0, protein = 10.0, carbohydrate = 5.0, fat = 2.0),
                    entry(energy = 250.0, protein = 20.0, carbohydrate = 15.0, fat = null),
                ),
            )
        assertEquals(350.0, totals.energyKcal)
        assertNull(totals.fatGrams)
        assertTrue(!totals.isComplete)
    }

    @Test
    fun `adding an unknown component keeps it unknown`() {
        val sum = NutritionTotals(energyKcal = 100.0, fatGrams = null) + NutritionTotals(energyKcal = 50.0, fatGrams = 5.0)
        assertEquals(150.0, sum.energyKcal)
        assertNull(sum.fatGrams)
    }

    @Test
    fun `adding is commutative`() {
        val first = NutritionTotals(energyKcal = 100.0, proteinGrams = 10.0)
        val second = NutritionTotals(energyKcal = 50.0, proteinGrams = 5.0)
        assertEquals(first + second, second + first)
    }

    @Test
    fun `scaling multiplies every known component`() {
        val totals = NutritionTotals(energyKcal = 200.0, proteinGrams = 20.0).scaledBy(1.5)
        assertEquals(300.0, totals.energyKcal)
        assertEquals(30.0, totals.proteinGrams)
        assertNull(totals.carbohydrateGrams)
    }

    @Test
    fun `the remaining amount never goes below zero`() {
        val goal = NutritionGoal(energyKcal = 2000, proteinGrams = 100, carbohydrateGrams = 200, fatGrams = 50)
        val remaining = NutritionTotals(energyKcal = 2500.0, proteinGrams = 40.0).remainingAgainst(goal)
        assertEquals(0.0, remaining.energyKcal)
        assertEquals(60.0, remaining.proteinGrams)
    }

    @Test
    fun `the remaining amount is unknown when the total is unknown`() {
        val goal = NutritionGoal()
        val remaining = NutritionTotals(energyKcal = null, proteinGrams = 10.0).remainingAgainst(goal)
        assertNull(remaining.energyKcal)
        assertEquals(110.0, remaining.proteinGrams)
    }

    @Test
    fun `a goal rejects a non positive target`() {
        assertFailsWith<IllegalArgumentException> { NutritionGoal(energyKcal = 0) }
        assertFailsWith<IllegalArgumentException> { NutritionGoal(proteinGrams = -1) }
    }

    @Test
    fun `the default goal is the documented reference`() {
        val goal = NutritionGoal()
        assertEquals(2000, goal.energyKcal)
        assertEquals(120, goal.proteinGrams)
        assertEquals(220, goal.carbohydrateGrams)
        assertEquals(70, goal.fatGrams)
    }

    private fun entry(
        energy: Double?,
        protein: Double?,
        carbohydrate: Double? = null,
        fat: Double? = null,
    ): DiaryEntry =
        DiaryEntry(
            productCode = "code",
            productName = "Product",
            mealType = MealType.LUNCH,
            quantityGrams = 100.0,
            energyKcal = energy,
            proteinGrams = protein,
            carbohydrateGrams = carbohydrate,
            fatGrams = fat,
        )
}
