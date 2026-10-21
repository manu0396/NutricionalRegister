package com.example.nutricionalregister.savedproducts

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.nutricionalregister.feature.savedproducts.SavedProductsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * T032: the saved list renders (including entries with no image), each opens for
 * detail, removal removes it, and the empty list shows the explicit empty state
 * whose Search shortcut is offered (FR-002, FR-003, SC-009).
 */
class SavedProductsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun savedProductsRenderAndOpenForDetail() {
        val repository = TestSavedProductRepository()
        runBlocking {
            repository.save(saved("0001", "Oats", image = "https://example.test/oats.png"))
            repository.save(saved("0002", "Beans", image = null))
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val viewModel = SavedProductsViewModel(repository, testClock(200L), scope)

        composeRule.setContent {
            MaterialTheme {
                SavedProductsScreen(viewModel = viewModel, onNavigateToSearch = {})
            }
        }

        // both entries render; the image-less one still shows (offline placeholder)
        composeRule.onNodeWithText("Oats").assertIsDisplayed()
        composeRule.onNodeWithText("Beans").assertIsDisplayed()

        // opening one shows its detail with a removal control
        composeRule.onNodeWithContentDescription("View Oats, code 0001").performClick()
        composeRule.onNodeWithTag("save-product-control").assertIsDisplayed()
        composeRule.onNodeWithText("Remove from saved").assertIsDisplayed()
        scope.cancel()
    }

    @Test
    fun removingAnEntryTakesItOutOfTheList() {
        val repository = TestSavedProductRepository()
        runBlocking { repository.save(saved("0001", "Oats", image = null)) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val viewModel = SavedProductsViewModel(repository, testClock(200L), scope)

        composeRule.setContent {
            MaterialTheme {
                SavedProductsScreen(viewModel = viewModel, onNavigateToSearch = {})
            }
        }

        composeRule.onNodeWithTag("remove-saved-0001").performClick()
        composeRule.waitForIdle()

        // the list is now empty, so the explicit empty state shows (FR-002, SC-009)
        composeRule.onNodeWithTag("saved-products-empty").assertIsDisplayed()
        composeRule.onNodeWithTag("saved-products-empty-search").assertIsDisplayed()
        scope.cancel()
    }

    @Test
    fun emptyListShowsTheExplicitEmptyStateWithASearchShortcut() {
        val repository = TestSavedProductRepository()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val viewModel = SavedProductsViewModel(repository, testClock(200L), scope)
        var navigatedToSearch = false

        composeRule.setContent {
            MaterialTheme {
                SavedProductsScreen(viewModel = viewModel, onNavigateToSearch = { navigatedToSearch = true })
            }
        }

        composeRule.onNodeWithText("No saved products yet").assertIsDisplayed()
        composeRule.onNodeWithTag("saved-products-empty-search").performClick()
        composeRule.runOnIdle { check(navigatedToSearch) }
        scope.cancel()
    }

    private fun saved(
        code: String,
        name: String,
        image: String?,
    ) = com.example.nutricionalregister.domain.purchase.SavedProduct(
        code = code,
        name = name,
        imageThumbnailUrl = image,
        savedAtEpochMillis = code.hashCode().toLong(),
    )
}
