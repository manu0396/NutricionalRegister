package com.example.nutricionalregister.domain.diary

/**
 * The nutrition of a set of foods.
 *
 * Every component is nullable, and `null` means **unknown**, not zero:
 *
 * - `0.0` means "known to be zero".
 * - `null` means "the catalogue did not report this nutrient", so no number can
 *   honestly be claimed for it.
 *
 * The distinction matters because a nutrition tracker that silently reports
 * `0 g of fat` for a food with no fat data is telling the user something false.
 * The aggregation rule is conservative: as soon as one contributing entry lacks
 * a nutrient, the total for that nutrient is unknown as well.
 */
public data class NutritionTotals(
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
) {
    /** Adds two totals component-wise; an unknown component stays unknown. */
    public operator fun plus(other: NutritionTotals): NutritionTotals =
        NutritionTotals(
            energyKcal = energyKcal.combine(other.energyKcal),
            proteinGrams = proteinGrams.combine(other.proteinGrams),
            carbohydrateGrams = carbohydrateGrams.combine(other.carbohydrateGrams),
            fatGrams = fatGrams.combine(other.fatGrams),
        )

    /** Scales every known component by [factor]; unknown components stay unknown. */
    public fun scaledBy(factor: Double): NutritionTotals =
        NutritionTotals(
            energyKcal = energyKcal?.times(factor),
            proteinGrams = proteinGrams?.times(factor),
            carbohydrateGrams = carbohydrateGrams?.times(factor),
            fatGrams = fatGrams?.times(factor),
        )

    /**
     * The amount still available before the [goal] is reached.
     *
     * A component is unknown when the total is unknown, and never negative.
     */
    public fun remainingAgainst(goal: NutritionGoal): NutritionTotals =
        NutritionTotals(
            energyKcal = remaining(energyKcal, goal.energyKcal),
            proteinGrams = remaining(proteinGrams, goal.proteinGrams),
            carbohydrateGrams = remaining(carbohydrateGrams, goal.carbohydrateGrams),
            fatGrams = remaining(fatGrams, goal.fatGrams),
        )

    /** Whether every component of this total is known. */
    public val isComplete: Boolean
        get() =
            energyKcal != null &&
                proteinGrams != null &&
                carbohydrateGrams != null &&
                fatGrams != null

    public companion object {
        /** A total in which nothing has been counted: every component is a known zero. */
        public val ZERO: NutritionTotals = NutritionTotals(0.0, 0.0, 0.0, 0.0)

        /**
         * The total of [entries].
         *
         * An empty list is a known zero, because nothing has been eaten yet.
         * A non-empty list is unknown for any nutrient that at least one entry
         * does not report.
         */
        public fun fromEntries(entries: List<DiaryEntry>): NutritionTotals {
            if (entries.isEmpty()) return ZERO
            return entries
                .map { entry ->
                    NutritionTotals(
                        energyKcal = entry.energyKcal,
                        proteinGrams = entry.proteinGrams,
                        carbohydrateGrams = entry.carbohydrateGrams,
                        fatGrams = entry.fatGrams,
                    )
                }.reduce { accumulator, totals -> accumulator + totals }
        }

        private fun Double?.combine(other: Double?): Double? =
            if (this == null || other == null) null else this + other

        private fun remaining(total: Double?, goal: Int): Double? =
            total?.let { (goal - it).coerceAtLeast(0.0) }
    }
}
