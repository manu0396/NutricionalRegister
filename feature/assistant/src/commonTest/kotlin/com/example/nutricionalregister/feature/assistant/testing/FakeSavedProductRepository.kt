package com.example.nutricionalregister.feature.assistant.testing

import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * An in-memory [SavedProductRepository] that starts from a fixed library.
 *
 * The assistant only ever asks for the codes of the saved products, so the fake
 * keeps the products it was given.
 */
internal class FakeSavedProductRepository(
    initialProducts: List<SavedProduct> = emptyList(),
) : SavedProductRepository {
    private val stored = MutableStateFlow(initialProducts)

    override fun observeSaved(): Flow<List<SavedProduct>> = stored

    override fun isSaved(code: String): Flow<Boolean> = stored.map { products -> products.any { product -> product.code == code } }

    override suspend fun save(product: SavedProduct) {
        stored.value = stored.value.filterNot { saved -> saved.code == product.code } + product
    }

    override suspend fun remove(code: String) {
        stored.value = stored.value.filterNot { saved -> saved.code == code }
    }
}
