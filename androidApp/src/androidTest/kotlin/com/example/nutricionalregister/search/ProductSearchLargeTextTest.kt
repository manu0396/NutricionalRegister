package com.example.nutricionalregister.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ProductSearchLargeTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun essentialSearchActionRemainsAvailableAtLargeTextScale() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val viewModel =
            ProductSearchViewModel(
                TestProductSearchRepository {
                    flowOf(
                        ProductSearchOutcome.Cached(null),
                        ProductSearchOutcome.Failure(ProductSearchFailure.SERVICE, null),
                    )
                },
                TestRecentSearchRepository(),
                EpochClock { 1L },
                scope,
            )
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                MaterialTheme {
                    ProductSearchScreen(viewModel)
                }
            }
        }

        composeRule.onNodeWithText("Food name").assertExists()
        composeRule.onNodeWithText("Search").assertExists()
        composeRule.onNodeWithTag("product-search-input").performTextInput("unknown")
        composeRule.onNodeWithText("Search").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Retry").assertExists()
        composeRule.onNodeWithText("Edit search").assertExists()
        scope.cancel()
    }
}
