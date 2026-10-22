package com.example.nutricionalregister.domain.assistant.testing

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A [ProductSearchRepository] that resolves products from an in-memory list.
 *
 * The assistant executor only ever asks for a product by code, so the fake
 * answers that one question and leaves the search flow empty.
 *
 * [vanishAfterFirstLookup] makes the product disappear after it has been
 * found once, which reproduces a catalogue entry that vanishes between the
 * executor's validation and its write.
 */
internal class FakeProductSearchRepository(
    private val products: List<Product> = emptyList(),
    private val vanishAfterFirstLookup: Boolean = false,
) : ProductSearchRepository {
    private var lookups = 0

    override fun search(term: SearchTerm): Flow<ProductSearchOutcome> = flowOf(ProductSearchOutcome.Cached(null))

    override suspend fun getProduct(code: String): Product? {
        lookups += 1
        if (vanishAfterFirstLookup && lookups > 1) {
            return null
        }
        return products.firstOrNull { product -> product.code == code }
    }
}
