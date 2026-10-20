package com.example.nutricionalregister.domain.product

import kotlinx.coroutines.flow.Flow

public interface RecentSearchRepository {
    public fun observeRecentSearches(): Flow<List<RecentSearch>>

    public suspend fun recordCompletedSearch(
        term: SearchTerm,
        completedAtEpochMillis: Long,
    )

    public suspend fun deleteRecentSearch(normalizedTerm: String)
}
