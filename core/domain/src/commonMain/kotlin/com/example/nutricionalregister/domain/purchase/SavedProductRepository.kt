package com.example.nutricionalregister.domain.purchase

import kotlinx.coroutines.flow.Flow

/**
 * On-device storage for the products the user saved (FR-001, FR-002, FR-003).
 *
 * Removal deletes only the saved row; it never touches the cached catalogue
 * `Product`/nutrition data (mirrors the recent-search deletion policy).
 */
public interface SavedProductRepository {
    /** Saves [product]; saving the same code twice is an idempotent upsert (FR-004). */
    public suspend fun save(product: SavedProduct)

    /** Removes the saved product with [code], if present (FR-003). */
    public suspend fun remove(code: String)

    /** Emits the saved products, most recently saved first (FR-002). */
    public fun observeSaved(): Flow<List<SavedProduct>>

    /** Emits whether [code] is currently saved (FR-002). */
    public fun isSaved(code: String): Flow<Boolean>
}
