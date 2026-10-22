package com.example.nutricionalregister.domain.premium

import com.example.nutricionalregister.domain.purchase.PurchaseRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PremiumProductTest {
    @Test
    fun `a purchase of the premium code unlocks the assistant`() {
        assertTrue(PremiumProduct.isUnlocked(listOf(record(PremiumProduct.CODE))))
    }

    @Test
    fun `a purchase of any other code does not unlock the assistant`() {
        assertFalse(PremiumProduct.isUnlocked(listOf(record("oats_1kg"))))
    }

    @Test
    fun `no purchase at all does not unlock the assistant`() {
        assertFalse(PremiumProduct.isUnlocked(emptyList()))
    }

    @Test
    fun `the premium code is the single product the store sells`() {
        assertEquals("nutrilog_pro", PremiumProduct.CODE)
    }

    @Test
    fun `the premium purchase unlocks the assistant among other purchases`() {
        val records = listOf(record("oats_1kg"), record(PremiumProduct.CODE))

        assertTrue(PremiumProduct.isUnlocked(records))
    }

    private fun record(productCode: String): PurchaseRecord =
        PurchaseRecord(
            productCode = productCode,
            transactionId = "transaction-1",
            status = "COMPLETED",
            completedAtEpochMillis = 1_000L,
        )
}
