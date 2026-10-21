package com.example.nutricionalregister.feature.savedproducts

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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseViewModelTest {
    @Test
    fun verifiedPurchaseMovesToPurchasedAndIsShownPurchased() =
        runTest {
            val record = purchaseRecord("0001", "tx-1")
            val viewModel = viewModel(FakePurchaseRepository(PurchaseOutcome.Success(record)))

            viewModel.observe("0001")
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isEligibleForPurchase)
            assertFalse(viewModel.state.value.isPurchased)

            viewModel.startPurchase("0001")
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isPurchased)
            viewModel.clear()
        }

    @Test
    fun cancellingLeavesNoRecord() =
        runTest {
            val viewModel = viewModel(FakePurchaseRepository(PurchaseOutcome.Cancelled))

            viewModel.observe("0001")
            advanceUntilIdle()
            viewModel.startPurchase("0001")
            advanceUntilIdle()

            assertEquals(PurchaseState.Cancelled, viewModel.state.value.state)
            assertFalse(viewModel.state.value.isPurchased)
            viewModel.clear()
        }

    @Test
    fun failingOffersRetryAndWritesNoRecord() =
        runTest {
            val viewModel = viewModel(FakePurchaseRepository(PurchaseOutcome.Failed(PurchaseFailure.DECLINED)))

            viewModel.observe("0001")
            advanceUntilIdle()
            viewModel.startPurchase("0001")
            advanceUntilIdle()

            assertEquals(PurchaseState.Failed(PurchaseFailure.DECLINED), viewModel.state.value.state)
            assertFalse(viewModel.state.value.isPurchased)
            viewModel.clear()
        }

    @Test
    fun aSecondStartWhilePendingIsRefused() =
        runTest {
            val viewModel = viewModel(PendingPurchaseRepository())

            viewModel.observe("0001")
            advanceUntilIdle()
            viewModel.startPurchase("0001")
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isPending)
            // a second start while pending must be refused (FR-018)
            viewModel.startPurchase("0001")
            assertTrue(viewModel.state.value.isPending)
            viewModel.clear()
        }

    @Test
    fun anIneligibleProductOffersNoPurchaseAction() =
        runTest {
            val viewModel =
                PurchaseViewModel(
                    FakePurchaseRepository(PurchaseOutcome.Cancelled),
                    CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                ) { null }

            viewModel.observe("9999")
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isEligibleForPurchase)
            viewModel.startPurchase("9999")
            advanceUntilIdle()
            // nothing started, so the state stays idle (FR-015)
            assertEquals(PurchaseState.Idle, viewModel.state.value.state)
            viewModel.clear()
        }

    @Test
    fun unavailableReasonRequiresGuidance() =
        runTest {
            val viewModel = viewModel(FakePurchaseRepository(PurchaseOutcome.Unavailable(PurchaseFailure.NOT_CONFIGURED)))

            viewModel.observe("0001")
            advanceUntilIdle()
            viewModel.startPurchase("0001")
            advanceUntilIdle()

            assertTrue(viewModel.state.value.requiresGuidance)
            viewModel.clear()
        }

    @Test
    fun restoreReconcilesAnExistingVerifiedTransaction() =
        runTest {
            val record = purchaseRecord("0001", "tx-restored")
            val viewModel =
                viewModel(FakePurchaseRepository(PurchaseOutcome.Cancelled, entitlements = listOf(record)))

            viewModel.observe("0001")
            advanceUntilIdle()
            viewModel.restore()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isPurchased)
            val purchased = viewModel.state.value.state as PurchaseState.Purchased
            assertEquals("tx-restored", purchased.record.transactionId)
            viewModel.clear()
        }

    private fun TestScope.viewModel(repository: PurchaseRepository): PurchaseViewModel =
        PurchaseViewModel(repository, CoroutineScope(UnconfinedTestDispatcher(testScheduler))) { "EUR 1.99" }

    private fun purchaseRecord(
        code: String,
        transactionId: String,
    ) = PurchaseRecord(
        productCode = code,
        transactionId = transactionId,
        status = "PURCHASED",
        completedAtEpochMillis = 1L,
    )
}
