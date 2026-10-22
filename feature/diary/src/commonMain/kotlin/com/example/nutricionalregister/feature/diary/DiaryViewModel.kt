package com.example.nutricionalregister.feature.diary

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionCalculator
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.diary.NutritionGoalRepository
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.summary.SummaryCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation logic for the diary: the Today screen and the add-food flow.
 *
 * The view model is repository-driven: it holds no cache of its own, so the
 * state cannot drift from what is stored on the device. Every read and every
 * write is local, so the diary works with no connection at all (FR-018).
 *
 * The nutrition of a logged entry is computed by [NutritionCalculator] in the
 * shared domain, never here, so the Android UI never sums a nutrient itself
 * (NFR-005).
 *
 * @param scope the lifecycle scope that owns the collection; the caller clears
 *   it on dispose.
 */
public class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val goalRepository: NutritionGoalRepository,
    private val dayBoundaryProvider: DayBoundaryProvider,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(DiaryState())
    public val state: StateFlow<DiaryState> = mutableState.asStateFlow()

    private var dayJob: Job? = null
    private var goalJob: Job? = null

    init {
        observeDay(currentDay())
        observeGoal()
    }

    /**
     * Re-reads the current day.
     *
     * The day boundary can be crossed while the app is open, so the Today
     * screen calls this when it becomes visible again; the new day then starts
     * with an empty diary.
     */
    public fun refresh() {
        observeDay(currentDay())
    }

    /**
     * Logs [quantityGrams] of [product] into [mealType].
     *
     * The entry is a snapshot: the product name and its nutrient values are
     * copied at log time, so a later catalogue change cannot rewrite what the
     * user ate.
     */
    public fun log(
        product: Product,
        mealType: MealType,
        quantityGrams: Double,
    ) {
        val loggedAt = clock.nowEpochMillis()
        val day = dayBoundaryProvider.startOfDay(loggedAt)
        val entry =
            NutritionCalculator.entryFor(
                product = product,
                mealType = mealType,
                quantityGrams = quantityGrams,
                loggedAtEpochMillis = loggedAt,
                dayEpochMillis = day,
            )
        scope.launch {
            diaryRepository.addEntry(entry)
        }
    }

    /** Removes the entry with [id]; removing an unknown entry is a no-op. */
    public fun removeEntry(id: Long) {
        scope.launch {
            diaryRepository.removeEntry(id)
        }
    }

    /** Stores [goal] as the daily target. */
    public fun updateGoal(goal: NutritionGoal) {
        scope.launch {
            goalRepository.saveGoal(goal)
        }
    }

    /** Stops the collections; called when the diary's dependencies are closed. */
    public fun clear() {
        dayJob?.cancel()
        dayJob = null
        goalJob?.cancel()
        goalJob = null
    }

    private fun observeDay(day: Long) {
        dayJob?.cancel()
        mutableState.update { it.copy(dayEpochMillis = day, isLoading = true) }
        dayJob =
            scope.launch {
                diaryRepository.observeEntriesForDay(day).collect { entries ->
                    val summary = SummaryCalculator.daily(day, entries, mutableState.value.goal)
                    mutableState.update {
                        it.copy(
                            entriesByMeal = summary.entriesByMeal,
                            totals = summary.totals,
                            mealTotals = summary.mealTotals,
                            isLoading = false,
                        )
                    }
                }
            }
    }

    private fun observeGoal() {
        goalJob?.cancel()
        goalJob =
            scope.launch {
                goalRepository.observeGoal().collect { goal ->
                    mutableState.update { it.copy(goal = goal) }
                }
            }
    }

    private fun currentDay(): Long = dayBoundaryProvider.startOfDay(clock.nowEpochMillis())
}
