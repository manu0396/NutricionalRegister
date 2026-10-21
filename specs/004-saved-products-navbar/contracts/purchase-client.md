# Platform Payment Client Contract

**Status**: Design contract. Platform versions researched on 2026-10-20; store-side
paid-product configuration and payment accounts are **prerequisites that this
document does not assert are satisfied**. This is not permission to charge
customers.
**Plan**: [../plan.md](../plan.md) | **ADRs**: [ADR-0013](../../../docs/adr/0013-swift-storekit2-bridge.md), [ADR-0014](../../../docs/adr/0014-google-play-billing.md)

## Purpose

Define the single abstraction the shared code uses to take a real in-app
purchase, so that Google Play Billing (Android) and StoreKit 2 (iOS) are hidden
behind one project-owned contract and the store interaction stays in each
platform's source set.

## Contract (shared, `core:domain`)

```kotlin
interface PurchaseClient {
    /** Returns availability + the store's own price label for a product, or null. */
    suspend fun queryProduct(code: String): PurchasableProduct?

    /** Presents the platform's payment sheet. Never collects credentials itself. */
    suspend fun launchPurchase(code: String): PurchaseClientResult

    /** Verified outstanding/entitlement transactions, for reconciliation (FR-013). */
    suspend fun currentEntitlements(): List<VerifiedTransaction>
}

data class PurchasableProduct(val code: String, val amountLabel: String)

sealed interface PurchaseClientResult {
    data class Verified(val transaction: VerifiedTransaction) : PurchaseClientResult
    data object UserCancelled : PurchaseClientResult
    data class Failed(val reason: PurchaseFailure) : PurchaseClientResult
    data object Unavailable : PurchaseClientResult
}

data class VerifiedTransaction(
    val productCode: String,
    val transactionId: String,   // store-facing metadata: Play token/orderId | StoreKit Transaction.id
    val purchaseToken: String?,
    val completedAtEpochMillis: Long,
)
```

`PurchaseFailure` values: `USER_CANCELLED`, `DECLINED`, `NETWORK`,
`NOT_CONFIGURED`, `VERIFICATION_FAILED`, `UNKNOWN`.

## Platform obligations

### Google Play Billing (Android, `core:data` `androidMain`)

| Obligation | Detail |
|---|---|
| Version | `com.android.billingclient:billing-ktx` **9.1.0** (Play policy requires ≥8 in 2026; 9.1.0 is safe to 2028) |
| Scope | Declared only in the `androidMain` source set; never in common code |
| Activity | `launchBillingFlow` needs an `Activity`; it is supplied by the `androidApp` composition root, since the project has no DI framework |
| Ack/consume | Acknowledge (or consume) a completed purchase; unacknowledged purchases auto-refund after 3 days |
| Reconcile | Re-query purchases on foreground/launch to satisfy FR-013 |
| Verification | Use Billing's local signature verification; persist `purchaseToken` for a future backend. Backend verification is recommended for production but is **out of scope** for this feature. |
| Config errors | A missing paid-product configuration maps to `Failed(NOT_CONFIGURED)`, never a silent failure |

### StoreKit 2 (iOS, `iosApp` **Swift**)

| Obligation | Detail |
|---|---|
| Location | **Swift only.** Kotlin/Native cannot import pure-Swift StoreKit 2 (cinterop binds C/ObjC only). ADR-0013 records this. |
| Load | `Product.products(for:)` |
| Purchase | `product.purchase()` → `Product.PurchaseResult` |
| Verify | Only `.success(.verified(transaction))` grants a purchase; `.unverified` → `Failed(VERIFICATION_FAILED)` |
| Finish | `transaction.finish()` after granting (iOS has no server-side finish) |
| Restore | `Transaction.currentEntitlements` + explicit `AppStore.sync()` (App Store guideline requires a user-triggered restore affordance) |
| Cancelled | `.userCancelled` → `UserCancelled` |

## Client obligations (both platforms)

1. **Never collect credentials.** All payment data entry happens in the platform
   sheet (FR-014).
2. **Grant only on verified results.** Unverified, cancelled, or failed results
   never create a purchase record (FR-011, FR-012).
3. **Idempotent recording.** Purchases are keyed on the product code (a product
   can be bought only once), so a repeat confirmation for the same product is an
   idempotent upsert keyed on `product_code` and produces at most one record
   (FR-012). The store's `transactionId` is retained as metadata for
   acknowledging the store transaction and for future server-side verification;
   it is not the local uniqueness key.
4. **Reconcile on launch.** Outstanding verified transactions are recorded so a
   completed payment is never shown unpaid (FR-013).
5. **Eligibility.** A product the store does not offer yields `Unavailable` and
   no purchase action is shown (FR-015).
6. **In-progress state.** While `launchPurchase` is suspended the caller treats
   the attempt as pending and must not start a second purchase for the same
   product; the UI disables the purchase control for the duration (FR-018).
7. **Unavailable / not configured.** A store that is signed out, unreachable, or
   missing its paid-product configuration yields `Unavailable` (or
   `Failed(NOT_CONFIGURED)`), which the UI turns into an explicit "payments
   unavailable" state with guidance and a retry — never a silent failure (FR-019).
8. **No test against live stores.** Automated tests use fakes; real store
   interactions are exercised only against each platform's **test** environment.

## Licence and cost

- Google Play Billing: Apache-2.0, no fee beyond the store's standard revenue
  share on actual sales.
- StoreKit 2: first-party Apple framework, no licence cost.
- No third-party billing backend or revenue-share fee is introduced (contrast
  RevenueCat, rejected in [research.md](../research.md) §1).

## Exit strategy

Replace each platform's `PurchaseClient` implementation with a library-backed one
(e.g. RevenueCat) without touching the shared contracts, the repositories, or the
UI state: only the two `actual` implementations change. No persisted schema
depends on the platform API, so a migration is not required.
