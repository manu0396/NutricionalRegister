package com.example.nutricionalregister.domain.assistant.testing

import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A [DiaryRepository] that records every write it is asked to perform.
 *
 * The assistant tests have to prove that a rejected action leaves the diary
 * untouched, so the fake keeps the writes apart from the reads: a rejected
 * action may read the diary to find the entry it was asked about, but it must
 * never add, update or remove one.
 *
 * [vanishAfterFirstObservation] hides the stored entries from every read but
 * the first one, which reproduces an entry that is removed between the
 * executor's validation and its write.
 */
internal class RecordingDiaryRepository(
    private val vanishAfterFirstObservation: Boolean = false,
) : DiaryRepository {
    private val stored = MutableStateFlow<List<DiaryEntry>>(emptyList())

    private var nextId = 1L

    private var observations = 0

    /** The entries the repository was asked to store, in order. */
    val addedEntries: MutableList<DiaryEntry> = mutableListOf()

    /** The entries the repository was asked to update, in order. */
    val updatedEntries: MutableList<DiaryEntry> = mutableListOf()

    /** The identifiers the repository was asked to remove, in order. */
    val removedIds: MutableList<Long> = mutableListOf()

    /** How many writes the repository was asked to perform. */
    val writeCount: Int
        get() = addedEntries.size + updatedEntries.size + removedIds.size

    override fun observeEntriesForDay(dayEpochMillis: Long): Flow<List<DiaryEntry>> =
        stored.map { entries ->
            observations += 1
            if (vanishAfterFirstObservation && observations > 1) {
                emptyList()
            } else {
                entries.filter { entry -> entry.dayEpochMillis == dayEpochMillis }
            }
        }

    override fun observeEntriesForWeek(dayEpochMillis: Long): Flow<List<DiaryEntry>> =
        stored.map { entries -> entries.filter { entry -> entry.dayEpochMillis <= dayEpochMillis } }

    override suspend fun addEntry(entry: DiaryEntry): Long {
        val id = nextId++
        addedEntries += entry
        stored.value = stored.value + entry.copy(id = id)
        return id
    }

    override suspend fun updateEntry(entry: DiaryEntry) {
        updatedEntries += entry
        stored.value = stored.value.map { current -> if (current.id == entry.id) entry else current }
    }

    override suspend fun removeEntry(id: Long) {
        removedIds += id
        stored.value = stored.value.filterNot { entry -> entry.id == id }
    }
}
