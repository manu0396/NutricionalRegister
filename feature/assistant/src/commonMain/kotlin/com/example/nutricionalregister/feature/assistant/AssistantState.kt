package com.example.nutricionalregister.feature.assistant

import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantReply

/**
 * The state of the Coach screen.
 *
 * The state is a single immutable value, so the screen renders one consistent
 * snapshot instead of piecing several streams together.
 *
 * @property availability whether the assistant can be used, and why not when
 *   it cannot.
 * @property messages the conversation so far, the user's turns and the
 *   assistant's answers.
 * @property pendingProposal the change the assistant proposed and the user has
 *   not confirmed yet; nothing is written to the diary while it is pending.
 * @property isSending whether a request is in flight.
 * @property error the reason of the last failure, or null when there is none.
 */
public data class AssistantState(
    public val availability: AssistantAvailability = AssistantAvailability.NotConfigured,
    public val messages: List<AssistantMessage> = emptyList(),
    public val pendingProposal: AssistantReply? = null,
    public val isSending: Boolean = false,
    public val error: String? = null,
)
