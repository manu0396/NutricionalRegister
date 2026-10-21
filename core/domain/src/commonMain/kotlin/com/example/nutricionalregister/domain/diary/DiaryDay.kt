package com.example.nutricionalregister.domain.diary

/**
 * Calendar helpers built on top of a [DayBoundaryProvider].
 *
 * The domain owns the rules; the platform only supplies the time zone.
 */
public object DiaryDay {
    /** The local midnight of the day containing [epochMillis]. */
    public fun startOfDay(
        provider: DayBoundaryProvider,
        epochMillis: Long,
    ): Long = provider.startOfDay(epochMillis)

    /**
     * The [count] local days that end with the day containing [epochMillis],
     * oldest first.
     *
     * The list always has exactly [count] entries, so a weekly summary always
     * shows seven days whether or not any of them has entries.
     */
    public fun lastDays(
        provider: DayBoundaryProvider,
        epochMillis: Long,
        count: Int,
    ): List<Long> {
        require(count > 0) { "The number of days must be greater than zero" }
        val today = provider.startOfDay(epochMillis)
        return (count - 1 downTo 0).map { offset -> provider.addDays(today, -offset) }
    }
}
