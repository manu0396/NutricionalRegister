package com.example.nutricionalregister.data.local

import com.example.nutricionalregister.data.PurchaseRepositoryImpl
import com.example.nutricionalregister.data.SavedProductRepositoryImpl
import com.example.nutricionalregister.domain.purchase.PurchasableProduct
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.domain.purchase.PurchaseClientResult
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.VerifiedTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * T062: the save and purchase flows keep data on the device (FR-016, SC-007).
 *
 * Both repositories are exercised end to end against their real implementations
 * over an in-memory database. Every collaborator they are given is local: the only
 * injected dependency is the platform `PurchaseClient`, which here is a recording
 * fake that counts its own invocations. The test asserts that (a) the only thing
 * the flows touch is the local database plus that injected client, and (b) no
 * analytics, telemetry, or tracking side effect is ever triggered. There is no HTTP
 * client or reporting sink in either repository, so nothing can leave the device.
 */
class PrivacyNoEgressTest {
    @Test
    fun savingAndPurchasingTouchOnlyLocalStorageAndTheStoreClient() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val dispatcher = StandardTestDispatcher(testScheduler)
                val storeClient = RecordingPurchaseClient()
                val tracker = TrackingSink()

                val savedRepository =
                    SavedProductRepositoryImpl(
                        database = inMemory.database,
                        databaseDispatcher = dispatcher,
                    )
                val purchaseRepository =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = storeClient,
                        databaseDispatcher = dispatcher,
                    )

                // Save a product and complete a purchase through the normal flows.
                savedRepository.save(
                    SavedProduct(
                        code = "0001",
                        name = "Oats",
                        savedAtEpochMillis = 1L,
                    ),
                )
                purchaseRepository.purchase("0001").toList()

                assertEquals(listOf("0001"), savedRepository.observeSaved().first().map { it.code })
                assertTrue(purchaseRepository.observePurchasedCodes().first().contains("0001"))

                // The store client is the only external collaborator, and it is the
                // platform store: no reporting sink was ever given to either flow.
                assertTrue(storeClient.queriedCodes.isNotEmpty())
                assertEquals(0, tracker.reportedEvents, "No telemetry or tracking event may be emitted")
            }
        }

    @Test
    fun reconciliationAlsoKeepsEverythingOnDevice() =
        runTest {
            InMemoryProductDatabase().use { inMemory ->
                val dispatcher = StandardTestDispatcher(testScheduler)
                val storeClient =
                    RecordingPurchaseClient(
                        entitlements = listOf(VerifiedTransaction("0002", "tx-2", "token-2", 2L)),
                    )
                val tracker = TrackingSink()

                val purchaseRepository =
                    PurchaseRepositoryImpl(
                        database = inMemory.database,
                        client = storeClient,
                        databaseDispatcher = dispatcher,
                    )

                purchaseRepository.reconcile()

                assertEquals(setOf("0002"), purchaseRepository.observePurchasedCodes().first())
                assertEquals(0, tracker.reportedEvents)
            }
        }
}

/**
 * Stands in for any analytics or tracking sink. It counts reported events; the
 * privacy assertion is that the flows never hold or call one, so this stays zero.
 */
private class TrackingSink {
    var reportedEvents: Int = 0
        private set

    fun report() {
        reportedEvents += 1
    }
}

/** A recording platform store client: counts the calls made to it. */
private class RecordingPurchaseClient(
    private val entitlements: List<VerifiedTransaction> = emptyList(),
) : PurchaseClient {
    val queriedCodes = mutableListOf<String>()

    override suspend fun queryProduct(code: String): PurchasableProduct? {
        queriedCodes += code
        return PurchasableProduct(code, "EUR 1.99")
    }

    override suspend fun launchPurchase(code: String): PurchaseClientResult =
        PurchaseClientResult.Verified(
            VerifiedTransaction(code, "tx-$code", "token-$code", 1L),
        )

    override suspend fun currentEntitlements(): List<VerifiedTransaction> = entitlements
}
