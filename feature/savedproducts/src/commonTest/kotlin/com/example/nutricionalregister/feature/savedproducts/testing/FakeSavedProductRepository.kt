package com.example.nutricionalregister.feature.savedproducts.testing

import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An in-memory [SavedProductRepository] for presentation tests: it keeps the same
 * upsert-by-code and ordering semantics as the SQLDelight implementation, so the
 * view model sees realistic behaviour without a database.
 */
internal class FakeSavedProductRepository : SavedProductRepository {
    private val mutableSaved = MutableStateFlow<List<SavedProduct>>(emptyList())

    val saveCount: Int
        get() = saves

    private var saves = 0

    override suspend fun save(product: SavedProduct) {
        saves += 1
        mutableSaved.value =
            (mutableSaved.value.filterNot { it.code == product.code } + product)
                .sortedByDescending { it.savedAtEpochMillis }
    }

    override suspend fun remove(code: String) {
        mutableSaved.value = mutableSaved.value.filterNot { it.code == code }
    }

    override fun observeSaved(): Flow<List<SavedProduct>> = mutableSaved

    override fun isSaved(code: String): Flow<Boolean> = MutableStateFlow(mutableSaved.value.any { it.code == code })
}
