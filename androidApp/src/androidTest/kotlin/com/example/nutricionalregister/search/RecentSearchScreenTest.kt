package com.example.nutricionalregister.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.RecentSearch
import com.example.nutricionalregister.feature.search.ProductSearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecentSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recentTermsAreOrderedAndCanBeDeletedIndividually() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val history = TestRecentSearchRepository()
        val repository =
            TestProductSearchRepository {
                flowOf(
                    ProductSearchOutcome.Cached(null),
                    ProductSearchOutcome.Failure(ProductSearchFailure.OFFLINE, null),
                )
            }
        val viewModel =
            ProductSearchViewModel(
                repository,
                history,
                EpochClock { 10L },
                scope,
            )
        history.seed(
            listOf(
                RecentSearch("beans", "Beans", 1L, 1L),
                RecentSearch("oats", "Oats", 2L, 2L),
            ),
        )
        composeRule.setContent {
            MaterialTheme {
                ProductSearchScreen(viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Oats").assertExists()
        val oatsY =
            composeRule
                .onAllNodesWithText("Oats")
                .fetchSemanticsNodes()
                .first()
                .boundsInRoot.top
        val beansY =
            composeRule
                .onAllNodesWithText("Beans")
                .fetchSemanticsNodes()
                .first()
                .boundsInRoot.top
        assertTrue(oatsY < beansY)
        composeRule.onNodeWithContentDescription("Delete recent search Beans").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Beans").assertDoesNotExist()
        composeRule.onNodeWithText("Oats").assertExists()
        scope.cancel()
    }
}
