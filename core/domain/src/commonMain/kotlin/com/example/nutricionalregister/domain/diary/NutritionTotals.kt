package com.example.nutricionalregister.domain.diary

/**
 * The nutrition of a set of foods.
 *
 * Every component is nullable, and `null` means **unknown**, not zero:
 *
 * - `0.0` means "known to be zero".
 * - `null` means "no food in the set reported this nutrient at all", so no
 *   number can honestly be claimed for it.
 *
 * The distinction matters because a nutrition tracker that silently reports
 * `0 g of fat` for a food with no fat data is telling the user something false.
 *
 * When only *some* of the foods report a nutrient, the total is the sum of the
 * reported values and the nutrient is listed in [incomplete]: the real amount
 * is at least that sum. The UI shows a lower bound rather than presenting a
 * partial figure as the whole truth.
 */
public data class NutritionTotals(
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
    val incomplete: Set<Nutrient> = emptySet(),
) {
    /** Adds two totals component-wise, keeping track of the incomplete components. */
    public operator fun plus(other: NutritionTotals): NutritionTotals {
        val energy = sum(energyKcal, other.energyKcal)
        val protein = sum(proteinGrams, other.proteinGrams)
        val carbohydrate = sum(carbohydrateGrams, other.carbohydrateGrams)
        val fat = sum(fatGrams, other.fatGrams)
        return NutritionTotals(
            energyKcal = energy.value,
            proteinGrams = protein.value,
            carbohydrateGrams = carbohydrate.value,
            fatGrams = fat.value,
            incomplete =
                incomplete +
                    other.incomplete +
                    buildSet {
                        if (energy.partial) add(Nutrient.ENERGY)
                        if (protein.partial) add(Nutrient.PROTEIN)
                        if (carbohydrate.partial) add(Nutrient.CARBOHYDRATE)
                        if (fat.partial) add(Nutrient.FAT)
                    },
        )
    }

    /** Scales every known component by [factor]; unknown components stay unknown. */
    public fun scaledBy(factor: Double): NutritionTotals =
        NutritionTotals(
            energyKcal = energyKcal?.times(factor),
            proteinGrams = proteinGrams?.times(factor),
            carbohydrateGrams = carbohydrateGrams?.times(factor),
            fatGrams = fatGrams?.times(factor),
            incomplete = incomplete,
        )

    /**
     * The amount still available before the [goal] is reached.
     *
     * A component is unknown when the total is unknown, and never negative. A
     * component that was already incomplete stays incomplete, because an
     * uncertain intake leaves an uncertain remainder.
     */
    public fun remainingAgainst(goal: NutritionGoal): NutritionTotals =
        NutritionTotals(
            energyKcal = remaining(energyKcal, goal.energyKcal),
            proteinGrams = remaining(proteinGrams, goal.proteinGrams),
            carbohydrateGrams = remaining(carbohydrateGrams, goal.carbohydrateGrams),
            fatGrams = remaining(fatGrams, goal.fatGrams),
            incomplete = incomplete,
        )

    /** Whether every component of this total is known and complete. */
    public val isComplete: Boolean
        get() =
            incomplete.isEmpty() &&
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
         * A nutrient that no entry reports is unknown, and a nutrient that only
         * some entries report is a lower bound.
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

        private fun sum(
            left: Double?,
            right: Double?,
        ): Sum =
            when {
                left == null && right == null -> Sum(value = null, partial = false)
                left == null -> Sum(value = right, partial = true)
                right == null -> Sum(value = left, partial = true)
                else -> Sum(value = left + right, partial = false)
            }

        private fun remaining(
            total: Double?,
            goal: Int,
        ): Double? = total?.let { consumed -> (goal - consumed).coerceAtLeast(0.0) }
    }

    private data class Sum(
        val value: Double?,
        val partial: Boolean,
    )
}
