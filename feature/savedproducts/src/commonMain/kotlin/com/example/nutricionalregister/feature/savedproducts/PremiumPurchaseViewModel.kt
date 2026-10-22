package com.example.nutricionalregister.feature.savedproducts

import com.example.nutricionalregister.domain.premium.PremiumProduct
import com.example.nutricionalregister.domain.purchase.PurchaseOutcome
import com.example.nutricionalregister.domain.purchase.PurchaseRecord
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.domain.purchase.PurchaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the single premium unlock sold in the More destination (ADR-0018, FR-017,
 * FR-030).
 *
 * The view model is not parameterised by a product code: the one product the store
 * sells is [PremiumProduct.CODE], and there is no per-food purchase anywhere in
 * the app (FR-032).
 *
 * [refresh] loads the entitlement and the store's offering without ever charging.
 * [startPurchase] runs the store flow only when the premium product is eligible and
 * no attempt is already pending, so a second start while pending is refused
 * (FR-018). [restore] reconciles outstanding entitlements from the store (FR-013),
 * which is also the store's own "restore purchases" affordance. The store is the
 * source of truth: a product it reports as owned unlocks the feature, and one it
 * does not report stays locked (FR-013).
 *
 * @param purchaseRepository the durable record of the verified purchases.
 * @param scope the lifecycle scope that owns the collections.
 * @param queryProduct asks the store for the price label of a product code.
 */
public class PremiumPurchaseViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val scope: CoroutineScope,
    private val queryProduct: suspend (String) -> String?,
) {
    private val mutableState = MutableStateFlow(PremiumPurchaseState())
    public val state: StateFlow<PremiumPurchaseState> = mutableState.asStateFlow()

    private var purchaseJob: Job? = null

    /**
     * Loads the entitlement and the store's offering; never charges.
     *
     * The store's own price label is read as it is and reported verbatim, and the
     * premium product counts as eligible only when the store actually offers it
     * (FR-015). A pending attempt is left untouched, so a refresh can never let a
     * second attempt slip past the blocking flag (FR-018).
     */
    public fun refresh() {
        scope.launch {
            val record = purchaseRepository.observePurchase(PremiumProduct.CODE).first()
            val amountLabel = queryProduct(PremiumProduct.CODE)
            mutableState.update { current ->
                current.copy(
                    isUnlocked = isUnlockedBy(record),
                    isEligibleForPurchase = amountLabel != null,
                    amountLabel = amountLabel,
                    state = if (current.isPending) current.state else stateFor(record),
                )
            }
        }
    }

    /**
     * Starts the purchase of the single premium product.
     *
     * Refused while the product is ineligible (FR-015) or an attempt is already
     * pending (FR-018). Outcomes map to the shown state: a verified success unlocks
     * the feature, while a cancellation and a failure leave it locked and report
     * what happened (FR-011).
     */
    public fun startPurchase() {
        val current = mutableState.value
        if (!current.isEligibleForPurchase || current.isPending) return
        purchaseJob?.cancel()
        mutableState.update { it.copy(isPending = true, state = PurchaseState.Pending) }
        purchaseJob =
            scope.launch {
                purchaseRepository.purchase(PremiumProduct.CODE).collect { outcome ->
                    val next = outcome.toState()
                    val purchased = (next as? PurchaseState.Purchased)?.record
                    mutableState.update {
                        it.copy(
                            isUnlocked = it.isUnlocked || isUnlockedBy(purchased),
                            isPending = false,
                            state = next,
                        )
                    }
                }
            }
    }

    /**
     * Restores/reconciles outstanding purchases from the store (FR-013).
     *
     * The store is the source of truth: a transaction it reports as owned unlocks
     * the feature, and one it does not report leaves the feature locked.
     */
    public fun restore() {
        scope.launch {
            purchaseRepository.reconcile()
            val record = purchaseRepository.observePurchase(PremiumProduct.CODE).first()
            if (record != null) {
                mutableState.update {
                    it.copy(
                        isUnlocked = isUnlockedBy(record),
                        isPending = false,
                        state = PurchaseState.Purchased(record),
                    )
                }
            }
        }
    }

    /** Stops any in-flight purchase collection; called on dispose. */
    public fun clear() {
        purchaseJob?.cancel()
        purchaseJob = null
    }

    /** Whether [record] is a verified purchase of the one premium product. */
    private fun isUnlockedBy(record: PurchaseRecord?): Boolean = record != null && PremiumProduct.isUnlocked(listOf(record))

    /** The state a stored record maps to: owned, or nothing bought yet. */
    private fun stateFor(record: PurchaseRecord?): PurchaseState =
        if (record != null) PurchaseState.Purchased(record) else PurchaseState.Idle
}

/** Maps a terminal [PurchaseOutcome] to the [PurchaseState] the surface shows. */
private fun PurchaseOutcome.toState(): PurchaseState =
    when (this) {
        is PurchaseOutcome.Success -> PurchaseState.Purchased(record)
        PurchaseOutcome.Cancelled -> PurchaseState.Cancelled
        is PurchaseOutcome.Failed -> PurchaseState.Failed(reason)
        is PurchaseOutcome.Unavailable -> PurchaseState.Unavailable(reason)
    }
