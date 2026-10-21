package com.example.nutricionalregister

import com.example.nutricionalregister.data.ProductSearchRuntime
import com.example.nutricionalregister.data.createProductSearchRuntime
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.purchase.PurchasableProduct
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.domain.purchase.PurchaseOutcome
import com.example.nutricionalregister.domain.purchase.PurchaseRecord
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.domain.purchase.PurchaseState
import com.example.nutricionalregister.domain.purchase.SavedProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS-facing bridge over the shared runtime.
 *
 * The shared contracts are Kotlin interfaces and `Flow` streams, which Swift can
 * consume only awkwardly (a `Flow` needs a Swift `SharedFlowCollector`
 * implementation). This bridge keeps the Kotlin side authoritative and exposes
 * plain closure-based subscriptions plus imperative actions, so the SwiftUI
 * purchase and saved-products surfaces observe the shared state without
 * re-implementing any of it (FR-002, FR-009, FR-018, FR-019).
 *
 * This source set contains **no StoreKit call**: the Swift
 * `StoreKitPurchaseClient` is injected as the shared `PurchaseClient` by
 * [createIosNutriLogBridge] (ADR-0013).
 */
public class IosNutriLogBridge internal constructor(
    private val runtime: ProductSearchRuntime,
    private val purchaseClient: PurchaseClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val savedProductRepository = runtime.savedProductRepository
    private val purchaseRepository: PurchaseRepository = runtime.purchaseRepository

    private val jobs = mutableMapOf<Long, Job>()
    private var nextSubscriptionId: Long = 0L

    /**
     * Reports the store's price label for [code], or null when the store does not
     * offer it, so the UI shows a purchase action only for eligible products
     * (FR-015).
     */
    public fun queryProduct(
        code: String,
        onChange: (PurchasableProduct?) -> Unit,
    ) {
        scope.launch { onChange(purchaseClient.queryProduct(code)) }
    }

    /** Emits the saved products, most recently saved first, until cancelled (FR-002). */
    public fun observeSavedProducts(onChange: (List<SavedProduct>) -> Unit): Long =
        subscribe(savedProductRepository.observeSaved()) { saved -> onChange(saved) }

    /** Emits the stored record for [code], or null, until cancelled (FR-010). */
    public fun observePurchase(
        code: String,
        onChange: (PurchaseRecord?) -> Unit,
    ): Long = subscribe(purchaseRepository.observePurchase(code)) { record -> onChange(record) }

    /** Emits the purchased product codes as they change (FR-012). */
    public fun observePurchasedCodes(onChange: (List<String>) -> Unit): Long =
        subscribe(purchaseRepository.observePurchasedCodes()) { codes -> onChange(codes.toList()) }

    /** Removes the saved product with [code] (FR-003). */
    public fun removeSavedProduct(code: String) {
        scope.launch { savedProductRepository.remove(code) }
    }

    /**
     * Runs a purchase for [code] and reports terminal outcomes (FR-009, FR-011).
     *
     * The caller renders `Pending` for the duration of the call and disables the
     * purchase control while it is in flight (FR-018).
     */
    public fun purchase(
        code: String,
        onChange: (PurchaseState) -> Unit,
    ): Long = subscribe(purchaseRepository.purchase(code)) { outcome -> onChange(outcome.toState()) }

    /**
     * Reconciles local records against the store, which is the source of truth,
     * then reports the resulting purchased codes (FR-013, restore affordance).
     */
    public fun reconcile(onComplete: (List<String>) -> Unit) {
        scope.launch {
            purchaseRepository.reconcile()
            val codes = purchaseRepository.observePurchasedCodes().first()
            onComplete(codes.toList())
        }
    }

    /** Cancels the subscription with [subscriptionId], if it is still active. */
    public fun cancelSubscription(subscriptionId: Long) {
        jobs.remove(subscriptionId)?.cancel()
    }

    /** Releases every subscription and the underlying runtime. */
    public fun close() {
        scope.cancel()
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        runtime.close()
    }

    private fun <T> subscribe(
        flow: Flow<T>,
        onEach: (T) -> Unit,
    ): Long {
        val subscriptionId = ++nextSubscriptionId
        jobs[subscriptionId] =
            scope.launch {
                flow.collect { value -> onEach(value) }
            }
        return subscriptionId
    }
}

/** Maps a terminal purchase outcome to the presentation state (data-model.md). */
private fun PurchaseOutcome.toState(): PurchaseState =
    when (this) {
        is PurchaseOutcome.Success -> PurchaseState.Purchased(record)
        PurchaseOutcome.Cancelled -> PurchaseState.Cancelled
        is PurchaseOutcome.Failed -> PurchaseState.Failed(reason)
        is PurchaseOutcome.Unavailable -> PurchaseState.Unavailable(reason)
    }

/** Builds the iOS bridge over the shared runtime with a Swift-supplied client. */
public fun createIosNutriLogBridge(purchaseClient: PurchaseClient): IosNutriLogBridge =
    IosNutriLogBridge(
        runtime =
            createProductSearchRuntime(
                clock = EpochClock { (NSDate().timeIntervalSince1970 * 1000.0).toLong() },
                purchaseClient = purchaseClient,
            ),
        purchaseClient = purchaseClient,
    )
