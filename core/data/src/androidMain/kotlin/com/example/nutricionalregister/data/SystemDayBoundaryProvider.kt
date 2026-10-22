package com.example.nutricionalregister.data

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import java.util.Calendar

/**
 * The Android [DayBoundaryProvider], backed by the device's local time zone.
 *
 * `java.util.Calendar` is used rather than `java.time` because the minimum
 * supported API level is 24 and `java.time` is only available from API 26
 * without core-library desugaring.
 */
internal class SystemDayBoundaryProvider : DayBoundaryProvider {
    override fun startOfDay(epochMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    override fun addDays(
        dayStartEpochMillis: Long,
        days: Int,
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dayStartEpochMillis
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.timeInMillis
    }

    override fun startOfWeek(epochMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startOfDay(epochMillis)
        // Monday, counted from the day of week directly so the answer does not
        // depend on what day of the week the device's locale starts one with.
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
