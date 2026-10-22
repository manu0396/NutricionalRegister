package com.example.nutricionalregister.feature.assistant

/**
 * Whether the assistant can be used, and why not when it cannot.
 *
 * The states are kept apart because the user's next action differs for each
 * of them: a build without an AI service cannot be repaired from the app, a
 * missing consent is granted with a single tap and an absent connection is
 * restored by the user (FR-025, FR-026, FR-027).
 */
public sealed interface AssistantAvailability {
    /** The assistant is configured, consented to and reachable. */
    public data object Ready : AssistantAvailability

    /** No AI service is configured for this build, so the assistant is switched off. */
    public data object NotConfigured : AssistantAvailability

    /** The device has no connection, so the assistant cannot be reached. */
    public data object Offline : AssistantAvailability

    /** The user has not yet consented to sending messages to the AI service. */
    public data object ConsentRequired : AssistantAvailability
}
