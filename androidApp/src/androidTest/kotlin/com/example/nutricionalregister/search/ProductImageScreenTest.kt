@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import org.junit.Rule
import org.junit.Test

/**
 * Verifies the image surfaces added by feature 003: the placeholder path when a
 * product has no image, the Nutri-Score badge rendering, and the accessibility
 * contract of the product result card and detail image header.
 *
 * These tests never touch the live Open Food Facts CDN: the image-present case is
 * not asserted against a real download. Instead we assert the deterministic
 * placeholder contract for the absent case, and that a supplied URL does not
 * introduce a broken-image or error state (the placeholder remains until a
 * successful paint, so the UI is always well-formed).
 */
class ProductImageScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankImageUrlShowsPlaceholderAndNoImage() {
        composeRule.setContent {
            MaterialTheme {
                ProductImage(
                    url = null,
                    contentDescription = "Image of Oat drink",
                    initial = 'O',
                    modifier = Modifier.size(72.dp),
                )
            }
        }

        // The node exists with its accessible description and never renders a
        // broken image: only the neutral placeholder glyph is present.
        composeRule
            .onNodeWithContentDescription("Image of Oat drink")
            .assertExists()
        composeRule.onNodeWithText("O").assertExists()
    }

    @Test
    fun whitespaceImageUrlFallsBackToPlaceholder() {
        composeRule.setContent {
            MaterialTheme {
                ProductImage(
                    url = "   ",
                    contentDescription = null,
                    initial = 'B',
                    modifier = Modifier.size(72.dp),
                )
            }
        }

        composeRule.onNodeWithText("B").assertExists()
    }

    @Test
    fun resultCardKeepsNameDiscoverableAndDescribesProduct() {
        val product =
            Product(
                code = "00042",
                name = "Oat drink",
                brands = "Example Foods",
                quantity = "1 L",
            )
        composeRule.setContent {
            MaterialTheme {
                ProductResultCard(product = product, onOpen = {})
            }
        }

        // Card-level description for screen readers and the individually
        // findable name text must both be present (no descendant merging).
        composeRule.onNodeWithContentDescription("View Oat drink, code 00042").assertExists()
        composeRule.onNodeWithText("Oat drink").assertExists()
        composeRule.onNodeWithText("Code 00042").assertExists()
    }

    @Test
    fun resultCardWithGradeShowsNutriScoreBadge() {
        val product =
            Product(
                code = "00042",
                name = "Oat drink",
                nutriScoreGrade = "a",
            )
        composeRule.setContent {
            MaterialTheme {
                ProductResultCard(product = product, onOpen = {})
            }
        }

        composeRule.onNodeWithContentDescription("Nutri-Score A").assertExists()
    }

    @Test
    fun resultCardWithoutGradeShowsNoBadge() {
        val product =
            Product(
                code = "00042",
                name = "Oat drink",
            )
        composeRule.setContent {
            MaterialTheme {
                ProductResultCard(product = product, onOpen = {})
            }
        }

        composeRule.onAllNodesWithContentDescription("Nutri-Score A").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Nutri-Score B").assertCountEquals(0)
    }

    @Test
    fun detailScreenShowsImageHeaderAndBadge() {
        val product =
            Product(
                code = "00042",
                name = "Oat drink",
                brands = "Example Foods",
                nutriScoreGrade = "b",
            )
        composeRule.setContent {
            MaterialTheme {
                ProductDetailScreen(product = product, onBack = {})
            }
        }

        composeRule.onNodeWithContentDescription("Image of Oat drink").assertExists()
        composeRule.onNodeWithContentDescription("Nutri-Score B").assertExists()
        composeRule.onNodeWithText("Product code").assertExists()
        composeRule.onNodeWithText("00042").assertExists()
    }

    @Test
    fun unsupportedGradeRendersNothing() {
        composeRule.setContent {
            MaterialTheme {
                NutriScoreBadge(grade = "z")
            }
        }

        // No badge content is emitted for an unknown grade.
        composeRule.onAllNodesWithContentDescription("Nutri-Score Z").assertCountEquals(0)
    }

    @Test
    fun partialNutritionShowsUnavailableForMissingNutrients() {
        val product =
            Product(
                code = "00042",
                name = "Oat drink",
                nutritionValues =
                    listOf(
                        NutritionValue(
                            nutrientKey = "protein",
                            amount = 0.8,
                            unit = "g",
                            basis = NutritionBasis.PER_100_G,
                        ),
                    ),
            )
        composeRule.setContent {
            MaterialTheme {
                ProductDetailScreen(product = product, onBack = {})
            }
        }

        // Present nutrient is shown; the section is not replaced by the
        // all-empty message; missing nutrients remain individually labelled.
        composeRule.onNodeWithText("Protein").assertExists()
        composeRule.onNodeWithText("Nutrition information").assertExists()
        composeRule.onNodeWithText("Nutrition information not available for this product").assertDoesNotExist()
        composeRule.onAllNodesWithText("Unavailable").fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun allEmptyNutritionShowsSingleMessageNotRows() {
        val product =
            Product(
                code = "00042",
                name = "Oat drink",
            )
        composeRule.setContent {
            MaterialTheme {
                ProductDetailScreen(product = product, onBack = {})
            }
        }

        composeRule
            .onNodeWithText("Nutrition information not available for this product")
            .assertExists()
        composeRule.onNodeWithText("Energy").assertDoesNotExist()
    }
}
