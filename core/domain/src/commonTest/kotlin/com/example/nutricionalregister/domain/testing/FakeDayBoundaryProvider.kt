package com.example.nutricionalregister.domain.testing

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider

/**
 * A day boundary provider with a fixed day length.
 *
 * The shared domain has no time-zone library, so the tests use a provider whose
 * days are exactly 24 hours long. That keeps the day arithmetic verifiable
 * without depending on a platform calendar.
 */
public class FakeDayBoundaryProvider(
    private val dayLengthMillis: Long = MILLIS_PER_DAY,
    private val origin: Long = 0,
) : DayBoundaryProvider {
    override fun startOfDay(epochMillis: Long): Long {
        val offset = epochMillis - origin
        val days = offset / dayLengthMillis
        val floorDays = if (offset % dayLengthMillis < 0) days - 1 else days
        return origin + floorDays * dayLengthMillis
    }

    override fun addDays(dayStartEpochMillis: Long, days: Int): Long = dayStartEpochMillis + days * dayLengthMillis
}

/** The length of a day in milliseconds. */
public const val MILLIS_PER_DAY: Long = 86_400_000L
