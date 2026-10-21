# ADR-0013: Swift StoreKit 2 Bridge for iOS Purchases

- **Status**: Accepted
- **Date**: 2026-10-20
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: `specs/004-saved-products-navbar/`; [contracts/purchase-client.md](../../specs/004-saved-products-navbar/contracts/purchase-client.md); ADR-0014

## Context and need

Feature 004 requires **real in-app purchases with genuine charges on both Android
and iOS** (spec FR-009, FR-014). On iOS this means StoreKit 2, Apple's current
in-app purchase API (iOS 15+), which delivers App Store-signed transactions and
supports on-device verification via `VerificationResult`.

The project's rule is that shared behavior lives in Kotlin Multiplatform and
platform-specific integrations live in their matching source sets. The intent was
therefore to implement an `iosMain` Kotlin `PurchaseClient` over StoreKit 2, as
the Android side does over Google Play Billing (ADR-0014).

That is **not possible**. Kotlin's own documentation states that a Swift library
can be used from Kotlin only if its API is exported to Objective-C with `@objc`,
and that **"pure Swift modules are not yet supported."** StoreKit 2's `Product`,
`Transaction`, `PurchaseResult`, and `VerificationResult` types are **pure
Swift**, with no Objective-C equivalent. `cinterop` binds only C and
Objective-C headers, so it cannot import StoreKit 2. Kotlin's new *Swift export*
feature goes in the opposite direction (exports Kotlin to Swift) and does not
enable Kotlin to call StoreKit 2.

A decision is required about how to take an iOS purchase at all.

## Decision

**Implement the iOS purchase client in Swift inside `iosApp`, behind the
project-owned shared `PurchaseClient` contract.**

- `core:domain` (common) declares the `PurchaseClient` interface and the pure
  result types (`PurchasableProduct`, `PurchaseClientResult`, `VerifiedTransaction`,
  `PurchaseFailure`). These reference no platform API.
- `iosApp` provides a Swift `StoreKitPurchaseClient` that implements the store
  interaction using StoreKit 2 (`Product.products(for:)`, `product.purchase()`,
  `VerificationResult`, `transaction.finish()`, `Transaction.currentEntitlements`,
  `AppStore.sync()`).
- The Swift client is injected into the shared runtime (or exposed through the
  `shared` framework) so shared code depends only on the contract.
- The `iosMain` source set contains **no StoreKit calls** — only the wiring that
  accepts the Swift-provided implementation.

Only **verified** results grant a purchase: `.success(.verified(transaction))`
proceeds; `.unverified` maps to `Failed(VERIFICATION_FAILED)`. iOS has no
server-side `finish`, so finishing is a client call, unlike Play Billing's
acknowledge/consume model (ADR-0014).

## Alternatives considered

- **Kotlin/Native `iosMain` calling StoreKit 2 directly via cinterop.** Rejected:
  impossible — cinterop cannot bind pure-Swift modules (Kotlin docs), and
  StoreKit 2 has no Objective-C surface.
- **`@objc`-annotated Swift wrapper class imported into Kotlin.** Rejected as the
  primary mechanism: it works, but loses `async`/`await` ergonomics, forcing
  completion-handler bridging. The interface-implementation pattern keeps shared
  code clean. (Retained as a fallback if the framework wiring proves difficult.)
- **Adopt RevenueCat `purchases-kmp` (which already ships a Kotlin/Native core
  wrapping StoreKit).** Rejected in [research.md](../../specs/004-saved-products-navbar/research.md)
  §1: it mandatorily requires RevenueCat's hosted backend as the entitlement
  source of truth and charges a revenue-share fee above a revenue threshold.
- **A small Swift Package for the bridge.** Deferred: the bridge currently lives
  in `iosApp` because that is the only iOS UI surface today; extracting a package
  is a low-cost later step if the iOS surface grows.
- **Build a verification backend.** Rejected for this feature: StoreKit 2's
  on-device JWS verification is sufficient for one-time non-subscription
  purchases; a backend is recorded as future hardening, not a prerequisite
  (research.md §2).

## Binary-size impact

None measurable on Android (this ADR adds no Android artifact). On iOS, the
Swift bridge adds a small amount of first-party Swift code to `iosApp`; it links
the system StoreKit framework, which is provided by the OS and adds no bundled
binary. The `Shared.xcframework` is **unaffected by construction**, because the
StoreKit calls live in `iosApp` and only the contract types are shared. This is
re-measured in the validation record after implementation.

## Build-time impact

The iOS build gains the Swift bridge source files; no new Gradle dependency is
introduced, so the Android and shared builds are unaffected. The iOS build must
continue to link the Swift bridge correctly through the Xcode project, verified
by `xcodebuild`.

## Licence

No third-party dependency is introduced. StoreKit 2 is a first-party Apple
framework governed by the Apple SDK agreements already accepted to ship an iOS
app. There is no licence cost and no attribution obligation on the UI.

## Exit strategy

If a KMP billing library is later adopted, delete the Swift `StoreKitPurchaseClient`
and provide its `PurchaseClient` implementation from the library instead. The
shared contract, repositories, and persisted schema do not change, so no
migration is required.

## Consequences

- iOS purchases are possible despite the Kotlin/Native limitation, and the store
  interaction stays in the iOS source set, matching the constitution's platform
  boundary.
- The shared purchase-state machine (pending/cancelled/failed/verified, record-once,
  reconcile-on-launch) is reused unchanged on both platforms.
- The project owns a small Swift bridge to maintain, and the iOS purchase path
  cannot be exercised in shared common tests — it is verified via
  `:shared:iosSimulatorArm64Test` for the shared portion and by building the
  Swift bridge for the platform portion.
- A future move to a KMP billing library is a localized change.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-20
- **Rationale or conditions**: Approved the Swift StoreKit 2 bridge as the only
  viable way to take iOS purchases, given that Kotlin/Native cannot bind pure
  Swift. The shared contract must remain platform-free, only verified results may
  grant a purchase, and the `Shared.xcframework` must be unaffected.

## Validation record

- **Trigger constraint (verified 2026-10-20)**: Kotlin documentation states pure
  Swift modules are not supported for Kotlin use; StoreKit 2 is pure Swift, so
  cinterop cannot import it.
- **Post-implementation validation (2026-10-20)**: the Swift bridge
  (`iosApp/iosApp/StoreKitPurchaseClient.swift`) is compiled into the iOS app and
  conforms to the shared `PurchaseClient` protocol exported through
  `Shared.xcframework`; `:shared:assembleSharedXCFramework` succeeds with
  `:feature:savedproducts` exported, and the shared iOS simulator tests pass, so
  the shared purchase-state and saved-product logic is exercised on the iOS
  target. All StoreKit calls live in the iOS source set; no StoreKit call exists
  in `iosMain`. The `Shared.xcframework` binary size is unchanged by this work
  (the Swift file is an app target, not part of the framework); measured again on
  2026-10-21 at 93,836 KB, effectively unchanged from 003's 92,024 KB.
- **Exit strategy check (2026-10-21)**: still holds. The contract, repositories,
  and schema are unchanged, so swapping to a future KMP billing library remains a
  localized change to the Swift client only. `xcodebuild -scheme iosApp
  -destination 'generic/platform=iOS Simulator' build` reports `BUILD SUCCEEDED`
  with 0 errors, so the Swift bridge and the SwiftUI purchase/saved-products
  surfaces compile against the exported framework.
- **Not verifiable in this environment**: an end-to-end iOS purchase against an
  Xcode `.storekit` configuration or the App Store sandbox requires a signed build
  and store-side products; see the production-prerequisites note in
  `specs/004-saved-products-navbar/plan.md`. The path is built and the shared
  portion is tested; the live charge is not exercised here.
