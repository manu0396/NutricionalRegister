package com.example.nutricionalregister.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.domain.purchase.PurchaseClientResult
import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import com.example.nutricionalregister.domain.purchase.PurchaseOutcome
import com.example.nutricionalregister.domain.purchase.PurchaseRecord
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.domain.purchase.VerifiedTransaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Real in-app purchase driver over a platform [PurchaseClient] plus the on-device
 * `PurchaseRecord` table (FR-009 to FR-013).
 *
 * A record is written only from a platform-verified transaction; cancelled,
 * failed, or unverified results never create one (FR-011). Recording is keyed on
 * `product_code`, so a repeated confirmation for the same product is an idempotent
 * upsert and can never produce two purchase records (FR-012). [reconcile] re-queries
 * the store, which is the source of truth, and records any verified entitlement not
 * yet stored (FR-013).
 */
public class PurchaseRepositoryImpl(
    private val database: ProductDatabase,
    private val client: PurchaseClient,
    private val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PurchaseRepository {
    override fun purchase(code: String): Flow<PurchaseOutcome> =
        flow {
            require(code.isNotBlank()) { "Purchase code must not be blank" }
            val purchasable = client.queryProduct(code)
            if (purchasable == null) {
                emit(PurchaseOutcome.Unavailable(PurchaseFailure.NOT_CONFIGURED))
                return@flow
            }
            when (val outcome = client.launchPurchase(purchasable.code)) {
                is PurchaseClientResult.Verified -> {
                    val record = outcome.transaction.toRecord()
                    store(record)
                    emit(PurchaseOutcome.Success(record))
                }
                PurchaseClientResult.UserCancelled ->
                    emit(PurchaseOutcome.Cancelled)
                is PurchaseClientResult.Failed ->
                    emit(PurchaseOutcome.Failed(outcome.reason))
                PurchaseClientResult.Unavailable ->
                    emit(PurchaseOutcome.Unavailable(PurchaseFailure.NOT_CONFIGURED))
            }
        }

    override fun observePurchase(code: String): Flow<PurchaseRecord?> =
        database.savedProductQueries
            .selectPurchaseRecordByProduct(code)
            .asFlow()
            .mapToOneOrNull(databaseDispatcher)
            .map { row -> row?.toPurchaseRecord() }

    override fun observePurchasedCodes(): Flow<Set<String>> =
        database.savedProductQueries
            .selectPurchasedProductCodes()
            .asFlow()
            .mapToList(databaseDispatcher)
            .map { codes -> codes.toSet() }

    override suspend fun reconcile() {
        val entitlements = client.currentEntitlements()
        withContext(databaseDispatcher) {
            entitlements.forEach { transaction -> storeBlocking(transaction.toRecord()) }
        }
    }

    private suspend fun store(record: PurchaseRecord) {
        withContext(databaseDispatcher) { storeBlocking(record) }
    }

    private fun storeBlocking(record: PurchaseRecord) {
        database.savedProductQueries.upsertPurchaseRecord(
            product_code = record.productCode,
            transaction_id = record.transactionId,
            status = record.status,
            purchase_token = record.purchaseToken,
            completed_at_epoch_millis = record.completedAtEpochMillis,
        )
    }
}

private fun VerifiedTransaction.toRecord(): PurchaseRecord =
    PurchaseRecord(
        productCode = productCode,
        transactionId = transactionId,
        status = PURCHASED_STATUS,
        purchaseToken = purchaseToken,
        completedAtEpochMillis = completedAtEpochMillis,
    )

private fun com.example.nutricionalregister.data.PurchaseRecord.toPurchaseRecord(): PurchaseRecord =
    PurchaseRecord(
        productCode = product_code,
        transactionId = transaction_id,
        status = status,
        purchaseToken = purchase_token,
        completedAtEpochMillis = completed_at_epoch_millis,
    )

/** The persisted status of a verified purchase; the only value ever written. */
public const val PURCHASED_STATUS: String = "PURCHASED"
