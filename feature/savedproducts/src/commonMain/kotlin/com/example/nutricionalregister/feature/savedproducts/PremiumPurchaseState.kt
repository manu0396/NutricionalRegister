package com.example.nutricionalregister.feature.savedproducts

import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import com.example.nutricionalregister.domain.purchase.PurchaseState

/**
 * The presentation state of the single premium unlock (ADR-0018, FR-017).
 *
 * The app sells exactly one unlock, so the state is not parameterised by a
 * product code: there is no per-food purchase anywhere (FR-032).
 *
 * [isUnlocked] is the entitlement the assistant is gated on. It is advisory on the
 * client: the authoritative gate is the server-side AI gateway, because a local
 * check can be bypassed by a modified app (ADR-0018).
 *
 * [isEligibleForPurchase] is false when the store does not offer the premium
 * product, in which case the surface must not offer a purchase action (FR-015).
 *
 * [amountLabel] is the store's own formatted price label. It is shown verbatim and
 * is never parsed into a number or into a project currency type.
 *
 * [isPending] is the blocking flag the surface uses to disable the purchase
 * control, so a second attempt cannot start while one is in flight (FR-018).
 *
 * [state] is the attempt's current [PurchaseState]. It carries the failure reasons
 * the surface turns into an explicit message instead of failing silently (FR-019).
 */
public data class PremiumPurchaseState(
    val isUnlocked: Boolean = false,
    val isEligibleForPurchase: Boolean = false,
    val amountLabel: String? = null,
    val isPending: Boolean = false,
    val state: PurchaseState = PurchaseState.Idle,
) {
    /**
     * True when the store could not be reached or is not configured, which the
     * surface renders as an explicit "payments unavailable" message with a retry
     * control rather than as a silent failure (FR-019).
     */
    public val requiresGuidance: Boolean
        get() = state is PurchaseState.Unavailable || (state as? PurchaseState.Failed)?.reason == PurchaseFailure.NOT_CONFIGURED
}
