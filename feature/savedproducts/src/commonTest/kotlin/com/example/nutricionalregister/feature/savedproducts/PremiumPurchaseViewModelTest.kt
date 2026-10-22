package com.example.nutricionalregister.feature.savedproducts

import com.example.nutricionalregister.domain.premium.PremiumProduct
import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import com.example.nutricionalregister.domain.purchase.PurchaseOutcome
import com.example.nutricionalregister.domain.purchase.PurchaseRecord
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.domain.purchase.PurchaseState
import com.example.nutricionalregister.feature.savedproducts.testing.FakePurchaseRepository
import com.example.nutricionalregister.feature.savedproducts.testing.PendingPurchaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The purchase flow of the single premium unlock (ADR-0018, FR-017, FR-032).
 *
 * The tests use the hand-written fakes only: the shared modules carry no mocking
 * framework, so every store interaction is scripted by hand.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PremiumPurchaseViewModelTest {
    @Test
    fun refreshReportsTheStorePriceLabelAndEligibility() =
        runTest {
            val viewModel = viewModel(FakePurchaseRepository(PurchaseOutcome.Cancelled))

            viewModel.refresh()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isEligibleForPurchase)
            assertEquals("EUR 1.99", viewModel.state.value.amountLabel)
            assertFalse(viewModel.state.value.isUnlocked)
            assertEquals(PurchaseState.Idle, viewModel.state.value.state)
            viewModel.clear()
        }

    @Test
    fun refreshAsksTheStoreAboutTheSinglePremiumProduct() =
        runTest {
            val queried = mutableListOf<String>()
            val viewModel =
                PremiumPurchaseViewModel(
                    purchaseRepository = FakePurchaseRepository(PurchaseOutcome.Cancelled),
                    scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                    queryProduct = { code ->
                        queried += code
                        "EUR 1.99"
                    },
                )

            viewModel.refresh()
            advanceUntilIdle()

            // the store is asked about the one premium product, never about a food
            assertEquals(listOf(PremiumProduct.CODE), queried)
            assertEquals("nutrilog_pro", PremiumProduct.CODE)
            viewModel.clear()
        }

    @Test
    fun refreshOnAProductTheStoreDoesNotOfferIsNotEligibleAndShowsNoPrice() =
        runTest {
            val viewModel = viewModel(FakePurchaseRepository(PurchaseOutcome.Cancelled), priceLabel = null)

            viewModel.refresh()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isEligibleForPurchase)
            assertNull(viewModel.state.value.amountLabel)
            assertFalse(viewModel.state.value.isUnlocked)
            viewModel.clear()
        }

    @Test
    fun aVerifiedPurchaseUnlocksThePremiumFeature() =
        runTest {
            val record = premiumRecord("tx-1")
            val repository = FakePurchaseRepository(PurchaseOutcome.Success(record))
            val viewModel = viewModel(repository)

            viewModel.refresh()
            advanceUntilIdle()
            viewModel.startPurchase()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isUnlocked)
            assertEquals(PurchaseState.Purchased(record), viewModel.state.value.state)
            assertEquals(listOf(PremiumProduct.CODE), repository.purchaseCalls)
            viewModel.clear()
        }

    @Test
    fun startPurchaseDoesNothingWhenTheProductIsNotEligible() =
        runTest {
            val repository = FakePurchaseRepository(PurchaseOutcome.Success(premiumRecord("tx-1")))
            val viewModel = viewModel(repository, priceLabel = null)

            viewModel.refresh()
            advanceUntilIdle()
            viewModel.startPurchase()
            advanceUntilIdle()

            // nothing was started, so the state stays idle (FR-015)
            assertTrue(repository.purchaseCalls.isEmpty())
            assertFalse(viewModel.state.value.isUnlocked)
            assertEquals(PurchaseState.Idle, viewModel.state.value.state)
            viewModel.clear()
        }

    @Test
    fun startPurchaseDoesNothingWhileAnAttemptIsAlreadyPending() =
        runTest {
            val repository = PendingPurchaseRepository()
            val viewModel = viewModel(repository)

            viewModel.refresh()
            advanceUntilIdle()
            viewModel.startPurchase()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isPending)
            assertEquals(PurchaseState.Pending, viewModel.state.value.state)

            // a second start while pending must be refused (FR-018)
            viewModel.startPurchase()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isPending)
            assertEquals(1, repository.purchaseCalls.size)
            viewModel.clear()
        }

    @Test
    fun aCancelledAttemptLeavesTheFeatureLockedAndReportsTheCancellation() =
        runTest {
            val viewModel = viewModel(FakePurchaseRepository(PurchaseOutcome.Cancelled))

            viewModel.refresh()
            advanceUntilIdle()
            viewModel.startPurchase()
            advanceUntilIdle()

            assertEquals(PurchaseState.Cancelled, viewModel.state.value.state)
            assertFalse(viewModel.state.value.isUnlocked)
            assertFalse(viewModel.state.value.isPending)
            viewModel.clear()
        }

    @Test
    fun aFailedAttemptReportsTheFailureAndCanBeRetried() =
        runTest {
            val repository =
                FakePurchaseRepository(
                    outcome = PurchaseOutcome.Success(premiumRecord("tx-2")),
                    scriptedOutcomes = listOf(PurchaseOutcome.Failed(PurchaseFailure.DECLINED)),
                )
            val viewModel = viewModel(repository)

            viewModel.refresh()
            advanceUntilIdle()
            viewModel.startPurchase()
            advanceUntilIdle()

            assertEquals(PurchaseState.Failed(PurchaseFailure.DECLINED), viewModel.state.value.state)
            assertFalse(viewModel.state.value.isUnlocked)
            assertFalse(viewModel.state.value.requiresGuidance)

            // the retry reloads the offering and clears the failure
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(PurchaseState.Idle, viewModel.state.value.state)

            viewModel.startPurchase()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isUnlocked)
            assertEquals(2, repository.purchaseCalls.size)
            viewModel.clear()
        }

    @Test
    fun anUnconfiguredStoreAsksForGuidanceInsteadOfFailingSilently() =
        runTest {
            val viewModel =
                viewModel(FakePurchaseRepository(PurchaseOutcome.Unavailable(PurchaseFailure.NOT_CONFIGURED)))

            viewModel.refresh()
            advanceUntilIdle()
            viewModel.startPurchase()
            advanceUntilIdle()

            assertEquals(PurchaseState.Unavailable(PurchaseFailure.NOT_CONFIGURED), viewModel.state.value.state)
            assertTrue(viewModel.state.value.requiresGuidance)
            assertFalse(viewModel.state.value.isUnlocked)
            viewModel.clear()
        }

    @Test
    fun restoreReconcilesAndUnlocksTheFeatureWhenTheStoreReportsAnOwnedTransaction() =
        runTest {
            val record = premiumRecord("tx-restored")
            val repository = FakePurchaseRepository(PurchaseOutcome.Cancelled, entitlements = listOf(record))
            val viewModel = viewModel(repository)

            viewModel.refresh()
            advanceUntilIdle()
            assertFalse(viewModel.state.value.isUnlocked)

            viewModel.restore()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isUnlocked)
            val purchased = viewModel.state.value.state as PurchaseState.Purchased
            assertEquals("tx-restored", purchased.record.transactionId)
            assertEquals(1, repository.reconcileCalls)
            viewModel.clear()
        }

    @Test
    fun aRecordOfAnotherProductDoesNotUnlockThePremiumFeature() =
        runTest {
            val other =
                PurchaseRecord(
                    productCode = "oats_1kg",
                    transactionId = "tx-other",
                    status = "PURCHASED",
                    completedAtEpochMillis = 1L,
                )
            val viewModel = viewModel(FakePurchaseRepository(PurchaseOutcome.Cancelled, initialRecord = other))

            viewModel.refresh()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isUnlocked)
            assertEquals(PurchaseState.Idle, viewModel.state.value.state)
            viewModel.clear()
        }

    private fun TestScope.viewModel(
        repository: PurchaseRepository,
        priceLabel: String? = "EUR 1.99",
    ): PremiumPurchaseViewModel =
        PremiumPurchaseViewModel(
            purchaseRepository = repository,
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            queryProduct = { priceLabel },
        )

    private fun premiumRecord(transactionId: String): PurchaseRecord =
        PurchaseRecord(
            productCode = PremiumProduct.CODE,
            transactionId = transactionId,
            status = "PURCHASED",
            completedAtEpochMillis = 1L,
        )
}
