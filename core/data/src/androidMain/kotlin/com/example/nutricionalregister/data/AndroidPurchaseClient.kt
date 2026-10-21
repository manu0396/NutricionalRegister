package com.example.nutricionalregister.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.nutricionalregister.domain.purchase.PurchasableProduct
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import com.example.nutricionalregister.domain.purchase.PurchaseClientResult
import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import com.example.nutricionalregister.domain.purchase.VerifiedTransaction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Android [PurchaseClient] backed by Google Play Billing (ADR-0014).
 *
 * This is the real store integration: it queries the store's own product details
 * (including the store's formatted price label), presents Google Play's payment
 * sheet via `launchBillingFlow`, and reads verified outstanding purchases for
 * reconciliation. It never collects payment credentials itself; all payment data
 * entry happens in the Play sheet (FR-014).
 *
 * Every Billing call is asynchronous through a `BillingClientStateListener` /
 * `PurchasesResponseListener`; each is bridged to a suspend function so the shared
 * repository can drive the flow imperatively. An unconfigured or unreachable store
 * maps to [PurchaseClientResult.Unavailable] / [PurchaseFailure.NOT_CONFIGURED] so
 * the UI can show the explicit "payments unavailable" state (FR-019).
 */
public class AndroidPurchaseClient(
    private val context: Context,
    /** Supplies the foreground [Activity] that hosts the Play payment sheet. */
    private val activityProvider: () -> Activity?,
) : PurchaseClient {
    private val client: BillingClient =
        BillingClient
            .newBuilder(context)
            .setListener { result, purchases ->
                onPurchasesUpdated(result, purchases)
            }.enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
            ).build()

    /** The in-flight purchase attempt awaiting the Play sheet's callback, if any. */
    private var pendingPurchase: CompletableDeferred<PurchaseClientResult>? = null

    override suspend fun queryProduct(code: String): PurchasableProduct? {
        val connected = ensureConnected()
        if (!connected) return null
        val productDetails = queryProductDetails(code) ?: return null
        val oneTime = productDetails.oneTimePurchaseOfferDetails ?: return null
        return PurchasableProduct(code = code, amountLabel = oneTime.formattedPrice)
    }

    override suspend fun launchPurchase(code: String): PurchaseClientResult {
        if (!ensureConnected()) return PurchaseClientResult.Unavailable
        val activity = activityProvider() ?: return PurchaseClientResult.Unavailable
        val productDetails = queryProductDetails(code) ?: return PurchaseClientResult.Unavailable
        val offerToken = productDetails.oneTimePurchaseOfferDetails?.offerToken
        val paramsBuilder =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(productDetails)
        if (offerToken != null) paramsBuilder.setOfferToken(offerToken)
        val flowParams =
            BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                .build()

        val deferred = CompletableDeferred<PurchaseClientResult>()
        pendingPurchase = deferred
        val launchResult = client.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            pendingPurchase = null
            return PurchaseClientResult.Failed(launchResult.toFailure())
        }
        // The listener completes the deferred; time out defensively so a lost
        // callback can never wedge the purchase control forever.
        return withTimeoutOrNull(PURCHASE_TIMEOUT_MILLIS) { deferred.await() }
            ?: PurchaseClientResult.Failed(PurchaseFailure.UNKNOWN)
    }

    override suspend fun currentEntitlements(): List<VerifiedTransaction> {
        if (!ensureConnected()) return emptyList()
        val purchases = queryPurchases()
        return purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .flatMap { purchase ->
                purchase.products.map { productCode ->
                    VerifiedTransaction(
                        productCode = productCode,
                        transactionId = purchase.orderId ?: purchase.purchaseToken,
                        purchaseToken = purchase.purchaseToken,
                        completedAtEpochMillis = purchase.purchaseTime,
                    )
                }
            }
    }

    /** Releases the underlying Billing client; called by the composition root. */
    public fun close() {
        client.endConnection()
    }

    private fun onPurchasesUpdated(
        result: BillingResult,
        purchases: List<Purchase>?,
    ) {
        val deferred = pendingPurchase ?: return
        pendingPurchase = null
        val verified =
            purchases
                ?.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        when {
            verified != null -> {
                val record =
                    VerifiedTransaction(
                        productCode = verified.products.firstOrNull().orEmpty(),
                        transactionId = verified.orderId ?: verified.purchaseToken,
                        purchaseToken = verified.purchaseToken,
                        completedAtEpochMillis = verified.purchaseTime,
                    )
                deferred.complete(PurchaseClientResult.Verified(record))
            }
            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED ->
                deferred.complete(PurchaseClientResult.UserCancelled)
            else -> deferred.complete(PurchaseClientResult.Failed(result.toFailure()))
        }
    }

    private suspend fun ensureConnected(): Boolean {
        if (client.isReady) return true
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine { continuation ->
                    client.startConnection(
                        object : BillingClientStateListener {
                            override fun onBillingSetupFinished(result: BillingResult) {
                                if (continuation.isActive) {
                                    continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                                }
                            }

                            override fun onBillingServiceDisconnected() {
                                // Reconnection is attempted on the next call.
                            }
                        },
                    )
                }
            } ?: false
        }
    }

    private suspend fun queryProductDetails(code: String): ProductDetails? =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val params =
                    QueryProductDetailsParams
                        .newBuilder()
                        .setProductList(
                            listOf(
                                QueryProductDetailsParams.Product
                                    .newBuilder()
                                    .setProductId(code)
                                    .setProductType(BillingClient.ProductType.INAPP)
                                    .build(),
                            ),
                        ).build()
                client.queryProductDetailsAsync(params) { result, detailsResult ->
                    if (continuation.isActive) {
                        val product =
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                detailsResult.productDetailsList.firstOrNull { it.productId == code }
                            } else {
                                null
                            }
                        continuation.resume(product)
                    }
                }
            }
        }

    private suspend fun queryPurchases(): List<Purchase> =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val params =
                    QueryPurchasesParams
                        .newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                client.queryPurchasesAsync(params) { result, purchases ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases else emptyList(),
                        )
                    }
                }
            }
        }
}

/**
 * Maps a Play Billing result code to the shared [PurchaseFailure] (FR-019).
 *
 * Kept internal and free of any Billing client so the mapping is unit-testable
 * without a live store (T040). A missing billing configuration maps to
 * `NOT_CONFIGURED`, which the UI turns into the explicit "payments unavailable"
 * guidance with a retry control.
 */
internal fun BillingResult.toFailure(): PurchaseFailure =
    when (responseCode) {
        BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseFailure.USER_CANCELLED
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.NETWORK_ERROR,
        -> PurchaseFailure.NETWORK
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
            PurchaseFailure.NOT_CONFIGURED
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
            PurchaseFailure.NOT_CONFIGURED
        else -> PurchaseFailure.UNKNOWN
    }

private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val PURCHASE_TIMEOUT_MILLIS = 5 * 60 * 1_000L
