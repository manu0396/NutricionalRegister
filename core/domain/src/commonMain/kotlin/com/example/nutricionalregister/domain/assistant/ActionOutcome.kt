package com.example.nutricionalregister.domain.assistant

/**
 * The result of applying a proposed action.
 */
public sealed interface ActionOutcome {
    /**
     * The action was applied.
     *
     * [entryId] is the identifier of the affected entry, or null when the
     * action did not produce one.
     */
    public data class Applied(
        public val entryId: Long?,
    ) : ActionOutcome

    /** The action was rejected, and [reason] explains why. */
    public data class Rejected(
        public val reason: String,
    ) : ActionOutcome
}
