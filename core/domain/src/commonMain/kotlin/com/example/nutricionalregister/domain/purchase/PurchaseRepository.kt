package com.example.nutricionalregister.domain.purchase

import kotlinx.coroutines.flow.Flow

/**
 * Drives and records real in-app purchases (FR-010, FR-011, FR-012, FR-013).
 *
 * A record is written only from a platform-verified transaction; cancelled,
 * failed, or unverified results never create or update one (FR-011).
 */
public interface PurchaseRepository {
    /** Runs a purchase for [code] and emits its outcome (FR-009, FR-011). */
    public fun purchase(code: String): Flow<PurchaseOutcome>

    /** Emits the stored record for [code], or null when it has not been purchased. */
    public fun observePurchase(code: String): Flow<PurchaseRecord?>

    /** Emits the codes of every purchased product (FR-012). */
    public fun observePurchasedCodes(): Flow<Set<String>>

    /**
     * Reconciles local records against the store, which is the source of truth: a
     * product the store reports as owned is recorded, and one it does not report
     * stays unpaid (FR-013).
     */
    public suspend fun reconcile()
}
