package com.example.nutricionalregister.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.nutricionalregister.domain.product.RecentSearch
import com.example.nutricionalregister.domain.product.RecentSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

public class RecentSearchRepositoryImpl(
    private val database: ProductDatabase,
    private val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RecentSearchRepository {
    override fun observeRecentSearches(): Flow<List<RecentSearch>> =
        database.recentSearchQueries
            .selectRecentSearches()
            .asFlow()
            .mapToList(databaseDispatcher)
            .map { rows ->
                rows.map { row ->
                    RecentSearch(
                        normalizedTerm = row.normalized_term,
                        displayTerm = row.display_term,
                        lastCompletedAtEpochMillis = row.last_completed_at_epoch_millis,
                        completionSequence = row.completion_sequence,
                    )
                }
            }

    override suspend fun recordCompletedSearch(
        term: SearchTerm,
        completedAtEpochMillis: Long,
    ) {
        withContext(databaseDispatcher) {
            val queries = database.recentSearchQueries
            database.transaction {
                val sequence = queries.nextCompletionSequence().executeAsOne()
                queries.advanceCompletionSequence(sequence)
                queries.deleteRecentSearch(term.normalizedTerm)
                queries.insertRecentSearch(
                    normalized_term = term.normalizedTerm,
                    display_term = term.displayTerm,
                    last_completed_at_epoch_millis = completedAtEpochMillis,
                    completion_sequence = sequence,
                )
                queries.trimRecentSearches()
            }
        }
    }

    override suspend fun deleteRecentSearch(normalizedTerm: String) {
        require(normalizedTerm.isNotBlank()) { "Normalized term must not be blank" }
        withContext(databaseDispatcher) {
            database.recentSearchQueries.deleteRecentSearch(normalizedTerm)
        }
    }
}
