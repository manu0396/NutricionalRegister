package com.example.nutricionalregister.feature.scan

import com.example.nutricionalregister.domain.product.ProductSearchRepository

/**
 * Turns a scanned barcode into a catalogue product.
 *
 * The resolution goes through the existing [ProductSearchRepository], so a
 * scanned code uses the same offline cache as the rest of the catalogue and no
 * second data source is introduced (FR-014).
 *
 * The resolver never fails: a code that cannot be resolved is reported as such
 * so the screen can say honestly that the offline catalogue does not know it
 * and offer a search by name or a retry.
 */
public class BarcodeResolver(
    private val productRepository: ProductSearchRepository,
) {
    /**
     * Resolves [code] against the catalogue.
     *
     * A blank code is [BarcodeResolution.Unknown], because it was never a
     * usable barcode. A code the catalogue does not know is
     * [BarcodeResolution.NotInCache], which is a different situation: the code
     * is a real barcode the offline catalogue simply does not have.
     */
    public suspend fun resolve(code: String): BarcodeResolution {
        val barcode = code.trim()
        if (barcode.isEmpty()) {
            return BarcodeResolution.Unknown(barcode)
        }
        val product = productRepository.getProduct(barcode)
        return if (product == null) {
            BarcodeResolution.NotInCache(barcode)
        } else {
            BarcodeResolution.Resolved(product)
        }
    }
}
