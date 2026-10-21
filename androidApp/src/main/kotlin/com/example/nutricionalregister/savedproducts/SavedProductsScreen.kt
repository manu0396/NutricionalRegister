@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.savedproducts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.feature.savedproducts.SavedProductsViewModel
import com.example.nutricionalregister.search.ProductDetailScreen
import com.example.nutricionalregister.search.ProductResultCard
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.NutriLogScaffold
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The Saved products destination (FR-002, FR-003, SC-009).
 *
 * Renders the on-device saved list, each entry opening the product detail within
 * this destination (FR-007) with a removal control. List entries reuse
 * [ProductResultCard], which shows the offline placeholder image when a thumbnail
 * is missing, so a saved product with no image still renders gracefully. When the
 * list is genuinely empty it shows an explicit "no saved products yet" message with
 * a shortcut that navigates to the Search destination (FR-002).
 *
 * @param onNavigateToSearch invoked by the empty-state shortcut to switch to the
 *   Search destination (navigation contract rule 6).
 */
@Composable
public fun SavedProductsScreen(
    viewModel: SavedProductsViewModel,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    openProductCode: String? = null,
    purchaseContent: (@Composable (productCode: String) -> Unit)? = null,
) {
    LaunchedEffect(openProductCode) {
        openProductCode?.let(viewModel::openProduct)
    }
    val state by viewModel.state.collectAsState()
    val selected = state.selectedProduct
    if (selected != null) {
        ProductDetailScreen(
            product = selected,
            onBack = viewModel::closeProductDetails,
            isSaved = true,
            onToggleSave = { viewModel.remove(selected.code) },
            purchaseContent = purchaseContent?.let { section -> { section(selected.code) } },
            modifier = modifier.fillMaxSize().padding(contentPadding),
        )
    } else {
        NutriLogScaffold(
            title = "Saved products",
            subtitle = "Products you keep on this device",
            modifier = modifier.fillMaxSize().padding(contentPadding),
        ) { innerPadding ->
            when {
                state.isEmpty ->
                    SavedProductsEmptyState(
                        onNavigateToSearch = onNavigateToSearch,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize(),
                    )
                else ->
                    SavedProductsList(
                        saved = state.saved,
                        onOpen = viewModel::openProduct,
                        onRemove = viewModel::remove,
                        contentPadding = innerPadding,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}

@Composable
private fun SavedProductsList(
    saved: List<SavedProduct>,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
        contentPadding =
            PaddingValues(
                start = NutriLogSpacing.large,
                end = NutriLogSpacing.large,
                top = contentPadding.calculateTopPadding() + NutriLogSpacing.medium,
                bottom = contentPadding.calculateBottomPadding() + NutriLogSpacing.large,
            ),
        modifier = modifier.testTag("saved-products-list"),
    ) {
        items(items = saved, key = { it.code }) { product ->
            Column(verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall)) {
                ProductResultCard(
                    product = product.toProduct(),
                    onOpen = { onOpen(product.code) },
                    modifier = Modifier.testTag("saved-product-${product.code}"),
                )
                OutlinedButton(
                    onClick = { onRemove(product.code) },
                    modifier =
                        Modifier
                            .heightIn(min = MinimumTouchTarget)
                            .testTag("remove-saved-${product.code}"),
                ) {
                    Text(text = "Remove")
                }
            }
        }
    }
}

@Composable
private fun SavedProductsEmptyState(
    onNavigateToSearch: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(contentPadding),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(NutriLogSpacing.large),
        ) {
            Text(
                text = "No saved products yet",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("saved-products-empty"),
            )
            Text(
                text = "Products you save will appear here, available offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onNavigateToSearch,
                modifier =
                    Modifier
                        .heightIn(min = MinimumTouchTarget)
                        .testTag("saved-products-empty-search"),
            ) {
                Text(text = "Search for a product")
            }
        }
    }
}

private fun SavedProduct.toProduct(): Product =
    Product(
        code = code,
        name = name,
        brands = brands,
        quantity = quantity,
        nutriScoreGrade = nutriScoreGrade,
        imageThumbnailUrl = imageThumbnailUrl,
    )
