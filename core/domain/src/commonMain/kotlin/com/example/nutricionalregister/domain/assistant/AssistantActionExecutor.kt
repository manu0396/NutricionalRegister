package com.example.nutricionalregister.domain.assistant

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.diary.NutritionCalculator
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.flow.first

/**
 * Applies the actions the assistant proposes.
 *
 * The assistant may only *propose*: this class is the single place where a
 * proposal becomes a diary change, and the shared domain rules are applied
 * first. A proposal that breaks a rule is rejected with an explanation and
 * the diary is left exactly as it was, because a hallucinated action must
 * never be able to damage the record of what the user actually ate (FR-024).
 *
 * The executor never talks to a network: the product it logs is resolved
 * through the local catalogue cache, so the assistant path stays offline for
 * everything except the conversation itself.
 */
public class AssistantActionExecutor(
    private val diaryRepository: DiaryRepository,
    private val productRepository: ProductSearchRepository,
    private val savedProductRepository: SavedProductRepository,
    private val clock: EpochClock,
    private val dayBoundaryProvider: DayBoundaryProvider,
) {
    /**
     * Checks [action] against the domain rules and against the catalogue.
     *
     * Nothing is written: this is the dry run the confirmation step uses, so
     * the user can be told that a proposal is impossible before anything
     * happens.
     */
    public suspend fun validate(action: AssistantAction): ActionValidation =
        when (action) {
            is AssistantAction.LogFood -> validateLogFood(action)
            is AssistantAction.RemoveEntry -> validateEntry(action.entryId)
            is AssistantAction.ChangeEntry -> validateChange(action)
        }

    /**
     * Applies [action] when it is valid, and rejects it otherwise.
     *
     * An invalid action writes nothing at all: the validation runs first and
     * a rejection returns before the diary is touched, so a rejected
     * proposal leaves the day exactly as the user left it.
     */
    public suspend fun apply(action: AssistantAction): ActionOutcome {
        val validation = validate(action)
        if (validation is ActionValidation.Rejected) {
            return ActionOutcome.Rejected(validation.reason)
        }
        return when (action) {
            is AssistantAction.LogFood -> applyLogFood(action)
            is AssistantAction.RemoveEntry -> applyRemoveEntry(action)
            is AssistantAction.ChangeEntry -> applyChangeEntry(action)
        }
    }

    private suspend fun validateLogFood(action: AssistantAction.LogFood): ActionValidation {
        if (!action.quantityGrams.isFinite() || action.quantityGrams <= 0.0) {
            return ActionValidation.Rejected(QUANTITY_REASON)
        }
        return if (productFor(action.productCode) == null) {
            ActionValidation.Rejected(unknownProductReason(action.productCode))
        } else {
            ActionValidation.Valid
        }
    }

    private suspend fun validateEntry(entryId: Long): ActionValidation {
        val entry = findEntry(entryId)
        return if (entry == null) {
            ActionValidation.Rejected(unknownEntryReason(entryId))
        } else {
            ActionValidation.Valid
        }
    }

    private suspend fun validateChange(action: AssistantAction.ChangeEntry): ActionValidation {
        val quantity = action.quantityGrams
        if (quantity != null && (!quantity.isFinite() || quantity <= 0.0)) {
            return ActionValidation.Rejected(QUANTITY_REASON)
        }
        return validateEntry(action.entryId)
    }

    private suspend fun applyLogFood(action: AssistantAction.LogFood): ActionOutcome {
        val product = productFor(action.productCode)
        if (product == null) {
            return ActionOutcome.Rejected(unknownProductReason(action.productCode))
        }
        val loggedAt = clock.nowEpochMillis()
        val entry =
            NutritionCalculator.entryFor(
                product = product,
                mealType = action.mealType,
                quantityGrams = action.quantityGrams,
                loggedAtEpochMillis = loggedAt,
                dayEpochMillis = dayBoundaryProvider.startOfDay(loggedAt),
            )
        val entryId = diaryRepository.addEntry(entry)
        return ActionOutcome.Applied(entryId)
    }

    private suspend fun applyRemoveEntry(action: AssistantAction.RemoveEntry): ActionOutcome {
        diaryRepository.removeEntry(action.entryId)
        return ActionOutcome.Applied(action.entryId)
    }

    private suspend fun applyChangeEntry(action: AssistantAction.ChangeEntry): ActionOutcome {
        val existing = findEntry(action.entryId)
        if (existing == null) {
            return ActionOutcome.Rejected(unknownEntryReason(action.entryId))
        }
        val quantityGrams = action.quantityGrams ?: existing.quantityGrams
        val mealType = action.mealType ?: existing.mealType
        val totals = existing.totals.scaledBy(quantityGrams / existing.quantityGrams)
        val updated =
            existing.copy(
                quantityGrams = quantityGrams,
                mealType = mealType,
                energyKcal = totals.energyKcal,
                proteinGrams = totals.proteinGrams,
                carbohydrateGrams = totals.carbohydrateGrams,
                fatGrams = totals.fatGrams,
            )
        diaryRepository.updateEntry(updated)
        return ActionOutcome.Applied(updated.id)
    }

    /**
     * The foods the user keeps, with the nutrition the catalogue reports for
     * each one.
     *
     * The context the assistant talks from is built here, because the executor
     * owns the rule of which food a proposal may name: a food of the user's
     * library, read from the catalogue when it can be. A food the catalogue
     * does not know is carried with no nutrition at all, and the assistant can
     * then speak of the food, but not of what is in it.
     */
    public suspend fun libraryFoods(): List<AssistantLibraryFood> =
        savedProductRepository
            .observeSaved()
            .first()
            .map { saved ->
                // The nutrition per 100 g, which is what a plan is computed
                // from, whether the catalogue declared it per 100 g or per
                // serving.
                val per100 = productFor(saved.code)?.let { NutritionCalculator.contribution(it, 100.0) }
                AssistantLibraryFood(
                    code = saved.code,
                    name = saved.name,
                    energyKcal = per100?.energyKcal,
                    proteinGrams = per100?.proteinGrams,
                    carbohydrateGrams = per100?.carbohydrateGrams,
                    fatGrams = per100?.fatGrams,
                )
            }

    /**
     * The food a proposal may name, or null when the app does not know it.
     *
     * The catalogue copy is read first, because it carries the nutrition; a
     * food the user keeps but the catalogue cache does not have is read from
     * the saved record, the same fallback the product detail uses. Without it
     * a proposal for a food the user keeps would always be rejected, and the
     * Coach would be unable to log the food it was given (FR-024).
     */
    private suspend fun productFor(code: String): Product? =
        productRepository.getProduct(code)
            ?: savedProductRepository
                .observeSaved()
                .first()
                .firstOrNull { saved -> saved.code == code }
                ?.toProduct()

    /**
     * The entry with [entryId] among the entries of the current day, or null
     * when there is no such entry.
     *
     * The lookup is scoped to the current day because the assistant talks
     * about the day the user is living in; the diary stores the day an entry
     * belongs to, so a past entry keeps its own day.
     */
    private suspend fun findEntry(entryId: Long): DiaryEntry? {
        val dayStart = dayBoundaryProvider.startOfDay(clock.nowEpochMillis())
        val entries = diaryRepository.observeEntriesForDay(dayStart).first()
        return entries.firstOrNull { entry -> entry.id == entryId }
    }

    private fun unknownProductReason(productCode: String): String = "The product $productCode is not in the catalogue"

    private fun unknownEntryReason(entryId: Long): String = "The diary entry $entryId does not exist"

    private companion object {
        /** The explanation used whenever a quantity breaks the shared rules. */
        const val QUANTITY_REASON: String = "The quantity must be a finite amount greater than zero"
    }
}
