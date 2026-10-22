package com.example.nutricionalregister.domain.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DiaryEntryTest {
    @Test
    fun `an entry rejects a blank product identity`() {
        assertFailsWith<IllegalArgumentException> {
            DiaryEntry(productCode = " ", productName = "Oats", mealType = MealType.BREAKFAST, quantityGrams = 30.0)
        }
        assertFailsWith<IllegalArgumentException> {
            DiaryEntry(productCode = "1", productName = " ", mealType = MealType.BREAKFAST, quantityGrams = 30.0)
        }
    }

    @Test
    fun `an entry rejects a quantity that is not a positive amount`() {
        assertFailsWith<IllegalArgumentException> {
            DiaryEntry(productCode = "1", productName = "Oats", mealType = MealType.BREAKFAST, quantityGrams = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            DiaryEntry(productCode = "1", productName = "Oats", mealType = MealType.BREAKFAST, quantityGrams = -10.0)
        }
        assertFailsWith<IllegalArgumentException> {
            DiaryEntry(
                productCode = "1",
                productName = "Oats",
                mealType = MealType.BREAKFAST,
                quantityGrams = Double.NaN,
            )
        }
    }

    @Test
    fun `an entry rejects a negative nutrient`() {
        assertFailsWith<IllegalArgumentException> {
            DiaryEntry(
                productCode = "1",
                productName = "Oats",
                mealType = MealType.BREAKFAST,
                quantityGrams = 30.0,
                energyKcal = -1.0,
            )
        }
    }

    @Test
    fun `an entry accepts an absent nutrient`() {
        val entry =
            DiaryEntry(
                productCode = "1",
                productName = "Oats",
                mealType = MealType.BREAKFAST,
                quantityGrams = 30.0,
                fatGrams = null,
            )
        assertNull(entry.totals.fatGrams)
    }

    @Test
    fun `an entry exposes its own nutrition`() {
        val entry =
            DiaryEntry(
                productCode = "1",
                productName = "Oats",
                mealType = MealType.BREAKFAST,
                quantityGrams = 30.0,
                energyKcal = 150.0,
                proteinGrams = 5.0,
                carbohydrateGrams = 27.0,
                fatGrams = 3.0,
            )
        assertEquals(150.0, entry.totals.energyKcal)
        assertEquals(5.0, entry.totals.proteinGrams)
        assertEquals(27.0, entry.totals.carbohydrateGrams)
        assertEquals(3.0, entry.totals.fatGrams)
    }
}
