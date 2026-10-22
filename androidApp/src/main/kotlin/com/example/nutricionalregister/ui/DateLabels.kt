@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The long label of a day: "Wednesday, 21 October".
 *
 * The device's locale and time zone are used, so the label reads the way the
 * user's own calendar reads. The timestamp is the local midnight of the day, so
 * formatting it in the default time zone always yields the same day.
 */
public fun formatDayLabel(dayEpochMillis: Long): String = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(dayEpochMillis))

/**
 * The short label of a day: "Wed".
 */
public fun formatWeekdayLabel(dayEpochMillis: Long): String = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dayEpochMillis))

/**
 * The day of the month: "21".
 */
public fun formatDayOfMonth(dayEpochMillis: Long): String = SimpleDateFormat("d", Locale.getDefault()).format(Date(dayEpochMillis))
