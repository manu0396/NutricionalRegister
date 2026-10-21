# Implementation Plan: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21
**Spec**: [spec.md](spec.md)
**Research**: [research.md](research.md)
**Data Model**: [data-model.md](data-model.md)
**Contracts**: [contracts/app-restructure-contracts.md](contracts/app-restructure-contracts.md)
**ADRs**: [0016](../docs/adr/0016-ai-assistant-remote-llm-proxy.md), [0017](../docs/adr/0017-barcode-scanning-camera-mlkit.md), [0018](../docs/adr/0018-single-premium-product.md), [0019](../docs/adr/0019-tracker-shell-navigation-model.md)

## Summary

Turn the app from a catalogue browser with a save button into a nutrition tracker:
a Today home with meal-based diary logging, a daily and weekly summary against
editable goals, a personal food library, barcode scanning, a conversational
coach, a corrected navigation model, and a commercial design system applied to
every screen.

The work spans the shared KMP core (new diary, assistant and navigation domains,
new persistence, new feature modules) and the Android Compose UI (new shell, three
destinations plus a FAB, new screens, a full re-skin). iOS keeps compiling; the
SwiftUI screens are not rebuilt.

## Technical Context

- **Language/Framework**: Kotlin 2.4 (KMP), Jetpack Compose (Android), SwiftUI (iOS)
- **UI**: Compose Multiplatform Material 3 (`compose-material3` 1.12.0-alpha03)
- **Navigation**: `org.jetbrains.androidx.navigation:navigation-compose` 2.9.2
  (existing, ADR-0015), scoped to `androidApp`
- **Persistence**: SQLDelight 2.4.1 (existing), schema v2 -> v3 migration
- **Async**: kotlinx.coroutines 1.11.0, Flow
- **HTTP**: Ktor 3.6.0 (existing, ADR-0005) - reused for the AI gateway, so no
  new HTTP dependency
- **Images**: Coil 3.6.3 (existing)
- **Icons**: `androidx.compose.material:material-icons-core` (existing, ADR-0011)
- **Camera/barcode**: CameraX and ML Kit barcode scanning (NEW, ADR-0017) -
  `androidApp` only
- **Testing**: kotlin-test (common), JUnit 4 + Compose UI Test (androidTest),
  Konsist (architecture), Kover (coverage, 80 % gate on `core:domain`/`core:data`)

### New dependencies

| Dependency | Scope | ADR |
|---|---|---|
| `androidx.camera:camera-core` / `camera-camera2` / `camera-lifecycle` / `camera-view` | `androidApp` only | ADR-0017 |
| `com.google.mlkit:barcode-scanning` (bundled model) | `androidApp` only | ADR-0017 |

No other new dependency is introduced. The AI gateway reuses the already-approved
Ktor client, so constitution rule III is satisfied for everything else.

## Project Structure (after the restructure)

```
core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/
├── product/                 (unchanged)
├── purchase/                (unchanged, plus the premium product constant)
├── diary/                   (new in this feature, already committed)
│   ├── MealType.kt, DiaryEntry.kt, DiaryDay.kt, DiaryRepository.kt
│   ├── NutritionTotals.kt, NutritionGoal.kt, NutritionGoalRepository.kt
│   ├── NutritionCalculator.kt, DayBoundaryProvider.kt
├── navigation/              NEW - platform-neutral route model
│   └── NutriLogDestination.kt, NutriLogRoutes.kt
└── assistant/               NEW - conversational logging domain
    ├── AssistantMessage.kt, AssistantAction.kt, AssistantContext.kt
    ├── AssistantReply.kt, AssistantClient.kt, AssistantActionExecutor.kt

core/data/src/commonMain/
├── sqldelight/.../DiaryEntry.sq        NEW (DiaryEntry + NutritionGoal tables)
├── sqldelight/.../2.sqm                 NEW (migration v2 -> v3)
├── sqldelight/databases/{1,2,3}.db      NEW (2.db and 3.db reconstructed)
└── kotlin/.../DiaryRepositoryImpl.kt, NutritionGoalRepositoryImpl.kt  NEW
└── kotlin/.../assistant/AiGatewayClient.kt, AssistantClientImpl.kt    NEW
└── kotlin/{androidMain,iosMain}/.../SystemDayBoundaryProvider.kt     NEW

feature/diary/src/commonMain/.../feature/diary/      NEW
├── DiaryState.kt, DiaryViewModel.kt
feature/summary/src/commonMain/.../feature/summary/   NEW
├── DailySummary.kt, WeeklySummary.kt, SummaryCalculator.kt
feature/assistant/src/commonMain/.../feature/assistant/  NEW MODULE
├── AssistantState.kt, CoachViewModel.kt
feature/scan/src/commonMain/.../feature/scan/          NEW
├── BarcodeResolution.kt, BarcodeResolver.kt
feature/savedproducts/                    KEPT - not renamed (see decisions)

androidApp/src/main/kotlin/com/example/nutricionalregister/
├── App.kt, AppShell.kt                   (rewritten navigation shell)
├── ui/
│   ├── NutriLogDestination.kt            (removed - the model is shared now)
│   ├── NutriLogBottomBar.kt              (three destinations)
│   ├── NutriLogScaffold.kt, NutriLogFloatingActionButton.kt
│   ├── theme/                            (new design system)
│   └── components/                       (shared building blocks)
├── today/TodayScreen.kt                  NEW
├── addfood/AddFoodScreen.kt              NEW (nested graph, two steps)
├── library/LibraryScreen.kt              NEW (replaces the saved-products list)
├── more/MoreScreen.kt                    NEW
├── coach/CoachScreen.kt                  NEW
├── weekly/WeeklySummaryScreen.kt         NEW
├── scan/                                 NEW (Android camera only)
│   ├── BarcodeScannerScreen.kt, BarcodeDecoder.kt, MlKitBarcodeDecoder.kt
├── search/                               (kept, detail becomes a route, re-skinned)
└── savedproducts/                        (purchase section moved to More)
```

## Decisions taken in this feature

| # | Decision | Rationale |
|---|---|---|
| D-15 | The bottom bar has three destinations - Today, Library, More - plus a FAB | The complaint was that the app opens on a search box. A catalogue is an input to a diary, not a peer of it |
| D-16 | Product detail becomes the route `product/{code}` | Fixes D2, D3 and D4 by construction; every entry point behaves identically |
| D-17 | The add-food flow is a nested graph; the meal picker is a modal sheet | A modal is not navigation and does not need a back-stack entry |
| D-18 | Bar-bottom-bar visibility is derived from the route hierarchy | An enum lookup silently returns "no tab" inside a nested graph |
| D-19 | The route model moves into `core:domain` as plain data | ADR-0015 promised a platform-neutral destination model; the iOS shell will need it |
| D-20 | `feature:savedproducts` is NOT renamed to `feature:library` | The user decided against the rename. The rename would churn the dependency allowlist, the framework exports and every import, with no product value |
| D-21 | The design work is a full re-skin of every screen, not a token change | The user rejected a token-only pass; the legacy visuals are the problem |
| D-22 | The purchase surface is re-pointed to one premium product and removed from the food screens | Per-food pricing on a nutrition tracker is not a credible commercial model |
| D-23 | The Coach is the assistant screen; no rule-based insight engine is built | The user chose the assistant as the Coach surface |
| D-24 | The assistant talks to a remote LLM through a first-party gateway | The user chose a remote LLM. The provider key must not ship in the APK |
| D-25 | Barcode scanning is in scope, implemented with CameraX and ML Kit | The user confirmed it; the bundled model keeps decoding offline |
| D-26 | The weekly summary is reachable from a Today card as a child route | It is the "progress over time" surface, and it does not need a fourth tab |
| D-27 | `applicationId` is unchanged | It is immutable after a first publish |

## Approach

### Phase 0 - Spec kit artifacts

Commit the spec kit, then record the restructure, the rejected directions and the
four navigation defects in `AI-LOG.md`.

### Phase 1 - Design system

- Replace the green/amber candy palette with a cool neutral ramp and one deep
  evergreen accent. Neutrals carry the layout; the accent is reserved for the
  primary action and the selected tab.
- Rebuild the type scale so all fifteen Material 3 slots are defined, with
  distinct numeric roles and tabular figures for the quantities.
- Reduce the shape radii from 16-24 dp to a restrained 8-14 dp scale.
- Keep `NutriLogSpacing` as the single spacing scale and apply it everywhere.
- Add shared components under `ui/components`: section header, progress bar,
  macro row, stat tile, list item, surface card, empty state.
- Replace the saturated pastel product placeholder with a neutral monogram.
- Replace the two inconsistent app bars with the single shared app bar.

### Phase 2 - Shared domain

- `diary/` - already committed: `MealType`, `DiaryEntry`, `DiaryDay`,
  `NutritionTotals`, `NutritionGoal`, `NutritionCalculator`,
  `DayBoundaryProvider` and the two repository interfaces.
- `navigation/` - `NutriLogDestination` and `NutriLogRoutes` as plain data.
- `assistant/` - the message, action, context and reply types, the
  `AssistantClient` contract, and an `AssistantActionExecutor` that validates and
  applies an action against the diary repository.
- `purchase/` - the single premium product code as one shared constant.

### Phase 3 - Persistence

- `DiaryEntry.sq`: the `DiaryEntry` table (id autoincrement, product identity,
  meal, quantity, nutrient snapshot, timestamps, `day_epoch_millis`) and the
  `NutritionGoal` singleton table, with their queries.
- `2.sqm`: the additive migration (CREATE TABLE only) from schema v2 to v3.
- `databases/2.db`: the v2 snapshot, reconstructed from `1.db` plus `1.sqm`, and
  `databases/3.db`: the current snapshot. Both are committed so the migration
  chain is verifiable from v1 on a fresh checkout.
- `DiaryRepositoryImpl` and `NutritionGoalRepositoryImpl` over the new tables.
- `ProductSearchRuntime` gains the two new repositories.
- `SystemDayBoundaryProvider` for Android and iOS, backed by the platform
  calendar APIs, so days are stepped as calendar days rather than as 86 400 000
  milliseconds.

### Phase 4 - Shared presentation logic

- `feature:diary`: `DiaryState` (entries by meal, totals, goal, loading) and
  `DiaryViewModel` with the intents the Today screen needs, plus the day-change
  refresh.
- `feature:summary`: `SummaryCalculator` producing the daily summary (entries
  grouped by meal, totals, remaining against goals) and the weekly summary
  (seven days, per-day totals, average over days with entries).
- `feature:assistant`: `AssistantState` (the transcript, the pending proposal,
  the consent state, the availability state) and `CoachViewModel`.
- `feature:scan`: `BarcodeResolver`, which turns a scanned code into a catalogue
  product through the existing `ProductSearchRepository.getProduct`, and models
  the "not in the offline cache" outcome.

All four get unit tests for their calculation and decision rules, including the
"empty day is not zero intake" rule.

### Phase 5 - Navigation model

- The shared `NutriLogDestination` replaces the Android-local enum.
- The shell renders a `NavHost` whose top-level routes are `today`, `library`
  and `more`, plus the nested `addfood` graph and the child routes
  `product/{code}`, `coach` and `weekly`.
- Destination selection navigates to the destination root with
  `popUpTo(start) { inclusive = false }` and `launchSingleTop = true`, so any
  child screen above the home is removed. Re-selecting the current destination
  is a no-op.
- The bottom bar is visible only on the three top-level routes. The visibility
  is derived from the route hierarchy, not from a destination lookup.
- The FAB opens the add-food graph from `today` and `library`.
- The product detail is resolved from the local cache through
  `ProductSearchRepository.getProduct(code)`, so it works offline.

### Phase 6 - Android UI

**6.1 Today screen**

- Date in the app bar, with the day of the week, and a coach action.
- Summary card: calories consumed against the goal with a progress ring, then
  three macro rows with their own progress bars and remaining amounts.
- Four meal sections, each with an icon, the meal name, the meal's calorie
  subtotal, the list of entries, and an add action. An empty meal shows a
  compact empty state with the add action.
- An entry row shows the product name, the quantity and the entry's calories,
  and exposes a remove action.
- A seven-day card at the end, with a link to the weekly screen.

**6.2 Add-food flow**

- A nested graph with two steps: source and quantity.
- Source step: three segments - search, my foods, and scan. Search carries the
  existing search behaviour; my foods lists the library; scan opens the camera.
- Quantity step: the product summary, a quantity field in grams, quick
  multipliers of the product's serving, the resulting calories and macros, a
  meal selector that opens a modal sheet, and a confirm action.

**6.3 Library screen**

- The saved products as a list of rows with the add action and a remove action.
- An empty state that explains the purpose and opens the add-food flow on its
  search step.

**6.4 More screen**

- Goals section with editable targets.
- Coach section with the assistant entry and the premium unlock.
- Privacy section stating that assistant messages are sent to a third-party AI
  service.
- About section with the version and the Open Food Facts attribution.

**6.5 Coach screen**

- The transcript, the composer, the consent disclosure on first use, the
  explicit "not configured" state, and a confirmation sheet for every proposed
  action.

**6.6 Scanner**

- The camera preview, the runtime permission request with an explanation, the
  manual-entry fallback, and the honest "not in the offline cache" state.

**6.7 Search and product detail**

- The search screen keeps its behaviour and is re-skinned to the new system.
- The product detail becomes a route and gains an "Add to diary" action. Its
  back control says where it will actually return to.

### Phase 7 - Tests and validation

- Unit tests for the new domain, data, diary, summary, assistant and scan
  modules.
- Instrumented tests for the new screens and for the corrected navigation
  (tab reset, back behaviour, no stale detail, bar visibility).
- The existing instrumented tests are updated for the new destinations and
  routes.
- The full verification gate is run before the branch is proposed.

## Migration and data compatibility

The schema goes from version 2 to version 3 with an additive migration:

```sql
CREATE TABLE DiaryEntry (...);
CREATE INDEX diary_entry_day_index ON DiaryEntry(day_epoch_millis);
CREATE TABLE NutritionGoal (...);
```

No existing table is altered or dropped, so saved products, purchase records and
the catalogue cache survive the upgrade untouched. SQLDelight's
`verifyMigrations` stays enabled and validates the chain `1.db` -> `2.db` ->
`3.db` against the generated schema.

## Risk mitigation

| Risk | Mitigation |
| --- | --- |
| The navigation rewrite breaks existing instrumented tests | The navigation tests are rewritten in the same change and cover the new model explicitly |
| The theme change touches every screen | The theme lands first, then the screens are migrated one at a time, compiling after each |
| The new shared code drops below the coverage gate | Tests are written with the implementation, module by module |
| The module dependency policy rejects the new module edges | The allowlist is updated in the same commit that adds the dependency |
| The migration fails verification | `verifyMigrations` is enabled, so the failure surfaces in the build, not on a device |
| The scanner dependencies inflate the APK | They are `androidApp`-only, R8-shrinkable, and the size is measured and recorded in ADR-0017 |
| The assistant cannot be demonstrated because no gateway is deployed | The disabled state is explicit and honest; the shared domain is provider-agnostic so an on-device engine could serve the same contract |
| The remote assistant leaks diary data | The payload is restricted to the user's message and a minimal context, gated behind consent, and asserted by a test |

## File changes (expected)

**New**
- `core/domain/.../domain/navigation/{NutriLogDestination,NutriLogRoutes}.kt`
- `core/domain/.../domain/assistant/{AssistantMessage,AssistantAction,AssistantContext,AssistantReply,AssistantClient,AssistantActionExecutor}.kt`
- `core/domain/src/commonTest/.../domain/{navigation,assistant}/*`
- `core/data/.../sqldelight/.../DiaryEntry.sq`, `.../2.sqm`, `.../databases/2.db`, `.../databases/3.db`
- `core/data/.../DiaryRepositoryImpl.kt`, `.../NutritionGoalRepositoryImpl.kt`
- `core/data/.../assistant/AiGatewayClient.kt`, `.../AssistantClientImpl.kt`
- `core/data/src/{androidMain,iosMain}/.../SystemDayBoundaryProvider.kt`
- `core/data/src/androidHostTest/.../{DiaryRepositoryImplTest,NutritionGoalRepositoryImplTest,AiGatewayClientTest}.kt`
- `feature/diary/src/commonMain/.../feature/diary/{DiaryState,DiaryViewModel}.kt`
- `feature/summary/src/commonMain/.../feature/summary/{DailySummary,WeeklySummary,SummaryCalculator}.kt`
- `feature/assistant/src/commonMain/.../feature/assistant/{AssistantState,CoachViewModel}.kt`
- `feature/scan/src/commonMain/.../feature/scan/{BarcodeResolution,BarcodeResolver}.kt`
- `androidApp/.../ui/theme/*` (rewritten), `androidApp/.../ui/components/*`
- `androidApp/.../today/TodayScreen.kt`, `androidApp/.../addfood/AddFoodScreen.kt`
- `androidApp/.../library/LibraryScreen.kt`, `androidApp/.../more/MoreScreen.kt`
- `androidApp/.../coach/CoachScreen.kt`, `androidApp/.../weekly/WeeklySummaryScreen.kt`
- `androidApp/.../scan/{BarcodeScannerScreen,BarcodeDecoder,MlKitBarcodeDecoder}.kt`
- `androidApp/src/androidTest/.../today/*`, `.../navigation/*` (rewritten), `.../addfood/*`, `.../coach/*`

**Modified**
- `settings.gradle.kts` (add `:feature:assistant`)
- `gradle/libs.versions.toml` (CameraX and ML Kit, ADR-0017)
- `shared/build.gradle.kts` (export `:feature:assistant`)
- `feature/assistant/build.gradle.kts` (new), `feature/diary/build.gradle.kts`, `feature/summary/build.gradle.kts`, `feature/scan/build.gradle.kts`
- `core/data/.../ProductSearchRuntime.kt`, `core/data/.../PlatformDataFactories.kt` (android and ios)
- `androidApp/build.gradle.kts` (CameraX and ML Kit)
- `androidApp/src/main/AndroidManifest.xml` (the `CAMERA` permission)
- `androidApp/.../App.kt`, `androidApp/.../AppShell.kt`
- `androidApp/.../ui/NutriLogBottomBar.kt`, `ui/NutriLogScaffold.kt`
- `androidApp/.../search/ProductSearchScreen.kt`, `search/ProductDetailScreen.kt`, `search/ProductImage.kt`
- `androidApp/.../savedproducts/*` (the list screen is replaced by the library; the purchase section moves to More)
- `build-logic/.../ModuleDependencyPolicyPlugin.kt` (allow the new module edges)
- `architecture-tests/.../ModuleArchitectureTest.kt` (real package roots)
- `.github/workflows/android.yml` (run the migration verification)
- `AI-LOG.md`

**Deleted**
- `androidApp/.../diary/DiaryPlaceholderScreen.kt` (replaced by the real diary)
- `androidApp/.../savedproducts/SavedProductsScreen.kt` (replaced by the library)
- `androidApp/.../ui/NutriLogDestination.kt` (the model moves to `core:domain`)
