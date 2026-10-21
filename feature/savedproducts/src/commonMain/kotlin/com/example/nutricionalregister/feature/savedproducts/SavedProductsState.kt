package com.example.nutricionalregister.feature.savedproducts

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.purchase.SavedProduct

/**
 * The Saved products destination's presentation state (FR-002).
 *
 * [saved] is the persisted list, most recently saved first. [isLoading] covers the
 * brief window before the first emission from storage, so the screen can avoid
 * flashing an empty state. [selectedProduct] drives the in-destination detail view
 * (FR-007). [savedCodes] is the set of codes currently saved, so any surface (the
 * detail view) can show the correct save/remove flag without an extra query.
 */
public data class SavedProductsState(
    val saved: List<SavedProduct> = emptyList(),
    val isLoading: Boolean = true,
    val selectedProduct: Product? = null,
    val savedCodes: Set<String> = emptySet(),
) {
    /** True when the list is loaded and genuinely empty (drives the empty state). */
    public val isEmpty: Boolean
        get() = !isLoading && saved.isEmpty()
}
