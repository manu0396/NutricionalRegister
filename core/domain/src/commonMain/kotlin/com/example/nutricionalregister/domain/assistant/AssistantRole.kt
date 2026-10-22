package com.example.nutricionalregister.domain.assistant

/**
 * The author of one turn of the conversation with the assistant.
 *
 * The role is explicit rather than inferred from the position in the list,
 * because the conversation travels to the assistant service as data and the
 * service has to be able to tell who said what.
 */
public enum class AssistantRole {
    /** A turn the user wrote. */
    USER,

    /** A turn the assistant wrote. */
    ASSISTANT,
}
