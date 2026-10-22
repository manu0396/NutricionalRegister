package com.example.nutricionalregister.feature.diary

import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionTotals
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.feature.diary.testing.FakeDayBoundaryProvider
import com.example.nutricionalregister.feature.diary.testing.FakeDiaryRepository
import com.example.nutricionalregister.feature.diary.testing.FakeProductSearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The behaviour of the add-food flow.
 *
 * The flow is checked through its state only: the step of the flow belongs to
 * the route and not to the view model, so what is left to check is the food, the
 * amount, the meal and the entry that is written when the user confirms.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddFoodViewModelTest {
    @Test
    fun `a new session resets the flow`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            viewModel.start(sessionId = FIRST_SESSION, meal = MealType.LUNCH)
            viewModel.selectProduct(product())
            viewModel.setQuantity(50.0)

            viewModel.start(sessionId = SECOND_SESSION)

            val state = viewModel.state.value
            assertNull(state.mealType)
            assertNull(state.product)
            assertEquals(0.0, state.quantityGrams)
            assertEquals(NutritionTotals.ZERO, state.preview)
        }

    @Test
    fun `the same session keeps what was already chosen`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            viewModel.start(sessionId = FIRST_SESSION, meal = MealType.LUNCH)
            viewModel.selectProduct(product())
            viewModel.setQuantity(50.0)

            viewModel.start(sessionId = FIRST_SESSION)

            val state = viewModel.state.value
            assertEquals(MealType.LUNCH, state.mealType)
            assertEquals(PRODUCT_CODE, state.product?.code)
            assertEquals(50.0, state.quantityGrams)
            assertEquals(190.0, state.preview.energyKcal)
        }

    @Test
    fun `a meal given to start preselects it and leaves the meal sheet closed`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            viewModel.start(sessionId = FIRST_SESSION, meal = MealType.DINNER)

            val state = viewModel.state.value
            assertEquals(MealType.DINNER, state.mealType)
            assertFalse(state.isMealSheetOpen)
        }

    @Test
    fun `a product code in the catalogue preselects the product with its default amount`() =
        runTest {
            val productSearchRepository = catalogue(product())
            val viewModel = viewModel(scope = scope(testScheduler), productSearchRepository = productSearchRepository)

            viewModel.start(sessionId = FIRST_SESSION, productCode = PRODUCT_CODE)
            advanceUntilIdle()

            assertEquals(listOf(PRODUCT_CODE), productSearchRepository.requestedCodes)
            val state = viewModel.state.value
            assertEquals(PRODUCT_CODE, state.product?.code)
            assertEquals(DEFAULT_QUANTITY_GRAMS, state.quantityGrams)
            assertEquals(114.0, state.preview.energyKcal)
            assertFalse(state.isResolvingProduct)
        }

    @Test
    fun `the lookup of a product code is announced while it runs`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler), productSearchRepository = catalogue(product()))

            viewModel.start(sessionId = FIRST_SESSION, productCode = PRODUCT_CODE)

            assertTrue(viewModel.state.value.isResolvingProduct)

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isResolvingProduct)
        }

    @Test
    fun `a product code that is not in the catalogue leaves the flow empty`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            viewModel.start(sessionId = FIRST_SESSION, productCode = UNKNOWN_CODE)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertNull(state.product)
            assertEquals(0.0, state.quantityGrams)
            assertEquals(NutritionTotals.ZERO, state.preview)
            assertFalse(state.isResolvingProduct)
        }

    @Test
    fun `choosing a meal sets it and closes the meal sheet`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            viewModel.openMealSheet()
            assertTrue(viewModel.state.value.isMealSheetOpen)

            viewModel.selectMeal(MealType.SNACKS)

            val state = viewModel.state.value
            assertEquals(MealType.SNACKS, state.mealType)
            assertFalse(state.isMealSheetOpen)
        }

    @Test
    fun `the meal sheet opens and closes without choosing a meal`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            viewModel.openMealSheet()
            assertTrue(viewModel.state.value.isMealSheetOpen)

            viewModel.closeMealSheet()

            val state = viewModel.state.value
            assertFalse(state.isMealSheetOpen)
            assertNull(state.mealType)
        }

    @Test
    fun `choosing a product sets the product the default amount and the preview`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            viewModel.selectProduct(product())

            val state = viewModel.state.value
            assertEquals(PRODUCT_CODE, state.product?.code)
            assertEquals(DEFAULT_QUANTITY_GRAMS, state.quantityGrams)
            assertEquals(114.0, state.preview.energyKcal)
        }

    @Test
    fun `setting the amount updates the preview`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))
            viewModel.selectProduct(product())

            viewModel.setQuantity(60.0)

            val state = viewModel.state.value
            assertEquals(60.0, state.quantityGrams)
            assertEquals(228.0, state.preview.energyKcal)
        }

    @Test
    fun `an amount that is not a positive number is ignored`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))
            viewModel.selectProduct(product())

            viewModel.setQuantity(0.0)
            viewModel.setQuantity(-10.0)
            viewModel.setQuantity(Double.NaN)

            val state = viewModel.state.value
            assertEquals(DEFAULT_QUANTITY_GRAMS, state.quantityGrams)
            assertEquals(114.0, state.preview.energyKcal)
        }

    @Test
    fun `the flow can only be confirmed when the food the meal and the amount are chosen`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            assertFalse(viewModel.state.value.canConfirm)

            viewModel.selectProduct(product())
            assertFalse(viewModel.state.value.canConfirm)

            viewModel.selectMeal(MealType.LUNCH)
            assertTrue(viewModel.state.value.canConfirm)

            val withoutAmount = AddFoodState(product = product(), mealType = MealType.LUNCH)
            assertFalse(withoutAmount.canConfirm)
        }

    @Test
    fun `confirming writes exactly one entry with the food the meal the amount and the day`() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val viewModel = viewModel(scope = scope(testScheduler), diaryRepository = diaryRepository)

            viewModel.start(sessionId = FIRST_SESSION, meal = MealType.LUNCH)
            viewModel.selectProduct(product())
            viewModel.setQuantity(50.0)

            viewModel.confirm()
            advanceUntilIdle()

            assertEquals(1, diaryRepository.addedEntries.size)
            val logged = diaryRepository.addedEntries.single()
            assertEquals(PRODUCT_CODE, logged.productCode)
            assertEquals("Oats", logged.productName)
            assertEquals(MealType.LUNCH, logged.mealType)
            assertEquals(50.0, logged.quantityGrams)
            assertEquals(190.0, logged.energyKcal)
            assertEquals(NOW_MILLIS, logged.loggedAtEpochMillis)
            assertEquals(0L, logged.dayEpochMillis)
        }

    @Test
    fun `confirming writes nothing while the flow is not ready`() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val viewModel = viewModel(scope = scope(testScheduler), diaryRepository = diaryRepository)

            viewModel.selectProduct(product())
            viewModel.confirm()
            advanceUntilIdle()

            assertTrue(diaryRepository.addedEntries.isEmpty())
        }

    @Test
    fun `the source and the query of the flow are kept`() =
        runTest {
            val viewModel = viewModel(scope = scope(testScheduler))

            viewModel.selectSource(AddFoodSource.LIBRARY)
            viewModel.setQuery("oats")

            val state = viewModel.state.value
            assertEquals(AddFoodSource.LIBRARY, state.source)
            assertEquals("oats", state.query)
        }

    private fun scope(scheduler: TestCoroutineScheduler): CoroutineScope = CoroutineScope(StandardTestDispatcher(scheduler))

    private fun catalogue(vararg products: Product): FakeProductSearchRepository = FakeProductSearchRepository(products.toList())

    private fun viewModel(
        scope: CoroutineScope,
        diaryRepository: FakeDiaryRepository = FakeDiaryRepository(),
        productSearchRepository: FakeProductSearchRepository = FakeProductSearchRepository(),
        clock: EpochClock = EpochClock { NOW_MILLIS },
    ): AddFoodViewModel =
        AddFoodViewModel(
            diaryRepository = diaryRepository,
            productSearchRepository = productSearchRepository,
            dayBoundaryProvider = FakeDayBoundaryProvider(),
            clock = clock,
            scope = scope,
        )

    private fun product(): Product =
        Product(
            code = PRODUCT_CODE,
            name = "Oats",
            brands = "Brand",
            quantity = "30 g",
            nutritionValues =
                listOf(
                    NutritionValue("energy", 380.0, null, NutritionBasis.PER_100_G),
                    NutritionValue("protein", 10.0, null, NutritionBasis.PER_100_G),
                ),
        )

    private companion object {
        const val PRODUCT_CODE: String = "0001"
        const val UNKNOWN_CODE: String = "9999"
        const val FIRST_SESSION: String = "first"
        const val SECOND_SESSION: String = "second"
        const val NOW_MILLIS: Long = 100L
        const val DEFAULT_QUANTITY_GRAMS: Double = 30.0
    }
}
