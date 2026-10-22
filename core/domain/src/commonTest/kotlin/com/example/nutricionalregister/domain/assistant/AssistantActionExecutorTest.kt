package com.example.nutricionalregister.domain.assistant

import com.example.nutricionalregister.domain.assistant.testing.FakeProductSearchRepository
import com.example.nutricionalregister.domain.assistant.testing.FakeSavedProductRepository
import com.example.nutricionalregister.domain.assistant.testing.RecordingDiaryRepository
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.testing.FakeDayBoundaryProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AssistantActionExecutorTest {
    @Test
    fun `a valid log writes one entry with the product the quantity and the meal`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val outcome =
                executor.apply(
                    AssistantAction.LogFood(
                        productCode = "0001",
                        quantityGrams = 50.0,
                        mealType = MealType.LUNCH,
                    ),
                )

            val applied = assertIs<ActionOutcome.Applied>(outcome)
            assertEquals(1L, applied.entryId)
            val written = repository.addedEntries.single()
            assertEquals("0001", written.productCode)
            assertEquals("Oats", written.productName)
            assertEquals(50.0, written.quantityGrams)
            assertEquals(MealType.LUNCH, written.mealType)
            assertEquals(190.0, written.energyKcal)
            assertEquals(5.0, written.proteinGrams)
            assertEquals(0, repository.updatedEntries.size)
            assertEquals(0, repository.removedIds.size)
        }

    @Test
    fun `a logged entry belongs to the current day`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            executor.apply(
                AssistantAction.LogFood(
                    productCode = "0001",
                    quantityGrams = 50.0,
                    mealType = MealType.LUNCH,
                ),
            )

            val written = repository.addedEntries.single()
            assertEquals(100L, written.loggedAtEpochMillis)
            assertEquals(0L, written.dayEpochMillis)
        }

    @Test
    fun `a valid removal removes the entry`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val entryId = repository.addEntry(entry())
            val executor = executor(repository)

            val outcome = executor.apply(AssistantAction.RemoveEntry(entryId = entryId))

            assertEquals(ActionOutcome.Applied(entryId), outcome)
            assertEquals(listOf(entryId), repository.removedIds)
            assertEquals(emptyList(), repository.updatedEntries)
            assertEquals(1, repository.addedEntries.size)
        }

    @Test
    fun `a valid change updates the entry with the new quantity and meal`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val entryId = repository.addEntry(entry())
            val executor = executor(repository)

            val outcome =
                executor.apply(
                    AssistantAction.ChangeEntry(
                        entryId = entryId,
                        quantityGrams = 50.0,
                        mealType = MealType.LUNCH,
                    ),
                )

            assertEquals(ActionOutcome.Applied(entryId), outcome)
            val updated = repository.updatedEntries.single()
            assertEquals(50.0, updated.quantityGrams)
            assertEquals(MealType.LUNCH, updated.mealType)
            assertEquals(190.0, updated.energyKcal)
            assertEquals(5.0, updated.proteinGrams)
            assertEquals(30.0, updated.carbohydrateGrams)
            assertEquals(3.5, updated.fatGrams)
        }

    @Test
    fun `a change without a new quantity keeps the logged one`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val entryId = repository.addEntry(entry())
            val executor = executor(repository)

            executor.apply(
                AssistantAction.ChangeEntry(
                    entryId = entryId,
                    mealType = MealType.DINNER,
                ),
            )

            val updated = repository.updatedEntries.single()
            assertEquals(100.0, updated.quantityGrams)
            assertEquals(MealType.DINNER, updated.mealType)
            assertEquals(380.0, updated.energyKcal)
        }

    @Test
    fun `a change without a new meal keeps the logged one`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val entryId = repository.addEntry(entry())
            val executor = executor(repository)

            executor.apply(
                AssistantAction.ChangeEntry(
                    entryId = entryId,
                    quantityGrams = 50.0,
                ),
            )

            val updated = repository.updatedEntries.single()
            assertEquals(50.0, updated.quantityGrams)
            assertEquals(MealType.BREAKFAST, updated.mealType)
        }

    @Test
    fun `a change with a quantity that is not a number is rejected`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val entryId = repository.addEntry(entry())
            val executor = executor(repository)

            val validation = executor.validate(AssistantAction.ChangeEntry(entryId = entryId, quantityGrams = Double.NaN))

            assertIs<ActionValidation.Rejected>(validation)
        }

    @Test
    fun `a food the catalogue does not know is rejected`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val validation =
                executor.validate(
                    AssistantAction.LogFood(
                        productCode = "unknown-code",
                        quantityGrams = 50.0,
                        mealType = MealType.LUNCH,
                    ),
                )

            val rejected = assertIs<ActionValidation.Rejected>(validation)
            assertTrue(rejected.reason.contains("unknown-code"))
        }

    @Test
    fun `a quantity that is not greater than zero is rejected`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val quantities = listOf(0.0, -10.0, Double.NaN, Double.POSITIVE_INFINITY)
            quantities.forEach { quantity ->
                val validation =
                    executor.validate(
                        AssistantAction.LogFood(
                            productCode = "0001",
                            quantityGrams = quantity,
                            mealType = MealType.LUNCH,
                        ),
                    )
                assertIs<ActionValidation.Rejected>(validation)
            }
        }

    @Test
    fun `an unknown entry is rejected`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val validation = executor.validate(AssistantAction.RemoveEntry(entryId = 42L))

            val rejected = assertIs<ActionValidation.Rejected>(validation)
            assertTrue(rejected.reason.contains("42"))
        }

    @Test
    fun `a change of an unknown entry is rejected`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val validation = executor.validate(AssistantAction.ChangeEntry(entryId = 42L, quantityGrams = 50.0))

            assertIs<ActionValidation.Rejected>(validation)
        }

    @Test
    fun `a change with an impossible quantity is rejected`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val entryId = repository.addEntry(entry())
            val executor = executor(repository)

            val validation = executor.validate(AssistantAction.ChangeEntry(entryId = entryId, quantityGrams = -5.0))

            assertIs<ActionValidation.Rejected>(validation)
        }

    @Test
    fun `a rejected action writes nothing`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val outcome = executor.apply(AssistantAction.RemoveEntry(entryId = 42L))

            val rejected = assertIs<ActionOutcome.Rejected>(outcome)
            assertTrue(rejected.reason.contains("42"))
            assertEquals(0, repository.writeCount)
        }

    @Test
    fun `a rejected log writes nothing`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val outcome =
                executor.apply(
                    AssistantAction.LogFood(
                        productCode = "unknown-code",
                        quantityGrams = 50.0,
                        mealType = MealType.LUNCH,
                    ),
                )

            assertIs<ActionOutcome.Rejected>(outcome)
            assertEquals(0, repository.writeCount)
        }

    @Test
    fun `a rejected change writes nothing`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val outcome = executor.apply(AssistantAction.ChangeEntry(entryId = 42L, quantityGrams = 50.0))

            assertIs<ActionOutcome.Rejected>(outcome)
            assertEquals(0, repository.writeCount)
        }

    @Test
    fun `a valid action is validated as valid`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor = executor(repository)

            val validation =
                executor.validate(
                    AssistantAction.LogFood(
                        productCode = "0001",
                        quantityGrams = 50.0,
                        mealType = MealType.LUNCH,
                    ),
                )

            assertEquals(ActionValidation.Valid, validation)
        }

    @Test
    fun `an existing entry is validated as valid`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val entryId = repository.addEntry(entry())
            val executor = executor(repository)

            assertEquals(ActionValidation.Valid, executor.validate(AssistantAction.RemoveEntry(entryId = entryId)))
            assertEquals(
                ActionValidation.Valid,
                executor.validate(AssistantAction.ChangeEntry(entryId = entryId, quantityGrams = 50.0)),
            )
        }

    @Test
    fun `a product that vanishes before the write is rejected and nothing is written`() =
        runTest {
            val repository = RecordingDiaryRepository()
            val executor =
                AssistantActionExecutor(
                    diaryRepository = repository,
                    productRepository = FakeProductSearchRepository(listOf(product()), vanishAfterFirstLookup = true),
                    savedProductRepository = FakeSavedProductRepository(),
                    clock = EpochClock { 100L },
                    dayBoundaryProvider = FakeDayBoundaryProvider(),
                )

            val outcome =
                executor.apply(
                    AssistantAction.LogFood(
                        productCode = "0001",
                        quantityGrams = 50.0,
                        mealType = MealType.LUNCH,
                    ),
                )

            assertIs<ActionOutcome.Rejected>(outcome)
            assertEquals(0, repository.writeCount)
        }

    @Test
    fun `an entry that vanishes before the write is rejected and nothing is written`() =
        runTest {
            val repository = RecordingDiaryRepository(vanishAfterFirstObservation = true)
            val entryId = repository.addEntry(entry())
            val executor = executor(repository)

            val outcome =
                executor.apply(
                    AssistantAction.ChangeEntry(
                        entryId = entryId,
                        quantityGrams = 50.0,
                    ),
                )

            assertIs<ActionOutcome.Rejected>(outcome)
            assertEquals(0, repository.updatedEntries.size)
            assertEquals(0, repository.removedIds.size)
        }

    private fun executor(
        repository: RecordingDiaryRepository,
        products: List<Product> = listOf(product()),
    ): AssistantActionExecutor =
        AssistantActionExecutor(
            diaryRepository = repository,
            productRepository = FakeProductSearchRepository(products),
            savedProductRepository = FakeSavedProductRepository(),
            clock = EpochClock { 100L },
            dayBoundaryProvider = FakeDayBoundaryProvider(),
        )

    private fun entry(
        quantityGrams: Double = 100.0,
        mealType: MealType = MealType.BREAKFAST,
    ): DiaryEntry =
        DiaryEntry(
            productCode = "0001",
            productName = "Oats",
            mealType = mealType,
            quantityGrams = quantityGrams,
            energyKcal = 380.0,
            proteinGrams = 10.0,
            carbohydrateGrams = 60.0,
            fatGrams = 7.0,
            loggedAtEpochMillis = 100L,
            dayEpochMillis = 0L,
        )

    private fun product(): Product =
        Product(
            code = "0001",
            name = "Oats",
            brands = "Brand",
            nutritionValues =
                listOf(
                    NutritionValue("energy", 380.0, "kcal", NutritionBasis.PER_100_G),
                    NutritionValue("protein", 10.0, "g", NutritionBasis.PER_100_G),
                ),
        )
}
