package com.example.nutricionalregister.domain.diary

/**
 * One logged food.
 *
 * The entry is a **snapshot**: the product name and the nutrient values are
 * copied at log time, so a later change in the catalogue can never rewrite what
 * the user actually ate. The same reasoning applies to the image URL, which is
 * kept so the diary renders offline without a catalogue lookup.
 *
 * The nutrient components are nullable and follow the same rule as
 * [NutritionTotals]: `null` means the catalogue did not report the nutrient.
 *
 * @property id the row identifier; `0` for an entry that has not been stored.
 * @property productCode the catalogue identity of the logged product.
 * @property productName the product name at log time.
 * @property brands the brand at log time.
 * @property imageThumbnailUrl the product image at log time.
 * @property mealType the meal the entry belongs to.
 * @property quantityGrams the logged quantity, in grams; always greater than zero.
 * @property unit the quantity unit, `g` by default.
 * @property energyKcal the calories the logged quantity contributes.
 * @property proteinGrams the protein the logged quantity contributes.
 * @property carbohydrateGrams the carbohydrates the logged quantity contributes.
 * @property fatGrams the fat the logged quantity contributes.
 * @property loggedAtEpochMillis when the entry was logged.
 * @property dayEpochMillis the local midnight of the day the entry belongs to.
 */
public data class DiaryEntry(
    val id: Long = 0,
    val productCode: String,
    val productName: String,
    val brands: String? = null,
    val imageThumbnailUrl: String? = null,
    val mealType: MealType,
    val quantityGrams: Double,
    val unit: String = "g",
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
    val loggedAtEpochMillis: Long = 0,
    val dayEpochMillis: Long = 0,
) {
    init {
        require(productCode.isNotBlank()) { "Diary entry product code must not be blank" }
        require(productName.isNotBlank()) { "Diary entry product name must not be blank" }
        require(quantityGrams.isFinite() && quantityGrams > 0.0) {
            "Diary entry quantity must be a finite amount greater than zero"
        }
        require(energyKcal.isValidNutrient()) { "Diary entry energy must be a finite, non-negative amount" }
        require(proteinGrams.isValidNutrient()) { "Diary entry protein must be a finite, non-negative amount" }
        require(carbohydrateGrams.isValidNutrient()) {
            "Diary entry carbohydrates must be a finite, non-negative amount"
        }
        require(fatGrams.isValidNutrient()) { "Diary entry fat must be a finite, non-negative amount" }
    }

    /** The nutrition this single entry contributes. */
    public val totals: NutritionTotals
        get() =
            NutritionTotals(
                energyKcal = energyKcal,
                proteinGrams = proteinGrams,
                carbohydrateGrams = carbohydrateGrams,
                fatGrams = fatGrams,
            )

    private fun Double?.isValidNutrient(): Boolean = this == null || (isFinite() && this >= 0.0)
}
