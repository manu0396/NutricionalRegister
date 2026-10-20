@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies the nutrition amount formatting removes floating-point noise. */
class NutritionFormatTest {
    @Test
    fun roundsFloatingPointNoiseToTwoDecimals() {
        assertEquals("7.69", formatAmount(7.6899999999999995))
        assertEquals("63.46", formatAmount(63.46))
        assertEquals("44.23", formatAmount(44.23))
    }

    @Test
    fun wholeNumbersLoseDecimalPoint() {
        assertEquals("25", formatAmount(25.0))
        assertEquals("0", formatAmount(0.0))
        assertEquals("100", formatAmount(100.0))
    }

    @Test
    fun keepsMeaningfulDecimals() {
        assertEquals("0.8", formatAmount(0.8))
        assertEquals("1.25", formatAmount(1.25))
        assertEquals("519.23", formatAmount(519.23))
    }

    @Test
    fun roundsHalfUpAtTwoDecimals() {
        assertEquals("1.24", formatAmount(1.235))
        assertEquals("1.23", formatAmount(1.234))
    }
}
