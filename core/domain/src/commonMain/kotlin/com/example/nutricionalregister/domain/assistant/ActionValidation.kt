package com.example.nutricionalregister.domain.assistant

/**
 * The verdict on a proposed action, before anything is written.
 */
public sealed interface ActionValidation {
    /** The action may be applied. */
    public data object Valid : ActionValidation

    /**
     * The action may not be applied, and [reason] explains why in words the
     * user can read.
     */
    public data class Rejected(
        public val reason: String,
    ) : ActionValidation
}
