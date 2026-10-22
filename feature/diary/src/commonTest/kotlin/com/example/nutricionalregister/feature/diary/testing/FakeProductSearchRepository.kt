package com.example.nutricionalregister.feature.diary.testing

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A [ProductSearchRepository] that resolves products from an in-memory catalogue.
 *
 * The add-food flow only ever asks for a product by code, so the fake answers
 * that one question and leaves the search flow empty.
 */
internal class FakeProductSearchRepository(
    private val products: List<Product> = emptyList(),
) : ProductSearchRepository {
    /** The codes the catalogue was asked to resolve, in order. */
    val requestedCodes: MutableList<String> = mutableListOf()

    override fun search(term: SearchTerm): Flow<ProductSearchOutcome> = flowOf(ProductSearchOutcome.Cached(null))

    override suspend fun getProduct(code: String): Product? {
        requestedCodes += code
        return products.firstOrNull { product -> product.code == code }
    }
}
