package com.example.nutricionalregister.domain.assistant

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.NutritionGoal

/**
 * The minimal context the assistant is allowed to see.
 *
 * FR-028 forbids sending the product catalogue, the recent search terms or the
 * purchase records to the AI service, so the context holds exactly three
 * things: the entries of the day, the goal and the product codes of the
 * user's own library. Anything more would leak data the user never agreed to
 * share.
 *
 * @property entries the entries of the day the conversation is about.
 * @property goal the daily targets, so the assistant can talk about them.
 * @property libraryCodes the codes of the foods the user keeps.
 */
public data class AssistantContext(
    public val entries: List<DiaryEntry>,
    public val goal: NutritionGoal,
    public val libraryCodes: List<String>,
)
