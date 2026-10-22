package com.example.nutricionalregister.domain.diary

/**
 * The daily targets the user's progress is measured against.
 *
 * There is exactly one goal record. The defaults are sensible adult reference
 * values, so the summary is meaningful before the user configures anything, and
 * every target is editable from the More destination.
 *
 * @property energyKcal the daily calorie target.
 * @property proteinGrams the daily protein target, in grams.
 * @property carbohydrateGrams the daily carbohydrate target, in grams.
 * @property fatGrams the daily fat target, in grams.
 */
public data class NutritionGoal(
    val energyKcal: Int = DEFAULT_ENERGY_KCAL,
    val proteinGrams: Int = DEFAULT_PROTEIN_GRAMS,
    val carbohydrateGrams: Int = DEFAULT_CARBOHYDRATE_GRAMS,
    val fatGrams: Int = DEFAULT_FAT_GRAMS,
) {
    init {
        require(energyKcal > 0) { "The calorie goal must be greater than zero" }
        require(proteinGrams > 0) { "The protein goal must be greater than zero" }
        require(carbohydrateGrams > 0) { "The carbohydrate goal must be greater than zero" }
        require(fatGrams > 0) { "The fat goal must be greater than zero" }
    }

    public companion object {
        public const val DEFAULT_ENERGY_KCAL: Int = 2000
        public const val DEFAULT_PROTEIN_GRAMS: Int = 120
        public const val DEFAULT_CARBOHYDRATE_GRAMS: Int = 220
        public const val DEFAULT_FAT_GRAMS: Int = 70
    }
}
