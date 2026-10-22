# ADR-0018: Single Premium Product for the In-App Purchase

- **Status**: Proposed
- **Date**: 2026-10-21
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: `specs/007-app-restructure/`; [spec.md](../../specs/007-app-restructure/spec.md); ADR-0013; ADR-0014; ADR-0016

## Context and need

ADR-0014 built the in-app purchase on a **per-food** model. The store was
expected to offer a purchasable code for catalogue products, and the app
rendered a per-product purchase surface: the shared `PurchaseClient` contract
(`core/domain/.../domain/purchase/PurchaseClient.kt`) is code-keyed
(`queryProduct(code)`, `launchPurchase(code)`), and `androidApp` mounts that
surface for whichever product code is passed in (`PurchaseDestination(productCode)`
in `androidApp/.../App.kt`, reached from the search results, the saved-products
list and the product detail). The visible result is a "Buy for EUR 1.99" action
on an individual food.

That model is commercially wrong and reads badly. A nutrition tracker that sells
a bag of oats is not a credible product, and putting a purchase button on every
food is a self-inflicted wound in front of a client. The specification for the
restructure agrees: the purchase MUST be a single premium unlock and MUST NOT be
offered on individual catalogue products (spec FR-017 and FR-032), and the
success criterion SC-010 requires that the application contain no purchase
affordance on any individual catalogue product and exactly one premium unlock,
reachable from More.

The decision is therefore not "does the project want a purchase"; the
entitlement infrastructure is built, tested and works. The decision is **what is
sold, where it is sold, and what it unlocks**.

Constraints that shape the answer:

- The shared purchase contract and its data model stay **unchanged**. The
  contract is already code-keyed, so a single product code needs no signature
  change.
- ADR-0014's platform clients remain in force: Google Play Billing 9.1.0 in
  `core:data`'s `androidMain` (`AndroidPurchaseClient.kt`) and the StoreKit 2
  Swift client (`iosApp/iosApp/StoreKitPurchaseClient.swift`), both behind the
  project-owned `PurchaseClient` contract (ADR-0013).
- The purchase flow's existing states and its reconcile behaviour are proven
  and must not regress: available, pending, owned, failed, restore, plus
  `PurchaseRepository.reconcile()` against the store as the source of truth.
- The unlock must be worth something to the publisher. Only one candidate in
  the roadmap has a per-use cost to the publisher: the AI assistant (the
  remote-LLM coach, gated by the server-side AI gateway in ADR-0016).

Known limitation, unchanged by this ADR: the store-side paid products, payment
accounts and test tracks are **absent** (recorded in `AI-LOG.md`), so a live
charge cannot be exercised. The purchase path is unit-, instrumented- and
simulator-verified against fakes only.

## Decision

**Re-point the in-app purchase to ONE premium product, and remove the purchase
affordance from every food surface.** This ADR **supersedes the per-food
purchase model in ADR-0014**; everything else in ADR-0014 (the platform
clients, the `androidMain` scoping, the code-keyed idempotency and the
reconcile-on-foreground behaviour) remains in force unchanged.

- **One product code.** A single premium product code, for example
  `nutrilog_pro`, replaces every per-food purchasable code. The per-food
  purchase affordance is REMOVED from the product detail and from every food
  surface (search results, saved products, product detail). The existing
  `PurchaseSection` composable is retained, but it is mounted once, for the
  premium code, and is no longer parameterised by a food's code.
- **One place to buy it.** The premium unlock lives in ONE place: the More
  destination, as a single "Premium" section. The purchase flow keeps its
  existing states (available, pending, owned, failed, restore) and its existing
  restore/reconcile behaviour, including `PurchaseRepository.reconcile()`
  against the store as the source of truth.
- **What the unlock grants.** In this feature the premium unlock grants access
  to the **AI assistant** (the remote-LLM coach), because it is the only
  feature with a per-use cost to the publisher (spec FR-029). Barcode scanning
  and the rest of the tracker stay free. This is stated explicitly so the
  scope of the unlock is never ambiguous: buying the premium product unlocks
  the assistant, and nothing else is gated.
- **The contract does not change.** `PurchaseClient`, `PurchaseRepository` and
  `PurchaseRecord` keep their existing signatures, and the code-keyed
  idempotency is preserved: a repeated confirmation of the same `product_code`
  is an idempotent upsert on `product_code` and can never produce two purchase
  records.
- **The entitlement check is client-side advisory.** The app's local
  entitlement state decides what the UI offers, and it is advisory only. The
  **authoritative gate for the AI assistant is the server-side AI gateway**
  (ADR-0016), which is the only place where the entitlement can actually be
  enforced, because a client-side check can be bypassed by a modified or
  repackaged app. Any local check must therefore be understood as a convenience
  for the paying user, never as a security boundary.
- **Debug builds may bypass the purchase.** A debug build MAY enable the
  assistant without a purchase, so the feature can be exercised while the store
  is unconfigured. The specification requires that a **release** build require a
  verified entitlement to use the assistant (spec FR-029), and that release
  requirement is the hard boundary; the debug affordance exists only on the
  other side of it. This is a development affordance with a real risk: a
  release build that reaches it ships a free assistant, and the revenue is lost
  for every user. The bypass MUST be unreachable in a release build (gated on
  the build type), and a test MUST assert that the release path requires the
  verified entitlement.
- **One named constant.** The single product code is a named constant in shared
  code, for example `PREMIUM_PRODUCT_CODE` in
  `core/domain/.../domain/purchase/`, so Android, iOS and the UI all reference
  one value. On iOS the constant is mapped to the App Store product identifier
  explicitly through `StoreKitPurchaseClient.productIdsByCode`, so the shared
  constant is not forced to equal the store-side identifier.

The supersession is scoped deliberately: ADR-0014 is superseded **only** on the
per-food purchasable code model and the placement of the purchase surface in
the food screens. ADR-0014's platform clients, its idempotent record-once
behaviour, its acknowledge/consume correctness rule and its reconcile behaviour
are unaffected and stay in force.

## Alternatives considered

- **Keep per-food purchases.** Rejected: commercially wrong, and it puts a
  purchase button on every food in the catalogue, which is exactly the
  experience the restructure is meant to remove. It also contradicts spec
  FR-017 and FR-032 explicitly, and fails SC-010.
- **A monthly or yearly subscription instead of a one-time unlock.** Rejected
  **for this increment**, and recorded as the natural next commercial step. A
  subscription needs `ProductType.SUBS` and `querySubscriptionsAsync` on the
  Play side, base-plan selection, an expiry and period on `PurchaseRecord`, a
  manage-subscription deep link, and grace-period and billing-retry states
  that the current one-time state machine does not model. The shared
  `VerifiedTransaction` already has room for it (it retains `transactionId`,
  `purchaseToken` and `completedAtEpochMillis`), but a subscription needs its
  own ADR and its own purchase-model migration, which is exactly how the
  specification records it (out of scope: "A subscription plan. The premium
  product is a one-time unlock"). It is a good next step, not this one.
- **Remove the purchase feature entirely.** Rejected: the entitlement
  infrastructure is already built, tested and works, and a sellable app needs
  the upgrade path. Deleting working, tested infrastructure to avoid a pricing
  decision is not a decision.
- **Server-verified receipts.** Not yet. The store-side prerequisites are
  absent, so a server verification step could not be exercised end to end.
  `VerifiedTransaction.transactionId` and `purchaseToken` are already retained
  for exactly this purpose, and the AI gateway (ADR-0016) is the natural place
  to add verification when the assistant's server side is built.

## Binary-size impact

No new dependency is introduced; the change is placement only. The per-food
purchase wiring is removed from the food screens and one premium section is
added to More, so the expected effect on the release APK is neutral to
marginally smaller, and the `Shared.xcframework` is unaffected in kind: the
shared change is one constant and one documentation-level edit.

The Play Billing dependency itself is unchanged and remains `androidMain`-only
(ADR-0014), so the iOS framework is **unaffected by construction** with
respect to billing.

The sizes are re-measured after implementation and compared against the
recorded baselines: the unsigned release APK measured 12,520,644 bytes and the
debug APK 17,167,130 bytes (ADR-0014 and ADR-0015, 2026-10-20), and
`Shared.xcframework` 93,836 KB (ADR-0013, 2026-10-21). The measurement command
is the one already used for those records (a release APK assembly plus
`:shared:assembleSharedXCFramework` and a size of the resulting framework), and
the new numbers are recorded in this ADR's validation record.

## Build-time impact

Negligible. No new dependency, no new Gradle module and no source-set change:
one shared constant is added to `core:domain`, the platform clients are
untouched, and the Android UI change is a moved composable. The shared module
recompiles for the constant alone, and the iOS build recompiles for the same
reason. Clean and incremental durations are re-measured after implementation;
hosted PR CI must stay within the project's 10-minute target.

## Licence

No new dependency is introduced, so no new licence obligation arises. Google
Play Billing remains under the **Apache License 2.0** (ADR-0014) and StoreKit 2
remains under the Apple SDK agreements already accepted to ship an iOS app
(ADR-0013). There is no copyleft, no attribution obligation on the UI and no
licence fee. The premium product is a store listing, not a dependency, and
Google Play's standard revenue share applies to actual sales.

## Exit strategy

The product code is a single named constant, so the exit is a deletion:

- **Remove the commercial offer.** Delete the Premium section from More and
  the `PREMIUM_PRODUCT_CODE` constant. The purchase surface disappears with
  them.
- **Remove the purchase feature entirely.** Delete the `purchase` package in
  `core:domain`, `PurchaseRepositoryImpl`, the `PurchaseSection` composable,
  `AndroidPurchaseClient`, the Swift `StoreKitPurchaseClient`, and drop the
  `purchase_record` table through a schema migration. The constant is the only
  thing that must be found first; everything else is reachable from it.
- **Replace the commercial model.** A subscription, a different store, or a
  server-side entitlement all touch the constant and the `PurchaseClient`
  implementation, not the shared contract. The contract's shape
  (`PurchasableProduct`, `PurchaseClientResult`, `VerifiedTransaction`,
  `PurchaseState`, `PurchaseOutcome`, `PurchaseRecord`, `PurchaseRepository`)
  is unchanged, so no consumer of the contract has to be rewritten.

## Consequences

- The commercial story becomes credible: one product, one price, one place to
  buy it, and no purchase button on a bag of oats.
- The scope of the unlock is explicit and non-ambiguous: the AI assistant.
  Barcode scanning and the rest of the tracker remain free, and nothing else
  is silently gated.
- The entitlement is **advisory on the client**. Until the server-side AI
  gateway (ADR-0016) is in place, a modified client can unlock the assistant
  without paying. This is a known, accepted risk of this increment, not an
  oversight; the client check is a convenience for the paying user, never a
  security boundary.
- The debug-only bypass is a development affordance with a real risk of
  leaking into a release build. It must be gated on the build type and
  covered by a test asserting the release path requires the verified
  entitlement.
- Purchase records written under the old per-food model are keyed by
  per-food codes and become inert: nothing consults them any more, because the
  only code that now matters is the premium code. No schema change is
  required for this decision, so the stale rows stay until a later cleanup
  migration; they are harmless but should not be mistaken for entitlements.
- The store-side configuration becomes a hard dependency of the revenue path:
  the Play Console and the App Store Connect configuration must each carry
  exactly one product. Simpler to configure, but now a single point of
  failure for the commercial path.
- **The store-side configuration remains a publication prerequisite.** This ADR
  does not make the app publishable on its own: the paid products, the payment
  accounts and the test tracks are still absent, so a live charge cannot be
  exercised. The path is verified against fakes only.

## Review record

- **Review outcome**: Pending
- **Reviewed by**: —
- **Review date**: —
- **Rationale or conditions**: —

## Validation record

- **Pre-implementation state (verified 2026-10-21)**: the shared purchase
  contract is already code-keyed (`PurchaseClient.queryProduct(code)`,
  `launchPurchase(code)`, `currentEntitlements()`; `PurchaseRecord.productCode`
  is the upsert key in `core/data/.../PurchaseRepositoryImpl.kt`), so a single
  product code requires no signature change. The per-food purchase surface is
  currently mounted for a food's code in `androidApp/.../App.kt`
  (`PurchaseDestination(productCode)`) and rendered by the search, saved
  products and product detail screens through `PurchaseSection`, which is the
  code that this ADR removes from those surfaces.
- **Store-side prerequisites (unchanged, verified 2026-10-21)**: the store-side
  paid products, payment accounts and test tracks are absent, so a live charge
  is not exercised in this environment; the purchase path is unit,
  instrumented and simulator verified against fakes (see the
  production-prerequisites note in `AI-LOG.md` and in
  `specs/004-saved-products-navbar/plan.md`).
- **Pending after implementation**: re-measure the release and debug APKs and
  `Shared.xcframework` against the recorded baselines; add a test asserting
  that the assistant is not reachable in a release build without the verified
  entitlement; re-run the purchase unit, instrumented and simulator suites to
  confirm the states and the reconcile behaviour are unchanged.
