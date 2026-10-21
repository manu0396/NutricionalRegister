package com.example.nutricionalregister.savedproducts

import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.feature.savedproducts.PurchaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Composition root for the purchase presentation (mirrors
 * `SavedProductsDependencies`).
 *
 * The [queryProduct] lambda reads the store's offering through the shared
 * [PurchaseClient], so the price label shown is the store's own and eligibility is
 * decided by the store (FR-015). On construction it reconciles the platform store
 * once (FR-013): the store is the source of truth, so a product it reports as owned
 * is persisted and shown as purchased, and one it does not report stays unpaid. The
 * scope owns the in-flight purchase collection and the reconciliation.
 */
public class PurchaseDependencies(
    private val purchaseRepository: PurchaseRepository,
    purchaseClient: PurchaseClient,
    private val scope: CoroutineScope,
) : AutoCloseable {
    public val viewModel: PurchaseViewModel =
        PurchaseViewModel(
            purchaseRepository = purchaseRepository,
            scope = scope,
            queryProduct = { code -> purchaseClient.queryProduct(code)?.amountLabel },
        )

    init {
        // Reconcile with the store on launch so previously completed (and restored)
        // purchases appear as purchased without the user re-buying (FR-013, T048).
        scope.launch { purchaseRepository.reconcile() }
    }

    override fun close() {
        viewModel.clear()
    }
}
