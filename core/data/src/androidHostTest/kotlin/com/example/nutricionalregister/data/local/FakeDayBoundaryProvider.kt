package com.example.nutricionalregister.data.local

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider

/**
 * A day boundary provider with a fixed day length, used to exercise the day
 * bucketing without depending on the device calendar.
 */
internal class FakeDayBoundaryProvider(
    private val dayLengthMillis: Long = MILLIS_PER_DAY,
) : DayBoundaryProvider {
    override fun startOfDay(epochMillis: Long): Long {
        val days = epochMillis / dayLengthMillis
        val floorDays = if (epochMillis % dayLengthMillis < 0) days - 1 else days
        return floorDays * dayLengthMillis
    }

    override fun addDays(
        dayStartEpochMillis: Long,
        days: Int,
    ): Long = dayStartEpochMillis + days * dayLengthMillis

    override fun startOfWeek(epochMillis: Long): Long {
        val day = startOfDay(epochMillis)
        // 1970-01-01 was a Thursday, so a day's index inside its week is its
        // position from the epoch plus three, folded to 0..6.
        val daysFromEpoch = day / dayLengthMillis
        val daysSinceMonday = (daysFromEpoch + 3L) % 7L
        return day - daysSinceMonday * dayLengthMillis
    }
}

internal const val MILLIS_PER_DAY = 86_400_000L
