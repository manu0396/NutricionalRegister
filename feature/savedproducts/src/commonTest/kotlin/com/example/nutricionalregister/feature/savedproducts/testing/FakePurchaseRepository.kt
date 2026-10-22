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
 *
 * The fake records what it was asked for: [purchaseCalls] holds the product codes
 * [purchase] was called with, in order, and [reconcileCalls] counts the
 * reconciliations. That makes "the view model did not start a purchase" an
 * assertion rather than an assumption.
 *
 * [scriptedOutcomes] is consumed one entry per purchase, in order, and the single
 * [outcome] is used for every purchase once the script runs out. It is how a
 * failure that is retried successfully is expressed.
 */
internal class FakePurchaseRepository(
    private val outcome: PurchaseOutcome,
    initialRecord: PurchaseRecord? = null,
    private val entitlements: List<PurchaseRecord> = emptyList(),
    private val scriptedOutcomes: List<PurchaseOutcome> = emptyList(),
) : PurchaseRepository {
    private val records = MutableStateFlow<Map<String, PurchaseRecord>>(emptyMap())
    private val calls = mutableListOf<String>()
    private var scriptIndex = 0

    /** The product codes [purchase] was called with, in order. */
    val purchaseCalls: List<String> get() = calls.toList()

    /** How many times [reconcile] was called. */
    var reconcileCalls: Int = 0
        private set

    init {
        if (initialRecord != null) records.value = mapOf(initialRecord.productCode to initialRecord)
    }

    override fun purchase(code: String): Flow<PurchaseOutcome> =
        flow {
            calls += code
            val next = scriptedOutcomes.getOrNull(scriptIndex) ?: outcome
            if (scriptedOutcomes.isNotEmpty()) scriptIndex++
            if (next is PurchaseOutcome.Success) {
                records.value = records.value + (code to next.record)
            }
            emit(next)
        }

    override fun observePurchase(code: String): Flow<PurchaseRecord?> = records.map { it[code] }

    override fun observePurchasedCodes(): Flow<Set<String>> = records.map { it.keys }

    override suspend fun reconcile() {
        reconcileCalls++
        if (entitlements.isNotEmpty()) {
            records.value = records.value + entitlements.associateBy { it.productCode }
        }
    }
}

/** A repository whose purchase flow never emits, so a pending attempt stays pending. */
internal class PendingPurchaseRepository : PurchaseRepository {
    private val calls = mutableListOf<String>()

    /** The product codes [purchase] was called with, in order. */
    val purchaseCalls: List<String> get() = calls.toList()

    override fun purchase(code: String): Flow<PurchaseOutcome> =
        flow {
            calls += code
            kotlinx.coroutines.awaitCancellation()
        }

    override fun observePurchase(code: String): Flow<PurchaseRecord?> = flowOf(null)

    override fun observePurchasedCodes(): Flow<Set<String>> = flowOf(emptySet())

    override suspend fun reconcile() = Unit
}

/** The state a successful outcome maps to, for assertions. */
internal fun purchasedState(record: PurchaseRecord): PurchaseState = PurchaseState.Purchased(record)
