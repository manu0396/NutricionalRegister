package com.example.nutricionalregister.domain.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MealTypeTest {
    @Test
    fun `meals are ordered from breakfast to snacks`() {
        assertEquals(
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACKS),
            MealType.ordered,
        )
    }

    @Test
    fun `every meal has a display name and a distinct order`() {
        assertEquals(
            MealType.entries.size,
            MealType.ordered
                .map { it.order }
                .distinct()
                .size,
        )
        assertEquals(
            MealType.entries.size,
            MealType.entries
                .map { it.displayName }
                .distinct()
                .size,
        )
    }

    @Test
    fun `a stored meal value resolves back to its constant`() {
        MealType.entries.forEach { meal ->
            assertEquals(meal, MealType.fromStorage(meal.name))
        }
    }

    @Test
    fun `an unknown stored meal value fails loudly`() {
        assertFailsWith<IllegalArgumentException> { MealType.fromStorage("BRUNCH") }
    }
}
