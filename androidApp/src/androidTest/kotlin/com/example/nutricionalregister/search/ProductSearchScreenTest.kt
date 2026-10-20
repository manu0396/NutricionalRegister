package com.example.nutricionalregister.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchSnapshot
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ProductSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun userCanSearchAndInspectWithoutDiaryActions() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val product = Product("00042", "Oat drink", brands = "Example Foods", quantity = "1 L")
        val repository =
            TestProductSearchRepository {
                flowOf(
                    ProductSearchOutcome.Cached(null),
                    ProductSearchOutcome.Success(
                        ProductSearchSnapshot(it, listOf(product), 100L),
                    ),
                )
            }
        val viewModel =
            ProductSearchViewModel(
                repository,
                TestRecentSearchRepository(),
                EpochClock { 100L },
                scope,
            )
        composeRule.setContent {
            MaterialTheme {
                ProductSearchScreen(viewModel)
            }
        }

        composeRule.onNodeWithTag("product-search-input").performTextInput("Oat")
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("View Oat drink, code 00042").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Oat drink").assertExists()
        composeRule.onNodeWithText("Product code").assertExists()
        composeRule.onNodeWithText("00042").assertExists()
        composeRule.onNodeWithText("Nutrition information not available for this product").assertExists()
        composeRule.onNodeWithText("Add to diary").assertDoesNotExist()
        scope.cancel()
    }
}
