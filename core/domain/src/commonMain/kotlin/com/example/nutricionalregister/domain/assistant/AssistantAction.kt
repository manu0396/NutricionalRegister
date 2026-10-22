package com.example.nutricionalregister.domain.assistant

import com.example.nutricionalregister.domain.diary.MealType

/**
 * A structured change the assistant proposes.
 *
 * An action is a **proposal**, never an applied change: it is validated
 * against the shared domain rules and applied only after the user confirms it
 * (FR-024). Free text that looks like an instruction is displayed and never
 * executed, which is why the assistant can only ever speak in these three
 * shapes.
 */
public sealed interface AssistantAction {
    /**
     * Log [quantityGrams] of the product [productCode] into [mealType].
     *
     * The values are not validated here on purpose: an invalid proposal has to
     * be *rejected with an explanation*, not crash the conversation, so the
     * rules live in the executor.
     */
    public data class LogFood(
        public val productCode: String,
        public val quantityGrams: Double,
        public val mealType: MealType,
    ) : AssistantAction

    /** Remove the diary entry with [entryId]. */
    public data class RemoveEntry(
        public val entryId: Long,
    ) : AssistantAction

    /**
     * Change the quantity and/or the meal of the diary entry with [entryId].
     *
     * A null [quantityGrams] or [mealType] leaves that part of the entry
     * untouched, because the assistant may propose only one of the two.
     */
    public data class ChangeEntry(
        public val entryId: Long,
        public val quantityGrams: Double? = null,
        public val mealType: MealType? = null,
    ) : AssistantAction
}
