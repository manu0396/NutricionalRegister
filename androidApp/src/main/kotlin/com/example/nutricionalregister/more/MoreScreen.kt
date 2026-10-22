@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.ui.NutriLogScaffold
import com.example.nutricionalregister.ui.SectionHeader
import com.example.nutricionalregister.ui.components.NutriLogListItem
import com.example.nutricionalregister.ui.components.NutriLogSurfaceCard
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The More destination: the goals, the coach, the premium unlock and the
 * privacy policy (FR-016, FR-017, FR-026, FR-032).
 *
 * The goals come first, because they are what the daily summary is measured
 * against and the user comes here to change them. The coach and the privacy
 * policy are rows, and the premium unlock is the one thing the screen does not
 * own: it is handed in as a slot, so this screen knows nothing about a product
 * code, a price or a store, and the purchase can only ever appear here.
 *
 * The screen is stateless: it renders the goal it is given and reports every
 * change through [onGoalChanged].
 *
 * @param goal the daily goal the user has set.
 * @param onGoalChanged invoked with the goal that carries the value the user changed.
 * @param purchaseContent the premium unlock, or null when the build has none.
 * @param onOpenPrivacyPolicy invoked when the privacy policy row is tapped.
 * @param contentPadding the padding of the enclosing shell.
 * @param modifier the modifier applied to the screen.
 */
@Composable
public fun MoreScreen(
    goal: NutritionGoal,
    onGoalChanged: (NutritionGoal) -> Unit,
    purchaseContent: (@Composable () -> Unit)?,
    onOpenPrivacyPolicy: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NutriLogScaffold(
        title = "More",
        subtitle = "Your goals and the rest of the app.",
        modifier = modifier.fillMaxSize().padding(contentPadding),
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraLarge),
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(
                        horizontal = NutriLogSpacing.large,
                        vertical = NutriLogSpacing.large,
                    ),
        ) {
            GoalSection(
                goal = goal,
                onGoalChanged = onGoalChanged,
            )

            if (purchaseContent != null) {
                NutriLogSurfaceCard(modifier = Modifier.testTag("more-purchase-section")) {
                    purchaseContent()
                }
            }

            MoreRow(
                title = "Privacy policy",
                subtitle = "What the app sends and what it keeps on the device",
                icon = Icons.Filled.Lock,
                onClick = onOpenPrivacyPolicy,
                modifier = Modifier.testTag("more-privacy-row"),
            )
        }
    }
}

/**
 * The daily goal editor.
 *
 * The four numbers are the daily targets the summary is measured against, and
 * each one is edited on its own row so a change to one never touches another.
 */
@Composable
private fun GoalSection(
    goal: NutritionGoal,
    onGoalChanged: (NutritionGoal) -> Unit,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier.testTag("more-goal-section")) {
        SectionHeader(title = "Daily goal")
        GoalNutrient.entries.forEachIndexed { index, nutrient ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            GoalField(
                nutrient = nutrient,
                goal = goal,
                onGoalChanged = onGoalChanged,
            )
        }
    }
}

/**
 * One goal number: its name on the left, the value the user types on the right.
 *
 * The field keeps the text the user typed, so a value that is still being typed
 * is never snapped back under the cursor. A value the goal type refuses is not
 * stored: the field says so and the goal keeps the value it had.
 *
 * @param nutrient the number this field edits.
 * @param goal the goal the value is read from and written to.
 * @param onGoalChanged invoked with the goal that carries the new value.
 * @param modifier the modifier applied to the field.
 */
@Composable
private fun GoalField(
    nutrient: GoalNutrient,
    goal: NutritionGoal,
    onGoalChanged: (NutritionGoal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = goal.valueOf(nutrient)
    var text by rememberSaveable { mutableStateOf(current.toString()) }
    var applied by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(current) {
        if (current != applied) {
            text = current.toString()
        }
    }

    val typed = text.trim().toIntOrNull()
    val isAccepted = typed != null && goal.accepts(nutrient, typed)

    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = NutriLogSpacing.small),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = nutrient.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { typedText ->
                    text = typedText
                    val value = typedText.trim().toIntOrNull()
                    if (value != null) {
                        goal.with(nutrient, value)?.let { updated ->
                            applied = value
                            onGoalChanged(updated)
                        }
                    }
                },
                suffix = { Text(nutrient.unit) },
                singleLine = true,
                isError = !isAccepted,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                modifier =
                    Modifier
                        .width(132.dp)
                        .semantics { contentDescription = nutrient.label }
                        .testTag("goal-field-${nutrient.name.lowercase()}"),
            )
        }
        if (!isAccepted) {
            Text(
                text = goalErrorMessage(text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("goal-error-${nutrient.name.lowercase()}"),
            )
        }
    }
}

/**
 * A row that opens another part of the app.
 *
 * The row has a visible name, so it is usable with a screen reader, and the
 * icons around it are decorative: they repeat what the text already says.
 */
@Composable
private fun MoreRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        NutriLogListItem(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            leading = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}

/**
 * The four numbers a daily goal is made of.
 *
 * The labels and the units live with the editor, so the field and the value it
 * writes can never drift apart.
 */
private enum class GoalNutrient(
    val label: String,
    val unit: String,
) {
    ENERGY("Calories", "kcal"),
    PROTEIN("Protein", "g"),
    CARBOHYDRATE("Carbohydrate", "g"),
    FAT("Fat", "g"),
}

/**
 * The value [nutrient] currently has in this goal.
 */
private fun NutritionGoal.valueOf(nutrient: GoalNutrient): Int =
    when (nutrient) {
        GoalNutrient.ENERGY -> energyKcal
        GoalNutrient.PROTEIN -> proteinGrams
        GoalNutrient.CARBOHYDRATE -> carbohydrateGrams
        GoalNutrient.FAT -> fatGrams
    }

/**
 * The goal that carries [candidate] for [nutrient] instead of the value it has
 * now, or null when the goal type refuses the value.
 *
 * The rule about what a valid goal is belongs to [NutritionGoal], so the screen
 * asks the goal instead of inventing a rule of its own: the copy runs the goal's
 * own validation, and a value it rejects never reaches the caller.
 */
private fun NutritionGoal.with(
    nutrient: GoalNutrient,
    candidate: Int,
): NutritionGoal? =
    try {
        when (nutrient) {
            GoalNutrient.ENERGY -> copy(energyKcal = candidate)
            GoalNutrient.PROTEIN -> copy(proteinGrams = candidate)
            GoalNutrient.CARBOHYDRATE -> copy(carbohydrateGrams = candidate)
            GoalNutrient.FAT -> copy(fatGrams = candidate)
        }
    } catch (refused: IllegalArgumentException) {
        null
    }

/**
 * Whether [candidate] is a value this goal accepts for [nutrient].
 */
private fun NutritionGoal.accepts(
    nutrient: GoalNutrient,
    candidate: Int,
): Boolean = with(nutrient, candidate) != null

/**
 * The message shown when the typed value is not one the goal accepts.
 *
 * A value that is not a number at all and a value that is out of range are two
 * different mistakes, so they are two different messages.
 */
private fun goalErrorMessage(text: String): String =
    if (text.trim().toIntOrNull() == null) {
        "Enter a whole number."
    } else {
        "Enter a number above zero."
    }
