# Research: Saved Products Bottom Navigation and In-App Purchase

**Feature**: `004-saved-products-navbar` | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

**Date**: 2026-10-20

This document records the investigation behind the plan. Three decisions needed
external evidence: how to do multiplatform navigation, how to take real in-app
payments on both platforms, and whether a backend is required. Each resolved
question becomes an ADR.

## 1. In-app purchase: library or direct platform APIs?

### Question

The spec requires **real payments with genuine charges** on both Android and iOS.
Should this use a third-party Kotlin Multiplatform billing library, or the
platform APIs directly?

### Findings (verified 2026-10-20)

- **Google Play Billing** latest is **9.1.0** (confirmed on
  `dl.google.com/dl/android/maven2`). Google Play policy requires **Billing 8 or
  later** for new apps and updates from **31 Aug 2026**, with an extension to
  **1 Nov 2026**; Billing 7 and below are deprecated. Shipping 9.1.0 is
  forward-safe until 2028. Billing 8.1.0 raised `minSdk` to 23 and supports
  Kotlin 2.2+; the project's `minSdk 24` and Kotlin 2.4.21 are compatible.
- **StoreKit 2 is pure Swift.** Kotlin's own documentation states that a Swift
  library can be used from Kotlin only if its API is exported to Objective-C
  with `@objc`, and that **"pure Swift modules are not yet supported."** StoreKit
  2's `Product`, `Transaction` and `VerificationResult` types are pure Swift, so
  **cinterop cannot bind them** and Kotlin/Native cannot call StoreKit 2
  directly. The new *Swift export* feature goes the other way (Kotlin→Swift) and
  does not help. StoreKit 2 must therefore be wrapped in **Swift** and exposed to
  Kotlin.
- **RevenueCat `purchases-kmp` 3.14.0** (MIT, exists on Maven Central) is the
  only mature KMP billing library, but it **mandatorily requires RevenueCat's
  hosted backend** and charges **1% of monthly tracked revenue above $2,500**.
  Its own README lists Kotlin 2.3.20+ as a minimum; compatibility with the
  project's Kotlin 2.4.21 was **not verifiable** without a spike.
- **`io.github.aditya-gupta99:inAppPurchase-kmp` 1.0.43** (Apache-2.0) is a thin
  client-side wrapper with **no** backend requirement, but it is a small
  single-author project (13 stars, 20 commits), built against an older Kotlin,
  and its README states it wraps **Google Play Billing v6** — which is
  **deprecated under 2026 Play policy**.

### Options considered

| Option | Verdict | Reason |
|---|---|---|
| Direct platform APIs: Play Billing (`androidMain`) + StoreKit 2 (Swift bridge) | **Chosen** | No backend or fee, no deprecated Billing, shared purchase-state logic stays in common code; matches the project's existing no-DI-library, thin-wrapper philosophy |
| RevenueCat `purchases-kmp` 3.14.0 | Rejected | Requires a third-party backend as the entitlement source of truth and adds a revenue-share fee; Kotlin 2.4.21 compatibility unverified |
| `inAppPurchase-kmp` 1.0.43 | Rejected | Immature and pins deprecated Google Play Billing v6, a Play-policy risk |
| No purchase feature | Rejected | Explicitly required by the spec |

**Decision**: implement the two platform purchase clients directly, behind a
small project-owned `PurchaseClient` abstraction so the exit strategy (adopting a
library later) stays cheap. The Android dependency and the Swift bridge are
ratified in **ADR-0014** and **ADR-0013**.

## 2. Is a backend required to verify purchases?

### Question

Do Google Play and StoreKit require a server to verify a purchase, or is
client-side verification sufficient?

### Findings (verified 2026-10-20)

- **Google Play.** Client-side acknowledge/consume is the documented path for a
  client-only app, and Billing verifies the purchase signature locally. However,
  Google's security guidance **strongly recommends** sending the purchase token
  to a backend and using the Play Developer API, because only a backend can
  confirm a token has not been refunded, voided, or reused (Voided Purchases API,
  RTDN).
- **StoreKit 2.** Transactions are App Store-signed JWS, and
  `VerificationResult.verified` performs on-device cryptographic verification
  against Apple's certificate chain, so the client can trust a verified
  transaction without a server. A server is needed only for subscription renewal
  tracking, refunds/revocations in real time, and the App Store Server API.

### Decision

For **one-time purchases of catalogue products** (not subscriptions), client-side
verification is a legitimate, complete solution on both platforms, and the app
records the verified transaction locally. **A backend is not built in this
feature.** The plan:

- persists the platform's own transaction identifier and marks a purchase
  complete only from a **verified** transaction;
- records the backend as the recommended future hardening in ADR-0013/0014;
- never grants an entitlement from an unverified result.

This keeps scope bounded and avoids inventing a server that the project does not
have, while being explicit that production hardening would add one.

## 3. Multiplatform navigation for the bottom bar

### Question

How should the bottom navigation bar route between Search, Saved products and
Diary with correct back-stack behaviour (FR-007), given the constitution forbids
shared UI and the app is Android-first?

### Findings (verified 2026-10-20)

- `org.jetbrains.androidx.navigation:navigation-compose` is the Compose
  Multiplatform navigation artifact; **2.9.2 is stable** on Maven Central
  (2.10.x is alpha/beta/rc). Using it keeps routing multiplatform, so the same
  destination model can back a future SwiftUI navigation on iOS.
- The project already uses JetBrains Compose Multiplatform artifacts; the
  AndroidX `navigation-compose` 2.10.2 also exists but would introduce a second
  version line, as happened with Material icons in ADR-0011.
- A hand-rolled `when (currentTab)` switch cannot express
  detail→back-to-origin (FR-007) and would need re-implementing per platform.

### Options considered

| Option | Verdict | Reason |
|---|---|---|
| JetBrains `navigation-compose` 2.9.2 | **Chosen** | Stable, multiplatform, on the project's existing CMP line, models back stack correctly |
| AndroidX `navigation-compose` 2.10.2 | Rejected | Introduces a second Compose version line |
| Hand-rolled tab state | Rejected | Cannot model back-to-origin; not shareable to iOS |

**Decision**: adopt JetBrains `navigation-compose` 2.9.2 for Android UI routing,
ratified in **ADR-0015**. The bar itself is a project-owned
`androidApp` composable so the dependency is isolated to the UI layer.

## 4. Persistence shape

### Question

How are saved products and purchase records stored?

### Findings

- The existing SQLDelight database (`product-search.db`) already holds `Product`,
  `ProductNutrition`, `SearchSnapshot*`, `RecentSearch`. Product identity is the
  catalogue `code`, already the de-duplication key, so a saved product can
  reference `Product.code` and cannot be saved twice (FR-004) with a primary key
  on the code.
- Purchase records must guarantee a completed payment is recorded at most once
  (FR-012). The clarify decision (2026-10-20) is that the purchased state is
  **keyed on the product code**: a product can only ever be bought once, so a
  repeat confirmation is an idempotent upsert and can never produce a second
  record. The store's own transaction identity (Play `purchaseToken`/orderId;
  StoreKit `Transaction.id`) is still captured, but only as store-facing
  metadata used to acknowledge/finish the store transaction and for future
  server-side verification — it is not the local uniqueness key.
- Schema changes in SQLDelight must ship a migration; the plan adds one rather
  than recreating the database, so existing cached search data survives the
  upgrade.

**Decision**: two new tables — `SavedProduct` (keyed by product code) and
`PurchaseRecord` (also keyed by product code, with `transaction_id` retained as
store metadata plus status and completion time) — added through a forward
migration. Details in [data-model.md](data-model.md).

## 5. Platform-boundary finding (drives ADR-0013)

The single most consequential finding is that **StoreKit 2 cannot be called from
Kotlin/Native**. Concretely this means:

- `core:data`'s `iosMain` cannot implement a StoreKit 2 purchase client in
  Kotlin. It must delegate to Swift.
- The Swift implementation lives in `iosApp` and is injected into the shared
  runtime, or exposed via the `shared` framework.
- The shared side declares the `PurchaseClient` contract and all state/outcome
  types; the Swift side implements only the store interaction.

This is the correct reading of the constitution's "platform-specific
integrations in their matching source sets" and is recorded in **ADR-0013**
together with the rejected pure-Kotlin alternatives.

## 6. Purchase and saved-products state decisions (clarify 2026-10-20)

The `/speckit-clarify` session resolved five behaviours that shape the plan:

- **Reconciliation is store-authoritative (FR-013).** On launch the app queries
  the platform store for owned products; a product the store reports as owned is
  shown as purchased, and one it does not report stays unpaid. Local records are
  a cache of the store's truth, not an alternative truth. This keeps the shared
  `PurchaseRepository.reconcile()` platform-agnostic: it drives
  `PurchaseClient.currentEntitlements()` and upserts the result.
- **Purchased state keyed on product code (FR-012).** See §4. The upsert is
  idempotent on `product_code`, so no double-recording is possible even if the
  store reports the same completion twice.
- **Empty saved list is an explicit state with a shortcut to Search (FR-002).**
  The Saved products destination renders a "no saved products yet" message plus
  an inline action that navigates to the Search destination; never a blank
  screen. This is a Compose UI concern in `androidApp` (and the iOS list surface
  in `iosApp`), not shared logic.
- **In-progress purchase is blocking (FR-018).** Between confirming payment and
  the store returning a result, the UI shows a "purchase in progress" state and
  disables the purchase control, so a second attempt cannot be started. This is
  modelled by `PurchaseState.Pending` in shared state; the disable behaviour is
  UI.
- **Unconfigured/unavailable payment system is an explicit state (FR-019).** A
  missing store configuration or signed-out state maps to
  `PurchaseFailure.NOT_CONFIGURED` / `PurchaseOutcome.Unavailable` and renders a
  "payments unavailable" state with guidance and retry. This reuses the
  `Unavailable` outcome already in the `PurchaseClient` contract, so no new
  contract type is needed.

**Decision**: no new shared contract types are introduced by these
clarifications; they are satisfied by the existing `PurchaseState`,
`PurchaseOutcome`, `PurchaseFailure` set plus UI states in each platform's app
module. The `SavedProductsScreen` gains an empty-state branch, and the purchase
UI gains in-progress and unavailable branches.

## 7. Open questions

- **Store-account prerequisites.** Completing a real purchase requires configured
  paid products and payment accounts on both stores. This feature is verified
  against Play Console licensing testers / internal testing and an Xcode
  `.storekit` configuration file; the production prerequisite is documented and
  tracked, not assumed satisfied.
- **Kotlin 2.4.21 × Billing 9.1.0 / navigation 2.9.2.** Billing declares Kotlin
  2.2+ support and navigation 2.9.2 is on the CMP line; both are expected
  compatible but must be confirmed by a compile in implementation before any
  catalog entry is finalised.
- **Hosted CI duration** for the new instrumented tests remains subject to the
  project's under-10-minute target, as with feature 002.
