package com.example.nutricionalregister.feature.savedproducts.testing

import com.example.nutricionalregister.domain.purchase.PurchaseOutcome
import com.example.nutricionalregister.domain.purchase.PurchaseRecord
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.domain.purchase.PurchaseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * A fake [PurchaseRepository] whose purchase flow emits a scripted outcome, so the
 * purchase state machine can be tested without any store or platform code.
 */
internal class FakePurchaseRepository(
    private val outcome: PurchaseOutcome,
    initialRecord: PurchaseRecord? = null,
    private val entitlements: List<PurchaseRecord> = emptyList(),
) : PurchaseRepository {
    private val records = MutableStateFlow<Map<String, PurchaseRecord>>(emptyMap())

    init {
        if (initialRecord != null) records.value = mapOf(initialRecord.productCode to initialRecord)
    }

    override fun purchase(code: String): Flow<PurchaseOutcome> =
        flow {
            if (outcome is PurchaseOutcome.Success) {
                records.value = records.value + (code to outcome.record)
            }
            emit(outcome)
        }

    override fun observePurchase(code: String): Flow<PurchaseRecord?> = records.map { it[code] }

    override fun observePurchasedCodes(): Flow<Set<String>> = records.map { it.keys }

    override suspend fun reconcile() {
        if (entitlements.isNotEmpty()) {
            records.value = records.value + entitlements.associateBy { it.productCode }
        }
    }
}

/** A repository whose purchase flow never emits, so a pending attempt stays pending. */
internal class PendingPurchaseRepository : PurchaseRepository {
    override fun purchase(code: String): Flow<PurchaseOutcome> = flow { kotlinx.coroutines.awaitCancellation() }

    override fun observePurchase(code: String): Flow<PurchaseRecord?> = flowOf(null)

    override fun observePurchasedCodes(): Flow<Set<String>> = flowOf(emptySet())

    override suspend fun reconcile() = Unit
}

/** The state a successful outcome maps to, for assertions. */
internal fun purchasedState(record: PurchaseRecord): PurchaseState = PurchaseState.Purchased(record)
