package com.example.nutricionalregister.feature.scan

import com.example.nutricionalregister.domain.product.Product

/**
 * The outcome of resolving a scanned barcode against the catalogue.
 *
 * The outcomes are kept apart because the user's next action differs for each
 * of them: a resolved code offers the product, a code the offline catalogue
 * does not know offers a search by name or a retry, and an unusable code
 * offers the manual entry (FR-014).
 */
public sealed interface BarcodeResolution {
    /** The catalogue knows the code and the product is ready to be logged. */
    public data class Resolved(
        public val product: Product,
    ) : BarcodeResolution

    /** The code is a usable barcode, but the offline catalogue does not know it. */
    public data class NotInCache(
        public val code: String,
    ) : BarcodeResolution

    /** The input was never a usable code. */
    public data class Unknown(
        public val code: String,
    ) : BarcodeResolution
}
