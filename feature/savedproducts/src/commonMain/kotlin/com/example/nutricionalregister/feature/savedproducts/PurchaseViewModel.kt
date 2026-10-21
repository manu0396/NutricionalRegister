package com.example.nutricionalregister.feature.savedproducts

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
 * Drives the purchase flow for one product (FR-009 to FR-019).
 *
 * [observe] reconciles and loads the product's store offering + purchase state
 * without starting a payment. [startPurchase] runs the store flow only when the
 * product is eligible and no attempt is already `Pending`, so a second start while
 * pending is refused (FR-018). [restore] reconciles outstanding entitlements from
 * the store (FR-013), which is also the App Store "restore purchases" affordance.
 * The store is the source of truth: a product it reports as owned is shown as
 * purchased, and one it does not report stays unpaid (FR-013).
 */
public class PurchaseViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val scope: CoroutineScope,
    private val queryProduct: suspend (String) -> String?,
) {
    private val mutableState = MutableStateFlow(PurchaseSheetState())
    public val state: StateFlow<PurchaseSheetState> = mutableState.asStateFlow()

    private var purchaseJob: Job? = null

    /** Loads the product's eligibility and current purchase state; never charges. */
    public fun observe(code: String) {
        scope.launch {
            val record = purchaseRepository.observePurchase(code).first()
            val amountLabel = queryProduct(code)
            mutableState.value =
                PurchaseSheetState(
                    attempt = PurchaseAttempt(code, amountLabel = amountLabel, state = PurchaseState.Idle),
                    isEligibleForPurchase = amountLabel != null,
                    state =
                        if (record != null) {
                            PurchaseState.Purchased(record)
                        } else {
                            PurchaseState.Idle
                        },
                )
        }
    }

    /**
     * Starts a real purchase for [code] in a single action (SC-005).
     *
     * Refused while the product is ineligible (FR-015) or an attempt is already
     * pending (FR-018). Outcomes map to the shown state: a verified success records
     * and shows purchased; cancel and failure leave no record (SC-006).
     */
    public fun startPurchase(code: String) {
        val current = mutableState.value
        if (!current.isEligibleForPurchase || current.isPending) return
        purchaseJob?.cancel()
        mutableState.update { it.copy(state = PurchaseState.Pending) }
        purchaseJob =
            scope.launch {
                purchaseRepository.purchase(code).collect { outcome ->
                    val next = outcome.toState()
                    mutableState.update {
                        it.copy(
                            attempt = PurchaseAttempt(code, amountLabel = it.amountLabel, state = next),
                            state = next,
                        )
                    }
                }
            }
    }

    /** Restores/reconciles outstanding purchases from the store (FR-013, T052). */
    public fun restore() {
        scope.launch {
            purchaseRepository.reconcile()
            val code = mutableState.value.attempt?.productCode ?: return@launch
            val record = purchaseRepository.observePurchase(code).first()
            if (record != null) {
                mutableState.update { it.copy(state = PurchaseState.Purchased(record)) }
            }
        }
    }

    /** Stops any in-flight purchase collection; called on dispose. */
    public fun clear() {
        purchaseJob?.cancel()
        purchaseJob = null
    }

    /** Retry after a transient failure resets the state to idle (FR-019). */
    public fun retry() {
        mutableState.update { it.copy(state = PurchaseState.Idle) }
    }
}
