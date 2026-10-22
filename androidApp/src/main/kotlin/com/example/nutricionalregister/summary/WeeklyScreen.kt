@file:Suppress("ktlint:standard:function-naming")

package com.example.nutricionalregister.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.diary.Nutrient
import com.example.nutricionalregister.domain.summary.DaySummary
import com.example.nutricionalregister.domain.summary.WeeklySummary
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.UNKNOWN_VALUE
import com.example.nutricionalregister.ui.components.NutriLogSurfaceCard
import com.example.nutricionalregister.ui.formatDayOfMonth
import com.example.nutricionalregister.ui.formatNutrient
import com.example.nutricionalregister.ui.formatWeekdayLabel
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The weekly summary: the last seven days (FR-015, SC-001).
 *
 * The screen answers one question, "how did the week go?", and it answers it
 * with the average of the days that were logged and one row per day of the last
 * seven. A day without entries is shown as a day without entries: it is not
 * counted in the average and it is not dressed up as a day of zero calories.
 *
 * The screen is stateless: it renders the summary it is given and reports the
 * back action through a callback.
 *
 * @param summary the last seven days, or null while they are being read.
 * @param onBack invoked when the back control is used.
 * @param contentPadding the padding of the enclosing shell.
 * @param modifier the modifier applied to the screen.
 */
@Composable
public fun WeeklyScreen(
    summary: WeeklySummary?,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
    ) {
        WeeklyTopBar(onBack = onBack)
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraLarge),
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(NutriLogSpacing.large),
        ) {
            WeekAverageCard(summary = summary)
            WeekDaysCard(summary = summary)
        }
    }
}

/**
 * The top bar of the screen: the way back and the name of the screen.
 */
@Composable
private fun WeeklyTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = NutriLogSpacing.small, vertical = NutriLogSpacing.small),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.sizeIn(minWidth = MinimumTouchTarget, minHeight = MinimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
        }
        Text(
            text = "Week",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The hero of the screen: the average of the days that have entries.
 *
 * The average is taken over the days with entries only, so a day the user did
 * not log never pulls the number down. When the week is completely empty there
 * is no average to show and the card says so.
 */
@Composable
private fun WeekAverageCard(
    summary: WeeklySummary?,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
            modifier = Modifier.padding(NutriLogSpacing.large),
        ) {
            when {
                summary == null -> {
                    Text(
                        text = "Reading the last seven days",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "The week is still being loaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                summary.daysWithEntries == 0 -> {
                    Text(
                        text = "Nothing logged",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "No food was logged in the last seven days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        text = formatNutrient(summary.averageTotals, Nutrient.ENERGY, "kcal"),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "daily average over the last seven days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${summary.daysWithEntries} of 7 days with entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * The seven days of the week, one row each.
 */
@Composable
private fun WeekDaysCard(
    summary: WeeklySummary?,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        if (summary == null) {
            Text(
                text = "The days of the week are still being loaded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            summary.days.forEachIndexed { index, day ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                WeekDayRow(day = day)
            }
        }
    }
}

/**
 * One day of the week: its date, how much was logged and its calories.
 *
 * A day without entries says "Nothing logged" and shows no calorie figure,
 * because a day the user did not log is not a day of zero calories.
 */
@Composable
private fun WeekDayRow(
    day: DaySummary,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(vertical = NutriLogSpacing.small),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp),
        ) {
            Text(
                text = formatWeekdayLabel(day.dayEpochMillis),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDayOfMonth(day.dayEpochMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            text = entryCountLabel(day.entryCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text =
                if (day.isEmpty) {
                    UNKNOWN_VALUE
                } else {
                    formatNutrient(day.totals, Nutrient.ENERGY, "kcal")
                },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/**
 * The number of entries of a day, in words.
 */
private fun entryCountLabel(entryCount: Int): String =
    when {
        entryCount <= 0 -> "Nothing logged"
        entryCount == 1 -> "1 entry"
        else -> "$entryCount entries"
    }
