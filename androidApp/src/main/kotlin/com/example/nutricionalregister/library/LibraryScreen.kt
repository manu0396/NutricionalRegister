@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.NutriLogScaffold
import com.example.nutricionalregister.ui.components.NutriLogEmptyState
import com.example.nutricionalregister.ui.components.NutriLogListItem
import com.example.nutricionalregister.ui.components.NutriLogSurfaceCard
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The Library: the foods the user keeps for one-tap logging (FR-007, FR-008).
 *
 * The library is not a bookmark list, it is a shortcut into the add-food flow, so
 * every row carries the two actions that make it a shortcut: "Add", which starts
 * the flow with that food already chosen, and the row itself, which opens the
 * food's detail. Removing a food is the third action, and it is the destructive
 * one, so it is the only one that is not a text button.
 *
 * The screen is stateless: it renders the list it is given and reports every
 * action through its callbacks, so the shell owns the data and the navigation.
 *
 * @param foods the foods the user keeps.
 * @param onOpenProduct invoked with the code of the food whose row was tapped.
 * @param onAddFood invoked with the food whose add action was used.
 * @param onRemove invoked with the code of the food to remove.
 * @param onFindFoods invoked when the user asks to look for foods.
 * @param contentPadding the padding of the enclosing shell.
 * @param modifier the modifier applied to the screen.
 */
@Composable
public fun LibraryScreen(
    foods: List<SavedProduct>,
    onOpenProduct: (String) -> Unit,
    onAddFood: (SavedProduct) -> Unit,
    onRemove: (String) -> Unit,
    onFindFoods: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NutriLogScaffold(
        title = "Library",
        subtitle = "The foods you keep, ready to log in one tap.",
        modifier = modifier.fillMaxSize().padding(contentPadding),
    ) { innerPadding ->
        if (foods.isEmpty()) {
            LibraryEmptyState(
                onFindFoods = onFindFoods,
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LibraryList(
                foods = foods,
                onOpenProduct = onOpenProduct,
                onAddFood = onAddFood,
                onRemove = onRemove,
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The list of kept foods.
 *
 * Every food is its own surface, because a food is a thing the user recognises
 * rather than a line in a table, and because each one carries two actions that
 * need room of their own.
 */
@Composable
private fun LibraryList(
    foods: List<SavedProduct>,
    onOpenProduct: (String) -> Unit,
    onAddFood: (SavedProduct) -> Unit,
    onRemove: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        contentPadding =
            PaddingValues(
                start = NutriLogSpacing.large,
                end = NutriLogSpacing.large,
                top = contentPadding.calculateTopPadding() + NutriLogSpacing.medium,
                bottom = contentPadding.calculateBottomPadding() + NutriLogSpacing.large,
            ),
        modifier = modifier.testTag("library-list"),
    ) {
        items(items = foods, key = { it.code }) { food ->
            LibraryFoodCard(
                food = food,
                onOpen = { onOpenProduct(food.code) },
                onAdd = { onAddFood(food) },
                onRemove = { onRemove(food.code) },
                modifier = Modifier.testTag("library-food-${food.code}"),
            )
        }
    }
}

/**
 * One kept food: its name, its brand and its pack size, with the actions that
 * turn it into a shortcut.
 *
 * The row itself opens the food's detail. The add action is a text button, so
 * it is unambiguous without an icon, and the remove action is an icon button
 * whose description names the food it removes.
 */
@Composable
private fun LibraryFoodCard(
    food: SavedProduct,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NutriLogSurfaceCard(modifier = modifier) {
        NutriLogListItem(
            title = food.name,
            subtitle = food.subtitle(),
            onClick = onOpen,
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onAdd,
                        modifier =
                            Modifier
                                .heightIn(min = MinimumTouchTarget)
                                .testTag("library-add-${food.code}"),
                    ) {
                        Text("Add")
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier =
                            Modifier
                                .sizeIn(minWidth = MinimumTouchTarget, minHeight = MinimumTouchTarget)
                                .testTag("library-remove-${food.code}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Remove ${food.name} from the library",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}

/**
 * The empty library.
 *
 * An empty library is not a dead end: the state explains what the library is for
 * and offers the one action that fills it, which is finding a food.
 */
@Composable
private fun LibraryEmptyState(
    onFindFoods: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(contentPadding),
    ) {
        NutriLogEmptyState(
            icon = Icons.Outlined.FavoriteBorder,
            title = "No foods yet",
            body = "Keep the foods you eat often here and log them in one tap.",
            modifier = Modifier.testTag("library-empty"),
            action = {
                Button(
                    onClick = onFindFoods,
                    modifier =
                        Modifier
                            .heightIn(min = MinimumTouchTarget)
                            .testTag("library-find-foods"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.padding(end = NutriLogSpacing.extraSmall),
                    )
                    Text("Find foods")
                }
            },
        )
    }
}

/**
 * The line under the food's name: its brand and its pack size.
 *
 * Both parts are optional in the catalogue, so the line only shows what the
 * product actually reports. A food with neither is shown with its name alone
 * rather than with an empty line.
 */
private fun SavedProduct.subtitle(): String? =
    listOfNotNull(
        brands?.takeIf { it.isNotBlank() },
        quantity?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { null }
