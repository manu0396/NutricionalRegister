package com.example.nutricionalregister.domain.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * A day boundary provider with a fixed day length, used to exercise the day
 * arithmetic without depending on a platform calendar.
 */
private class FakeDayBoundaryProvider(
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

private const val MILLIS_PER_DAY = 86_400_000L

class DiaryDayTest {
    @Test
    fun `the start of a day is the local midnight`() {
        val provider = FakeDayBoundaryProvider()
        assertEquals(0L, provider.startOfDay(0L))
        assertEquals(0L, provider.startOfDay(MILLIS_PER_DAY - 1))
        assertEquals(MILLIS_PER_DAY, provider.startOfDay(MILLIS_PER_DAY))
    }

    @Test
    fun `the last days always end with today and are ordered oldest first`() {
        val provider = FakeDayBoundaryProvider()
        val now = 3 * MILLIS_PER_DAY + 5_000L
        val days = DiaryDay.lastDays(provider, now, 7)
        assertEquals(7, days.size)
        assertEquals(provider.startOfDay(now), days.last())
        assertEquals(days.sorted(), days)
        assertEquals(MILLIS_PER_DAY, days[1] - days[0])
    }

    @Test
    fun `the last days require a positive count`() {
        assertFailsWith<IllegalArgumentException> { DiaryDay.lastDays(FakeDayBoundaryProvider(), 0L, 0) }
    }

    @Test
    fun `a day that is not midnight long still yields whole days`() {
        val provider = FakeDayBoundaryProvider(dayLengthMillis = 23 * 3_600_000L)
        val now = 5 * provider.startOfDay(1L) + 10L
        val days = DiaryDay.lastDays(provider, now, 3)
        assertEquals(3, days.size)
        assertEquals(provider.startOfDay(now), days.last())
    }
}
