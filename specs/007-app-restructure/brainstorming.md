# Brainstorming: Complete App Restructure

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21
**Status**: Brainstorm complete, decisions pending reviewer approval

## Trigger

The user reported that the application "shows just a search box with a saved product
section" and must become "a sellable application which are ready to offer to the clients
to sell it". The report also named a failing flow: navigating from a saved product to the
search screen.

This document records the exploration performed before the specification was rewritten.
It is a decision log, not a contract. The normative requirements live in `spec.md`; the
architecture lives in `plan.md`.

## Method

Four independent analyses were run against the real repository at commit `1705e35`:

| Lane | Scope | Question answered |
|---|---|---|
| Architecture | IA, navigation graph, module structure | What should the app's spine be? |
| Frontend | Visual design, tokens, screen composition | Why does it read as a child game, and what fixes it? |
| Data | Domain model, persistence, migration | What must be stored, and how is it migrated safely? |
| Commercial | Product readiness, sellable cut | What does a client evaluate, and what is the smallest credible product? |

Every claim below was verified against source. Findings that contradict the project brief
are flagged explicitly in [Corrections to the brief](#corrections-to-the-brief).

---

## Part 1 - Diagnosis: why the app reads as a search box with a saved list

### 1.1 The structural gap

The repository is a well-engineered **product catalogue application with a purchase
scaffold**, not a nutrition diary. The four things a client calls a nutrition tracker do
not exist:

| Brief feature | State today | Evidence |
|---|---|---|
| F1 product search | Done | `feature/search/`, `core/data/.../ProductSearchRepositoryImpl.kt` |
| F3 daily diary | Not started | `feature/diary/build.gradle.kts` is the entire module; `core/domain/.../domain/` has only `product/` and `purchase/` packages |
| F2 barcode scan | Not started | `feature/scan/build.gradle.kts` is the entire module; no `CAMERA` permission in `androidApp/src/main/AndroidManifest.xml` |
| F4 weekly summary | Not started | `feature/summary/build.gradle.kts` is the entire module; zero occurrences of `kcal`, `macro`, `weekly` or `goal` in any source file |
| F5 offline-first | Partial, search only | Search is offline-first; there is no diary to be offline-first about, and no staleness marker in any UI |

The user-visible consequence is exactly what the complaint describes: the start
destination is `search` (`androidApp/.../ui/NutriLogDestination.kt`, `NutriLogDestination.start = SEARCH`),
so the app opens on a text field. The second destination holds a list of saved products.
The third says "Coming soon" (`androidApp/.../diary/DiaryPlaceholderScreen.kt`).

### 1.2 The four concrete navigation defects

These are the mechanics behind "navigate from saved product to search screen" being
reported as broken.

**D1 - the Saved to Search shortcut exists only in the empty state.**
`androidApp/.../savedproducts/SavedProductsScreen.kt` branches on `state.isEmpty`. Only the
empty branch renders `SavedProductsEmptyState`, which owns the `saved-products-empty-search`
button. The non-empty branch renders `SavedProductsList`, which contains no search
affordance at all. With one or more saved products there is no in-screen route to Search;
only the bottom bar works.

**D2 - the detail back control lies about its origin.**
`androidApp/.../search/ProductDetailScreen.kt` hardcodes `BACK_DESCRIPTION = "Back to search"`.
The same composable is rendered from the Saved destination, so a user who opened a product
from Saved is told they will return to Search.

**D3 - `openProductCode` is never wired.**
`ProductSearchScreen` and `SavedProductsScreen` both expose an `openProductCode` parameter,
and both have a `LaunchedEffect` handler for it. `androidApp/.../App.kt` supplies neither.
A saved product can therefore never be opened in the Search destination, and a search
result can never be opened in the Saved destination.

**D4 - per-destination detail state outlives the navigation stack.**
Product detail is not a route. It is `selectedProduct` inside `ProductSearchViewModel` and
`SavedProductsViewModel`, and those ViewModels are created once in `App.kt` and held for the
application lifetime, outside the `NavHost`. `AppShell.navigateToDestination` uses
`popUpTo(startDestination) { saveState = true }` with `restoreState = true`. The nav entry
is restored, but the ViewModel is the same object, so a detail left open in Saved
resurrects itself when the user returns to the Saved tab. The same root cause makes the
system back gesture close the tab instead of the detail.

**D5 - the architecture test cannot fail.**
`architecture-tests/src/test/kotlin/com/nutrition/architecture/ModuleArchitectureTest.kt`
matches packages `com.nutrition.core.domain` and `com.nutrition.feature`. The real packages
are `com.example.nutricionalregister.*`. The rule matches nothing, so the inward
dependency rule is currently convention, not enforcement.

### 1.3 Why the visual result reads as a child game

This is a systemic mismatch between friendly consumer tokens and measurement-instrument
content, not one bad colour choice.

- **Shape language is uniformly bubbly.** `ui/theme/Shape.kt` defines `large = RoundedCornerShape(16.dp)`.
  Nine call sites use it for buttons, cards, chips and rows, including
  `search/ProductSearchScreen.kt:214` (the Search button), `search/ProductResultCard.kt:61`
  and `ui/InfoChip.kt:31`. A 16 dp radius on a full-width filled button is the strongest
  toy signal in the codebase.
- **The palette is candy and every neutral carries a green cast.** `ui/theme/Color.kt`
  defines `Green40 #006D3B`, `Amber40 #825600`, `Teal40 #006876` at similar chroma, neon
  containers such as `Green90 #8AF9B6` and `Teal90 #A3EEFC`, and green-tinted neutrals
  (`NeutralLightSurface #F6FBF4`, `NeutralLightSurfaceVariant #DDE5DB`,
  `NeutralLightOutline #71796F`, `NeutralDarkSurface #0F1511`). The result is a minty wash.
- **The product placeholder is a rainbow of gradient tiles with oversized letters.**
  `search/ProductImage.kt` defines an 8-entry saturated pastel `PlaceholderPalette` and
  renders a large centred initial at `typography.displaySmall`. That slot is not defined
  in `ui/theme/Type.kt`, so it silently falls back to the stock Material 3 36 sp. A random
  coloured letter tile appears on every row of every list.
- **Empty states are 64 dp tinted glyphs.** `savedproducts/SavedProductsScreen.kt:170` and
  `diary/DiaryPlaceholderScreen.kt:64` both render a 64 dp outlined icon tinted `primary`.
- **The type scale has holes and no numeric role.** `ui/theme/Type.kt` overrides 10 of the
  15 Material 3 slots; `displayLarge`, `displayMedium`, `displaySmall`, `labelMedium` and
  `labelSmall` fall back to stock values inside a themed app. `ui/StatRow.kt` renders the
  value at `bodyLarge` Regular, identical in size and weight to its own label. There is no
  tabular-figure style anywhere, so no number in the app aligns.
- **There is no primary action and no aggregate data.** No FAB exists. The only add
  affordance in the application is a full-width 52 dp slab button labelled Search. There
  is no calorie total, no macro total, no progress indicator anywhere.
- **Two incompatible app bars.** `ui/NutriLogScaffold.kt` uses a real Material 3
  `TopAppBar`; `search/ProductDetailScreen.kt:213` uses a hand-rolled transparent `Row`
  with a `primary`-coloured title. Two app bars that do not match read as assembled parts.
- **Copy is developer voice.** "Search for a food by name to inspect its catalogue details",
  "Coming soon", "Your daily food log". There is no `strings.xml` localisation layer at all.

### 1.4 What is genuinely already good

The commercial analysis found assets worth preserving and protecting.

- **A real, two-platform entitlement system.** `core/domain/.../purchase/PurchaseClient.kt`
  defines one contract; `core/data/src/androidMain/.../AndroidPurchaseClient.kt` implements
  it against Google Play Billing 9.1.0 with `withTimeoutOrNull` guards on connect and
  purchase, and a pure unit-tested `BillingResult` to `PurchaseFailure` mapping.
  `iosApp/iosApp/StoreKitPurchaseClient.swift` implements the same contract with StoreKit 2.
  `PurchaseRepositoryImpl` writes a record only from a verified transaction and upserts
  idempotently on `product_code`, so a repeated confirmation cannot create two records.
  `reconcile()` re-derives local entitlements from the store on launch.
- **Privacy proven by test, not asserted.**
  `core/data/src/androidHostTest/.../PrivacyNoEgressTest.kt` runs the real save and purchase
  repositories against an in-memory database with an injected tracking sink and asserts
  that zero telemetry events are emitted. `feature/search/src/commonTest/.../SearchPrivacyTest.kt`
  does the same for search terms.
- **Offline-first as an architectural property.** `ProductSearchRepositoryImpl` emits the
  cached snapshot first and attaches the cache to the failure, so the app degrades to "here
  is what you had" rather than to an error page.
- **A release-engineering spine.** Convention plugins, a version catalog, ktlint, an
  architecture test, an 80 percent Kover line-coverage gate on `core:domain` and
  `core:data`, hosted CI measured under the ten-minute target, and published APK and
  XCFramework size metrics.
- **An honest AI trail.** `AI-LOG.md` records real failures and corrections, including
  the review-gate process conflict and the unsatisfied store-side purchase prerequisites.

---

## Part 2 - Restructuring directions

### 2.1 Information architecture

#### Direction A - "Tracker Shell" (today-first, diary as the spine)

```
root graph (startDestination = today)
├── today            tab 1 - meal sections, daily macro ring, goals
├── diary            tab 2 - history
├── library          tab 3 - saved, frequent and recent foods
├── insights         tab 4 - weekly summary
├── addfood          nested graph, bottom bar hidden
│   ├── addfood/source    Search | Library | Scan
│   └── addfood/quantity  quantity and meal selection
└── product/{code}   full-screen route, shared by every entry point
```

Search stops being a destination and becomes an input to the diary. Product detail becomes
a real route so that system back, deep links and every entry point behave identically.
The meal picker is a bottom sheet, not a route, because a modal is not navigation.

Cost: roughly 55 to 70 files touched, the highest test churn of the four directions, and
one SQLDelight migration.

#### Direction B - "Journal Feed" (chronological timeline, meals as a filter)

A reverse-chronological feed with day headers and per-day totals. Meals become filter
chips on the feed rather than fixed buckets. The add flow is a `ModalBottomSheet` with no
back-stack entry at all; its internal Search, Recent and Library segments are a local
state machine.

Cost: roughly 50 to 60 files, the cheapest navigation, but five search instrumented test
files churn because the search screen must be re-parented into a sheet. Commercially it is
a stronger retention product and a weaker demo product: a reviewer expecting four meal
buckets may read the timeline as a missing feature.

#### Direction C - "Catalogue plus Tracker" (keep Search top-level)

Search stays a peer tab; Diary is promoted to a working Today. Add food pushes
`product/{code}` and opens a modal meal and quantity picker. No nested graphs.

Cost: roughly 35 to 45 files, the lowest churn and the lowest migration risk. But the
information architecture stays flat and Search remains a peer of Today, which is precisely
the complaint that was filed. It is defensible commercially, since MyFitnessPal ships
Diary, Search, Nutrition and More, but it does not deliver "restructure the whole app".

#### Direction D - "Log-first plus Coach" (persistent meal context, premium layer)

The `log` nested graph replaces the bottom bar with a persistent meal rail so the target
meal is always visible. A rule-based, deterministic, offline coaching engine computes
plateau detection, protein deficit, sodium-heavy days and logging streaks inside
`core:domain`, and the existing purchase infrastructure is re-pointed from per-food codes
to a single `nutrilog_pro_yearly` product.

Cost: roughly 70 to 85 files, the highest. It depends on a working diary, on a goals
model, and on reversing the purchase model recorded in ADR-0014, which needs its own ADR
and another migration.

### 2.2 Visual design

#### Direction A - "Clinical Ledger"

Cool neutral canvas, near-black ink, one ink-blue accent, tabular numerals, ruled tables
instead of cards, circles only for the progress ring. The lineage of Cronometer, MacroFactor
and Stripe Dashboard. Trustworthy and precise, but austere for a consumer wellness buyer.

#### Direction B - "Warm Editorial"

Warm off-white canvas, terracotta accent, a bundled display serif paired with a neutral
sans, generous whitespace, large hero numerals, food photography as the hero. The lineage
of Oura and a printed food journal. It needs a photography pipeline and licensed assets to
work; without real images it collapses into beige emptiness.

#### Direction C - "Modern Minimal" (recommended)

One deep evergreen accent over a cool neutral ramp, hairline surfaces, modest radii, one
elevated hero (the calorie ring), and dense-but-calm lists. The lineage of Linear, Notion
and Apple Health. It borrows exactly one idea from the Ledger direction (tabular numerals
and semantic over/under colours) and one from the Editorial direction (a single-hero page
composition), and nothing else. Critically, it is achievable with the existing stack:
Material 3, `material-icons-core`, and Compose Canvas, with no new dependency.

### 2.3 Domain and persistence

#### Candidate A - "Flat diary row"

One new `DiaryEntry` table carrying the product code, a name copy, quantity, meal, day and
eight denormalised macro columns. Totals summed in Kotlin.

Rejected: it pushes the interesting logic out of the measured modules into the feature
layer, where the Kover gate does not reach, so coverage silently escapes.

#### Candidate B - "Normalised diary aggregate with a frozen nutrition snapshot" (recommended)

A new `core/domain/.../domain/diary/` package with `DiaryDate`, `MealType`, `QuantityUnit`,
`LoggedAmount`, `TrackedNutrients`, `ResolvedNutrition`, `FoodLogEntry`, `NutritionTotals`,
`DailySummary`, `NutritionGoals`, `GoalProgress`, `FrequentFood`, plus the pure services
`NutritionCalculator`, `FoodLogger` and `DiarySummaries`.

The defining decision: when an entry is logged, its macro contribution is computed once and
frozen onto the entry. Historical totals therefore never change when the Open Food Facts
catalogue refreshes. Totals are summed from those frozen values in Kotlin, never recomputed
from the catalogue and never computed with SQL `SUM`, because the "honest total" rule below
cannot be expressed in SQL.

The honest total rule is the single most important semantic in the model:

| Situation | Total value | `incompleteNutrients` |
|---|---|---|
| Every contributing entry had a value | exact sum | not listed |
| Some entries had a value, some did not | sum of the known ones, a lower bound | that key is listed |
| No entry had a value | `null`, meaning unknown | that key is listed |

A `null` total renders as "unknown", never as "0 kcal". Zero calories is a different and
meaningful claim.

#### Candidate C - "Full measurement model with a unit conversion registry"

`FoodItem` to `FoodPortion` to `DiaryEntry` to `DiaryEntryPortion`, with a unit conversion
registry covering grams, kilograms, ounces, pounds, millilitres, cups and tablespoons.

Rejected for this increment: it needs a large hand-written conversion table or a new
dependency, and the catalogue provides no piece masses. The recommended model is
deliberately shaped so it can be absorbed later, because `LoggedAmount.grams` is already the
canonical mass and a portion registry only replaces the amount-resolution step.

### 2.4 Commercial positioning

#### Option A - "The private offline diary" (consumer, one-time unlock)

Free: text search, the full daily diary, the saved library, unlimited offline caching.
Premium one-time unlock: barcode scanning, history beyond 30 days, weekly summary, export.
This maps almost verbatim onto the existing purchase infrastructure, because
`AndroidPurchaseClient` already handles one-time products only. Effort to monetise is small.

#### Option B - "The honest macro coach" (consumer, subscription)

Free: diary, one calorie goal, seven-day summary. Premium subscription: multiple goals,
trends, export, barcode scanning. Only the entitlement half of the existing purchase layer
is reusable. A subscription needs `ProductType.SUBS`, `querySubscriptionsAsync`, base-plan
selection, expiry and period on `PurchaseRecord`, a manage-subscription deep link, and
grace-period and billing-retry states. Highest effort, lowest evidence of demand, and it
contradicts the "no backend, no accounts" constraint that the brief sets.

#### Option C - "The white-label nutrition app starter" (B2B)

Everything works, the paywall is bypassed in a demo build, and the deliverable is the
architecture plus the entitlement template. This is the only option that can be sold
before the diary exists, and it is a services business rather than an app business.

---

## Part 3 - Recommendation

**Adopt Direction A for the information architecture, Direction C for the visual design, and
Candidate B for the domain model.** Defer Direction D's coaching layer, barcode scanning
and the purchase repurposing to follow-on specifications.

Why this combination:

1. **Only Direction A treats the diary as the spine.** The brief describes a nutrition
   diary, and F3 is the feature the repository lacks entirely. Direction A makes the
   missing feature central and demotes the search catalogue to what it actually is, which
   is an input to logging. Direction C explicitly declines to do this, and that refusal is
   the complaint that was filed.
2. **Direction C is the only design direction that needs no new dependency and no asset
   pipeline.** It fits the existing Material 3, `material-icons-core` and Compose Canvas
   stack, and the new information architecture was deliberately designed to stay inside
   `material-icons-core`.
3. **Candidate B keeps the arithmetic inside `core:domain`,** where the 80 percent Kover
   gate applies, and its frozen snapshot makes historical totals stable against catalogue
   refreshes. Candidate A leaks the logic into the ungated feature layer.
4. **Direction D is deferred rather than rejected.** The coaching engine is the most
   commercially interesting idea in this document, but it needs a working diary, a goals
   model and a reversed purchase model. Shipping it inside this restructure turns one
   change into three.

### 3.1 Concrete decisions proposed

| # | Decision | Rationale |
|---|---|---|
| D-01 | Product detail becomes the route `product/{code}`, resolved from the local cache via the existing `ProductSearchRepository.getProduct(code)` | Fixes D2, D3 and D4 by construction, and makes the detail reachable from every entry point |
| D-02 | The start destination becomes `today`, and the graph start route and the bottom bar home tab are the same constant | Satisfies the home-screen requirement and keeps `popUpTo` semantics correct |
| D-03 | Search stops being a top-level tab and becomes a segment of the add-food flow, still reachable from the Library empty state and the Today app bar | A catalogue is an input to a diary, not a peer of it |
| D-04 | Bottom bar visibility is derived from the route hierarchy, not from an enum lookup | An enum lookup silently returns "no tab" inside a nested graph and blanks the bar |
| D-05 | The meal picker is a bottom sheet, not a route | A modal is not navigation, and a modal does not need a back-stack entry |
| D-06 | The bottom navigation becomes `Today`, `Library`, `More`, with a floating action button as the global primary action | Three destinations plus one primary action matches what the diary needs, and keeps the bar legible at large text scales |
| D-07 | `feature:savedproducts` is renamed to `feature:library` as its own atomic change, not in the same commit as the diary work | Keeps the reviewable diff legible |
| D-08 | Totals are computed in Kotlin from frozen per-entry snapshots, in a domain-defined order | Deterministic, plan-independent, and the only way to express the honest-total rule |
| D-09 | `DiaryDate` is an `epochDay` value type in `core:domain`, with no new date library | `java.time` needs API 26 and `minSdk` is 24; `kotlinx-datetime` would need an ADR |
| D-10 | The purchase surface is re-pointed away from the food barcode, or removed from the product detail | Shipping "Buy for EUR 1.99" on a bag of oats in a client meeting is a self-inflicted wound |
| D-11 | The Konsist architecture test is rewritten to match the real package roots | Converts the strict-boundary claim from unverifiable to enforced |
| D-12 | Barcode scanning, the coaching engine, the weekly summary, data export and store hardening are deferred to follow-on specifications | They each need their own ADR, migration or platform work, and would turn one restructure into three |
| D-13 | The theme work is rescoped from "remove playful elements" to a token-level correction: neutral ramp, tightened radii, a complete type scale, numeric roles, hairline surfaces | The palette, radii and type scale really are the problem, but a full re-skin is churn the diary does not need |
| D-14 | `applicationId` is not changed in this specification | It is immutable after a first publish, and churning it mid-restructure is the wrong time |

### 3.2 Wildcards worth a follow-on specification

1. **On-device nutrition-label OCR.** Photograph a nutrition label and parse energy,
   protein, carbohydrates and fat into an entry, entirely on device. The home-cooked and
   unpackaged segment is the one every competitor handles worst. Needs an ADR for the text
   recognition dependency.
2. **An explainable, offline coaching engine.** Deterministic rules over the logged data,
   every card citing the numbers it is based on, no network and no accounts. A genuine
   differentiator against every cloud-based tracker, and testable as pure functions.
3. **Data portability and platform health write-back.** One-tap human-readable export, plus
   publishing daily energy and macros to Health Connect and HealthKit. Both are platform
   APIs rather than third-party libraries, so neither needs a dependency ADR.

### 3.3 What the restructure must not do

- It must not claim to be a weekly summary or a goal tracker. Those are F4 and are deferred.
- It must not ship a half-built diary. A partially built Today screen with approximate
  totals and no edge-case handling reads as a broken product, which is worse than the
  current honest "Coming soon" placeholder.
- It must not absorb the scanner. A camera permission, a new dependency and a new
  platform story have no room in this specification.
- It must not churn the `applicationId`, the signing configuration or the store listing.
  Those belong to a commercial-hardening specification.

---

## Corrections to the brief

Findings that contradict statements in the project brief or in the pre-existing 007
artifacts. They were verified against the repository, not inferred.

| # | Assumption | Reality | Evidence |
|---|---|---|---|
| C-01 | "The current schema has NO migration mechanism" | **False.** SQLDelight migrations are fully wired and verified | `build-logic/src/main/kotlin/nutrition.sqldelight.gradle.kts:11` sets `verifyMigrations.set(true)`; `core/data/src/commonMain/sqldelight/.../data/1.sqm` exists; the generated `ProductDatabaseImpl` reports `version = 2` |
| C-02 | "Schema version is currently default" | **False.** The schema version is 2 | `core/data/build/generated/sqldelight/.../ProductDatabaseImpl.kt`, `override val version: Long get() = 2` |
| C-03 | The `2.db` schema snapshot is a committed baseline | **It is untracked.** `2.db` must be committed before a new `.sqm` is added | `core/data/src/commonMain/sqldelight/databases/` holds `1.db` only |
| C-04 | Migration verification runs in CI | **It does not.** The Android workflow never invokes the verification task | `.github/workflows/android.yml` runs assemble, unit tests, architecture test, coverage, lint and ktlint |
| C-05 | The theme overhaul (007 tasks T001 to T004) is still needed | **Partly.** A theme system exists, but the analysis above shows the palette, radii, type scale and placeholder treatment are still the "child game" source | `ui/theme/Color.kt`, `ui/theme/Shape.kt`, `ui/theme/Type.kt`, `search/ProductImage.kt` |
| C-06 | Scan integration can be updated in this specification (007 task T018) | **There is no scan integration to update.** `feature/scan` is an empty module with no camera permission and no scanner dependency | `feature/scan/build.gradle.kts`, `androidApp/src/main/AndroidManifest.xml` |
| C-07 | Specification 006 is a separate, still-open scope | **006 overlaps 007 almost entirely** and was never implemented. It must be closed as superseded | `specs/006-production-ui-redesign/` has no implementation commit; the only 007-related commit is `1705e35` |
| C-08 | The architecture test enforces the inward dependency rule | **It matches no package** and therefore always passes | `architecture-tests/.../ModuleArchitectureTest.kt:20-24` |

---

## Open questions for the reviewer

These change the shape of the specification and are answered in the clarification pass.

1. Is the purchase surface kept per food, re-pointed to a single premium product, or
   removed from the food surfaces in this specification?
2. Is the `feature:savedproducts` to `feature:library` rename in scope, or deferred to
   avoid the dependency-policy and framework-export churn?
3. Is the theme correction limited to tokens, or does it also rewrite the screens?
4. Is a "More" screen with goals in scope, given that goal targets are F4 and were
   deferred?
5. Is the iOS surface in scope for this restructure at all, given that the brief limits
   iOS to scanner and diary screens at a later delivery step?
