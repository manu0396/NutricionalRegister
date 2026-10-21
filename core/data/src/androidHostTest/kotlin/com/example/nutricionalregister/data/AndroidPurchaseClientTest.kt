package com.example.nutricionalregister.data

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * T040: Play Billing result codes map to the shared purchase failures the UI
 * understands (FR-019). No live store call is made: only the pure mapping runs.
 */
class AndroidPurchaseClientTest {
    @Test
    fun userCancelledMapsToUserCancelled() {
        assertEquals(PurchaseFailure.USER_CANCELLED, billingResult(BillingClient.BillingResponseCode.USER_CANCELED).toFailure())
    }

    @Test
    fun networkProblemsMapToNetwork() {
        assertEquals(PurchaseFailure.NETWORK, billingResult(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE).toFailure())
        assertEquals(PurchaseFailure.NETWORK, billingResult(BillingClient.BillingResponseCode.NETWORK_ERROR).toFailure())
    }

    @Test
    fun missingConfigurationMapsToNotConfigured() {
        assertEquals(PurchaseFailure.NOT_CONFIGURED, billingResult(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE).toFailure())
        assertEquals(PurchaseFailure.NOT_CONFIGURED, billingResult(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE).toFailure())
    }

    @Test
    fun anythingElseIsAnUnknownFailure() {
        assertEquals(PurchaseFailure.UNKNOWN, billingResult(BillingClient.BillingResponseCode.ERROR).toFailure())
    }

    private fun billingResult(responseCode: Int): BillingResult = BillingResult.newBuilder().setResponseCode(responseCode).build()
}
