package com.example.nutricionalregister.domain.purchase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PurchaseStateTest {
    @Test
    fun pendingIsADistinctBlockingState() {
        // GIVEN the in-progress purchase state
        val pending: PurchaseState = PurchaseState.Pending

        // THEN it is its own state and not equal to idle, so the UI can disable the
        // control while an attempt is in flight (FR-018)
        assertEquals(PurchaseState.Pending, pending)
        assertTrue(pending != PurchaseState.Idle)
    }

    @Test
    fun purchasedCarriesTheRecord() {
        // GIVEN a verified record
        val record = PurchaseRecord("0001", "tx", "PURCHASED", null, 1L)
        val state = PurchaseState.Purchased(record)

        // THEN the record travels with the state so the list can show it purchased
        assertEquals(record, state.record)
    }

    @Test
    fun failedAndUnavailableCarryTheirReason() {
        // GIVEN the failure reasons
        // WHEN they are presented as states
        val failed = PurchaseState.Failed(PurchaseFailure.DECLINED)
        val unavailable = PurchaseState.Unavailable(PurchaseFailure.NOT_CONFIGURED)

        // THEN each carries its reason so the UI can explain the outcome and offer retry
        assertEquals(PurchaseFailure.DECLINED, failed.reason)
        assertEquals(PurchaseFailure.NOT_CONFIGURED, unavailable.reason)
    }

    @Test
    fun successCarriesTheRecordAndTheOtherOutcomesDoNot() {
        // GIVEN a verified record
        val record = PurchaseRecord("0001", "tx", "PURCHASED", null, 1L)
        val success = PurchaseOutcome.Success(record)
        val failed = PurchaseOutcome.Failed(PurchaseFailure.NETWORK)
        val unavailable = PurchaseOutcome.Unavailable(PurchaseFailure.NOT_CONFIGURED)

        // THEN only success carries a record, so no other outcome can write one (FR-011)
        assertEquals(record, success.record)
        assertEquals(PurchaseOutcome.Cancelled, PurchaseOutcome.Cancelled)
        assertEquals(PurchaseFailure.NETWORK, failed.reason)
        assertEquals(PurchaseFailure.NOT_CONFIGURED, unavailable.reason)
    }

    @Test
    fun failureEnumerationCoversEveryPlatformMappedCause() {
        // GIVEN the platform-mapped failure causes
        // THEN every expected cause exists, including NOT_CONFIGURED which backs FR-019
        val names = PurchaseFailure.entries.map { it.name }
        assertTrue(
            names.containsAll(
                listOf(
                    "USER_CANCELLED",
                    "DECLINED",
                    "NETWORK",
                    "NOT_CONFIGURED",
                    "VERIFICATION_FAILED",
                    "UNKNOWN",
                ),
            ),
        )
    }

    @Test
    fun purchasableProductRetainsTheStorePriceLabel() {
        // GIVEN a store offering
        val product = PurchasableProduct(code = "0001", amountLabel = "EUR 1.99")

        // THEN the store's own label is retained verbatim and no currency parsing happens
        assertEquals("0001", product.code)
        assertEquals("EUR 1.99", product.amountLabel)

        // AND a blank code is rejected
        assertFailsWith<IllegalArgumentException>("blank purchasable code") {
            PurchasableProduct(code = " ", amountLabel = "EUR 1.99")
        }
    }

    @Test
    fun verifiedTransactionRetainsStoreMetadataAndRejectsBlankIdentity() {
        // GIVEN a platform-verified transaction
        val transaction = VerifiedTransaction("0001", "tx-1", "token-1", 1L)

        // THEN the store metadata is retained
        assertEquals("0001", transaction.productCode)
        assertEquals("tx-1", transaction.transactionId)
        assertEquals("token-1", transaction.purchaseToken)

        // AND the token is optional
        assertNull(transaction.copy(purchaseToken = null).purchaseToken)

        // AND a blank code or transaction id is rejected
        assertFailsWith<IllegalArgumentException>("blank code") {
            VerifiedTransaction(" ", "tx", null, 1L)
        }
        assertFailsWith<IllegalArgumentException>("blank transaction id") {
            VerifiedTransaction("0001", "", null, 1L)
        }
    }

    @Test
    fun verifiedClientResultIsTheOnlyResultCarryingATransaction() {
        // GIVEN a verified transaction
        val transaction = VerifiedTransaction("0001", "tx", null, 1L)
        val verified = PurchaseClientResult.Verified(transaction)
        val failed = PurchaseClientResult.Failed(PurchaseFailure.VERIFICATION_FAILED)

        // THEN only Verified carries the transaction that grants a purchase
        assertEquals(transaction, verified.transaction)
        assertEquals(PurchaseClientResult.UserCancelled, PurchaseClientResult.UserCancelled)
        assertEquals(PurchaseFailure.VERIFICATION_FAILED, failed.reason)
        assertEquals(PurchaseClientResult.Unavailable, PurchaseClientResult.Unavailable)
    }
}
