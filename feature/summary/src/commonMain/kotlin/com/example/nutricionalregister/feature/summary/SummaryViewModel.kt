package com.example.nutricionalregister.feature.summary

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.summary.SummaryCalculator
import com.example.nutricionalregister.domain.summary.WeeklySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Presentation logic for the weekly summary.
 *
 * The view model reads the last seven local days from the diary and turns them
 * into a [WeeklySummary]. The summary is computed on demand from the stored
 * entries, never cached in the database, so it can never be stale.
 *
 * The state is null while the first week is being read, which lets the screen
 * show a progress indicator instead of an empty week that is not empty yet.
 *
 * @param scope the lifecycle scope that owns the collection; the caller clears
 *   it on dispose.
 */
public class SummaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val dayBoundaryProvider: DayBoundaryProvider,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<WeeklySummary?>(null)
    public val weeklySummary: StateFlow<WeeklySummary?> = mutableState.asStateFlow()

    private var weekJob: Job? = null

    init {
        observeWeek(currentDay())
    }

    /** Re-reads the current week, so a day boundary crossed while the app is open is picked up. */
    public fun refresh() {
        observeWeek(currentDay())
    }

    /** Stops the collection; called when the summary's dependencies are closed. */
    public fun clear() {
        weekJob?.cancel()
        weekJob = null
    }

    private fun observeWeek(day: Long) {
        weekJob?.cancel()
        weekJob =
            scope.launch {
                diaryRepository.observeEntriesForWeek(day).collect { entries ->
                    mutableState.value = SummaryCalculator.weekly(dayBoundaryProvider, day, entries)
                }
            }
    }

    private fun currentDay(): Long = dayBoundaryProvider.startOfDay(clock.nowEpochMillis())
}
