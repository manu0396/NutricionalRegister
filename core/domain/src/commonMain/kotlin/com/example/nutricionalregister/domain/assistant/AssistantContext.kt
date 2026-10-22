package com.example.nutricionalregister.domain.assistant

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.NutritionGoal

/**
 * The minimal context the assistant is allowed to see.
 *
 * FR-028 forbids sending the product catalogue, the recent search terms or the
 * purchase records to the AI service, so the context holds exactly three
 * things: the entries of the day, the goal and the foods of the user's own
 * library, each with the nutrition the catalogue reports for it, so the
 * assistant can plan a day without being told what anything else contains.
 * Anything more would leak data the user never agreed to share.
 *
 * @property entries the entries of the day the conversation is about.
 * @property goal the daily targets, so the assistant can talk about them.
 * @property library the foods the user keeps, with what each one contains.
 */
public data class AssistantContext(
    public val entries: List<DiaryEntry>,
    public val goal: NutritionGoal,
    public val library: List<AssistantLibraryFood> = emptyList(),
)

/**
 * One food of the user's library, as the assistant sees it.
 *
 * A proposal may only name a food the user actually keeps, and a plan for the
 * day needs to know what a food contains, so the library is carried with the
 * nutrition the catalogue reports for it: per 100 g, like the product detail
 * shows. A value the catalogue does not report for the food is absent, so the
 * assistant can only speak of the amounts it can see, never of ones it
 * invents (FR-028).
 *
 * @property code the catalogue identity of the food.
 * @property name the food's display name, so a plan can say what it proposes.
 * @property energyKcal the calories per 100 g, or null when not reported.
 * @property proteinGrams the protein per 100 g, or null when not reported.
 * @property carbohydrateGrams the carbohydrate per 100 g, or null.
 * @property fatGrams the fat per 100 g, or null.
 */
public data class AssistantLibraryFood(
    public val code: String,
    public val name: String,
    public val energyKcal: Double? = null,
    public val proteinGrams: Double? = null,
    public val carbohydrateGrams: Double? = null,
    public val fatGrams: Double? = null,
) {
    init {
        require(code.isNotBlank()) { "A library food's code must not be blank" }
        require(name.isNotBlank()) { "A library food's name must not be blank" }
    }
}
