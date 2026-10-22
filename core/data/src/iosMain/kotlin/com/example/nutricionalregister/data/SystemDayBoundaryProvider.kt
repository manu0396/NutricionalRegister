package com.example.nutricionalregister.data

import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

/**
 * The iOS [DayBoundaryProvider], backed by the device's local calendar.
 *
 * The calendar is asked for the start of the day rather than for a fixed
 * number of milliseconds, so the result follows the user's clock across
 * daylight-saving transitions.
 */
internal class SystemDayBoundaryProvider : DayBoundaryProvider {
    private val calendar: NSCalendar = NSCalendar.currentCalendar

    override fun startOfDay(epochMillis: Long): Long {
        val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / MILLIS_PER_SECOND)
        val start = calendar.startOfDayForDate(date)
        return (start.timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()
    }

    override fun addDays(
        dayStartEpochMillis: Long,
        days: Int,
    ): Long {
        val date = NSDate.dateWithTimeIntervalSince1970(dayStartEpochMillis / MILLIS_PER_SECOND)
        val components = NSDateComponents()
        components.day = days.toLong()
        val shifted = calendar.dateByAddingComponents(components, toDate = date, options = 0u) ?: date
        return (shifted.timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()
    }

    private companion object {
        private const val MILLIS_PER_SECOND = 1000.0
    }
}
