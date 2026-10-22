package com.example.nutricionalregister.domain.diary

import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NutritionCalculatorTest {
    @Test
    fun `a per hundred gram value is scaled to the logged quantity`() {
        val product = product(nutritionDataPer = "100g", quantity = null)
        val totals = NutritionCalculator.contribution(product, quantityGrams = 250.0)
        assertEquals(950.0, totals.energyKcal)
        assertEquals(25.0, totals.proteinGrams)
        assertEquals(50.0, totals.carbohydrateGrams)
        assertEquals(5.0, totals.fatGrams)
    }

    @Test
    fun `a per serving value is scaled by the declared serving size`() {
        val product = product(nutritionDataPer = "serving", quantity = "30 g")
        val totals = NutritionCalculator.contribution(product, quantityGrams = 60.0)
        assertEquals(760.0, totals.energyKcal)
        assertEquals(20.0, totals.proteinGrams)
    }

    @Test
    fun `a serving size with extra wording still resolves`() {
        val product = product(nutritionDataPer = "serving", quantity = "1 bar (40g)")
        assertEquals(40.0, NutritionCalculator.servingGrams(product))
    }

    @Test
    fun `a serving size that is not in grams does not resolve`() {
        val product = product(nutritionDataPer = "serving", quantity = "250 ml")
        assertNull(NutritionCalculator.servingGrams(product))
        assertNull(NutritionCalculator.contribution(product, quantityGrams = 100.0).energyKcal)
    }

    @Test
    fun `a nutrient the catalogue does not report is absent rather than zero`() {
        val product =
            Product(
                code = "1",
                name = "Oats",
                nutritionValues = listOf(nutrition(NutritionCalculator.ENERGY_KEY, 380.0, NutritionBasis.PER_100_G)),
            )
        val totals = NutritionCalculator.contribution(product, quantityGrams = 100.0)
        assertEquals(380.0, totals.energyKcal)
        assertNull(totals.proteinGrams)
        assertNull(totals.fatGrams)
    }

    @Test
    fun `an unknown basis yields no number`() {
        val product = product(nutritionDataPer = null, quantity = null)
        assertNull(NutritionCalculator.contribution(product, quantityGrams = 100.0).energyKcal)
    }

    @Test
    fun `the default quantity is the serving size when there is one`() {
        assertEquals(30.0, NutritionCalculator.defaultQuantityGrams(product(nutritionDataPer = "serving", quantity = "30 g")))
        assertEquals(
            NutritionCalculator.DEFAULT_QUANTITY_GRAMS,
            NutritionCalculator.defaultQuantityGrams(product(nutritionDataPer = "100g", quantity = null)),
        )
    }

    @Test
    fun `a contribution rejects a quantity that is not a positive amount`() {
        val product = product(nutritionDataPer = "100g", quantity = null)
        assertFailsWith<IllegalArgumentException> { NutritionCalculator.contribution(product, quantityGrams = 0.0) }
    }

    @Test
    fun `an entry snapshots the nutrition of the logged quantity`() {
        val product = product(nutritionDataPer = "100g", quantity = null)
        val entry =
            NutritionCalculator.entryFor(
                product = product,
                mealType = MealType.BREAKFAST,
                quantityGrams = 50.0,
                loggedAtEpochMillis = 1_000L,
                dayEpochMillis = 0L,
            )
        assertEquals("1", entry.productCode)
        assertEquals("Oats", entry.productName)
        assertEquals(MealType.BREAKFAST, entry.mealType)
        assertEquals(50.0, entry.quantityGrams)
        assertEquals(190.0, entry.energyKcal)
        assertEquals(5.0, entry.proteinGrams)
        assertEquals(1_000L, entry.loggedAtEpochMillis)
        assertEquals(0L, entry.dayEpochMillis)
    }

    private fun product(
        nutritionDataPer: String?,
        quantity: String?,
    ): Product =
        Product(
            code = "1",
            name = "Oats",
            brands = "Acme",
            quantity = quantity,
            nutritionDataPer = nutritionDataPer,
            nutritionValues =
                listOf(
                    nutrition(NutritionCalculator.ENERGY_KEY, 380.0, basis(nutritionDataPer)),
                    nutrition(NutritionCalculator.PROTEIN_KEY, 10.0, basis(nutritionDataPer)),
                    nutrition(NutritionCalculator.CARBOHYDRATE_KEY, 20.0, basis(nutritionDataPer)),
                    nutrition(NutritionCalculator.FAT_KEY, 2.0, basis(nutritionDataPer)),
                ),
        )

    private fun nutrition(
        key: String,
        amount: Double,
        basis: NutritionBasis,
    ): NutritionValue = NutritionValue(nutrientKey = key, amount = amount, unit = null, basis = basis)

    private fun basis(nutritionDataPer: String?): NutritionBasis =
        when {
            nutritionDataPer == null -> NutritionBasis.UNKNOWN
            nutritionDataPer.contains("100g") -> NutritionBasis.PER_100_G
            else -> NutritionBasis.PER_SERVING
        }
}
