package com.example.nutricionalregister.domain.diary

import kotlinx.coroutines.flow.Flow

/**
 * On-device storage for the user's food diary.
 *
 * The diary is the primary record of the app, so the repository is the only
 * way the UI ever learns what was eaten. Everything is local: no method
 * performs a network call (FR-018).
 */
public interface DiaryRepository {
    /**
     * Emits the entries of exactly the local day that starts at
     * [dayEpochMillis], oldest first, and re-emits whenever the day changes.
     */
    public fun observeEntriesForDay(dayEpochMillis: Long): Flow<List<DiaryEntry>>

    /**
     * Emits the entries of the seven local days that end with the day that
     * starts at [dayEpochMillis], oldest first.
     */
    public fun observeEntriesForWeek(dayEpochMillis: Long): Flow<List<DiaryEntry>>

    /** Stores [entry] and returns the identifier of the new row. */
    public suspend fun addEntry(entry: DiaryEntry): Long

    /** Updates the stored entry with the same identifier as [entry]. */
    public suspend fun updateEntry(entry: DiaryEntry)

    /** Removes the entry with [id]; removing an unknown entry is a no-op. */
    public suspend fun removeEntry(id: Long)
}
