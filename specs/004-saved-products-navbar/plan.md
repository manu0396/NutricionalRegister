# Implementation Plan: Saved Products Bottom Navigation and In-App Purchase

**Branch**: `004-saved-products-navbar` | **Date**: 2026-10-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-saved-products-navbar/spec.md`

## Summary

Add an app shell with a persistent bottom navigation bar (Search, Saved products,
Diary) and give users the ability to save catalogue products on the device and to
purchase a product through each platform's native in-app purchase mechanism.

Work is split along the project's existing boundaries. `core:domain` gains
product-agnostic contracts: `SavedProduct`, `SavedProductRepository`,
`PurchaseState`, `PurchaseOutcome`, `PurchaseRecord`, `PurchaseRepository`, and a
`PurchaseClient` abstraction. `core:data` owns persistence (new SQLDelight tables
for saved products and purchase records) and implements the purchase repository
against the `PurchaseClient`. `feature:savedproducts` is a new KMP module holding
the saved-products and purchase presentation state. `androidApp` gains the bottom
navigation shell (new `feature`-agnostic `ui/NutriLogBottomBar.kt`) and the
Android `PurchaseClient` implementation over Google Play Billing. `iosApp` gains
a Swift `StoreKit 2` `PurchaseClient` implementation, because **StoreKit 2 is a
pure-Swift API that Kotlin/Native cannot bind via cinterop** — this is the single
most consequential constraint in this plan and is recorded in ADR-0013/0014.

Purchase state, double-recording protection, reconciliation, and outcome
modelling are shared behaviour and live in common code. Only the payment sheet
and the platform purchases API are platform-specific.

The clarify session (2026-10-20) fixed five behaviours the plan must honour: the
platform store is the source of truth for reconciliation; the purchased state is
keyed on the product code; an empty saved list shows an explicit message plus a
shortcut to Search; an in-progress purchase shows a blocking state that disables
the purchase control; and an unavailable/unconfigured payment system shows an
explicit "payments unavailable" state with retry. These are covered by the
existing shared state/outcome types plus UI states in each app module, so no new
shared contract is introduced (research.md §6).

## Technical Context

**Language/Version**: Kotlin Multiplatform, Kotlin 2.4.21 and Gradle 9.5.1 (as
already pinned; no toolchain change)

**Primary Dependencies**:
- Existing: Compose Multiplatform 1.12.2, Material 3 1.12.0-alpha03, Coil 3.6.3,
  SQLDelight 2.4.1, Ktor 3.6.0, coroutines 1.11.0, `androidx.compose.material:material-icons-core` 1.7.8.
- New (each requires its own ADR before a catalog entry, per project governance):
  - `org.jetbrains.androidx.navigation:navigation-compose` 2.9.2 (JetBrains CMP
    navigation, stable) — multiplatform navigation for the bottom bar. ADR-0015.
  - `com.android.billingclient:billing-ktx` 9.1.0 (Android-only, `androidMain`) —
    Google Play Billing. ADR-0014.
  - No third-party KMP billing library: see research.md §1 for why RevenueCat
    (requires their backend + fee) and `inAppPurchase-kmp` (immature, pinned to
    deprecated Billing v6) were both rejected.
  - iOS: **no Gradle dependency**. StoreKit 2 is a first-party Apple framework
    used from Swift code in `iosApp`; ADR-0013 records the Swift bridge decision.

**Storage**: SQLDelight (existing `product-search.db`). Two new tables:
`SavedProduct` and `PurchaseRecord`, added by a schema migration. Saved products
store identity + display fields; purchase records are keyed on the product code
(a product can be bought only once, which is what prevents double-recording) and
also store the store's transaction identifier as metadata, plus status and
completion time.

**Testing**: Kotlin common tests for domain and feature state (`commonTest`);
Android host tests for the SQLDelight repository (`androidHostTest`); Compose
instrumented tests for the bottom bar and purchase UI states (`androidTest`,
emulator-5556); Kotlin/Native iOS simulator tests for shared behaviour
(`:shared:iosSimulatorArm64Test`). Platform purchase clients are verified against
each platform's **test** environment (Play Console licensing testers / internal
tracking; an Xcode `.storekit` configuration file), never with live charges.

**Target Platform**: Android (minSdk 24, compileSdk 37) and iOS (SwiftUI entry
point). Payment is required on both platforms and both get a purchase UI surface
(Android Compose in `androidApp`, iOS SwiftUI in `iosApp`). Saved products also
get an iOS list surface. The bottom navigation *bar* is Android UI in this
feature; native SwiftUI navigation is a later step, consistent with the
constitution's native-UI boundary and the existing precedent that Android UI
leads iOS UI.

**Project Type**: Kotlin Multiplatform mobile application (shared logic + native UI)

**Performance Goals**: Preserve existing goals. The saved-products list and the
bottom navigation must render without jank; switching destinations is a
navigation transition with no data refetch required; a saved product appears in
the list immediately after saving (no network dependency); an empty saved list
renders its explicit empty state without a loading flash; purchase confirmation
is reflected on screen as soon as the platform reports a verified transaction;
and while a purchase is pending the control is disabled so no second attempt can
start.

**Constraints**:
- Respect module boundaries: `core:domain` has no project dependencies;
  `core:data` depends on `core:domain` only among core modules; `feature:*` depend
  on `core:domain`; `androidApp` composes everything. **Finding (verified
  2026-10-20): the Konsist `:architectureTest` currently matches on the package
  prefix `com.nutrition.*`, but the code uses `com.example.nutricionalregister.*`,
  so the rule is inert today.** This feature does not rely on it: boundaries are
  respected by construction (contracts in `core:domain`, persistence in
  `core:data`, state in `feature:savedproducts`, UI/clients in `androidApp`/
  `iosApp`). Fixing the prefix is a separate, deliberate change and is **not**
  bundled into this feature.
- **No shared UI.** Bottom navigation and purchase UI are native per platform.
- **No payment credentials** are ever collected or stored by the app; all payment
  data entry happens in the platform's own sheet.
- Saved products and purchase records stay **on-device**; no analytics or
  tracking (existing privacy stance).
- **Reconciliation trusts the platform store** as the source of truth for owned
  products (FR-013); local records are a cache, not an alternative truth.
- The purchase UI **disables the purchase control while a purchase is pending**
  (FR-018) and shows an explicit **"payments unavailable"** state with retry when
  the store is unconfigured or unreachable (FR-019); the saved list shows an
  explicit **empty state with a shortcut to Search** (FR-002). These are UI
  states in each app module, backed by shared `PurchaseState`/`PurchaseOutcome`.
- Preserve accessibility: 48 dp targets, accessible names, enlarged text.
- Real payments introduce **store-side prerequisites** (paid-products
  configuration, accounts) and **platform-policy constraints**; the feature is
  verified against test environments and must not ship a purchase path that
  charges without a verified transaction.

**Scale/Scope**: One new feature module, one new app-shell UI surface, two new
SQLDelight tables with a migration, one Android billing client, one Swift
StoreKit 2 bridge, one new navigation dependency. No accounts, no
cross-device sync, no diary functionality (Diary is an explicit placeholder).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Assessment |
|---|---|
| **I. Shared Logic, Deliberate Platform Boundaries** | PASS. Saved-product and purchase logic (state, outcomes, double-record protection, reconciliation) is shared in `core:domain`/`core:data`/`feature:savedproducts`. The payment sheet and store API are platform-specific by necessity: Google Play Billing in `androidMain`, StoreKit 2 in `iosApp` Swift. This is the deliberate platform boundary the principle anticipates. |
| **II. Cross-Platform Compatibility** | PASS **with a documented asymmetry**. Payments are required on both platforms and BOTH get a purchase UI surface: Android in `androidApp`, iOS in `iosApp` SwiftUI (`ContentView`), both driven by the shared purchase state. The bottom navigation bar is Android UI in this feature; native SwiftUI navigation is deferred, matching the existing Android-leads-iOS precedent (002/003). Saved products likewise get an iOS list surface so the feature is usable on iOS. The remaining asymmetry (the nav *bar* itself) is recorded in the spec (Assumptions) and ADR-0013. |
| **III. Verify Behavior with Tests** | PASS. Shared behaviour gets common tests; persistence gets Android host tests; UI gets instrumented tests; iOS shared behaviour gets simulator tests (`:shared:iosSimulatorArm64Test`, task T056). Untestable live payments are verified against documented test environments and that limitation is reported. |
| **IV. Focused, Maintainable Changes** | PASS **subject to gates**. Every new dependency (navigation, billing) is justified in its own ADR with alternatives, size/build impact, licence, and exit strategy. Only behaviour in the spec is implemented; Diary remains a placeholder. |
| **V. Plain-Text Documentation and Code** | PASS. All new documentation, specifications, and source are plain text. The repository contains no emoji glyphs; enforced by `scripts/check-no-emoji.sh` (Gradle task `plainTextCheck`) in CI. |
| **Platform and Technology Constraints** | PASS. KMP holds shared logic, not shared UI; Compose for Android UI, SwiftUI for iOS. No supported target is removed. |
| **Governance** | PASS. New dependencies follow the established ADR-before-catalog-entry process (as ADRs 0009-0012 did). |

**Gate result: PASS.** No constitution exception is required. The one item that
must be tracked rather than waived is the **store-account dependency**: real
purchases cannot be completed end-to-end without store-side configuration, so the
plan marks the completion of the purchase path against test environments and
records the production prerequisite explicitly (see Complexity Tracking).

**Post-design re-check (after Phase 1)**: still PASS. The design introduces no
shared UI (Principle II holds — the nav bar is Android-only and the purchase UI
is native per platform), adds no un-justified dependency (Principle IV holds —
navigation and billing each have their own ADR), and keeps every new state
(`empty`, `pending`, `unavailable`) backed by shared types plus per-platform UI
rather than duplicating logic. Principle V holds: all artifacts are plain text
and `plainTextCheck` passes.

## Project Structure

### Documentation (this feature)

```text
specs/004-saved-products-navbar/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks - not created here)
```

### Source Code (repository root)

```text
core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/
├── product/                      # existing (Product, NutritionValue, ...)
└── purchase/                     # NEW
    ├── SavedProduct.kt           # saved-product entity + validation
    ├── SavedProductRepository.kt # save/remove/observe contracts
    ├── PurchaseState.kt          # sealed purchase state + outcomes
    ├── PurchaseRecord.kt         # persisted record + transaction identity
    ├── PurchaseRepository.kt     # reconcile/record/observe contracts
    └── PurchaseClient.kt         # platform payment abstraction (expect-free interface)

core/data/src/commonMain/
├── sqldelight/.../SavedProduct.sq     # NEW tables + queries
├── .../SavedProductRepositoryImpl.kt  # NEW
├── .../PurchaseRepositoryImpl.kt      # NEW
└── .../ProductSearchRuntime.kt        # extended to expose new repositories
core/data/src/androidHostTest/.../     # NEW repository tests
core/data/src/androidMain/.../         # NEW Android PurchaseClient (Play Billing)
core/data/src/iosMain/.../             # NEW iOS PurchaseClient (delegates to Swift)

feature/savedproducts/                 # NEW module (mirrors feature/search)
├── build.gradle.kts
└── src/commonMain/kotlin/com/example/nutricionalregister/feature/savedproducts/
    ├── SavedProductsState.kt
    └── SavedProductsViewModel.kt
└── src/commonTest/...

androidApp/src/main/kotlin/com/example/nutricionalregister/
├── App.kt                             # hosts the navigation shell
├── ui/NutriLogBottomBar.kt            # NEW bottom navigation bar
├── savedproducts/                     # NEW destination + purchase UI
│   ├── SavedProductsScreen.kt         # list + empty state (message + Search shortcut)
│   ├── SavedProductsDependencies.kt
│   └── PurchaseSheetState.kt          # Idle/Pending/Purchased/Cancelled/Failed/Unavailable
└── diary/DiaryPlaceholderScreen.kt    # NEW "not yet available" destination

iosApp/iosApp/
├── StoreKitPurchaseClient.swift       # NEW StoreKit 2 bridge (pure Swift)
└── ContentView.swift                  # extended: purchase UI + saved-products list

shared/build.gradle.kts                  # add :feature:savedproducts to api/export
shared/src/iosMain/...                 # only if a Kotlin-side bridge type is needed;
                                       # today `shared` exposes no purchase API
```

**Structure Decision**: Keep the established five-layer layout. New
purchase/saved-product **contracts** go in `core:domain` (pure Kotlin, no project
deps). **Persistence and the purchase repository** go in `core:data`, whose
existing `ProductSearchRuntime` composition root is extended. A **new
`feature:savedproducts` module** holds presentation state, mirroring
`feature:search` exactly. **UI and platform clients** live in `androidApp` and
`iosApp` respectively. No new module dependency direction is introduced.

The `shared` module must additionally **export `:feature:savedproducts`** (today
it exports `core:data`, `core:domain`, and the four `feature:*` scaffolds), so the
iOS `iosApp` can reach the shared purchase contracts and state from Swift. The
Swift `StoreKitPurchaseClient` implements the exported `PurchaseClient` interface;
`shared` today exposes no purchase API, so this wiring is new, not an extension of
existing surface. Note the architecture-test caveat above: boundaries are upheld
by construction here, not by the currently-inert Konsist rule.

## Complexity Tracking

> Filled because the feature has external, non-code prerequisites that must be
> justified and tracked rather than silently assumed.

| Item | Why Needed | Simpler Alternative Rejected Because |
|------|------------|--------------------------------------|
| New `feature:savedproducts` module | Presentation state for saved products and purchases must not leak into `core:data` or `androidApp`, and `feature:diary`/`feature:scan` are empty scaffolds owned by other features. | Putting state in `androidApp` would make purchase state Android-only and break the constitution's shared-logic principle. |
| New navigation dependency | The bottom bar needs multiplatform destination routing with back-stack correctness (FR-007). | A hand-rolled `when(currentTab)` switch cannot model detail→back-to-origin correctly and would need rewriting for iOS. |
| Android billing SDK dependency | Real purchases require Google Play Billing; Play policy requires Billing 8+ in 2026 (9.1.0 is forward-safe to 2028). | Removing the purchase requirement, or a third-party KMP billing library, either drops the feature or adds a backend/fee (RevenueCat) or a deprecated Billing v6 (inAppPurchase-kmp). |
| Swift `StoreKit 2` bridge in `iosApp` | Kotlin/Native **cannot** import pure-Swift StoreKit 2 (cinterop binds only C/Objective-C; Kotlin's own docs state pure Swift modules are unsupported). | Expect/actual in `iosMain` alone is impossible for StoreKit 2; a Swift bridge is the only correct option. |
| Store-side configuration (external) | A real purchase cannot complete without configured paid products and payment accounts on both stores. | Cannot be avoided; the plan verifies against each platform's test environment and records the production prerequisite. |
| `shared` export of `:feature:savedproducts` | The iOS `iosApp` must reach the shared purchase contracts/state from Swift; `shared` currently exports only `core:data`, `core:domain`, and the four `feature:*` scaffolds. | Wrapping the contracts in `iosMain` alone is unnecessary duplication; the module export is the project's established mechanism (`shared` already exports `feature:search` etc.). |
| iOS UI surfaces for purchase and saved products | FR-014 requires the native purchase experience on both platforms and the spec states the iOS purchase path is reached from the iOS app surface; saved products must be viewable on iOS too (FR-002). Native SwiftUI is therefore required, not optional. | Delivering only the iOS `PurchaseClient` with no UI would leave the iOS user unable to buy anything, failing FR-014; a shared-Compose UI would violate the constitution's no-shared-UI principle. |
| UI states for empty list, in-progress purchase, and unavailable payments | FR-002, FR-018, and FR-019 each require a specific user-visible state, and they must appear on both platforms. | Omitting them would leave a blank saved list, a re-tappable purchase control that risks a double charge, and a silent failure when the store is unconfigured — each an explicit spec violation. The states are thin and reuse the existing shared `PurchaseState`/`PurchaseOutcome` types. |

## Requirement Traceability

Every functional requirement and success criterion maps to at least one task in
`tasks.md` and at least one automated check, so no requirement is implemented
without a tracking task.

| Requirement | Task IDs | Primary automated evidence |
|---|---|---|
| FR-001 (save from wherever shown; on-device) | T005, T014, T033, T035, T036 | `SavedProductRepositoryImplTest`, `SavedProductsViewModelTest`, `SaveProductScreenTest` |
| FR-002 (view saved; open details; empty state + Search shortcut) | T012, T034, T036 | `SavedProductsScreenTest`, `SavedProductsViewModelTest` |
| FR-003 (remove; must not reappear) | T014, T034, T036 | `SavedProductRepositoryImplTest`, `SavedProductsScreenTest` |
| FR-004 (no duplicates) | T012, T015, T034 | `SavedProductRepositoryImplTest`, `SavedProductsViewModelTest` |
| FR-005 (persistent bar; current destination indicated) | T024, T025, T027 | `BottomBarScreenTest` |
| FR-006 (Diary "not yet available") | T026, T027 | `DiaryPlaceholderScreenTest` |
| FR-007 (detail back returns to origin) | T027, T029 | `DetailNavigationTest` |
| FR-008 (bar does not obscure content; gesture nav; large text) | T025, T027 | `BottomBarAccessibilityTest` |
| FR-009 (start purchase; platform sheet) | T043, T044, T045 | `PurchaseViewModelTest`, `AndroidPurchaseClientTest`, `PurchaseFlowScreenTest` |
| FR-010 (confirm + persist purchased state) | T015, T034, T043 | `PurchaseRepositoryImplTest`, `PurchaseViewModelTest` |
| FR-011 (cancel/fail records nothing; clear outcome) | T015, T039, T043 | `PurchaseRepositoryImplTest`, `PurchaseFlowScreenTest` |
| FR-012 (never recorded twice; keyed on code) | T012, T015, T039 | `PurchaseRepositoryImplTest`, `PurchaseViewModelTest` |
| FR-013 (reconcile on launch; store is source of truth) | T015, T048 | `PurchaseRepositoryImplTest`, `PurchaseViewModelTest` |
| FR-014 (native IAP both platforms; no credentials) | T044, T045, T050, T052 | `AndroidPurchaseClientTest`, `PurchaseFlowScreenTest`, Swift `StoreKitPurchaseClient` |
| FR-015 (ineligible products offer no action) | T043, T049 | `PurchaseFlowScreenTest`, `PurchaseViewModelTest` |
| FR-016 (data stays on-device; no analytics) | T014, T015, T062 | `PrivacyNoEgressTest` |
| FR-017 (48 dp; accessible names; large text) | T025, T035, T043 | `BottomBarAccessibilityTest`, `PurchaseFlowScreenTest` |
| FR-018 (pending disables the control) | T039, T043 | `PurchaseFlowScreenTest`, `PurchaseViewModelTest` |
| FR-019 (unavailable shows guidance + retry) | T040, T043 | `PurchaseFlowScreenTest`, `AndroidPurchaseClientTest` |
| SC-001 (single action, no network, no restart) | T014, T034 | `SavedProductsViewModelTest` |
| SC-002 (survives reopen offline) | T014, T018 | `SavedProductRepositoryImplTest` |
| SC-003 (any destination in one interaction) | T025, T027 | `BottomBarScreenTest` |
| SC-004 (current destination not by colour alone) | T025 | `BottomBarAccessibilityTest` |
| SC-005 (purchased after restart; never twice) | T015, T019 | `PurchaseRepositoryImplTest` |
| SC-006 (cancel/fail never records) | T015, T019 | `PurchaseRepositoryImplTest`, `PurchaseViewModelTest` |
| SC-007 (no saved/purchase data to analytics) | T014, T015, T062 | `PrivacyNoEgressTest` |
| SC-008 (accessible controls; largest text) | T023, T025 | `BottomBarAccessibilityTest` |
| SC-009 (empty list message + working Search shortcut) | T032, T036 | `SavedProductsScreenTest` |
| SC-010 (pending cannot be re-activated) | T039, T041 | `PurchaseFlowScreenTest` |
| SC-011 (unavailable never a silent failure) | T041 | `PurchaseFlowScreenTest` |

## Production Prerequisites Not Satisfied

This feature was built and verified against **test environments only**. The
following external, store-side prerequisites are **not satisfied** in this
repository and are required before the purchase path can charge a real customer.
No reader should mistake the test-environment verification for a live, chargeable
release.

- **Store-side paid products**: no product is defined in Google Play Console or
  App Store Connect for the catalogue codes the app purchases, so the store
  reports no offering and the UI correctly shows "payments unavailable"
  (`Failed(NOT_CONFIGURED)` / `Unavailable`) rather than a purchase action.
- **Payment accounts and testers**: there is no Play Console internal-testing
  track build uploaded with a licensing tester, and no Xcode `.storekit`
  configuration file with a sandbox Apple ID, so a completed charge cannot be
  exercised end to end.
- **Policy compliance**: Google Play and Apple in-app purchase policy compliance
  for how the purchase action is presented has been reasoned about in ADR-0013/
  0014 but not submitted for store review; the restore-purchases affordance
  required by the App Store guideline is implemented (Android and iOS) but not
  exercised against a live store account.
- **Product identifier mapping**: the iOS `StoreKitPurchaseClient` derives App
  Store product identifiers from a configurable prefix
  (`fallbackProductIdPrefix`); the actual identifiers must be configured to match
  the products created in App Store Connect.

The purchase path is fully built, compiles on both platforms, and is verified by
unit, host, instrumented, and iOS simulator tests against fakes; the live charge
is the only step not exercised here.
