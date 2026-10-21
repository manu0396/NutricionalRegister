# ADR-0014: Google Play Billing for Android Purchases

- **Status**: Accepted
- **Date**: 2026-10-20
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: `specs/004-saved-products-navbar/`; [contracts/purchase-client.md](../../specs/004-saved-products-navbar/contracts/purchase-client.md); ADR-0013

## Context and need

Feature 004 requires real in-app purchases with genuine charges on Android
(spec FR-009, FR-014). On Android that means Google Play Billing, and Google Play
policy constrains which version may ship.

The library must:

- work from the `androidMain` source set without leaking into common code;
- sit on the project's pinned Kotlin 2.4.21 / `compileSdk 37` / `minSdk 24`;
- satisfy Play policy for new apps and updates in 2026 (a hard requirement, since
  the app is distributed through Play);
- allow a clean exit if a KMP billing library is adopted later.

## Decision

**Adopt `com.android.billingclient:billing-ktx` version 9.1.0 as an
`androidMain`-only dependency in `core:data`.**

- **Version rationale (verified 2026-10-20).** Google Play requires **Billing 8 or
  later** for new apps and updates from **31 Aug 2026**, with an extension to
  **1 Nov 2026**. Billing 7 and below are deprecated. **9.1.0** is the current
  release and is forward-safe until 2028, so it satisfies policy with margin.
  Billing 8.1.0 raised `minSdk` to 23 and supports Kotlin 2.2+; the project's
  `minSdk 24` and Kotlin 2.4.21 are compatible.
- **`billing-ktx`.** The KTX artifact provides coroutine/suspend extensions
  (`queryProductDetails`, `consumePurchase`, `acknowledgePurchase`), which suit
  the shared suspend/Flow `PurchaseClient` contract better than the raw
  callback API.
- **Scope.** Declared only in `core:data`'s `androidMain` source set. It is never
  a common dependency, so the shared module and the iOS build are unaffected.
- **Activity plumbing.** `launchBillingFlow` requires an `Activity`. Because the
  project has no DI framework, the `Activity`/`Context` is supplied by the
  `androidApp` composition root, exactly as the existing SQLDelight/HTTP factories
  are wired today.
- **Acknowledge/consume.** A completed purchase is acknowledged (or consumed);
  unacknowledged purchases are auto-refunded after 3 days, so this is a
  correctness requirement, not an optimisation.
- **Verification.** Billing verifies the purchase signature locally; the
  `purchaseToken` is persisted for a future server-side check. A backend is
  **out of scope** for this feature (research.md §2).

## Alternatives considered

- **RevenueCat `purchases-kmp` 3.14.0.** Rejected: mandatorily requires
  RevenueCat's hosted backend and a revenue-share fee; Kotlin 2.4.21 compatibility
  unverified. It could be adopted later without changing the project's contracts
  (exit strategy below).
- **`io.github.aditya-gupta99:inAppPurchase-kmp` 1.0.43.** Rejected: immature
  (small single-author project) and documented as wrapping **Google Play Billing
  v6**, which is deprecated and would violate 2026 Play policy.
- **No billing library (drop Android purchase).** Rejected: the feature is
  explicitly required on both platforms.
- **Hand-rolled wrapper around raw `billing` (non-KTX).** Rejected: `billing-ktx`
  is the same SDK with coroutine extensions; using the non-KTX artifact would mean
  re-implementing what the KTX artifact already provides.

## Binary-size impact

`billing-ktx` is a small AAR with no native libraries; its main transitive weight
is the Billing client, which is modest and R8-shrinkable. Measured for the
validation record after integration, alongside the existing debug/release APK
sizes. The iOS `Shared.xcframework` is **unaffected by construction** because the
dependency is `androidMain`-only.

## Build-time impact

One additional Android-only artifact on the `androidApp`/`core:data` Android
classpath, resolved from Google's Maven repository. `settings.gradle.kts`
restricts `google()` to `androidx`, `com.android`, `com.google` groups, and
`com.android.billingclient` falls under `com.android`, so no repository change is
needed. Clean and incremental durations are measured after integration; hosted PR
CI must stay under the project's 10-minute target.

## Licence

Google Play Billing is published by Google under the **Apache License 2.0**,
compatible with this public repository. It imposes no copyleft; there is no
attribution obligation on the UI and no licence fee. Google Play's standard
revenue share applies to actual sales but is not a dependency cost.

Source: <https://developer.android.com/google/play/billing>

## Exit strategy

Because the store is accessed only through the project-owned `PurchaseClient`
contract, adopting a KMP billing library later means replacing the Android
`actual` implementation and deleting this dependency's usage; the shared
contract, repositories, and schema are unchanged, so no migration is required.

## Consequences

- Android purchases satisfy Play policy with a version valid until 2028.
- Purchase correctness requirements (acknowledge/consume, reconcile on foreground,
  record-once by token) are explicit and testable.
- The Android build gains one small Apache-2.0 dependency; the iOS build and
  shared module are untouched.
- The project owns the upgrade work for future Billing majors, which is bounded
  and localized behind the contract.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-20
- **Rationale or conditions**: Approved `billing-ktx` 9.1.0 scoped to
  `androidMain` to meet 2026 Play policy. Must remain `androidMain`-only, must
  keep all store access behind the shared `PurchaseClient` contract, and must not
  change the shared module or the iOS build.

## Validation record

- **Version/policy (verified 2026-10-20)**: 9.1.0 present on Google Maven; Play
  policy requires Billing 8+ from 31 Aug 2026 (extension to 1 Nov 2026).
- **Post-implementation validation (2026-10-20)**: the client
  (`core/data/src/androidMain/.../AndroidPurchaseClient.kt`) compiles against the
  project's Kotlin / compileSdk / minSdk configuration; Play Billing result codes
  are mapped to the shared failures by a unit-tested pure function
  (`AndroidPurchaseClientTest`); purchase states (Idle/Pending/Purchased/Cancelled/
  Failed/Unavailable), the disabled-while-pending rule, and the unavailable
  guidance with retry are covered by instrumented tests on emulator-5556. The
  dependency is scoped to `core:data` `androidMain` only and does not enter the
  shared module or the iOS build.
- **Binary size (2026-10-20)**: the unsigned release APK measures 12,520,644 bytes
  (11,365,002 bytes before navigation + billing, a delta of +1,155,642 bytes for
  both features combined); the `Shared.xcframework` is unchanged.
- **Coverage treatment (2026-10-20)**: the Play Billing client is a thin bridge
  over the live Billing SDK whose branches all require a real store connection and
  a foreground Activity, so it is excluded from the `:core:data` coverage gate
  (`kover` `filters { excludes { classes(...) } }` in `core/data/build.gradle.kts`).
  The only pure logic in that file, the `BillingResult -> PurchaseFailure`
  mapping, remains covered by `AndroidPurchaseClientTest` and is not excluded.
- **Exit strategy check (2026-10-20)**: still holds. All store access is behind the
  shared `PurchaseClient` contract, so replacing the Android client is localized.
- **Not verifiable in this environment**: a completed charge against Play Console
  internal testing with a licensing tester requires store-side paid products and a
  payment account; see the production-prerequisites note in
  `specs/004-saved-products-navbar/plan.md`. The path is built and unit/instrumented
  tested; the live charge is not exercised here.
