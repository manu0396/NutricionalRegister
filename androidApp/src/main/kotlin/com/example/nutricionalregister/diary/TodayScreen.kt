@file:Suppress("ktlint:standard:function-naming")

package com.example.nutricionalregister.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.Nutrient
import com.example.nutricionalregister.domain.diary.NutritionTotals
import com.example.nutricionalregister.domain.summary.DaySummary
import com.example.nutricionalregister.domain.summary.WeeklySummary
import com.example.nutricionalregister.feature.diary.DiaryState
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.components.NutriLogEmptyState
import com.example.nutricionalregister.ui.components.NutriLogProgressBar
import com.example.nutricionalregister.ui.components.NutriLogStatTile
import com.example.nutricionalregister.ui.components.NutriLogSurfaceCard
import com.example.nutricionalregister.ui.formatAmount
import com.example.nutricionalregister.ui.formatDayLabel
import com.example.nutricionalregister.ui.formatDayOfMonth
import com.example.nutricionalregister.ui.formatNutrient
import com.example.nutricionalregister.ui.formatWeekdayLabel
import com.example.nutricionalregister.ui.progressOf
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The Today screen: the home of the app (FR-001, FR-022, FR-023, SC-001).
 *
 * The screen is the food log of the day and nothing else. It opens with the date
 * and the day's calories against the goal, continues with the three macros and
 * the coach, and finishes with the four meals of the day, each with its own
 * entries and its own add action.
 *
 * The screen is stateless: it renders the [DiaryState] it is given and reports
 * every action through a callback. It holds no view model, reads no repository
 * and navigates nowhere, so the whole behaviour of the screen lives in one place
 * and can be exercised without a device.
 *
 * The honesty rule is applied everywhere a number is shown. A nutrient the
 * catalogue does not report for every logged food is a **lower bound**, never a
 * zero: it is rendered with the "at least" marker, and an explicit note says
 * which nutrients are incomplete. A nutrient no food reports at all is rendered
 * as unknown, never as zero.
 *
 * @param state the day's entries, totals and goal.
 * @param weeklySummary the last seven days, or null while they are being read.
 * @param onAddFood invoked with the meal to add food to.
 * @param onOpenProduct invoked with the product code of an entry that was opened.
 * @param onRemoveEntry invoked with the identifier of the entry to remove.
 * @param onOpenWeekly invoked when the week button is tapped.
 * @param onOpenCoach invoked when the coach card is tapped.
 * @param contentPadding the padding of the enclosing shell.
 * @param modifier the modifier applied to the screen.
 */
@Composable
public fun TodayScreen(
    state: DiaryState,
    weeklySummary: WeeklySummary?,
    onAddFood: (MealType) -> Unit,
    onOpenProduct: (String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onOpenWeekly: () -> Unit,
    onOpenCoach: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
    ) {
        TodayTopArea(
            dayEpochMillis = state.dayEpochMillis,
            onOpenWeekly = onOpenWeekly,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraLarge),
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(NutriLogSpacing.large),
        ) {
            DaySummaryCard(state = state)
            MacroTiles(state = state)
            CoachCard(onOpenCoach = onOpenCoach)
            MealsArea(
                state = state,
                onAddFood = onAddFood,
                onOpenProduct = onOpenProduct,
                onRemoveEntry = onRemoveEntry,
            )
            WeekCard(weeklySummary = weeklySummary)
        }
    }
}

/**
 * The top of the screen: the day the user is looking at and the way into the
 * week.
 *
 * The date is the heading of the screen because the diary is always about one
 * day, and the week is one tap away because a single day only makes sense next
 * to the days around it.
 */
@Composable
private fun TodayTopArea(
    dayEpochMillis: Long,
    onOpenWeekly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = NutriLogSpacing.large, vertical = NutriLogSpacing.medium),
    ) {
        Text(
            text = formatDayLabel(dayEpochMillis),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = onOpenWeekly,
            modifier = Modifier.heightIn(min = MinimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Week",
                modifier = Modifier.padding(start = NutriLogSpacing.small),
            )
        }
    }
}

/**
 * The hero of the screen: the calories of the day against the goal.
 *
 * This is the only elevated surface of the app and its number is the loudest
 * thing on the screen. The remaining calories are spelled out underneath: when
 * the day is only partially known, what is left is an upper bound and the text
 * says so instead of presenting it as the exact remainder.
 */
@Composable
private fun DaySummaryCard(
    state: DiaryState,
    modifier: Modifier = Modifier,
) {
    val totals = state.totals
    val goal = state.goal
    val consumed = totals.valueOf(Nutrient.ENERGY)
    val isLowerBound = totals.isLowerBound(Nutrient.ENERGY)
    val remaining = state.remaining.valueOf(Nutrient.ENERGY)
    val remainingLine =
        when {
            state.isLoading && state.entryCount == 0 -> "Reading today's entries"
            consumed == null -> "Calories eaten today are unknown"
            isLowerBound -> "At most ${formatNutrient(remaining, "kcal")} left today"
            else -> "${formatNutrient(remaining, "kcal")} left today"
        }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier = Modifier.padding(NutriLogSpacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall)) {
                Text(
                    text = formatNutrient(totals, Nutrient.ENERGY, "kcal"),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "of ${goal.energyKcal} kcal goal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NutriLogProgressBar(
                progress = progressOf(consumed, goal.energyKcal),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = remainingLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val note = incompleteNote(totals)
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The three macros of the day, each with its amount and its goal.
 *
 * The tiles read as a row of measurements: the amount is the loud part and the
 * caption carries the macro and the target it is measured against.
 */
@Composable
private fun MacroTiles(
    state: DiaryState,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        modifier = modifier.fillMaxWidth(),
    ) {
        NutriLogStatTile(
            value = formatNutrient(state.totals, Nutrient.PROTEIN, "g"),
            caption = "Protein, goal ${state.goal.proteinGrams} g",
            modifier = Modifier.weight(1f),
        )
        NutriLogStatTile(
            value = formatNutrient(state.totals, Nutrient.CARBOHYDRATE, "g"),
            caption = "Carbs, goal ${state.goal.carbohydrateGrams} g",
            modifier = Modifier.weight(1f),
        )
        NutriLogStatTile(
            value = formatNutrient(state.totals, Nutrient.FAT, "g"),
            caption = "Fat, goal ${state.goal.fatGrams} g",
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * The card that opens the assistant.
 *
 * The coach is a first-class part of the product, not a hidden extra, so it gets
 * a card of its own with a title and a sentence that says what it is for.
 */
@Composable
private fun CoachCard(
    onOpenCoach: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Coach",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Answers questions about the food you logged today and can log it for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onOpenCoach,
                modifier = Modifier.heightIn(min = MinimumTouchTarget),
            ) {
                Text("Ask")
            }
        }
    }
}

/**
 * The meals of the day, with the state of the diary above them.
 *
 * The four meals are always shown, even on a day with nothing in it, because the
 * shape of the day is what the user comes for. The day level empty state sits
 * above them so the user is told once that the diary is empty, instead of
 * reading it four times.
 */
@Composable
private fun MealsArea(
    state: DiaryState,
    onAddFood: (MealType) -> Unit,
    onOpenProduct: (String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (state.isEmpty) {
            NutriLogEmptyState(
                icon = Icons.Filled.DateRange,
                title = "Nothing logged today",
                body = "Log your first food and the totals of the day build up from there.",
                action = {
                    TextButton(
                        onClick = { onAddFood(MealType.entries.first()) },
                        modifier = Modifier.heightIn(min = MinimumTouchTarget),
                    ) {
                        Text("Add food")
                    }
                },
            )
        }
        MealType.entries.forEach { meal ->
            MealSection(
                meal = meal,
                entries = state.entriesByMeal[meal].orEmpty(),
                totals = state.mealTotals[meal] ?: NutritionTotals.ZERO,
                onAddFood = { onAddFood(meal) },
                onOpenProduct = onOpenProduct,
                onRemoveEntry = onRemoveEntry,
            )
        }
    }
}

/**
 * One meal of the day: its name, its own subtotal and its entries.
 *
 * A meal with no entries says so instead of claiming a calorie subtotal of
 * zero, because "nothing was logged" and "nothing was eaten" are different
 * statements.
 */
@Composable
private fun MealSection(
    meal: MealType,
    entries: List<DiaryEntry>,
    totals: NutritionTotals,
    onAddFood: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = meal.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (entries.isEmpty()) {
                    Text(
                        text = "Nothing logged yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = formatNutrient(totals, Nutrient.ENERGY, "kcal"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = mealMacrosLine(totals),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(
                onClick = onAddFood,
                modifier =
                    Modifier
                        .heightIn(min = MinimumTouchTarget)
                        .semantics { contentDescription = "Add food to ${meal.displayName}" },
            ) {
                Text("Add")
            }
        }
        if (entries.isNotEmpty()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = NutriLogSpacing.small),
            )
            entries.forEach { entry ->
                DiaryEntryRow(
                    entry = entry,
                    onOpenProduct = { onOpenProduct(entry.productCode) },
                    onRemoveEntry = { onRemoveEntry(entry.id) },
                )
            }
        }
    }
}

/**
 * One logged food: its name, the amount that was logged and its calories.
 *
 * The two actions are the only way in and out of an entry: one opens the
 * product in the catalogue, the other takes the entry out of the diary.
 */
@Composable
private fun DiaryEntryRow(
    entry: DiaryEntry,
    onOpenProduct: () -> Unit,
    onRemoveEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(vertical = NutriLogSpacing.small),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = entry.productName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatQuantity(entry.quantityGrams),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatNutrient(entry.energyKcal, "kcal"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = onOpenProduct,
            modifier = Modifier.sizeIn(minWidth = MinimumTouchTarget, minHeight = MinimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Open ${entry.productName}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onRemoveEntry,
            modifier = Modifier.sizeIn(minWidth = MinimumTouchTarget, minHeight = MinimumTouchTarget),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove ${entry.productName}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * The last seven days at a glance.
 *
 * The card is a read, not a chart: the average and the seven day marks say how
 * the week went without pretending to be an analysis. The week button in the top
 * area is the way into the full summary.
 */
@Composable
private fun WeekCard(
    weeklySummary: WeeklySummary?,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.small)) {
            Text(
                text = "This week",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when {
                weeklySummary == null -> {
                    Text(
                        text = "Reading the last seven days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                weeklySummary.daysWithEntries == 0 -> {
                    Text(
                        text = "Nothing logged in the last seven days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        text =
                            "Average ${formatNutrient(weeklySummary.averageTotals, Nutrient.ENERGY, "kcal")} " +
                                "over ${weeklySummary.daysWithEntries} logged " +
                                (if (weeklySummary.daysWithEntries == 1) "day" else "days"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (weeklySummary != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val equalShare = Modifier.weight(1f)
                    weeklySummary.days.forEach { day ->
                        WeekDayMark(day = day, modifier = equalShare)
                    }
                }
            }
        }
    }
}

/**
 * One day of the week: a mark that says whether the day was logged.
 *
 * The mark is filled when the day has entries and empty when it does not. The
 * day is described for a screen reader in words, because a filled and an empty
 * bar are the same shape and only the fill tells them apart.
 */
@Composable
private fun WeekDayMark(
    day: DaySummary,
    modifier: Modifier = Modifier,
) {
    val description =
        if (day.isEmpty) {
            "${formatWeekdayLabel(day.dayEpochMillis)} ${formatDayOfMonth(day.dayEpochMillis)}: nothing logged"
        } else {
            "${formatWeekdayLabel(day.dayEpochMillis)} ${formatDayOfMonth(day.dayEpochMillis)}: " +
                "${day.entryCount} entries, ${formatNutrient(day.totals, Nutrient.ENERGY, "kcal")}"
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
        modifier =
            modifier
                .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        color =
                            if (day.isEmpty) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
        )
        Text(
            text = formatWeekdayLabel(day.dayEpochMillis).take(1),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The amount of a logged entry, in grams.
 */
private fun formatQuantity(quantityGrams: Double): String = "${formatAmount(quantityGrams)} g"

/**
 * The three macros of a meal, each with the amount it contributes.
 */
private fun mealMacrosLine(totals: NutritionTotals): String =
    listOf(
        "Protein ${formatNutrient(totals, Nutrient.PROTEIN, "g")}",
        "Carbs ${formatNutrient(totals, Nutrient.CARBOHYDRATE, "g")}",
        "Fat ${formatNutrient(totals, Nutrient.FAT, "g")}",
    ).joinToString(" · ")

/**
 * The note that says which nutrients are only a lower bound, or null when every
 * nutrient of the set is completely known.
 */
private fun incompleteNote(totals: NutritionTotals): String? {
    if (totals.incomplete.isEmpty()) return null
    val names = totals.incomplete.map { it.nutrientLabel() }
    return "Some foods do not report ${joinWords(names)}, so those numbers are a lower bound."
}

/**
 * The name of a nutrient as the user reads it.
 */
private fun Nutrient.nutrientLabel(): String =
    when (this) {
        Nutrient.ENERGY -> "calories"
        Nutrient.PROTEIN -> "protein"
        Nutrient.CARBOHYDRATE -> "carbohydrate"
        Nutrient.FAT -> "fat"
    }

/**
 * Joins [words] into a readable list: "fat", "protein or fat" or
 * "protein, carbohydrate or fat".
 */
private fun joinWords(words: List<String>): String =
    when (words.size) {
        0 -> ""
        1 -> words[0]
        2 -> "${words[0]} or ${words[1]}"
        else -> words.dropLast(1).joinToString(", ") + " or " + words.last()
    }
