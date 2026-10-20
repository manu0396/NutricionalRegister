package com.example.nutricionalregister.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchFailure
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

class SearchOutcomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorStateShowsDistinctMessageAndRetryAction() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val repository =
            TestProductSearchRepository {
                flowOf(
                    ProductSearchOutcome.Cached(null),
                    ProductSearchOutcome.Failure(ProductSearchFailure.SERVICE, null),
                )
            }
        val viewModel =
            ProductSearchViewModel(
                repository,
                TestRecentSearchRepository(),
                EpochClock { 1L },
                scope,
            )
        composeRule.setContent {
            MaterialTheme {
                ProductSearchScreen(viewModel)
            }
        }

        composeRule.onNodeWithTag("product-search-input").performTextInput("unknown")
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("The search could not be completed.").assertExists()
        composeRule.onNodeWithText("Retry").assertExists().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Edit search").assertExists()
        scope.cancel()
    }

    @Test
    fun offlineCacheAndKnownNoResultsHaveDistinctMessages() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val cachedProduct = Product("oat", "Oat drink")
        val repository =
            TestProductSearchRepository { term ->
                val snapshot = ProductSearchSnapshot(term, listOf(cachedProduct), 1L)
                flowOf(
                    ProductSearchOutcome.Cached(snapshot),
                    ProductSearchOutcome.Failure(ProductSearchFailure.OFFLINE, snapshot),
                )
            }
        val viewModel =
            ProductSearchViewModel(
                repository,
                TestRecentSearchRepository(),
                EpochClock { 1L },
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
        composeRule.onNodeWithText("You’re offline. Showing saved catalogue results.").assertExists()
        composeRule.onNodeWithText("Retry").assertExists()

        scope.cancel()
    }

    @Test
    fun successfulEmptyResponseShowsNoResultsMessage() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val repository =
            TestProductSearchRepository { term ->
                flowOf(
                    ProductSearchOutcome.Cached(null),
                    ProductSearchOutcome.Success(ProductSearchSnapshot(term, emptyList(), 1L)),
                )
            }
        val viewModel =
            ProductSearchViewModel(
                repository,
                TestRecentSearchRepository(),
                EpochClock { 1L },
                scope,
            )
        composeRule.setContent {
            MaterialTheme {
                ProductSearchScreen(viewModel)
            }
        }
        composeRule.onNodeWithTag("product-search-input").performTextInput("unknown")
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No products found. Try a different search term.").assertExists()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
        scope.cancel()
    }
}
