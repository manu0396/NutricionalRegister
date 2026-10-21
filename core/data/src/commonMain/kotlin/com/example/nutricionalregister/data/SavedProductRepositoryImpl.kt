package com.example.nutricionalregister.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed [SavedProductRepository] over the on-device `SavedProduct`
 * table (FR-001, FR-002, FR-003).
 *
 * The table is keyed by product code, so [save] is an idempotent upsert and the
 * same product can never be stored twice (FR-004). [remove] deletes only the saved
 * row and never touches the cached catalogue data.
 */
public class SavedProductRepositoryImpl(
    private val database: ProductDatabase,
    private val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SavedProductRepository {
    override suspend fun save(product: SavedProduct) {
        withContext(databaseDispatcher) {
            database.savedProductQueries.upsertSavedProduct(
                code = product.code,
                name = product.name,
                brands = product.brands,
                quantity = product.quantity,
                nutri_score_grade = product.nutriScoreGrade,
                image_thumbnail_url = product.imageThumbnailUrl,
                saved_at_epoch_millis = product.savedAtEpochMillis,
            )
        }
    }

    override suspend fun remove(code: String) {
        require(code.isNotBlank()) { "Saved product code must not be blank" }
        withContext(databaseDispatcher) {
            database.savedProductQueries.deleteSavedProduct(code)
        }
    }

    override fun observeSaved(): Flow<List<SavedProduct>> =
        database.savedProductQueries
            .selectSavedProducts()
            .asFlow()
            .mapToList(databaseDispatcher)
            .map { rows -> rows.map { it.toSavedProduct() } }

    override fun isSaved(code: String): Flow<Boolean> =
        database.savedProductQueries
            .selectSavedProductByCode(code)
            .asFlow()
            .mapToOneOrNull(databaseDispatcher)
            .map { row -> row != null }
}

private fun com.example.nutricionalregister.data.SavedProduct.toSavedProduct(): SavedProduct =
    SavedProduct(
        code = code,
        name = name,
        brands = brands,
        quantity = quantity,
        nutriScoreGrade = nutri_score_grade,
        imageThumbnailUrl = image_thumbnail_url,
        savedAtEpochMillis = saved_at_epoch_millis,
    )
