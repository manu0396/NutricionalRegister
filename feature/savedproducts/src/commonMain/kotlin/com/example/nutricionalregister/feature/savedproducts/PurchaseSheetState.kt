package com.example.nutricionalregister.feature.savedproducts

import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import com.example.nutricionalregister.domain.purchase.PurchaseOutcome
import com.example.nutricionalregister.domain.purchase.PurchaseState

/**
 * Presents one product's purchase flow (FR-009 to FR-019).
 *
 * [attempt] is the in-memory `PurchaseAttempt` entity from `data-model.md`: the
 * product code, the store's own price label shown verbatim, and the current
 * [state]. [isEligibleForPurchase] is false when the store does not offer the
 * product, in which case the UI must not show a purchase action (FR-015).
 *
 * [isPending] is the blocking flag the UI uses to disable the purchase control so
 * a second attempt cannot start (FR-018). [requiresGuidance] is true for the
 * unavailable/not-configured states, which the UI renders as an explicit message
 * with a retry control rather than failing silently (FR-019).
 */
public data class PurchaseSheetState(
    val attempt: PurchaseAttempt? = null,
    val isEligibleForPurchase: Boolean = false,
    val state: PurchaseState = PurchaseState.Idle,
) {
    /** The store's price label for the current product, or null when unknown. */
    public val amountLabel: String?
        get() = attempt?.amountLabel

    /** True while an attempt is in flight; the purchase control must be disabled. */
    public val isPending: Boolean
        get() = state is PurchaseState.Pending

    /** True when the product has been purchased and confirmed. */
    public val isPurchased: Boolean
        get() = state is PurchaseState.Purchased

    /** True when the unavailable/not-configured guidance should be shown. */
    public val requiresGuidance: Boolean
        get() = state is PurchaseState.Unavailable || (state as? PurchaseState.Failed)?.reason == PurchaseFailure.NOT_CONFIGURED
}

/**
 * The in-memory entity from `data-model.md` describing one purchase attempt.
 *
 * @param productCode the product being purchased; the same identity used everywhere.
 * @param amountLabel the store's own formatted price label, shown verbatim and
 *   never parsed into a project currency type.
 * @param state the attempt's current [PurchaseState].
 */
public data class PurchaseAttempt(
    val productCode: String,
    val amountLabel: String?,
    val state: PurchaseState,
) {
    init {
        require(productCode.isNotBlank()) { "Purchase attempt product code must not be blank" }
    }
}

/** Maps a terminal [PurchaseOutcome] to the [PurchaseState] the UI shows. */
public fun PurchaseOutcome.toState(): PurchaseState =
    when (this) {
        is PurchaseOutcome.Success -> PurchaseState.Purchased(record)
        PurchaseOutcome.Cancelled -> PurchaseState.Cancelled
        is PurchaseOutcome.Failed -> PurchaseState.Failed(reason)
        is PurchaseOutcome.Unavailable -> PurchaseState.Unavailable(reason)
    }
