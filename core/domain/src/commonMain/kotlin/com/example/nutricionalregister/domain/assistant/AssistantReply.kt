package com.example.nutricionalregister.domain.assistant

/**
 * The assistant's answer to one turn of the conversation.
 *
 * @property text the answer shown to the user.
 * @property actions the changes the assistant proposes; not one of them is
 *   applied until the user confirms.
 */
public data class AssistantReply(
    public val text: String,
    public val actions: List<AssistantAction>,
)
