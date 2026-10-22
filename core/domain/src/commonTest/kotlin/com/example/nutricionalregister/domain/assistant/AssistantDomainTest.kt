package com.example.nutricionalregister.domain.assistant

import com.example.nutricionalregister.domain.assistant.AssistantLibraryFood
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AssistantDomainTest {
    @Test
    fun `an assistant message must not be blank`() {
        assertFailsWith<IllegalArgumentException> { AssistantMessage(AssistantRole.USER, "") }
        assertFailsWith<IllegalArgumentException> { AssistantMessage(AssistantRole.ASSISTANT, "   ") }
    }

    @Test
    fun `a message keeps its author and its text`() {
        val message = AssistantMessage(AssistantRole.USER, "I had two eggs")

        assertEquals(AssistantRole.USER, message.role)
        assertEquals("I had two eggs", message.text)
    }

    @Test
    fun `a reply carries the actions it proposes`() {
        val action = AssistantAction.LogFood(productCode = "0001", quantityGrams = 50.0, mealType = MealType.LUNCH)

        val reply = AssistantReply(text = "I logged your oats", actions = listOf(action))

        assertEquals(listOf(action), reply.actions)
    }

    @Test
    fun `a change without a new quantity or meal proposes nothing`() {
        val change = AssistantAction.ChangeEntry(entryId = 7L)

        assertNull(change.quantityGrams)
        assertNull(change.mealType)
    }

    @Test
    fun `the context carries only the entries the goal and the library codes`() {
        val context =
            AssistantContext(
                entries = emptyList(),
                goal = NutritionGoal(energyKcal = 2400),
                library = listOf(AssistantLibraryFood(code = "0001", name = "Oats")),
            )

        assertEquals(2400, context.goal.energyKcal)
        assertEquals("0001", context.library.first().code)
        assertEquals(0, context.entries.size)
    }

    @Test
    fun `a valid validation is the valid case`() {
        assertEquals(ActionValidation.Valid, ActionValidation.Valid)
    }

    @Test
    fun `a rejected validation carries its reason`() {
        val rejected = ActionValidation.Rejected(reason = "unknown product")

        assertEquals("unknown product", rejected.reason)
    }

    @Test
    fun `an applied outcome carries the entry identifier`() {
        assertEquals(ActionOutcome.Applied(entryId = 7L), ActionOutcome.Applied(entryId = 7L))
        assertNull(ActionOutcome.Applied(entryId = null).entryId)
    }

    @Test
    fun `a rejected outcome carries its reason`() {
        val rejected = ActionOutcome.Rejected(reason = "unknown product")

        assertEquals("unknown product", rejected.reason)
    }

    @Test
    fun `the assistant client is asked for a reply and returns a proposal`() =
        runTest {
            val client =
                object : AssistantClient {
                    override suspend fun complete(
                        messages: List<AssistantMessage>,
                        context: AssistantContext,
                    ): AssistantReply = AssistantReply(text = "I logged your oats", actions = emptyList())
                }

            val reply = client.complete(messages = emptyList(), context = emptyContext())

            assertEquals("I logged your oats", reply.text)
            assertEquals(0, reply.actions.size)
        }

    private fun emptyContext(): AssistantContext =
        AssistantContext(
            entries = emptyList(),
            goal = NutritionGoal(),
            library = emptyList(),
        )
}
