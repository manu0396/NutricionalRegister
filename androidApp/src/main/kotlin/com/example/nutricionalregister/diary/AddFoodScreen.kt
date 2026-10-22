@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionTotals
import com.example.nutricionalregister.domain.navigation.AddFoodStep
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.feature.diary.AddFoodSource
import com.example.nutricionalregister.feature.diary.AddFoodState
import com.example.nutricionalregister.scan.BarcodeScannerView
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.components.NutriLogEmptyState
import com.example.nutricionalregister.ui.components.NutriLogListItem
import com.example.nutricionalregister.ui.components.NutriLogMacroRow
import com.example.nutricionalregister.ui.components.NutriLogSurfaceCard
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The add-food flow: the food and then the amount (FR-013 to FR-019).
 *
 * The flow has two steps and the step comes from the route, so the screen always
 * renders the step the navigation state says it is on and the two can never
 * disagree. The meal is not a step: it is picked from a modal sheet on the
 * quantity step, because choosing a meal is not a navigation step and giving it
 * one would make the system back button dismiss something that is not a
 * destination (ADR-0019).
 *
 * The screen is a stateless composable: it renders the [state] it is given and
 * reports the user's actions through its callbacks. It holds no view model,
 * reads no repository and navigates nowhere.
 *
 * @param state the state of the flow.
 * @param step the step the flow is on, which comes from the route.
 * @param results the results of the catalogue search.
 * @param library the foods the user keeps.
 * @param onSourceSelected invoked with the source the food is looked for in.
 * @param onQueryChanged invoked with the new search text.
 * @param onSearch invoked when the search should run.
 * @param onProductSelected invoked with the chosen food.
 * @param onBarcodeScanned invoked with the code the camera read.
 * @param onQuantityChanged invoked with the new amount in grams.
 * @param onMealSheetOpened invoked when the user asks for the meal sheet.
 * @param onMealSheetDismissed invoked when the sheet is dismissed without a meal.
 * @param onMealSelected invoked with the chosen meal.
 * @param onConfirm invoked when the food should be logged.
 * @param onBack invoked when the user leaves the flow.
 * @param contentPadding the padding of the enclosing shell.
 * @param modifier the modifier applied to the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AddFoodScreen(
    state: AddFoodState,
    step: AddFoodStep,
    results: List<Product>,
    library: List<SavedProduct>,
    onSourceSelected: (AddFoodSource) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onProductSelected: (Product) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onQuantityChanged: (Double) -> Unit,
    onMealSheetOpened: () -> Unit,
    onMealSheetDismissed: () -> Unit,
    onMealSelected: (MealType) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (step == AddFoodStep.QUANTITY) "How much" else "Add food",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            navigationIcon = {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier =
                        Modifier
                            .sizeIn(minWidth = MinimumTouchTarget, minHeight = MinimumTouchTarget)
                            .testTag("add-food-back"),
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
        )

        if (step == AddFoodStep.QUANTITY) {
            QuantityStep(
                state = state,
                onQuantityChanged = onQuantityChanged,
                onMealSheetOpened = onMealSheetOpened,
                onConfirm = onConfirm,
                modifier = Modifier.weight(1f),
            )
        } else {
            SourceStep(
                state = state,
                results = results,
                library = library,
                onSourceSelected = onSourceSelected,
                onQueryChanged = onQueryChanged,
                onSearch = onSearch,
                onProductSelected = onProductSelected,
                onBarcodeScanned = onBarcodeScanned,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.isMealSheetOpen) {
            MealSheet(
                onMealSheetDismissed = onMealSheetDismissed,
                onMealSelected = onMealSelected,
            )
        }
    }
}

/** The step where the food is looked for: search, the library or the camera. */
@Composable
private fun SourceStep(
    state: AddFoodState,
    results: List<Product>,
    library: List<SavedProduct>,
    onSourceSelected: (AddFoodSource) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onProductSelected: (Product) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.large),
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(NutriLogSpacing.large),
    ) {
        SourceTabs(
            selected = state.source,
            onSourceSelected = onSourceSelected,
        )

        when (state.source) {
            AddFoodSource.SEARCH ->
                SearchPanel(
                    query = state.query,
                    results = results,
                    onQueryChanged = onQueryChanged,
                    onSearch = onSearch,
                    onProductSelected = onProductSelected,
                )

            AddFoodSource.LIBRARY ->
                LibraryPanel(
                    library = library,
                    onProductSelected = onProductSelected,
                )

            AddFoodSource.SCAN ->
                BarcodeScannerView(onBarcodeDetected = onBarcodeScanned)
        }
    }
}

/**
 * The three sources the food is looked for in.
 *
 * The selected source is set off by a heavier border and a heavier weight, not
 * by colour alone, so the state survives a colour-blind eye and is announced by
 * the button's selected state.
 */
@Composable
private fun SourceTabs(
    selected: AddFoodSource,
    onSourceSelected: (AddFoodSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
        modifier = modifier.fillMaxWidth(),
    ) {
        AddFoodSource.entries.forEach { source ->
            val isSelected = source == selected
            OutlinedButton(
                onClick = { onSourceSelected(source) },
                shape = MaterialTheme.shapes.large,
                border =
                    if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = MinimumTouchTarget)
                        .testTag("add-food-source-${source.name.lowercase()}"),
            ) {
                Text(
                    text = source.label(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/** The label of a source, in the words of the app. */
private fun AddFoodSource.label(): String =
    when (this) {
        AddFoodSource.SEARCH -> "Search"
        AddFoodSource.LIBRARY -> "My foods"
        AddFoodSource.SCAN -> "Scan"
    }

/** The catalogue search and its results. */
@Composable
private fun SearchPanel(
    query: String,
    results: List<Product>,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onProductSelected: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.large),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { Text(text = "Search the food catalogue") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            trailingIcon = {
                IconButton(onClick = onSearch, modifier = Modifier.testTag("add-food-search-submit")) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("add-food-query"),
        )

        if (results.isEmpty()) {
            NutriLogEmptyState(
                icon = Icons.Filled.Search,
                title = "Nothing found",
                body =
                    "Search the catalogue by name, read the barcode of the pack, or keep " +
                        "the food in the library.",
            )
        } else {
            results.forEach { product ->
                NutriLogSurfaceCard {
                    NutriLogListItem(
                        title = product.name,
                        subtitle = product.subtitle(),
                        onClick = { onProductSelected(product) },
                        trailing = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add ${product.name}",
                            )
                        },
                    )
                }
            }
        }
    }
}

/** The foods the user keeps, as shortcuts into the flow. */
@Composable
private fun LibraryPanel(
    library: List<SavedProduct>,
    onProductSelected: (Product) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (library.isEmpty()) {
        NutriLogEmptyState(
            icon = Icons.Filled.Star,
            title = "No food kept yet",
            body =
                "The foods you keep are offered here first, so the food you eat most " +
                    "often is logged in one tap.",
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.large),
            modifier = modifier.fillMaxWidth(),
        ) {
            library.forEach { food ->
                NutriLogSurfaceCard {
                    NutriLogListItem(
                        title = food.name,
                        subtitle = listOfNotNull(food.brands, food.quantity).joinToString(" · "),
                        onClick = { onProductSelected(food.toProduct()) },
                        trailing = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add ${food.name}",
                            )
                        },
                    )
                }
            }
        }
    }
}

/** The step where the amount and the meal are confirmed. */
@Composable
private fun QuantityStep(
    state: AddFoodState,
    onQuantityChanged: (Double) -> Unit,
    onMealSheetOpened: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val product = state.product

    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraLarge),
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(NutriLogSpacing.large),
    ) {
        if (product == null) {
            NutriLogEmptyState(
                icon = Icons.Filled.Add,
                title = "No food chosen",
                body = "Go back and choose the food to log.",
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
                modifier = Modifier.fillMaxWidth().testTag("add-food-chosen-product"),
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                )
                listOfNotNull(product.brands, product.quantity)
                    .joinToString(" · ")
                    .takeIf { it.isNotBlank() }
                    ?.let { secondary ->
                        Text(
                            text = secondary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
            }

            AmountCard(
                grams = state.quantityGrams,
                onQuantityChanged = onQuantityChanged,
            )

            PreviewCard(totals = state.preview)

            MealControl(
                mealType = state.mealType,
                onOpened = onMealSheetOpened,
            )

            OutlinedButton(
                onClick = onConfirm,
                enabled = state.canConfirm,
                shape = MaterialTheme.shapes.large,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinimumTouchTarget)
                        .testTag("add-food-confirm"),
            ) {
                Text(text = "Log the food")
            }
        }
    }
}

/** The amount in grams, with a set of quick amounts. */
@Composable
private fun AmountCard(
    grams: Double,
    onQuantityChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "How much",
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = if (grams > 0.0) grams.toPlainString() else "",
                onValueChange = { value -> value.toDoubleOrNull()?.let(onQuantityChanged) },
                label = { Text(text = "Amount in grams") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("add-food-quantity"),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
                modifier = Modifier.fillMaxWidth(),
            ) {
                QUICK_AMOUNTS.forEach { amount ->
                    OutlinedButton(
                        onClick = { onQuantityChanged(amount.toDouble()) },
                        modifier = Modifier.weight(1f).testTag("add-food-quick-$amount"),
                    ) {
                        Text(text = "$amount g")
                    }
                }
            }
        }
    }
}

/**
 * The nutrition the chosen amount produces.
 *
 * A nutrient the food does not report reads as a dash, never as a zero, because
 * an unknown amount is not an amount of nothing (FR-006).
 */
@Composable
private fun PreviewCard(
    totals: NutritionTotals,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "This amount is",
                style = MaterialTheme.typography.titleSmall,
            )
            NutriLogMacroRow(label = "Energy", value = totals.energyKcal, goal = 100, unit = "kcal")
            NutriLogMacroRow(label = "Protein", value = totals.proteinGrams, goal = 100, unit = "g")
            NutriLogMacroRow(label = "Carbohydrate", value = totals.carbohydrateGrams, goal = 100, unit = "g")
            NutriLogMacroRow(label = "Fat", value = totals.fatGrams, goal = 100, unit = "g")
        }
    }
}

/** The meal the food is logged into, chosen from the sheet. */
@Composable
private fun MealControl(
    mealType: MealType?,
    onOpened: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Meal",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = mealType?.displayName ?: "No meal chosen",
                style = MaterialTheme.typography.titleLarge,
                color =
                    if (mealType == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            TextButton(
                onClick = onOpened,
                modifier =
                    Modifier
                        .heightIn(min = MinimumTouchTarget)
                        .testTag("add-food-open-meal-sheet"),
            ) {
                Text(text = "Choose the meal")
            }
        }
    }
}

/**
 * The meal sheet.
 *
 * It is a modal sheet and not a route, so it never creates a navigation entry
 * and the system back button dismisses it instead of leaving the flow (ADR-0019).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealSheet(
    onMealSheetDismissed: () -> Unit,
    onMealSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onMealSheetDismissed,
        modifier = modifier.testTag("add-food-meal-sheet"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = NutriLogSpacing.large,
                        end = NutriLogSpacing.large,
                        bottom = NutriLogSpacing.extraLarge,
                    ),
        ) {
            Text(
                text = "Which meal is this food part of?",
                style = MaterialTheme.typography.titleMedium,
            )
            MealType.ordered.forEach { meal ->
                NutriLogListItem(
                    title = meal.displayName,
                    onClick = { onMealSelected(meal) },
                )
            }
        }
    }
}

/** The secondary line of a product, made of the parts the catalogue reports. */
private fun Product.subtitle(): String = listOfNotNull(brands, quantity).joinToString(" · ")

/** The amount without floating-point noise. */
private fun Double.toPlainString(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}

private val QUICK_AMOUNTS = listOf(50, 100, 200)
