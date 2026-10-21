# Tasks: Saved Products Bottom Navigation and In-App Purchase

**Input**: Design documents from `/specs/004-saved-products-navbar/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Included. The project constitution (Principle III: Verify Behavior with Tests) mandates automated tests for new/changed behavior at the narrowest appropriate level, so test tasks are part of this task list, not optional.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing. US1 and US2 are both P1; US2 (the navigation shell) is sequenced first because Saved products is only reachable through the bar, and US1's acceptance criteria reference "the Saved products destination". US3 (purchase, P2) depends on US1 (a product eligible for purchase is saved-or-eligible) and US2 (the shell).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Kotlin Multiplatform mobile app (shared logic + native UI):

- `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/` — pure Kotlin contracts
- `core/data/src/{commonMain,androidMain,iosMain,androidHostTest}/` — persistence + platform clients
- `feature/savedproducts/src/{commonMain,commonTest}/` — presentation state
- `androidApp/src/{main,androidTest}/` — Android Compose UI
- `iosApp/iosApp/` — SwiftUI + Swift StoreKit 2 bridge
- `shared/build.gradle.kts` — iOS framework exports

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Register the three new dependencies (all ratified by ADRs 0013/0014/0015) and create the new feature module scaffold. No behavior yet.

- [X] T001 Add the navigation version + library to the catalog in `gradle/libs.versions.toml`: `[versions] navigation = "2.9.2"` and `[libraries] navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }` (ADR-0015)
- [X] T002 [P] Add the billing version + library to the catalog in `gradle/libs.versions.toml`: `[versions] billing = "9.1.0"` and `[libraries] billing-ktx = { module = "com.android.billingclient:billing-ktx", version.ref = "billing" }` (ADR-0014)
- [X] T003 Create the `feature/savedproducts` module directory structure and `feature/savedproducts/build.gradle.kts` mirroring `feature/search/build.gradle.kts` (plugins `nutrition.kmp-library`; `commonMain` depends on `:core:domain` + coroutines; `commonTest` depends on coroutines-test)
- [X] T004 Register `:feature:savedproducts` in `settings.gradle.kts` (include alongside the other `:feature:*` modules)
- [X] T005 [P] Add `:feature:savedproducts` to `androidApp/build.gradle.kts` `dependencies {}` as `implementation(project(":feature:savedproducts"))`, and add `implementation(libs.navigation.compose)` there (navigation is Android-UI-only, ADR-0015)

**Checkpoint**: Catalog entries exist and the empty module builds. `./gradlew :feature:savedproducts:compileKotlinMetadata` succeeds.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The shared contracts and the persistence layer that all three user stories build on. Per the constitution, contracts live in `core:domain` (pure Kotlin) and persistence in `core:data`.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T006 [P] Create the shared `PurchaseClient` contract in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/purchase/PurchaseClient.kt` exactly per `contracts/purchase-client.md`: `interface PurchaseClient { suspend fun queryProduct(code: String): PurchasableProduct?; suspend fun launchPurchase(code: String): PurchaseClientResult; suspend fun currentEntitlements(): List<VerifiedTransaction> }`, plus `data class PurchasableProduct(val code: String, val amountLabel: String)`, `sealed interface PurchaseClientResult` (`Verified(transaction)`, `UserCancelled`, `Failed(reason)`, `Unavailable`), and `data class VerifiedTransaction(val productCode: String, val transactionId: String, val purchaseToken: String?, val completedAtEpochMillis: Long)`
- [X] T007 [P] Create purchase state/failure types in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/purchase/PurchaseState.kt`: `enum class PurchaseFailure { USER_CANCELLED, DECLINED, NETWORK, NOT_CONFIGURED, VERIFICATION_FAILED, UNKNOWN }`, `sealed interface PurchaseState` (`Idle`, `Pending`, `Purchased`, `Cancelled`, `Failed(reason)`, `Unavailable`), and `sealed interface PurchaseOutcome` (`Success(record)`, `Cancelled`, `Failed(reason)`, `Unavailable(reason)`) per `data-model.md`
- [X] T008 [P] Create `PurchaseRecord` in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/purchase/PurchaseRecord.kt` with fields `productCode: String` (unique identity), `transactionId: String` (store-facing metadata), `status: String`, `purchaseToken: String?`, `completedAtEpochMillis: Long`; require `productCode.isNotBlank()` and `transactionId.isNotBlank()`
- [X] T009 [P] Create `SavedProduct` in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/purchase/SavedProduct.kt` with fields `code: String` (identity), `name: String`, `brands: String?`, `quantity: String?`, `nutriScoreGrade: String?`, `imageThumbnailUrl: String?`, `savedAtEpochMillis: Long`; require `code.isNotBlank()`, `name.isNotBlank()`; add a `SavedProduct.fromProduct(product: Product, savedAtEpochMillis: Long)` factory that copies display fields (contract in `data-model.md`)
- [X] T010 [P] Create `SavedProductRepository` contract in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/purchase/SavedProductRepository.kt`: `suspend fun save(product: SavedProduct)`, `suspend fun remove(code: String)`, `fun observeSaved(): Flow<List<SavedProduct>>`, `fun isSaved(code: String): Flow<Boolean>`
- [X] T011 [P] Create `PurchaseRepository` contract in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/purchase/PurchaseRepository.kt`: `fun purchase(code: String): Flow<PurchaseOutcome>`, `fun observePurchase(code: String): Flow<PurchaseRecord?>`, `fun observePurchasedCodes(): Flow<Set<String>>`, `suspend fun reconcile()`
- [X] T012 Add the `SavedProduct` and `PurchaseRecord` tables + queries to `core/data/src/commonMain/sqldelight/com/example/nutricionalregister/data/SavedProduct.sq`: `CREATE TABLE SavedProduct (code TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, brands TEXT, quantity TEXT, nutri_score_grade TEXT, image_thumbnail_url TEXT, saved_at_epoch_millis INTEGER NOT NULL);` and `CREATE TABLE PurchaseRecord (product_code TEXT NOT NULL PRIMARY KEY, transaction_id TEXT NOT NULL, status TEXT NOT NULL, purchase_token TEXT, completed_at_epoch_millis INTEGER NOT NULL);` (unique `product_code` = FR-012; `transaction_id` retained as store metadata). Add queries: `upsertSavedProduct` (`INSERT OR REPLACE`), `deleteSavedProduct`, `selectSavedProducts ORDER BY saved_at_epoch_millis DESC`, `selectSavedProductByCode`, `upsertPurchaseRecord` (`INSERT OR REPLACE` keyed on `product_code`, idempotent), `selectPurchaseRecordByProduct`, `selectPurchaseRecords`, `selectPurchasedProductCodes`
- [X] T013 Configure a forward SQLDelight migration for the two new tables in `core/data/build.gradle.kts` (`sqldelight { databases { create("ProductDatabase") { ... verifyMigrations / schemaOutputDirectory / srcDirs for migrations } } }` as required by the plugin version) so existing installs upgrade without recreating the database and cached search data survives (FR-001 offline persistence; `data-model.md` persistence section)
- [X] T014 [P] Implement `SavedProductRepositoryImpl` in `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/SavedProductRepositoryImpl.kt` against `ProductDatabase` on a `CoroutineDispatcher`; map rows to `SavedProduct`; `observeSaved`/`isSaved` return `Flow` over SQLDelight `asFlow()` (mirror `RecentSearchRepositoryImpl` patterns)
- [X] T015 [P] Implement `PurchaseRepositoryImpl` in `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/PurchaseRepositoryImpl.kt` over `PurchaseClient` + `ProductDatabase`: `purchase(code)` drives Pending → Verified/Cancelled/Failed and writes a `PurchaseRecord` only on a verified result (FR-011); `upsertPurchaseRecord` uses `INSERT OR REPLACE` keyed on `product_code` so a repeated confirmation for the same product is idempotent and never records two purchases (FR-012); `reconcile()` queries `client.currentEntitlements()` and upserts any verified transaction not already recorded (FR-013)
- [X] T016 Extend `ProductSearchRuntime` in `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/ProductSearchRuntime.kt` to expose `savedProductRepository: SavedProductRepository` and `purchaseRepository: PurchaseRepository` (add constructor params; keep `AutoCloseable`)
- [X] T017 Wire the new repositories in `core/data/src/androidMain/kotlin/com/example/nutricionalregister/data/PlatformDataFactories.kt` inside `createProductSearchRuntime`, and add a `createSavedProductsRuntime`-style factory (or extend the existing constructor call) that accepts a `PurchaseClient`; the Android `PurchaseClient` implementation is injected here (created in US3)
- [X] T018 [P] Android host tests for persistence in `core/data/src/androidHostTest/kotlin/com/example/nutricionalregister/data/SavedProductRepositoryImplTest.kt` using the existing `InMemoryProductDatabase` helper: saving persists and `observeSaved` lists it (FR-001/002); saved products survive a database reopen with no network (SC-002); saving the same code twice does not duplicate (FR-004); `remove` deletes only the saved row and does not touch `Product` (FR-003)
- [X] T019 [P] Android host tests for purchases in `core/data/src/androidHostTest/kotlin/com/example/nutricionalregister/data/PurchaseRepositoryImplTest.kt` with a fake `PurchaseClient`: a verified result writes exactly one record (FR-010); the record survives a database reopen and `observePurchasedCodes` reports it (SC-005); a second verified confirmation for the same product code writes no second record and updates rather than duplicates the row (FR-012); cancelled/failed/unverified writes no record (FR-011, SC-006); `reconcile()` records a verified outstanding product not yet stored and is a no-op when nothing is outstanding (FR-013)

**Checkpoint**: Contracts compile; persistence tests pass. `./gradlew :core:data:testAndroidHostTest :core:domain:testAndroidHostTest` green.

---

## Phase 3: User Story 2 - Move between the app's main areas (Priority: P1)

**Goal**: The persistent bottom navigation bar with Search, Saved products, and Diary, with correct current-destination indication, back-to-origin behaviour, insets, and a Diary placeholder.

**Independent Test**: From Search, use the bar to reach Saved products and Diary and back; verify the current destination is visibly indicated, Diary shows a "not yet available" state, and detail→back returns to the origin destination.

> Note: US2 is sequenced before US1 because the Saved products destination is only reachable through this bar and US1's acceptance criteria assume the destination exists.

### Tests for User Story 2

- [X] T020 [P] [US2] Instrumented test for the bar in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/navigation/BottomBarScreenTest.kt`: the bar shows exactly Search, Saved products, Diary; the current destination is indicated; selecting each destination shows its content (FR-005, SC-003, SC-004)
- [X] T021 [P] [US2] Instrumented test for back-to-origin in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/navigation/NavigationBackStackTest.kt`: opening a product detail from a destination and returning lands on that destination, not Search (FR-007)
- [X] T022 [P] [US2] Instrumented test for the Diary placeholder in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/navigation/DiaryPlaceholderTest.kt`: selecting Diary shows an explicit "not yet available" message, not a blank screen (FR-006)
- [X] T023 [P] [US2] Instrumented accessibility + large-text test in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/navigation/BottomBarAccessibilityTest.kt`: each destination has an accessible name, a ≥48 dp touch target, and remains readable/selectable at the largest system text size (FR-008, FR-017, SC-008)

### Implementation for User Story 2

- [X] T024 [US2] Define the destinations in `androidApp/src/main/kotlin/com/example/nutricionalregister/ui/NutriLogDestination.kt`: an enum/sealed list with route (`search`, `saved`, `diary`), label, and icon; plain data so a future SwiftUI navigation can reuse the model (ADR-0015)
- [X] T025 [US2] Implement the bottom bar composable in `androidApp/src/main/kotlin/com/example/nutricionalregister/ui/NutriLogBottomBar.kt`: Material 3 `NavigationBar`/`NavigationBarItem` driven by `NutriLogDestination`; 48 dp targets; selected state conveyed by icon + label + selected flag (not colour alone) (FR-005, FR-017)
- [X] T026 [US2] Create the Diary placeholder screen in `androidApp/src/main/kotlin/com/example/nutricionalregister/diary/DiaryPlaceholderScreen.kt`: a clear "not yet available" state with a title and explanatory line (FR-006)
- [X] T027 [US2] Create the app shell in `androidApp/src/main/kotlin/com/example/nutricionalregister/AppShell.kt` using `org.jetbrains.androidx.navigation.compose` (`NavHost` + `rememberNavController`): three destinations from `NutriLogDestination`; the shared `NutriLogBottomBar` in the `Scaffold`'s `bottomBar` so insets are consumed and content is not obscured, including gesture navigation (FR-008); each destination keeps its own back stack; re-selecting the current destination does not push a duplicate entry
- [X] T028 [US2] Update `androidApp/src/main/kotlin/com/example/nutricionalregister/App.kt` to host `AppShell` instead of rendering `ProductSearchScreen` directly, keeping the existing `ProductSearchDependencies` creation and `DisposableEffect` disposal
- [X] T029 [US2] Update `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductSearchScreen.kt` so a product opened within the Search destination renders detail *inside* that destination's nav graph (back returns to Search) rather than being the whole app; expose a hook so the same detail can be opened from the Saved products destination (FR-007)

**Checkpoint**: US2 is fully functional and testable on its own. The bar navigates all three destinations; Diary shows the placeholder; back-to-origin works.

---

## Phase 4: User Story 1 - Save products and revisit them (Priority: P1) - MVP

**Goal**: Save a product from wherever it is shown, list saved products in the Saved products destination, open them, remove them, with no duplicates and full offline persistence.

**Independent Test**: Search for a product, save it, open Saved products and see it, open it for details, return to Saved products; force-stop offline and confirm it is still listed; re-save it and confirm no duplicate; remove it and confirm it does not return from a new search.

### Tests for User Story 1

- [X] T030 [P] [US1] Common tests for saved-products presentation state in `feature/savedproducts/src/commonTest/kotlin/com/example/nutricionalregister/feature/savedproducts/SavedProductsViewModelTest.kt` with fake repositories (mirror `feature/search/.../testing/FakeRepositories.kt`): list reflects `observeSaved`; saving updates `isSaved` immediately in a single action with no network call (SC-001); removing removes from the list (FR-002, FR-003, FR-004)
- [X] T031 [P] [US1] Instrumented test for saving from search/detail in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/savedproducts/SaveProductScreenTest.kt`: the save control appears on the detail view and marks the product saved wherever shown afterwards (FR-001)
- [X] T032 [P] [US1] Instrumented test for the saved list + removal in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/savedproducts/SavedProductsScreenTest.kt`: saved products list renders (incl. offline/placeholder-image entries gracefully), each opens for detail, removal removes it, and an empty list shows the explicit empty state whose Search shortcut navigates to Search (FR-002, FR-003, SC-009, edge cases)

### Implementation for User Story 1

- [X] T033 [P] [US1] Create `SavedProductsState` in `feature/savedproducts/src/commonMain/kotlin/com/example/nutricionalregister/feature/savedproducts/SavedProductsState.kt`: the list, loading/empty states, and per-product saved flags
- [X] T034 [US1] Create `SavedProductsViewModel` in `feature/savedproducts/src/commonMain/kotlin/com/example/nutricionalregister/feature/savedproducts/SavedProductsViewModel.kt`: consumes `SavedProductRepository`; exposes `state`; `save(product: Product)`, `remove(code)`, `openProduct(code)`, `closeProductDetails()`; uses an `EpochClock` for `savedAtEpochMillis` (mirror `ProductSearchViewModel` structure and scope/lifecycle)
- [X] T035 [US1] Add a save/remove control to the product detail view in `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductDetailScreen.kt`, wired to the saved-products state, with a 48 dp target and accessible name (FR-001, FR-017). The detail view is the primary save surface per US1 acceptance scenario 1; do not also add it to `ProductResultCard.kt`.
- [X] T036 [US1] Create the Saved products destination in `androidApp/src/main/kotlin/com/example/nutricionalregister/savedproducts/SavedProductsScreen.kt`: renders the saved list from `SavedProductsState`, reuses `ProductResultCard`/`ProductImage` (offline placeholder handled), opens detail within the destination, and offers removal (FR-002, FR-003); when the list is empty it renders an explicit "no saved products yet" message with a shortcut that navigates to the Search destination (FR-002, SC-009)
- [X] T037 [US1] Create `androidApp/src/main/kotlin/com/example/nutricionalregister/savedproducts/SavedProductsDependencies.kt` (mirror `ProductSearchDependencies`) constructing `SavedProductsViewModel` from the shared runtime's `savedProductRepository`
- [X] T038 [US1] Wire the Saved products destination into the shell in `androidApp/src/main/kotlin/com/example/nutricionalregister/AppShell.kt`, replacing the US2 placeholder for `saved` with `SavedProductsScreen` and its dependencies

**Checkpoint**: US1 is the MVP. Saving, listing, opening, removing, no duplicates, and offline persistence all work on their own.

---

## Phase 5: User Story 3 - Buy a product through the platform payment system (Priority: P2)

**Goal**: Real in-app purchase of an eligible product through each platform's own payment sheet on both Android and iOS, with confirmed + persisted purchased state, no double-recording, launch reconciliation, and clear cancel/fail outcomes.

**Independent Test**: With a test payment account configured, start a purchase for a product that is eligible for purchase, complete the platform payment sheet, confirm the app shows a confirmed purchase that persists after restart; cancelling leaves no record.

**Store-side prerequisite**: Completing a real purchase requires configured paid products and payment accounts on both stores (spec Assumptions; `quickstart.md` Prerequisites). Without them the path is built and unit-tested against fakes but cannot be completed end-to-end — that limitation is expected and reported, not hidden.

### Tests for User Story 3

- [X] T039 [P] [US3] Common tests for purchase state machine in `feature/savedproducts/src/commonTest/kotlin/com/example/nutricionalregister/feature/savedproducts/PurchaseViewModelTest.kt` with a fake `PurchaseClient`/`PurchaseRepository`: Idle→Pending→Purchased writes and shows purchased (SC-005); Cancelled→no record (SC-006); Failed→retry offered (SC-006); a second start while `Pending` is refused (FR-018, SC-010); unavailable product offers no action (FR-009, FR-011, FR-015)
- [X] T040 [P] [US3] Android host test for the Android `PurchaseClient` mapping in `core/data/src/androidHostTest/kotlin/com/example/nutricionalregister/data/AndroidPurchaseClientTest.kt`: Play Billing responses map to `Verified`/`UserCancelled`/`Failed(NOT_CONFIGURED)`/`Unavailable` correctly (no live store calls)
- [X] T041 [P] [US3] Instrumented test for purchase UI states in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/savedproducts/PurchaseFlowScreenTest.kt`: Idle/Pending/Purchased/Cancelled/Failed/Unavailable each render correctly; while `Pending` the purchase control is disabled and cannot be activated a second time (FR-018, SC-010); `Unavailable`/`NOT_CONFIGURED` shows guidance with a retry control rather than failing silently (FR-019, SC-011); the app collects no card details (FR-011, FR-014)

### Implementation for User Story 3

- [X] T042 [US3] Create `PurchaseSheetState` in `androidApp/src/main/kotlin/com/example/nutricionalregister/savedproducts/PurchaseSheetState.kt` mapping `PurchaseState`/`PurchaseOutcome` to UI
- [X] T043 [US3] Add purchase UI to the product detail / saved product view in `androidApp/src/main/kotlin/com/example/nutricionalregister/savedproducts/PurchaseSection.kt`: a purchase action shown only for eligible products (FR-015), driven by `PurchaseSheetState`, showing the store's own price label; renders the `Unavailable`/`NOT_CONFIGURED` state with guidance and a retry control instead of failing silently (FR-019); the control is disabled while the attempt is `Pending` so no second purchase can start (FR-018); 48 dp target + accessible name (FR-017)
- [X] T044 [US3] Implement the Android `PurchaseClient` in `core/data/src/androidMain/kotlin/com/example/nutricionalregister/data/AndroidPurchaseClient.kt` using `com.android.billingclient:billing-ktx` 9.1.0: `queryProduct` via `queryProductDetails`; `launchPurchase` via `launchBillingFlow` (Activity supplied by the app composition root); acknowledge/consume on verified completion; `currentEntitlements` via `queryPurchasesAsync`; map missing configuration to `Failed(NOT_CONFIGURED)` (ADR-0014; `contracts/purchase-client.md`)
- [X] T045 [US3] Implement the iOS `PurchaseClient` in Swift at `iosApp/iosApp/StoreKitPurchaseClient.swift` using StoreKit 2: `Product.products(for:)`, `product.purchase()`, grant only on `.success(.verified(transaction))`, `transaction.finish()`, `Transaction.currentEntitlements` + `AppStore.sync()`; map `.userCancelled`/`.unverified` appropriately (ADR-0013; `contracts/purchase-client.md`)
- [X] T046 [US3] Export the shared purchase API for iOS: add `:feature:savedproducts` to `shared/build.gradle.kts` (`api(project(...))` and `export(project(...))`), and provide the Swift↔Kotlin wiring so the Swift `StoreKitPurchaseClient` is injected as the shared `PurchaseClient` (no StoreKit calls in `iosMain`)
- [X] T047 [US3] Add the Android billing dependency in `core/data/build.gradle.kts` `sourceSets.androidMain.dependencies { implementation(libs.billing.ktx) }` and inject the `AndroidPurchaseClient` (with the Activity) through `createProductSearchRuntime`/the app composition root in `androidApp/src/main/kotlin/com/example/nutricionalregister/App.kt` and `savedproducts/SavedProductsDependencies.kt`
- [X] T048 [US3] Wire reconcile-on-launch (FR-013): call `PurchaseRepository.reconcile()` from the purchase dependencies/view-model initialization so the platform store (the source of truth) decides the purchased state — a product the store reports as owned is shown as purchased, and one it does not report stays unpaid
- [X] T049 [US3] Verify purchase eligibility + iOS build wiring: confirm only eligible products show a purchase action (FR-015) and that `./gradlew :shared:assembleSharedXCFramework` plus `xcodebuild -scheme iosApp` succeed with the Swift bridge compiled (ADR-0013 validation record)
- [X] T050 [US3] Implement the iOS purchase UI surface in `iosApp/iosApp/ContentView.swift` (and any supporting SwiftUI view it needs): show the purchase action for eligible products, the store's price label, and the Idle/Pending/Purchased/Cancelled/Failed/Unavailable states by observing the shared purchase state exported from `shared`; while `Pending` the control is disabled (FR-018) and `Unavailable`/`NOT_CONFIGURED` shows guidance with a retry control (FR-019); this is the "iOS app surface" the spec requires for FR-014 and US3 acceptance scenario 6, so the iOS purchase path is reachable, not just wired
- [X] T051 [US3] Add the iOS saved-products surface in `iosApp/iosApp/ContentView.swift` (a saved-products list reachable from the iOS app, backed by the shared `SavedProductRepository` exported through `shared`, including the explicit empty state when nothing is saved), so the saved products the user creates are visible on iOS as well as Android (FR-002, SC-009 on iOS); if iOS UI for saved products is deliberately deferred, record that as an intentional platform difference in `specs/004-saved-products-navbar/plan.md` instead of silently omitting it
- [X] T052 [US3] Implement the "restore purchases" affordance required by the App Store guideline (`contracts/purchase-client.md` line 73): a user-triggered restore control in the iOS purchase UI (and the Android equivalent) that calls `PurchaseRepository.reconcile()` / the platform restore API, with a clear result message; add a test that restore reconciles an existing verified transaction into a `PurchaseRecord`
- [X] T053 [US3] Implement `PurchaseAttempt` as the in-memory entity from `data-model.md` (fields `productCode`, `amountLabel`, `state`) inside `androidApp/src/main/kotlin/com/example/nutricionalregister/savedproducts/PurchaseSheetState.kt` (linked from T042), so the entity named in the data model has a corresponding implementation; `data-model.md` already records that `PurchaseSheetState` is that implementation, so no duplicate entity is introduced

**Checkpoint**: All three user stories work on both platforms. Purchases complete against each platform's test environment; cancelled/failed attempts leave no record; purchased state survives restart; the iOS purchase and saved-products surfaces are reachable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification, docs, measurement, and the honest limitations record.

- [X] T054 [P] Run `quickstart.md` §2 verification gate (`:core:domain:allTests :core:data:allTests :feature:search:allTests :feature:savedproducts:allTests :shared:testAndroidHostTest :architectureTest :coverageVerify :kotlinLint :plainTextCheck :androidApp:assembleDebug :androidApp:lint`) and record the result; use `:core:domain:testAndroidHostTest` / `:core:domain:ktlintCommonMainSourceSetCheck` where task names are ambiguous
- [X] T055 Run the Android instrumented suite (`./gradlew :androidApp:connectedDebugAndroidTest`) on emulator-5556 and record the passing count
- [X] T056 Run the iOS simulator tests for shared behaviour (`./gradlew :shared:iosSimulatorArm64Test`) as required by the constitution's Development Workflow, and record the result (this covers the shared purchase-state and saved-product logic on the iOS target)
- [X] T057 Record the `Shared.xcframework` size and the release APK size; assert the XCFramework is unchanged (ADR-0013/0014/0015 binary-size claims) and record the APK delta for the navigation + billing dependencies
- [X] T058 [P] Record the measured **hosted CI durations** for the new work in `specs/004-saved-products-navbar/quickstart.md` and confirm they stay within the project's under-10-minute target (as with feature 002)
- [X] T059 [P] Update `AI-LOG.md` with the 004 implementation entry (decisions, evidence, measured sizes/durations)
- [X] T060 [P] Complete the **Validation record** sections of `docs/adr/0013-swift-storekit2-bridge.md`, `docs/adr/0014-google-play-billing.md`, and `docs/adr/0015-compose-multiplatform-navigation.md` with the measured values, and confirm each ADR's stated exit strategy still holds
- [X] T061 Record, in a new section of `specs/004-saved-products-navbar/plan.md` (or an appended note), the **production prerequisites not satisfied**: store-side paid products, payment accounts, licensing testers / `.storekit` config, and Play/Apple IAP policy compliance — so no reader mistakes test-environment verification for a live, chargeable release
- [X] T062 [P] Add an automated privacy assertion for FR-016 / SC-007: a test that exercises save and purchase flows with a fake/instrumented network layer and asserts no saved-product or purchase data leaves the device (no analytics or tracking call); audit `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/SavedProductRepositoryImpl.kt` and `PurchaseRepositoryImpl.kt` as part of this task
- [X] T063 Update the 004 spec `Status` from `Approved` to `Implemented` and add an FR/SC traceability table to `specs/004-saved-products-navbar/plan.md` (mirroring feature 002)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup (catalog entries + module exist) — **BLOCKS all user stories**.
- **User Stories (Phases 3–5)**: All depend on Foundational completion.
  - **US2 (Phase 3)** can start right after Foundational. It has no dependency on US1.
  - **US1 (Phase 4)** depends on US2 for its destination to be reachable, and on Foundational (`SavedProductRepository`).
  - **US3 (Phase 5)** depends on US1 (a product eligible for purchase/saved) and US2 (the shell/detail host).
- **Polish (Phase 6)**: Depends on all desired stories being complete.

### User Story Dependencies

- **US2 (P1, navigation)**: after Foundational. Independently testable (bar + placeholder + back-to-origin).
- **US1 (P1, saved products)**: after Foundational and US2. Independently testable (save/list/open/remove/offline).
- **US3 (P2, purchase)**: after Foundational, US1, US2. Independently testable against a test store environment. Includes the iOS purchase and saved-products UI surfaces (T050, T051), so FR-014's "both platforms" requirement is genuinely delivered, not only wired.

### Within Each User Story

- Tests are written first and expected to fail, then implementation.
- Contracts (Phase 2) → repositories (Phase 2) → state/view-models → UI.
- Story complete before moving to the next priority.

### Parallel Opportunities

- Setup: T002 and T005 can run alongside the others; T001/T003/T004 are sequential touch-points on shared files.
- Foundational: T006–T011 (contracts, distinct files) and T014/T015 (repositories) and T018/T019 (tests) are parallelizable; T012/T013 (schema + migration) and T016/T017 (wiring) are sequential.
- US2: T020–T023 (tests) in parallel; T024/T025/T026 in parallel, then T027 → T028 → T029.
- US1: T030–T032 (tests) in parallel; T033 + T035 in parallel, then T034 → T036 → T037 → T038.
- US3: T039–T041 (tests) in parallel; T042 + T045 in parallel, then T043/T044 → T046 → T047 → T048 → T049 → T050 → T051 → T052 → T053.
- Polish: T054, T058–T060, T062 are parallelizable.

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch all domain contracts together (different files, no interdependencies):
Task: "Create PurchaseClient contract in core/domain/.../purchase/PurchaseClient.kt"
Task: "Create PurchaseState types in core/domain/.../purchase/PurchaseState.kt"
Task: "Create PurchaseRecord in core/domain/.../purchase/PurchaseRecord.kt"
Task: "Create SavedProduct in core/domain/.../purchase/SavedProduct.kt"
Task: "Create SavedProductRepository contract in core/domain/.../purchase/SavedProductRepository.kt"
Task: "Create PurchaseRepository contract in core/domain/.../purchase/PurchaseRepository.kt"
```

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together:
Task: "Saved-products state tests in feature/savedproducts/src/commonTest/.../SavedProductsViewModelTest.kt"
Task: "Save-from-detail instrumented test in androidApp/src/androidTest/.../SaveProductScreenTest.kt"
Task: "Saved list/removal instrumented test in androidApp/src/androidTest/.../SavedProductsScreenTest.kt"
```

---

## Implementation Strategy

### MVP First (US2 + US1)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories).
3. Complete Phase 3: US2 (navigation shell).
4. Complete Phase 4: US1 (save/list/remove/offline) → **STOP and VALIDATE** the MVP independently.
5. Deploy/demo the saved-products + navigation experience.

### Incremental Delivery

1. Setup + Foundational → foundation ready.
2. US2 → the app has its shape → validate.
3. US1 → save/revisit works → validate → **MVP**.
4. US3 → purchase against a test store → validate.
5. Polish → verification, measurements, documentation, and the honest limitations record.

### Notes

- [P] tasks = different files, no dependencies on incomplete tasks.
- The `architectureTest` (Konsist) is currently inert (package-prefix mismatch, plan.md finding); do **not** rely on it as the boundary guard — keep boundaries correct by construction, and treat a prefix fix as a separate change.
- Commit after each task or logical group; keep 004 changes on branch `004-saved-products-navbar`.
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence.
- Store-side configuration (paid products, accounts, testers) is an external prerequisite; build and verify against test environments and record the limitation (T061).
- Documentation, specs, and code must be plain text (constitution Principle V): no emoji glyphs. The `plainTextCheck` Gradle task (`scripts/check-no-emoji.sh`) enforces this and runs in CI.
- The iOS payments work includes Swift UI surfaces (T050, T051) and the StoreKit 2 bridge (T045); the shared purchase state is verified on the iOS target by T056.
