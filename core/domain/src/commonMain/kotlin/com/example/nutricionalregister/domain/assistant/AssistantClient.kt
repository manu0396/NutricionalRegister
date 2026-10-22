package com.example.nutricionalregister.domain.assistant

/**
 * The conversational assistant, as the domain sees it.
 *
 * The client returns **proposals** and never applied changes: writing to the
 * diary is the executor's job and happens only after the user confirms
 * (FR-024).
 */
public interface AssistantClient {
    /** Answers the conversation [messages] in the given [context]. */
    public suspend fun complete(
        messages: List<AssistantMessage>,
        context: AssistantContext,
    ): AssistantReply
}
