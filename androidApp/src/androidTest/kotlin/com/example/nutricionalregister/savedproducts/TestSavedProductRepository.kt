package com.example.nutricionalregister.savedproducts

import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory saved-product store for instrumented tests. */
internal class TestSavedProductRepository : SavedProductRepository {
    private val mutableSaved = MutableStateFlow<List<SavedProduct>>(emptyList())

    override suspend fun save(product: SavedProduct) {
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

internal fun testClock(epochMillis: Long): EpochClock = EpochClock { epochMillis }

internal fun testProduct(
    code: String,
    name: String,
    imageThumbnailUrl: String? = null,
): Product = Product(code = code, name = name, imageThumbnailUrl = imageThumbnailUrl)
