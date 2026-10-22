package com.example.nutricionalregister.domain.premium

import com.example.nutricionalregister.domain.purchase.PurchaseRecord

/**
 * The single premium product the store sells.
 *
 * The app sells exactly one unlock, and it unlocks the assistant, because the
 * assistant is the only feature with a per-use cost for the publisher
 * (ADR-0018, FR-017). There is deliberately no per-food purchase: a purchase
 * button on a bag of oats is not a credible commercial model (FR-032).
 *
 * The check is **advisory on the client**: the authoritative gate for the
 * assistant is the server-side AI gateway, because a local check can be
 * bypassed by a modified app. It is a convenience for the paying user, never
 * a security boundary (ADR-0018).
 */
public object PremiumProduct {
    /** The one product the store sells. It grants the assistant. */
    public const val CODE: String = "nutrilog_pro"

    /** Whether [records] contain a verified purchase of the premium product. */
    public fun isUnlocked(records: List<PurchaseRecord>): Boolean = records.any { it.productCode == CODE }
}
