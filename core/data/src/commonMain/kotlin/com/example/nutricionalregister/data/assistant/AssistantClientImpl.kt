package com.example.nutricionalregister.data.assistant

import com.example.nutricionalregister.domain.assistant.AssistantClient
import com.example.nutricionalregister.domain.assistant.AssistantContext
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantReply

/**
 * The domain [AssistantClient] implemented on top of the AI gateway.
 *
 * The class exists so that the rest of the app never learns how the assistant
 * is reached: the presentation layer only knows the domain interface, and the
 * transport stays behind it (ADR-0016).
 *
 * @property gateway the client the conversation is sent through.
 */
public class AssistantClientImpl(
    private val gateway: AiGatewayClient,
) : AssistantClient {
    override suspend fun complete(
        messages: List<AssistantMessage>,
        context: AssistantContext,
    ): AssistantReply = gateway.complete(messages = messages, context = context)
}
