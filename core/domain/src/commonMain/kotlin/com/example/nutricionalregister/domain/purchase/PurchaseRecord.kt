package com.example.nutricionalregister.domain.purchase

/**
 * The durable record that a product was successfully purchased (FR-010, FR-012).
 *
 * [productCode] is the unique identity because a product can only ever be bought
 * once; inserting a second record for the same product is an idempotent upsert and
 * never produces two purchase records (FR-012). [transactionId] is store-facing
 * metadata, not the local uniqueness key. No card, bank, or credential data is
 * ever stored here (FR-014).
 */
public data class PurchaseRecord(
    val productCode: String,
    val transactionId: String,
    val status: String,
    val purchaseToken: String? = null,
    val completedAtEpochMillis: Long,
) {
    init {
        require(productCode.isNotBlank()) { "Purchase record product code must not be blank" }
        require(transactionId.isNotBlank()) { "Purchase record transaction id must not be blank" }
        require(status.isNotBlank()) { "Purchase record status must not be blank" }
    }
}
