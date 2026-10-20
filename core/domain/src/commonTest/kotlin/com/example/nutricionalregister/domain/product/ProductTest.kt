package com.example.nutricionalregister.domain.product

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProductTest {
    @Test
    fun searchTermTrimsAndNormalizesInput() {
        val term = SearchTerm.fromInput("  Oat Drink ")

        assertEquals("oat drink", term?.normalizedTerm)
        assertEquals("Oat Drink", term?.displayTerm)
    }

    @Test
    fun blankSearchTermIsRejected() {
        assertNull(SearchTerm.fromInput(" \t "))
    }

    @Test
    fun productRequiresOpaqueCodeAndDisplayName() {
        assertFailsWith<IllegalArgumentException> {
            Product(code = "", name = "Oats")
        }
        assertFailsWith<IllegalArgumentException> {
            Product(code = "00042", name = " ")
        }

        assertEquals("00042", Product(code = "00042", name = "Beans").code)
    }

    @Test
    fun nutritionRetainsValueUnitAndBasis() {
        val value =
            NutritionValue(
                nutrientKey = "protein",
                amount = 0.0,
                unit = "g",
                basis = NutritionBasis.PER_100_G,
            )

        assertEquals(0.0, value.amount)
        assertEquals("g", value.unit)
        assertEquals(NutritionBasis.PER_100_G, value.basis)
    }

    @Test
    fun nutritionRejectsNonFiniteValues() {
        assertFailsWith<IllegalArgumentException> {
            NutritionValue("protein", Double.NaN, null, NutritionBasis.UNKNOWN)
        }
        assertFailsWith<IllegalArgumentException> {
            NutritionValue("protein", Double.POSITIVE_INFINITY, null, NutritionBasis.UNKNOWN)
        }
    }
}
