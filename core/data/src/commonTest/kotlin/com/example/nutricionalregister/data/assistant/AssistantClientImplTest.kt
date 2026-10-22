package com.example.nutricionalregister.data.assistant

import com.example.nutricionalregister.domain.assistant.AssistantContext
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantRole
import com.example.nutricionalregister.domain.diary.NutritionGoal
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantClientImplTest {
    @Test
    fun `the assistant client answers through the gateway`() =
        runTest {
            val httpClient = aiGatewayTestClient { gatewayResponse("""{"reply":"Have a nice day","actions":[]}""") }
            val client = AssistantClientImpl(AiGatewayClient(httpClient, "https://gateway.example"))

            val reply =
                client.complete(
                    messages = listOf(AssistantMessage(AssistantRole.USER, "Hello")),
                    context = context,
                )

            assertEquals("Have a nice day", reply.text)
            assertTrue(reply.actions.isEmpty())
        }
}

private val context: AssistantContext =
    AssistantContext(
        entries = emptyList(),
        goal = NutritionGoal(),
        libraryCodes = listOf("00042"),
    )
