package com.example.nutricionalregister.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ProductDatabaseRowTypesTest {
    @Test
    fun nutritionRowRetainsAmountUnitAndBasis() {
        // GIVEN a nutrition row read from the product search tables
        val row =
            ProductNutrition(
                product_code = "0001",
                nutrient_key = "proteins",
                amount = 1.25,
                unit = "g",
                basis = "PER_100_G",
            )

        // THEN every column is retained on read
        assertEquals("0001", row.product_code)
        assertEquals("proteins", row.nutrient_key)
        assertEquals(1.25, row.amount)
        assertEquals("g", row.unit)
        assertEquals("PER_100_G", row.basis)

        // AND copying preserves equality unless the amount changes
        assertEquals(row, row.copy(amount = 2.5).copy(amount = 1.25))
        assertNotEquals(row, row.copy(amount = 2.5))

        // AND the unit column stays optional
        assertNull(row.copy(unit = null).unit)
    }

    @Test
    fun snapshotProductRowRetainsTermCodeAndPosition() {
        // GIVEN an ordered snapshot association row
        val row =
            SearchSnapshotProduct(
                normalized_term = "oat drink",
                product_code = "0001",
                result_position = 0L,
            )

        // THEN every column is retained on read
        assertEquals("oat drink", row.normalized_term)
        assertEquals("0001", row.product_code)
        assertEquals(0L, row.result_position)

        // AND copying preserves equality unless the position changes
        assertEquals(row, row.copy(result_position = 1L).copy(result_position = 0L))
        assertNotEquals(row, row.copy(result_position = 1L))
    }

    @Test
    fun recentSearchSequenceRowRetainsSingletonAndSequence() {
        // GIVEN the recent-search sequence bookkeeping row
        val row = RecentSearchSequence(singleton = 1L, sequence_value = 42L)

        // THEN every column is retained on read
        assertEquals(1L, row.singleton)
        assertEquals(42L, row.sequence_value)

        // AND copying preserves equality unless the sequence changes
        assertEquals(row, row.copy(sequence_value = 43L).copy(sequence_value = 42L))
        assertNotEquals(row, row.copy(sequence_value = 43L))
    }
}
