package com.example.nutricionalregister.domain.purchase

/**
 * Why a purchase attempt failed, mapped from each platform's error codes.
 *
 * `NOT_CONFIGURED` in particular must never be swallowed: it is what turns an
 * unconfigured store into the explicit "payments unavailable" UI state (FR-019).
 */
public enum class PurchaseFailure {
    USER_CANCELLED,
    DECLINED,
    NETWORK,
    NOT_CONFIGURED,
    VERIFICATION_FAILED,
    UNKNOWN,
}

/**
 * The presentation state of a single purchase attempt.
 *
 * `Pending` is a blocking state: while it is active the purchase control is
 * disabled so no second attempt can start (FR-018). `Unavailable` backs the
 * explicit "payments unavailable" state with retry (FR-019).
 */
public sealed interface PurchaseState {
    public data object Idle : PurchaseState

    public data object Pending : PurchaseState

    public data class Purchased(
        val record: PurchaseRecord,
    ) : PurchaseState

    public data object Cancelled : PurchaseState

    public data class Failed(
        val reason: PurchaseFailure,
    ) : PurchaseState

    public data class Unavailable(
        val reason: PurchaseFailure,
    ) : PurchaseState
}

/**
 * The terminal outcome of one purchase flow, consumed by the repository layer.
 *
 * Only [Success] carries a record; the other outcomes must never write one
 * (FR-011).
 */
public sealed interface PurchaseOutcome {
    public data class Success(
        val record: PurchaseRecord,
    ) : PurchaseOutcome

    public data object Cancelled : PurchaseOutcome

    public data class Failed(
        val reason: PurchaseFailure,
    ) : PurchaseOutcome

    public data class Unavailable(
        val reason: PurchaseFailure,
    ) : PurchaseOutcome
}
