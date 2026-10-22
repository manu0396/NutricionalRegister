package com.example.nutricionalregister.domain.diary

/**
 * Resolves the local day a timestamp belongs to.
 *
 * The shared code has no time-zone library, so the platform supplies this
 * contract: Android and iOS implement it with their own calendar APIs. The
 * domain only ever needs two things from it - where the current day starts and
 * how to step whole days - and both are defined in local time so that the
 * diary follows the user's clock rather than UTC.
 *
 * @see DiaryDay
 */
public interface DayBoundaryProvider {
    /** The epoch millis of the local midnight that starts the day containing [epochMillis]. */
    public fun startOfDay(epochMillis: Long): Long

    /**
     * The local midnight [days] days after the day that starts at
     * [dayStartEpochMillis].
     *
     * Stepping by whole days rather than by a fixed number of milliseconds
     * keeps the result exact across daylight-saving transitions, where a day
     * can be 23 or 25 hours long.
     */
    public fun addDays(
        dayStartEpochMillis: Long,
        days: Int,
    ): Long
}
