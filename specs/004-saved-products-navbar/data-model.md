# Data Model: Saved Products Bottom Navigation and In-App Purchase

**Feature**: `004-saved-products-navbar` | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

**Date**: 2026-10-20

## Scope of the model change

This feature adds two persisted entities and several domain contracts. There is
**no change to the existing `Product`, `ProductNutrition`, `SearchSnapshot*`, or
`RecentSearch` entities**; the change is additive and ships a forward SQLDelight
migration so existing cached search data survives the upgrade.

## New entity: SavedProduct

A product the user chose to keep, stored on-device so it survives restart and is
available offline (FR-001, FR-002, FR-005).

| Field | Type | Required | Notes |
|---|---|---|---|
| `code` | `String` | yes | Primary key. Catalogue product code; the same identity used by `Product`, so a product cannot be saved twice (FR-004). |
| `name` | `String` | yes | Non-blank display name, copied so the list renders offline. |
| `brands` | `String?` | no | Copied for display. |
| `quantity` | `String?` | no | Copied for display. |
| `nutriScoreGrade` | `String?` | no | Copied for display; unsupported grades are null. |
| `imageThumbnailUrl` | `String?` | no | Copied so the saved list can show a thumbnail when online; the generated placeholder is used otherwise. |
| `savedAtEpochMillis` | `Long` | yes | When the product was saved; orders the saved list most-recent-first. |

### Invariants

- `code` is non-blank and is the sole identity; saving the same code twice is an
  upsert, never a duplicate (FR-004).
- `name` is non-blank (reuses the `Product` invariant).
- Removing a saved product deletes only this row; it must not delete the
  catalogue `Product`/nutrition cache (mirrors the `RecentSearch` deletion
  policy).
- All fields are display-only copies; the list must render fully **offline**, so
  no field required for display may depend on a network fetch at read time.

## New entity: PurchaseRecord

The durable record that a product was successfully purchased (FR-010, FR-012,
FR-013).

| Field | Type | Required | Notes |
|---|---|---|---|
| `productCode` | `String` | yes | **Unique.** The purchased product; the purchased state is keyed on the product code because a product can only ever be bought once (FR-012). |
| `transactionId` | `String` | yes | Platform transaction identity (Play purchase token/order id; StoreKit `Transaction.id`), retained as metadata to acknowledge/finish the store transaction and for future server-side verification. Not the local uniqueness key. |
| `status` | `String` | yes | Persisted status; values map to `PurchaseState`. Only a verified completion is stored as purchased. |
| `purchaseToken` | `String?` | no | Platform receipt/token, retained for future server-side verification. Never a card credential. |
| `completedAtEpochMillis` | `Long` | yes | When the verified transaction completed. |

### Invariants

- `productCode` is **unique** (primary key); inserting a second record for the
  same product is rejected/ignored, so one product can never produce two purchase
  records and a repeat confirmation is idempotent (FR-012).
- A record is only written from a **platform-verified** transaction; unverified
  or failed results never create or update a record (FR-011).
- Reconcile-on-launch re-queries the platform store, which is the source of
  truth, and aligns local records to it: a product the store reports as owned is
  shown as purchased, and one it does not report stays unpaid (FR-013).
- The record stores **no card, bank, or credential data** (FR-014).

## New entity: PurchaseAttempt (in-memory)

A single user-initiated purchase flow; not persisted, but its states drive the UI
(FR-009, FR-011). **Implementation**: this entity is realized by the purchase
presentation state in `androidApp/.../savedproducts/PurchaseSheetState.kt`
(tasks T042 and T053); there is no separate persisted type for it.

| Field | Type | Required | Notes |
|---|---|---|---|
| `productCode` | `String` | yes | The product being purchased. |
| `amountLabel` | `String?` | no | The store's own formatted price string, shown verbatim; never parsed into a project currency type. |
| `state` | `PurchaseState` | yes | One of the states below. |

### State transitions

```text
Idle
 └─(start purchase)→ Pending
Pending
 ├─(verified transaction)→ Purchased   [writes PurchaseRecord]
 ├─(user cancelled)→ Cancelled         [no record]
 ├─(declined / failed)→ Failed         [no record, retry offered]
 └─(interrupted: app closed / no connection)→ ReconcileOnLaunch
ReconcileOnLaunch
 ├─(platform reports verified transaction)→ Purchased   [writes record if absent]
 └─(platform reports nothing outstanding)→ Idle
```

- `Purchased` is terminal for a given product **unless** reconciliation discovers
  an earlier completion that had not been recorded, in which case the record is
  written once (unique `productCode`).
- `Cancelled` and `Failed` never write a record.

## New domain contracts (core:domain, common source set)

| Contract | Responsibility |
|---|---|
| `SavedProductRepository` | `save(product)`, `remove(code)`, `observeSaved(): Flow<List<SavedProduct>>`, `isSaved(code): Flow<Boolean>` |
| `PurchaseRepository` | `purchase(attempt): Flow<PurchaseOutcome>`, `observePurchase(code): Flow<PurchaseRecord?>`, `reconcile()`, `observePurchasedCodes(): Flow<Set<String>>` |
| `PurchaseClient` | Platform abstraction: `queryProduct(code)`, `launchPurchase(code)`, `currentEntitlements()`. Implemented per platform; hides Play Billing and StoreKit 2. |
| `PurchaseState` | `Idle`, `Pending`, `Purchased`, `Cancelled`, `Failed(reason)`, `Unavailable` |
| `PurchaseOutcome` | Sealed results: `Success(record)`, `Cancelled`, `Failed(reason)`, `Unavailable(reason)` |
| `PurchaseFailure` | `USER_CANCELLED`, `DECLINED`, `NETWORK`, `NOT_CONFIGURED`, `VERIFICATION_FAILED`, `UNKNOWN` |

`core:domain` remains free of any project dependency; none of these types
reference Android or iOS APIs.

## Persistence and migration

- **Additive migration** on the existing `product-search.db`: create
  `SavedProduct` and `PurchaseRecord`. No existing table or column changes, so no
  data is lost.
- `SavedProduct` is keyed by product code; `PurchaseRecord` is likewise keyed by
  product code (a product can be bought only once), with `transactionId` retained
  as store-facing metadata.
- Saved products and purchase records are **never** sent to analytics or tracking
  (FR-016).

## Requirement traceability

| Requirement | Model / surface |
|---|---|
| FR-001 save from anywhere, on-device | `SavedProduct` + `SavedProductRepository.save` |
| FR-002 list + open saved | `SavedProductRepository.observeSaved` → Saved products destination |
| FR-003 remove, no reappearance | `SavedProductRepository.remove` (deletes only the saved row) |
| FR-004 no duplicates | `SavedProduct.code` primary key (upsert) |
| FR-005 bottom bar, current indicated | `ui/NutriLogBottomBar.kt` + navigation destination state |
| FR-006 Diary placeholder | `DiaryPlaceholderScreen` |
| FR-007 return to origin | navigation back stack (ADR-0015) |
| FR-008 bar clears content/system area | `Scaffold` insets in the app shell |
| FR-009 platform payment sheet | `PurchaseClient.launchPurchase` (Play Billing / StoreKit 2) + purchase UI on both platforms (Android `PurchaseSection`, iOS `ContentView`) |
| FR-010 confirm + persist purchase | `PurchaseRecord` + `PurchaseRepository` + purchase UI |
| FR-011 cancel/fail → no record | `PurchaseState.Cancelled`/`Failed` → no `PurchaseRecord` |
| FR-012 record once | unique `PurchaseRecord.productCode` (idempotent upsert) |
| FR-013 reconcile on launch | `PurchaseRepository.reconcile()` |
| FR-014 native mechanism, no credentials | `PurchaseClient` per platform (Play Billing on `androidMain`, StoreKit 2 in the iOS app); Android and iOS purchase UI surfaces; `PurchaseRecord` stores no credentials |
| FR-015 ineligible products | `PurchaseClient.queryProduct` returns unavailable → no purchase action |
| FR-016 on-device only | no analytics/tracking; local tables only; asserted by a privacy test (tasks T062) |
| FR-017 accessibility | 48 dp targets, accessible names in bar and purchase UI |
