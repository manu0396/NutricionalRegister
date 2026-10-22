package com.example.nutricionalregister.domain.assistant

/**
 * One turn of the conversation with the assistant.
 *
 * @property role who wrote the turn.
 * @property text what was said; never blank.
 */
public data class AssistantMessage(
    public val role: AssistantRole,
    public val text: String,
) {
    init {
        require(text.isNotBlank()) { "An assistant message must not be blank" }
    }
}
