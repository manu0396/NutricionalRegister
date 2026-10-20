@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.feature.search.ProductSearchState
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import com.example.nutricionalregister.ui.NutriLogScaffold
import com.example.nutricionalregister.ui.SectionHeader
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * Hosts the search experience and, once a product is opened, the detail screen.
 *
 * The root search view is wrapped in [NutriLogScaffold] so it gets a real app bar
 * and correct system-bar insets in the running app. The detail screen is rendered
 * standalone (no shell) because it owns its own back control and top inset; the
 * behaviour is identical when this composable is rendered directly by a test.
 */
@Composable
public fun ProductSearchScreen(
    viewModel: ProductSearchViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val product = state.selectedProduct
    if (product != null) {
        ProductDetailScreen(
            product = product,
            onBack = viewModel::closeProductDetails,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        NutriLogScaffold(
            title = "NutriLog",
            subtitle = "Search the catalogue by food name",
            modifier = modifier.fillMaxSize(),
        ) { innerPadding ->
            SearchContent(
                state = state,
                onQueryChanged = viewModel::onQueryChanged,
                onSearch = viewModel::submitSearch,
                onRetry = viewModel::retrySearch,
                onOpenProduct = viewModel::openProduct,
                onSelectRecent = viewModel::selectRecentSearch,
                onDeleteRecent = viewModel::deleteRecentSearch,
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SearchContent(
    state: ProductSearchState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onSelectRecent: (String) -> Unit,
    onDeleteRecent: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraLarge),
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(
                    horizontal = NutriLogSpacing.large,
                    vertical = NutriLogSpacing.large,
                ),
    ) {
        SearchField(
            query = state.query,
            onQueryChanged = onQueryChanged,
            searchEnabled = !state.isRefreshing,
            onSearch = {
                focusManager.clearFocus()
                onSearch()
            },
        )

        if (state.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        SearchOutcomeMessage(
            phase = state.phase,
            onRetry = onRetry,
            onEdit = { onQueryChanged("") },
        )

        if (state.products.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium)) {
                SectionHeader(title = "Products")
                state.products.forEach { product ->
                    ProductResultCard(product, onOpen = { onOpenProduct(product.code) })
                }
            }
        }

        RecentSearches(
            recentSearches = state.recentSearches,
            onSelectRecent = onSelectRecent,
            onDeleteRecent = onDeleteRecent,
        )

        OpenFoodFactsAttribution()
    }
}

/**
 * The search control: a full-width text field stacked over a full-width button.
 *
 * Splitting the field and action keeps both at a comfortable, thumb-sized width on
 * every screen size (the previous inline arrangement left the button floating at a
 * fraction of the width) and gives the primary action real elevation and presence.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    searchEnabled: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            label = { Text("Food name") },
            placeholder = { Text("e.g. Oat drink") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .testTag("product-search-input"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        Button(
            onClick = onSearch,
            enabled = searchEnabled,
            shape = MaterialTheme.shapes.large,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text("Search")
        }
    }
}
