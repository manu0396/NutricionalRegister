package com.example.nutricionalregister.feature.assistant.testing

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider

/**
 * A day boundary provider whose days are exactly 24 hours long.
 *
 * The shared domain has no time-zone library, so the tests use a provider with
 * a fixed day length. That keeps the day arithmetic verifiable without
 * depending on a platform calendar.
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
}

internal const val MILLIS_PER_DAY: Long = 86_400_000L
