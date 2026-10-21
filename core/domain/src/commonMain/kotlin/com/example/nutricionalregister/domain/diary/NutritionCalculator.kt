package com.example.nutricionalregister.domain.diary

import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.Product

/**
 * Turns a catalogue product and a quantity into the nutrition that quantity
 * contributes.
 *
 * The calculation lives in the shared domain so the UI never computes a
 * nutrition value itself: the Android screens only render what this returns.
 *
 * The catalogue reports nutrients on different bases. A value per 100 g is
 * scaled linearly. A value per serving is scaled by the serving size the
 * product declares. A value whose basis cannot be resolved yields **no**
 * number, because a wrong number is worse than an absent one.
 */
public object NutritionCalculator {
    /** The nutrient key the catalogue uses for calories. */
    public const val ENERGY_KEY: String = "energy"

    /** The nutrient key the catalogue uses for protein. */
    public const val PROTEIN_KEY: String = "protein"

    /** The nutrient key the catalogue uses for carbohydrates. */
    public const val CARBOHYDRATE_KEY: String = "carbohydrate"

    /** The nutrient key the catalogue uses for fat. */
    public const val FAT_KEY: String = "fat"

    /** The quantity used when the product declares no serving size. */
    public const val DEFAULT_QUANTITY_GRAMS: Double = 100.0

    /**
     * The nutrition [quantityGrams] of [product] contributes.
     *
     * A nutrient the catalogue does not report for the product is absent from
     * the result rather than reported as zero.
     */
    public fun contribution(
        product: Product,
        quantityGrams: Double,
    ): NutritionTotals {
        require(quantityGrams.isFinite() && quantityGrams > 0.0) {
            "The quantity must be a finite amount greater than zero"
        }
        return NutritionTotals(
            energyKcal = value(product, ENERGY_KEY, quantityGrams),
            proteinGrams = value(product, PROTEIN_KEY, quantityGrams),
            carbohydrateGrams = value(product, CARBOHYDRATE_KEY, quantityGrams),
            fatGrams = value(product, FAT_KEY, quantityGrams),
        )
    }

    /**
     * The serving size of [product] in grams, or null when the product does not
     * declare one that can be resolved.
     */
    public fun servingGrams(product: Product): Double? {
        val grams = product.quantity?.let { parseGrams(it) } ?: return null
        return grams.takeIf { it > 0.0 }
    }

    /**
     * The quantity the add flow starts from: the product's own serving when it
     * declares one, and 100 g otherwise.
     */
    public fun defaultQuantityGrams(product: Product): Double = servingGrams(product) ?: DEFAULT_QUANTITY_GRAMS

    /**
     * Builds the diary entry for logging [quantityGrams] of [product] into
     * [mealType].
     *
     * The nutrient values are snapshotted here, so the entry keeps the
     * nutrition the product had at the moment it was logged.
     */
    public fun entryFor(
        product: Product,
        mealType: MealType,
        quantityGrams: Double,
        loggedAtEpochMillis: Long,
        dayEpochMillis: Long,
    ): DiaryEntry {
        val totals = contribution(product, quantityGrams)
        return DiaryEntry(
            productCode = product.code,
            productName = product.name,
            brands = product.brands,
            imageThumbnailUrl = product.imageThumbnailUrl,
            mealType = mealType,
            quantityGrams = quantityGrams,
            energyKcal = totals.energyKcal,
            proteinGrams = totals.proteinGrams,
            carbohydrateGrams = totals.carbohydrateGrams,
            fatGrams = totals.fatGrams,
            loggedAtEpochMillis = loggedAtEpochMillis,
            dayEpochMillis = dayEpochMillis,
        )
    }

    private fun value(
        product: Product,
        nutrientKey: String,
        quantityGrams: Double,
    ): Double? {
        val nutritionValue = product.nutritionValues.firstOrNull { it.nutrientKey == nutrientKey } ?: return null
        val factor =
            when (nutritionValue.basis) {
                NutritionBasis.PER_100_G -> quantityGrams / 100.0
                NutritionBasis.PER_SERVING -> {
                    val serving = servingGrams(product) ?: return null
                    quantityGrams / serving
                }
                NutritionBasis.UNKNOWN -> return null
            }
        return nutritionValue.amount * factor
    }

    private fun parseGrams(quantity: String): Double? {
        val match = GRAMS_PATTERN.find(quantity) ?: return null
        return match.groupValues[1].replace(',', '.').toDoubleOrNull()
    }

    private val GRAMS_PATTERN = Regex("""(\d+(?:[.,]\d+)?)\s*g\b""", RegexOption.IGNORE_CASE)
}
