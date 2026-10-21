package com.example.nutricionalregister.feature.diary.testing

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * An in-memory [DiaryRepository] for the view-model tests.
 *
 * The fake behaves like the real one in the ways the tests rely on: it emits
 * the entries of a day, it re-emits on every change, and it assigns an
 * identifier to every stored entry.
 */
internal class FakeDiaryRepository : DiaryRepository {
    private val stored = MutableStateFlow<List<DiaryEntry>>(emptyList())

    /** The entries the repository has been asked to store, in order. */
    val addedEntries: MutableList<DiaryEntry> = mutableListOf()

    /** The identifiers the repository has been asked to remove, in order. */
    val removedIds: MutableList<Long> = mutableListOf()

    private var nextId = 1L

    override fun observeEntriesForDay(dayEpochMillis: Long): Flow<List<DiaryEntry>> =
        stored.map { entries -> entries.filter { it.dayEpochMillis == dayEpochMillis } }

    override fun observeEntriesForWeek(dayEpochMillis: Long): Flow<List<DiaryEntry>> =
        stored.map { entries -> entries.filter { it.dayEpochMillis <= dayEpochMillis } }

    override suspend fun addEntry(entry: DiaryEntry): Long {
        val id = nextId++
        addedEntries += entry
        stored.value = stored.value + entry.copy(id = id)
        return id
    }

    override suspend fun updateEntry(entry: DiaryEntry) {
        stored.value = stored.value.map { current -> if (current.id == entry.id) entry else current }
    }

    override suspend fun removeEntry(id: Long) {
        removedIds += id
        stored.value = stored.value.filterNot { entry -> entry.id == id }
    }
}
