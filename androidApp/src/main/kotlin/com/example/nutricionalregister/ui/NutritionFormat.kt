@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.ui

import com.example.nutricionalregister.domain.diary.Nutrient
import com.example.nutricionalregister.domain.diary.NutritionTotals

/**
 * The dash shown for a value that is not known.
 *
 * The app never shows a number it does not have. A nutrient the catalogue did
 * not report reads as a dash, which is visibly different from a zero.
 */
public const val UNKNOWN_VALUE: String = "—"

/**
 * Formats a nutrient amount without floating-point noise.
 *
 * Catalogue values are doubles such as `7.6899999999999995`; they are rounded to
 * at most two decimals, whole numbers lose their decimal point, and a trailing
 * `.0` is dropped so values read as a person would write them.
 */
internal fun formatAmount(amount: Double): String {
    val rounded = kotlin.math.round(amount * 100.0) / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}

/**
 * Formats a nutrient value.
 *
 * An unknown value is a dash, never a zero. A value that is only a lower bound
 * is prefixed with "≥", so the user knows the real amount is at least this.
 */
public fun formatNutrient(
    value: Double?,
    unit: String,
    lowerBound: Boolean = false,
): String =
    when {
        value == null -> UNKNOWN_VALUE
        else ->
            buildString {
                if (lowerBound) append("≥ ")
                append(formatAmount(value))
                append(' ')
                append(unit)
            }
    }

/**
 * Formats the value of [nutrient] in [totals].
 *
 * The totals own the knowledge of what is unknown and what is only a lower
 * bound, so the screen never has to work that out itself.
 */
public fun formatNutrient(
    totals: NutritionTotals,
    nutrient: Nutrient,
    unit: String,
): String = formatNutrient(totals.valueOf(nutrient), unit, totals.isLowerBound(nutrient))

/**
 * The fraction of [goal] that [value] reaches, clamped to 0..1.
 *
 * An unknown value has no progress, so it reads as an empty bar rather than as a
 * bar pretending to be full or empty.
 */
public fun progressOf(
    value: Double?,
    goal: Int,
): Float {
    if (value == null || goal <= 0) return 0f
    return (value / goal.toDouble()).toFloat().coerceIn(0f, 1f)
}

/**
 * The percentage of [goal] that [value] reaches, or null when the value is
 * unknown.
 */
public fun percentOf(
    value: Double?,
    goal: Int,
): Int? {
    if (value == null || goal <= 0) return null
    return ((value / goal.toDouble()) * 100.0).toInt().coerceAtLeast(0)
}
