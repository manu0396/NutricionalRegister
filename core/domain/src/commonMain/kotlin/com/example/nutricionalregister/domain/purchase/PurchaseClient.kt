package com.example.nutricionalregister.domain.purchase

/**
 * The single abstraction the shared code uses to take a real in-app purchase.
 *
 * Platform payment APIs (Google Play Billing on Android, StoreKit 2 on iOS) are
 * hidden behind this project-owned contract so purchase state, double-recording
 * protection, reconciliation, and outcome modelling stay in common code and only
 * the payment sheet and the store interaction are platform-specific.
 *
 * Implementations must never collect payment credentials themselves: all payment
 * data entry happens in the platform's own sheet (FR-014).
 */
public interface PurchaseClient {
    /**
     * Returns availability plus the store's own price label for [code], or null
     * when the product is not offered by the store (FR-015).
     */
    public suspend fun queryProduct(code: String): PurchasableProduct?

    /**
     * Presents the platform's payment sheet for [code] and reports the outcome.
     * Never collects credentials itself (FR-014).
     */
    public suspend fun launchPurchase(code: String): PurchaseClientResult

    /**
     * Verified outstanding entitlement/known transactions, used to reconcile local
     * state against the store, which is the source of truth (FR-013).
     */
    public suspend fun currentEntitlements(): List<VerifiedTransaction>
}

/**
 * A product the store offers for purchase, with the store's own formatted price
 * label shown verbatim and never parsed into a project currency type.
 */
public data class PurchasableProduct(
    val code: String,
    val amountLabel: String,
) {
    init {
        require(code.isNotBlank()) { "Purchasable product code must not be blank" }
    }
}

/** The result of one purchase attempt against the platform store. */
public sealed interface PurchaseClientResult {
    /** The store verified the transaction. Only this result grants a purchase. */
    public data class Verified(
        val transaction: VerifiedTransaction,
    ) : PurchaseClientResult

    /** The user dismissed or cancelled the platform sheet. */
    public data object UserCancelled : PurchaseClientResult

    /** The store reported a failure; [reason] carries the mapped cause. */
    public data class Failed(
        val reason: PurchaseFailure,
    ) : PurchaseClientResult

    /** The store is signed out, unreachable, or missing its product configuration. */
    public data object Unavailable : PurchaseClientResult
}

/**
 * A platform-verified transaction.
 *
 * [transactionId] is store-facing metadata (a Play purchase token/order id or a
 * StoreKit `Transaction.id`) retained to acknowledge/finish the store transaction
 * and for future server-side verification; it is not the local uniqueness key.
 */
public data class VerifiedTransaction(
    val productCode: String,
    val transactionId: String,
    val purchaseToken: String?,
    val completedAtEpochMillis: Long,
) {
    init {
        require(productCode.isNotBlank()) { "Verified transaction product code must not be blank" }
        require(transactionId.isNotBlank()) { "Verified transaction id must not be blank" }
    }
}
