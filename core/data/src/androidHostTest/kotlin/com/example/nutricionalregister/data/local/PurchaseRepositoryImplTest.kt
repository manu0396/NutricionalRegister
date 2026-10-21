package com.example.nutricionalregister.data.local

import com.example.nutricionalregister.data.PURCHASED_STATUS
import com.example.nutricionalregister.data.PurchaseRepositoryImpl
import com.example.nutricionalregister.domain.purchase.PurchasableProduct
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.domain.purchase.PurchaseClientResult
import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import com.example.nutricionalregister.domain.purchase.PurchaseOutcome
import com.example.nutricionalregister.domain.purchase.VerifiedTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PurchaseRepositoryImplTest {
    @Test
    fun verifiedResultWritesExactlyOneRecordThatSurvivesReopen() =
        runTest {
            val firstDatabase = InMemoryProductDatabase.createPersistent()
            val file = requireNotNull(firstDatabase.file)
            val dispatcher = StandardTestDispatcher(testScheduler)
            try {
                val repository =
                    PurchaseRepositoryImpl(
                        database = firstDatabase.database,
                        client = FakePurchaseClient(verifiedOutcome = transaction("0001", "tx-1")),
                        databaseDispatcher = dispatcher,
                    )

                val outcomes = repository.purchase("0001").toList()
                assertEquals(1, outcomes.size)
                assertTrue(outcomes.single() is PurchaseOutcome.Success)
                assertEquals(setOf("0001"), repository.observePurchasedCodes().first())
                assertEquals("tx-1", repository.observePurchase("0001").first()?.transactionId)
                assertEquals(PURCHASED_STATUS, repository.observePurchase("0001").first()?.status)

                // the record survives a database reopen (SC-005)
                firstDatabase.close()
                val reopened = InMemoryProductDatabase.reopenPersistent(file)
                try {
                    val reopenedRepository =
                        PurchaseRepositoryImpl(
                            database = reopened.database,
                            client = FakePurchaseClient(),
                            databaseDispatcher = dispatcher,
                        )
                    assertEquals(setOf("0001"), reopenedRepository.observePurchasedCodes().first())
                } finally {
                    reopened.close()
                }
            } finally {
                firstDatabase.close()
                file.delete()
                SqliteFileCleanup.delete(file)
            }
        }

    @Test
    fun secondVerifiedConfirmationForTheSameProductUpdatesRatherThanDuplicates() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val dispatcher = StandardTestDispatcher(testScheduler)
                val repository =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = FakePurchaseClient(verifiedOutcome = transaction("0001", "tx-1")),
                        databaseDispatcher = dispatcher,
                    )

                repository.purchase("0001").toList()
                assertEquals(1, repository.observePurchasedCodes().first().size)

                // a second verified confirmation for the same product writes no second record
                val second =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = FakePurchaseClient(verifiedOutcome = transaction("0001", "tx-2")),
                        databaseDispatcher = dispatcher,
                    )
                second.purchase("0001").toList()

                val codes = second.observePurchasedCodes().first()
                assertEquals(setOf("0001"), codes)
                assertEquals("tx-2", second.observePurchase("0001").first()?.transactionId)
            }
        }

    @Test
    fun cancelledFailedAndUnverifiedResultsWriteNoRecord() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val dispatcher = StandardTestDispatcher(testScheduler)
                val cancelled =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = FakePurchaseClient(result = PurchaseClientResult.UserCancelled),
                        databaseDispatcher = dispatcher,
                    )
                assertEquals(PurchaseOutcome.Cancelled, cancelled.purchase("0001").toList().single())

                val failed =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = FakePurchaseClient(result = PurchaseClientResult.Failed(PurchaseFailure.DECLINED)),
                        databaseDispatcher = dispatcher,
                    )
                assertEquals(PurchaseOutcome.Failed(PurchaseFailure.DECLINED), failed.purchase("0001").toList().single())

                val unavailable =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = FakePurchaseClient(productAvailable = false),
                        databaseDispatcher = dispatcher,
                    )
                assertTrue(unavailable.purchase("0001").toList().single() is PurchaseOutcome.Unavailable)

                assertTrue(cancelled.observePurchasedCodes().first().isEmpty())
                assertNull(cancelled.observePurchase("0001").first())
            }
        }

    @Test
    fun reconcileRecordsAVerifiedOutstandingProductAndIsANoOpWhenNothingIsOutstanding() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val dispatcher = StandardTestDispatcher(testScheduler)
                val repository =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = FakePurchaseClient(entitlements = listOf(transaction("0007", "tx-7"))),
                        databaseDispatcher = dispatcher,
                    )

                repository.reconcile()
                assertEquals(setOf("0007"), repository.observePurchasedCodes().first())

                // nothing outstanding: reconcile is a no-op
                val noEntitlements =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = FakePurchaseClient(entitlements = emptyList()),
                        databaseDispatcher = dispatcher,
                    )
                noEntitlements.reconcile()
                assertEquals(setOf("0007"), noEntitlements.observePurchasedCodes().first())
            }
        }

    private fun transaction(
        code: String,
        id: String,
    ): VerifiedTransaction = VerifiedTransaction(code, id, purchaseToken = "token-$id", completedAtEpochMillis = 100L)
}

private class FakePurchaseClient(
    private val productAvailable: Boolean = true,
    private val result: PurchaseClientResult? = null,
    private val verifiedOutcome: VerifiedTransaction? = null,
    private val entitlements: List<VerifiedTransaction> = emptyList(),
) : PurchaseClient {
    override suspend fun queryProduct(code: String): PurchasableProduct? =
        if (productAvailable) PurchasableProduct(code, "EUR 1.99") else null

    override suspend fun launchPurchase(code: String): PurchaseClientResult =
        result ?: verifiedOutcome?.let { PurchaseClientResult.Verified(it) } ?: PurchaseClientResult.UserCancelled

    override suspend fun currentEntitlements(): List<VerifiedTransaction> = entitlements
}

private object SqliteFileCleanup {
    fun delete(file: java.io.File) {
        java.io.File("${file.path}-journal").delete()
        java.io.File("${file.path}-shm").delete()
        java.io.File("${file.path}-wal").delete()
    }
}
