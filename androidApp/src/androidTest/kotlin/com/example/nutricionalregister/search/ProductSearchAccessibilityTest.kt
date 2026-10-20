package com.example.nutricionalregister.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
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

class ProductSearchAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchControlHasAccessibleLabelAndMinimumTouchSize() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val viewModel =
            ProductSearchViewModel(
                TestProductSearchRepository { term ->
                    flowOf(
                        ProductSearchOutcome.Cached(null),
                        ProductSearchOutcome.Success(
                            ProductSearchSnapshot(term, listOf(Product("42", "Oats")), 1L),
                        ),
                    )
                },
                TestRecentSearchRepository(),
                EpochClock { 1L },
                scope,
            )
        composeRule.setContent {
            MaterialTheme {
                ProductSearchScreen(viewModel)
            }
        }

        composeRule.onNodeWithTag("product-search-input").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Search").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Food name").assertExists()
        composeRule.onNodeWithTag("product-search-input").performTextInput("Oats")
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("View Oats, code 42").assertHeightIsAtLeast(48.dp)
        scope.cancel()
    }
}
