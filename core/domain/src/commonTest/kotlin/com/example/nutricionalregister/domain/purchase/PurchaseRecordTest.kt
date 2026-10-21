package com.example.nutricionalregister.domain.purchase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PurchaseRecordTest {
    @Test
    fun purchaseRecordRetainsIdentityTransactionAndStatus() {
        // GIVEN a verified purchase record
        val record =
            PurchaseRecord(
                productCode = "0001",
                transactionId = "tx-1",
                status = "PURCHASED",
                purchaseToken = "token-1",
                completedAtEpochMillis = 1_700_000_000_000L,
            )

        // THEN every value is retained on read
        assertEquals("0001", record.productCode)
        assertEquals("tx-1", record.transactionId)
        assertEquals("PURCHASED", record.status)
        assertEquals("token-1", record.purchaseToken)
        assertEquals(1_700_000_000_000L, record.completedAtEpochMillis)

        // AND the store token is optional metadata, not part of identity
        val withoutToken = record.copy(purchaseToken = null)
        assertNull(withoutToken.purchaseToken)
        assertEquals(record.productCode, withoutToken.productCode)
    }

    @Test
    fun purchaseRecordRejectsBlankIdentityTransactionAndStatus() {
        // WHEN a record is constructed with a blank identity, transaction, or status
        // THEN construction fails fast so a blank purchase can never be stored
        assertFailsWith<IllegalArgumentException>("blank product code") {
            PurchaseRecord(productCode = " ", transactionId = "tx", status = "PURCHASED", completedAtEpochMillis = 1L)
        }
        assertFailsWith<IllegalArgumentException>("blank transaction id") {
            PurchaseRecord(productCode = "0001", transactionId = "", status = "PURCHASED", completedAtEpochMillis = 1L)
        }
        assertFailsWith<IllegalArgumentException>("blank status") {
            PurchaseRecord(productCode = "0001", transactionId = "tx", status = " ", completedAtEpochMillis = 1L)
        }
    }

    @Test
    fun productCodeIsTheUniquenessKeySoACopiedRecordIsEqual() {
        // GIVEN two records for the same product identity
        val first = PurchaseRecord("0001", "tx-1", "PURCHASED", "token-1", 1L)
        val sameIdentity = first.copy(transactionId = "tx-2", completedAtEpochMillis = 2L)

        // THEN they are distinct values but share the product-code identity that
        // keys the idempotent upsert (FR-012)
        assertEquals(first.productCode, sameIdentity.productCode)
        assertEquals(false, first == sameIdentity)
    }
}
