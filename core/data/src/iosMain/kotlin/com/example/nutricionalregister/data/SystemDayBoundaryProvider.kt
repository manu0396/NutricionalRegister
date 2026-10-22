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

    override fun startOfWeek(epochMillis: Long): Long {
        val date = NSDate.dateWithTimeIntervalSince1970(startOfDay(epochMillis) / MILLIS_PER_SECOND)
        val components = NSDateComponents()
        components.day = (-daysSinceMondayOf(date)).toLong()
        val weekStart = calendar.dateByAddingComponents(components, toDate = date, options = 0u) ?: date
        return startOfDay((weekStart.timeIntervalSince1970 * MILLIS_PER_SECOND).toLong())
    }

    /** The number of days between [date] and the Monday of its week. */
    private fun daysSinceMondayOf(date: NSDate): Int {
        // NSCalendarUnit is a plain unsigned mask in the Kotlin/Native
        // Foundation binding, and no constant wrapper is generated for it, so
        // the weekday is asked for with the legacy NSWeekdayCalendarUnit value.
        val read = calendar.components(WEEKDAY_CALENDAR_UNIT_MASK, fromDate = date)
        val weekday = read.weekday.toInt()
        // Apple's weekday numbering starts the week with Sunday as 1, so
        // Monday is 2, and the difference is folded to 0..6 regardless of the
        // locale.
        return (weekday - 2 + 7) % 7
    }

    private companion object {
        private const val MILLIS_PER_SECOND = 1000.0
    }
}

/** The weekday mask of [NSCalendar.components], the legacy `NSWeekdayCalendarUnit` = 512. */
private const val WEEKDAY_CALENDAR_UNIT_MASK: ULong = 512uL
