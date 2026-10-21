package com.example.nutricionalregister.domain.purchase

import com.example.nutricionalregister.domain.product.Product

/**
 * A product the user chose to keep, stored on-device so it survives restart and is
 * available offline (FR-001, FR-002, FR-005).
 *
 * [code] is the sole identity, matching the catalogue `Product` code, so saving
 * the same product twice is an upsert rather than a duplicate (FR-004). Every
 * other field is a display-only copy so the saved list renders fully offline with
 * no network fetch at read time.
 */
public data class SavedProduct(
    val code: String,
    val name: String,
    val brands: String? = null,
    val quantity: String? = null,
    val nutriScoreGrade: String? = null,
    val imageThumbnailUrl: String? = null,
    val savedAtEpochMillis: Long,
) {
    init {
        require(code.isNotBlank()) { "Saved product code must not be blank" }
        require(name.isNotBlank()) { "Saved product name must not be blank" }
    }

    public companion object {
        /** Copies the display fields of [product] into a saved entry (data-model.md). */
        public fun fromProduct(
            product: Product,
            savedAtEpochMillis: Long,
        ): SavedProduct =
            SavedProduct(
                code = product.code,
                name = product.name,
                brands = product.brands,
                quantity = product.quantity,
                nutriScoreGrade = product.nutriScoreGrade,
                imageThumbnailUrl = product.imageThumbnailUrl,
                savedAtEpochMillis = savedAtEpochMillis,
            )
    }
}
