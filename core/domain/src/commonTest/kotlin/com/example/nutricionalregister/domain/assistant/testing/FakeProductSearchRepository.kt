package com.example.nutricionalregister.domain.assistant.testing
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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

/**
 * A saved-product repository whose library holds [saved], so the executor's
 * fallback to a food the catalogue cache does not have can be tested.
 */
internal class FakeSavedProductRepository(
    saved: List<SavedProduct> = emptyList(),
) : SavedProductRepository {
    private val saved = MutableStateFlow(saved)

    override fun observeSaved(): Flow<List<SavedProduct>> = saved

    override suspend fun save(product: SavedProduct) {
        saved.value = saved.value + product
    }

    override suspend fun remove(code: String) {
        saved.value = saved.value.filterNot { it.code == code }
    }

    override fun isSaved(code: String): Flow<Boolean> = saved.map { saved -> saved.any { it.code == code } }
}
