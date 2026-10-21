package com.example.nutricionalregister.feature.summary.testing

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * An in-memory [DiaryRepository] for the view-model tests.
 *
 * The fake behaves like the real one in the ways the tests rely on: it emits
 * the entries of the requested window and it re-emits on every change.
 */
internal class FakeDiaryRepository : DiaryRepository {
    private val stored = MutableStateFlow<List<DiaryEntry>>(emptyList())

    override fun observeEntriesForDay(dayEpochMillis: Long): Flow<List<DiaryEntry>> =
        stored.map { entries -> entries.filter { it.dayEpochMillis == dayEpochMillis } }

    override fun observeEntriesForWeek(dayEpochMillis: Long): Flow<List<DiaryEntry>> =
        stored.map { entries -> entries.filter { it.dayEpochMillis <= dayEpochMillis } }

    override suspend fun addEntry(entry: DiaryEntry): Long {
        stored.value = stored.value + entry.copy(id = stored.value.size + 1L)
        return stored.value.last().id
    }

    override suspend fun updateEntry(entry: DiaryEntry) {
        stored.value = stored.value.map { current -> if (current.id == entry.id) entry else current }
    }

    override suspend fun removeEntry(id: Long) {
        stored.value = stored.value.filterNot { entry -> entry.id == id }
    }
}
