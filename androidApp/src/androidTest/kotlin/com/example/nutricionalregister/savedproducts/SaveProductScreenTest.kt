package com.example.nutricionalregister.savedproducts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchSnapshot
import com.example.nutricionalregister.domain.product.SearchTerm
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import com.example.nutricionalregister.search.ProductSearchScreen
import com.example.nutricionalregister.search.TestProductSearchRepository
import com.example.nutricionalregister.search.TestRecentSearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

/**
 * T031: saving from the product detail view (the primary save surface) marks the
 * product saved, and that status is reflected wherever the product is shown
 * afterwards (FR-001).
 */
class SaveProductScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savingFromTheDetailViewMarksTheProductSaved() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val product = testProduct("0001", "Oats")
        val snapshot =
            ProductSearchSnapshot(
                term = SearchTerm("oats", "Oats"),
                products = listOf(product),
                fetchedAtEpochMillis = 1L,
            )
        val viewModel =
            ProductSearchViewModel(
                TestProductSearchRepository { flowOf(ProductSearchOutcome.Success(snapshot)) },
                TestRecentSearchRepository(),
                testClock(1L),
                scope,
            )

        composeRule.setContent {
            var savedCodes by remember { mutableStateOf(emptySet<String>()) }
            MaterialTheme {
                ProductSearchScreen(
                    viewModel = viewModel,
                    isProductSaved = { code -> savedCodes.contains(code) },
                    onToggleSaved = { toggled ->
                        savedCodes = if (savedCodes.contains(toggled.code)) savedCodes - toggled.code else savedCodes + toggled.code
                    },
                )
            }
        }

        // submit a search so the cached result renders, then open the detail and save
        composeRule.onNodeWithTag("product-search-input").performTextInput("Oats")
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("View Oats, code 0001").performClick()
        composeRule.onNodeWithTag("save-product-control").assertIsDisplayed()
        composeRule.onNodeWithText("Save product").assertIsDisplayed()
        composeRule.onNodeWithTag("save-product-control").performClick()

        // the control now offers removal, i.e. the product is marked saved (FR-001)
        composeRule.onNodeWithText("Remove from saved").assertIsDisplayed()
        scope.cancel()
    }
}
