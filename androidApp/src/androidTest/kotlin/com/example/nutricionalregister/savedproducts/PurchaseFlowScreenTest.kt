package com.example.nutricionalregister.savedproducts

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import com.example.nutricionalregister.domain.purchase.PurchaseRecord
import com.example.nutricionalregister.domain.purchase.PurchaseState
import com.example.nutricionalregister.feature.savedproducts.PurchaseAttempt
import com.example.nutricionalregister.feature.savedproducts.PurchaseSheetState
import org.junit.Rule
import org.junit.Test

/**
 * T041: the purchase surface renders each state correctly, refuses a second start
 * while an attempt is pending, and shows guidance with a retry control when
 * payments are unavailable (FR-018, FR-019, SC-010, SC-011). It never collects
 * card details: the only control hands off to the platform sheet (FR-011, FR-014).
 */
class PurchaseFlowScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pendingDisablesThePurchaseControlSoNoSecondPurchaseCanStart() {
        var buyClicks = 0
        composeRule.setContent {
            MaterialTheme {
                PurchaseSection(
                    state = eligibleState(PurchaseState.Pending),
                    onBuy = { buyClicks += 1 },
                    onRetry = {},
                    onRestore = {},
                )
            }
        }

        composeRule.onNodeWithTag("purchase-action").assertIsNotEnabled()
        composeRule.onNodeWithTag("purchase-action").performClick()
        composeRule.runOnIdle { check(buyClicks == 0) }
        composeRule.onNodeWithTag("purchase-pending").assertIsDisplayed()
    }

    @Test
    fun idleOffersOnePurchaseActionShowingTheStorePrice() {
        composeRule.setContent {
            MaterialTheme {
                PurchaseSection(
                    state = eligibleState(PurchaseState.Idle),
                    onBuy = {},
                    onRetry = {},
                    onRestore = {},
                )
            }
        }

        composeRule.onNodeWithTag("purchase-action").assertIsEnabled()
        composeRule.onNodeWithText("Buy for EUR 1.99").assertIsDisplayed()
    }

    @Test
    fun purchasedShowsThePurchasedState() {
        composeRule.setContent {
            MaterialTheme {
                PurchaseSection(
                    state =
                        eligibleState(
                            PurchaseState.Purchased(
                                PurchaseRecord(
                                    productCode = "0001",
                                    transactionId = "tx",
                                    status = "PURCHASED",
                                    completedAtEpochMillis = 1L,
                                ),
                            ),
                        ),
                    onBuy = {},
                    onRetry = {},
                    onRestore = {},
                )
            }
        }

        composeRule.onNodeWithTag("purchase-purchased").assertIsDisplayed()
    }

    @Test
    fun cancelledExplainsThatNoChargeWasMade() {
        composeRule.setContent {
            MaterialTheme {
                PurchaseSection(
                    state = eligibleState(PurchaseState.Cancelled),
                    onBuy = {},
                    onRetry = {},
                    onRestore = {},
                )
            }
        }

        composeRule.onNodeWithTag("purchase-cancelled").assertIsDisplayed()
    }

    @Test
    fun failedOffersRetry() {
        var retries = 0
        composeRule.setContent {
            MaterialTheme {
                PurchaseSection(
                    state = eligibleState(PurchaseState.Failed(PurchaseFailure.DECLINED)),
                    onBuy = {},
                    onRetry = { retries += 1 },
                    onRestore = {},
                )
            }
        }

        composeRule.onNodeWithTag("purchase-guidance").assertIsDisplayed()
        composeRule.onNodeWithTag("purchase-retry").performClick()
        composeRule.runOnIdle { check(retries == 1) }
    }

    @Test
    fun unavailableShowsGuidanceWithRetryRatherThanFailingSilently() {
        var retries = 0
        composeRule.setContent {
            MaterialTheme {
                PurchaseSection(
                    state = eligibleState(PurchaseState.Unavailable(PurchaseFailure.NOT_CONFIGURED)),
                    onBuy = {},
                    onRetry = { retries += 1 },
                    onRestore = {},
                )
            }
        }

        composeRule.onNodeWithTag("purchase-guidance").assertIsDisplayed()
        composeRule.onNodeWithTag("purchase-retry").performClick()
        composeRule.runOnIdle { check(retries == 1) }
    }

    @Test
    fun aProductWithNoStoreOfferingShowsNoPurchaseAction() {
        composeRule.setContent {
            MaterialTheme {
                PurchaseSection(
                    state =
                        PurchaseSheetState(
                            isEligibleForPurchase = false,
                            state = PurchaseState.Idle,
                        ),
                    onBuy = {},
                    onRetry = {},
                    onRestore = {},
                )
            }
        }

        composeRule.onNodeWithTag("purchase-not-eligible").assertIsDisplayed()
        composeRule.onNodeWithTag("purchase-action").assertDoesNotExist()
    }

    @Test
    fun restoreIsAlwaysOfferedForStoreGuidelineCompliance() {
        var restores = 0
        composeRule.setContent {
            MaterialTheme {
                PurchaseSection(
                    state = eligibleState(PurchaseState.Idle),
                    onBuy = {},
                    onRetry = {},
                    onRestore = { restores += 1 },
                )
            }
        }

        composeRule.onNodeWithTag("restore-purchases").performClick()
        composeRule.runOnIdle { check(restores == 1) }
    }

    private fun eligibleState(state: PurchaseState): PurchaseSheetState =
        PurchaseSheetState(
            attempt =
                PurchaseAttempt(
                    productCode = "0001",
                    amountLabel = "EUR 1.99",
                    state = state,
                ),
            isEligibleForPurchase = true,
            state = state,
        )
}
