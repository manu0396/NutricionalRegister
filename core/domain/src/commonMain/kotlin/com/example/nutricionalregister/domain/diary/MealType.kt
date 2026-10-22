package com.example.nutricionalregister.domain.diary

/**
 * The meals of the day.
 *
 * Meals are the spine of the diary: entries are grouped by meal, each meal has
 * its own calorie subtotal, and the meal sections are always shown in this
 * order. The set is deliberately fixed for now; making it user-editable is a
 * separate feature.
 *
 * @property displayName the label shown to the user.
 * @property order the position of the meal in the day, from 0 (Breakfast).
 */
public enum class MealType(
    public val displayName: String,
    public val order: Int,
) {
    BREAKFAST("Breakfast", 0),
    LUNCH("Lunch", 1),
    DINNER("Dinner", 2),
    SNACKS("Snacks", 3),
    ;

    public companion object {
        /** The meals in the order they are displayed. */
        public val ordered: List<MealType> = entries.sortedBy { it.order }

        /**
         * Resolves a stored meal value back to its constant.
         *
         * Throws [IllegalArgumentException] for an unknown value so that a
         * corrupted row fails loudly instead of silently dropping an entry from
         * the user's day.
         */
        public fun fromStorage(value: String): MealType =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown meal type: $value")
    }
}
