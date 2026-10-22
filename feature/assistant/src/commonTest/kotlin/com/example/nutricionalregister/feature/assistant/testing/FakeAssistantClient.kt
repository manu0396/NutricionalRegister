package com.example.nutricionalregister.feature.assistant.testing

import com.example.nutricionalregister.domain.assistant.AssistantClient
import com.example.nutricionalregister.domain.assistant.AssistantContext
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantReply

/**
 * An [AssistantClient] that answers with a canned reply.
 *
 * The fake records every conversation it was asked to answer, so the tests can
 * prove what left the device: the messages of the conversation and the
 * context that was built with them.
 *
 * [failure] makes the request fail, which reproduces the transport failure the
 * gateway reports.
 */
internal class FakeAssistantClient(
    private val reply: AssistantReply = AssistantReply(text = "Understood", actions = emptyList()),
    private val failure: Throwable? = null,
) : AssistantClient {
    /** The conversations the client was asked to answer, in order. */
    val requests: MutableList<Request> = mutableListOf()

    /** How many times the client was asked to answer. */
    val requestCount: Int
        get() = requests.size

    override suspend fun complete(
        messages: List<AssistantMessage>,
        context: AssistantContext,
    ): AssistantReply {
        requests += Request(messages = messages, context = context)
        failure?.let { error -> throw error }
        return reply
    }

    /** One question the client was asked. */
    internal data class Request(
        val messages: List<AssistantMessage>,
        val context: AssistantContext,
    )
}
