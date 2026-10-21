@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.nutricionalregister.data.AndroidPurchaseClient
import com.example.nutricionalregister.diary.DiaryPlaceholderScreen
import com.example.nutricionalregister.savedproducts.PurchaseDependencies
import com.example.nutricionalregister.savedproducts.PurchaseSection
import com.example.nutricionalregister.savedproducts.SavedProductsDependencies
import com.example.nutricionalregister.savedproducts.SavedProductsScreen
import com.example.nutricionalregister.search.ProductSearchDependencies
import com.example.nutricionalregister.search.ProductSearchScreen
import com.example.nutricionalregister.ui.theme.NutriLogTheme

@Composable
@Suppress("ktlint:standard:function-naming")
public fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val purchaseClient = remember(context) { AndroidPurchaseClient(context, context::findActivity) }
    val searchDependencies =
        remember(context, scope, purchaseClient) {
            ProductSearchDependencies(context, scope, purchaseClient)
        }
    val savedDependencies =
        remember(searchDependencies, scope) {
            SavedProductsDependencies(
                savedProductRepository = searchDependencies.savedProductRepository,
                clock = searchDependencies.clock,
                scope = scope,
            )
        }
    val purchaseDependencies =
        remember(searchDependencies, scope, purchaseClient) {
            PurchaseDependencies(
                purchaseRepository = searchDependencies.purchaseRepository,
                purchaseClient = purchaseClient,
                scope = scope,
            )
        }
    DisposableEffect(searchDependencies, savedDependencies, purchaseDependencies, purchaseClient) {
        onDispose {
            purchaseDependencies.close()
            savedDependencies.close()
            searchDependencies.close()
            purchaseClient.close()
        }
    }

    val savedState by savedDependencies.viewModel.state.collectAsState()

    NutriLogTheme {
        AppShell(
            searchContent = { contentPadding ->
                ProductSearchScreen(
                    viewModel = searchDependencies.viewModel,
                    contentPadding = contentPadding,
                    isProductSaved = { code -> savedState.savedCodes.contains(code) },
                    onToggleSaved = { product ->
                        if (savedState.savedCodes.contains(product.code)) {
                            savedDependencies.viewModel.remove(product.code)
                        } else {
                            savedDependencies.viewModel.save(product)
                        }
                    },
                    purchaseContent = { code ->
                        PurchaseDestination(dependencies = purchaseDependencies, productCode = code)
                    },
                )
            },
            savedContent = { contentPadding, onNavigateToSearch ->
                SavedProductsScreen(
                    viewModel = savedDependencies.viewModel,
                    onNavigateToSearch = onNavigateToSearch,
                    contentPadding = contentPadding,
                    purchaseContent = { code ->
                        PurchaseDestination(dependencies = purchaseDependencies, productCode = code)
                    },
                )
            },
            diaryContent = { contentPadding ->
                DiaryPlaceholderScreen(modifier = Modifier.padding(contentPadding))
            },
        )
    }
}

/**
 * Loads and renders the purchase surface for one product (FR-009 to FR-019).
 *
 * The surface is the app's own, but the payment itself happens in the platform's
 * sheet (FR-014); this only starts the flow and reflects its state.
 */
@Composable
private fun PurchaseDestination(
    dependencies: PurchaseDependencies,
    productCode: String,
) {
    LaunchedEffect(productCode) { dependencies.viewModel.observe(productCode) }
    val state by dependencies.viewModel.state.collectAsState()
    PurchaseSection(
        state = state,
        onBuy = { dependencies.viewModel.startPurchase(productCode) },
        onRetry = dependencies.viewModel::retry,
        onRestore = dependencies.viewModel::restore,
    )
}

/** Unwraps the host [Activity] from a [Context], or null when there is none. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
