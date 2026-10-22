# Research: Complete App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21

## 1. Problem statement

Feature 005 restyled the existing screens and the result was rejected: the app still
reads as a prototype rather than a product, and its information architecture is
wrong for what the app is supposed to be. The repository brief defines the product
as an **offline-first nutrition diary**: log what you eat, see daily macros, review
the week. What exists today is a **product catalogue browser with a save button**.

Three concrete failures were reported:

1. **The app has no core loop.** There is no diary, no meal, no quantity, no daily
   total. Nothing in the app is a *nutrition tracker* yet.
2. **Navigation is broken.** Leaving the Saved tab and tapping Search can land on a
   product **detail** screen instead of the search screen.
3. **It looks like a child game, not a sellable product.** The information
   hierarchy, colour system and screen composition are not those of a commercial
   nutrition app.

The task is therefore not a restyle. It is a **restructure of the product**: a new
information architecture, a new core domain (diary entries, meals, goals,
summaries), a new navigation model, and a design system that reads as commercial
software.

## 2. Root cause of the broken navigation

The bug is not a one-line mistake; it is a consequence of the current architecture.

`ProductSearchScreen` renders the product detail **inside the same destination**,
driven by `ProductSearchViewModel.selectedProduct`. That ViewModel is a **process
singleton** created once in `App.kt` and shared by every destination that needs it.
`AppShell.navigateToDestination` also uses `popUpTo(start) { saveState = true }` with
`restoreState = true`, so the Search destination's saved back stack is restored on
re-entry.

Consequence: the "is a detail open?" state is *global*, not *scoped to the
destination*. Opening a detail in Search, switching to Saved, and coming back
re-renders the Search destination with `selectedProduct != null`, so the detail is
shown again. The user asked for the search screen and got a detail screen.

Two independent fixes are needed:

- **State scoping:** transient "detail open" state must live in the navigation
  back stack, not in a singleton ViewModel, so it is scoped to a destination.
- **Navigation model:** switching a bottom-navigation tab must reset that tab to
  its root, so no tab can ever present a stale child screen.

Fixing only the second would still leave a shared-mutable-state smell; fixing only
the first would leave `restoreState` restoring a nested detail route. Both are
addressed.

## 3. Current state audit

### Modules

| Module | State | Content |
| --- | --- | --- |
| `core:domain` | populated | `product` (Product, NutritionValue, search), `purchase` (SavedProduct, Purchase*) |
| `core:data` | populated | SQLDelight DB (v1), Open Food Facts / Search-A-Licious client, repositories |
| `feature:search` | populated | `ProductSearchViewModel` + state |
| `feature:savedproducts` | populated | `SavedProductsViewModel`, `PurchaseViewModel` |
| `feature:diary` | **empty** | declared in `settings.gradle.kts`, no sources |
| `feature:summary` | **empty** | declared in `settings.gradle.kts`, no sources |
| `feature:scan` | **empty** | declared in `settings.gradle.kts`, no sources |
| `androidApp` | populated | Compose UI, `AppShell`, bottom bar |
| `iosApp` | minimal | SwiftUI shell |

The empty `feature:diary` and `feature:summary` modules are exactly the modules the
brief reserves for the diary and the summary. The restructure fills them instead of
inventing new module boundaries.

### Data

`ProductDatabase` (SQLDelight) holds `Product`, `ProductNutrition`,
`SearchSnapshot`, `SearchSnapshotProduct`, `RecentSearch`,
`RecentSearchSequence`, `SavedProduct`, `PurchaseRecord`. There is **no diary
table**, so a schema migration is required.

The schema was at version 2 when this feature started: `1.sqm` creates
`SavedProduct` and `PurchaseRecord` on top of the original catalogue tables.
The `databases/2.db` snapshot for that version was **missing**, which meant a
fresh checkout could not verify the migration chain past version 1 and the v2
state was never checked. This feature reconstructs `2.db` from `1.db` plus
`1.sqm`, adds `2.sqm` for the diary tables, and generates `3.db`, so the whole
chain verifies from v1.

### Screens

| Screen | Role today | Role after the restructure |
| --- | --- | --- |
| `ProductSearchScreen` | root, catalogue browser | The search step of the add-food flow, no longer a destination of its own |
| `ProductDetailScreen` | overlay inside Search/Saved | The `product/{code}` route, with an "Add to diary" action |
| `SavedProductsScreen` | tab of saved products | The Library tab, "my foods" with one-tap logging |
| `DiaryPlaceholderScreen` | "coming soon" placeholder | **Today tab, the real diary** |
| `PurchaseSection` | inline in Saved | Inline in the More tab, as the single premium unlock |

## 4. Brainstorming: what a sellable nutrition app actually is

A nutrition tracker is a **daily logging product**. The value is a repeat loop:
open the app, see what you have eaten, log what you just ate, see the numbers
change. Everything that does not serve that loop is secondary.

The competitive set that defines the category:

| App | Home screen | Navigation | What makes it sellable |
| --- | --- | --- | --- |
| MyFitnessPal | Dashboard + diary | Diary, Add, Plan, More | Meal-based logging, one big add action, streaks |
| Cronometer | Diary | Diary, Foods, Recipes, Trends, More | Data-dense, goal-oriented, credible numbers |
| Lose It! | Diary with meals | Log, Meals, Premium | Frictionless logging, premium upsell |
| Lifesum | Today overview | Diary, Scan, Recipes, More | Clean modern layout, calm palette |
| YAZIO | Dashboard | Diary, Scan, Insights, More | Modern card-based analytics |

The shared, non-negotiable anatomy of the category:

1. **A "Today" home** showing the day's meals and the day's totals.
2. **Meal sections** (breakfast, lunch, dinner, snacks) with a per-meal add action.
3. **A daily summary** with calories and macros against goals.
4. **A fast add flow** that reaches a food in one or two taps.
5. **A library of frequent foods** for one-tap logging.
6. **Weekly/progress view** so the user can see trends.
7. **A premium upsell** and settings, in a place that does not pollute the core loop.

### Restructuring opportunities identified

| # | Opportunity | Rationale | Priority |
| --- | --- | --- | --- |
| O1 | Make the diary the home screen | The brief's F3 is the product; the app must open on the user's day, not on a search box | P0 |
| O2 | Introduce meal-based logging | A nutrition tracker without meals is a list of calories; meals are how users think | P0 |
| O3 | Add quantity and per-entry macro computation | The brief requires calories, protein, carbohydrates and fat per entry and per day | P0 |
| O4 | Add daily goals | Progress without a target is not progress; goals are also the premium hook | P1 |
| O5 | Repurpose "Saved products" into "My foods" (library) | The saved list already exists; giving it a purpose in the logging loop is what makes it a feature rather than a bookmark list | P0 |
| O6 | Add a weekly summary | The brief's F4; it is what turns a logger into a product the user keeps open | P1 |
| O7 | Build a real add-food flow | The path from "I want to log" to "it is in my diary" must be short and unambiguous | P0 |
| O8 | Move the purchase upsell out of the saved list | Premium content inside a food list is noise; it belongs in its own destination | P1 |
| O9 | Fix the navigation model (route-scoped detail, tab-reset) | The reported bug, and the root cause of the whole "prototype" feel | P0 |
| O10 | Replace the visual system | Colour, type, shape and spacing define whether a client reads it as a product | P0 |
| O11 | Settings / About destination | Needed to look like a shipped product (version, attribution, data controls) | P2 |

### Options considered for the information architecture

**Option A - Diary-centric, four tabs, search demoted**

```
Bottom bar:  [ Today ] [ Search ] [ Library ] [ More ]
Today  -> daily summary, meal sections, weekly card
Search -> catalogue search (existing F1 feature, promoted to a first-class tab)
Library-> my foods, one-tap add
More   -> goals, premium, about
FAB / per-meal "+" -> add-food flow (meal -> source -> product -> quantity)
```

**Option B - Diary-centric, three tabs, search inside the add flow only**

```
Bottom bar:  [ Today ] [ Library ] [ More ]
Search exists only as a step of the add-food flow.
```

**Option C - Five tabs, scanner included**

```
Bottom bar:  [ Today ] [ Search ] [ Scan ] [ Library ] [ More ]
```

Decision: **Option B, three tabs plus a floating action button**, with the scanner
built as a step of the add-food flow rather than as a tab. Reasons:

- The complaint that was filed was precisely that the app opens on a search box.
  A catalogue is an **input to a diary**, not a peer of it, so search belongs
  inside the logging flow rather than beside it. The search feature itself is not
  lost: it is still the search step of the add flow, and it is still reachable in
  one tap from the Library empty state and from the Today app bar.
- Three destinations plus one primary action match what the diary actually needs,
  and they keep the bar legible at large text scales. A fourth tab would compete
  with the FAB for the same job.
- The scanner does not need its own destination. It is a way of choosing a food,
  so it belongs in the food-selection step, and it keeps the empty `feature:scan`
  module meaningful without inventing a fourth tab.
- The FAB gives the diary the single prominent "log something" action that the
  category anatomy demands, without spending a tab on it.

See ADR-0019 for the recorded navigation decision.

## 5. Design direction

The current green-and-amber palette with heavily rounded shapes reads as playful.
A commercial nutrition app reads as **calm, data-first and trustworthy**.

| Aspect | Decision | Rationale |
| --- | --- | --- |
| Colour | Restrained neutral base with a single deep teal/blue accent; no saturated "food" colours | Calm, clinical trust; the numbers stay the loudest thing on screen |
| Accent usage | Accent only for the primary action and the selected tab; everything else is neutral | Prevents the "toy" look caused by colour everywhere |
| Typography | Stronger weight contrast, tighter display sizes, generous body sizes | Hierarchy is what separates a product from a prototype |
| Shape | 8-14 dp radii, not 16-24 dp | Rounded-everything reads as a game; restrained radii read as software |
| Surfaces | Flat surfaces, hairline outlines, one elevated surface for the summary | Depth used once, deliberately, so it means something |
| Spacing | A single 4 dp spacing scale applied everywhere | Consistency is the cheapest way to look expensive |
| Motion | Material defaults only, no bespoke animation | Motion is the first thing that looks amateur when overdone |
| Empty states | Icon, heading, body, one call to action | Consistent, and always tells the user what to do next |
| Density | Data-dense where it is data (macros, weekly chart), airy where it is narrative | Matches how the category reads |

## 6. Technical decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Detail screen state | Navigation route (`product/{code}`) instead of ViewModel state | Scopes the detail to the tab that opened it; fixes the reported bug at the root |
| Tab switching | Pop to the home destination, then push the target | Every tab is always its own root; no stale child screen can survive a tab switch |
| Bottom-bar visibility | Derived from the route hierarchy, not from a destination lookup | An enum lookup silently returns "no tab" inside a nested graph and blanks the bar |
| Route model location | Shared, in `core:domain`, as plain data | ADR-0015 promised a platform-neutral destination model; the iOS shell needs the same routes |
| Diary persistence | New `DiaryEntry` table, SQLDelight schema v2 -> v3 with a `.sqm` migration | Reuses the existing offline-first store; `verifyMigrations` stays enabled |
| Schema snapshots | `2.db` reconstructed and `3.db` generated, both committed | The v2 snapshot was missing, so the chain could not be verified past v1 on a fresh checkout |
| Nutrition values on an entry | Snapshot the nutrient values at log time | The catalogue can change; a logged day must not |
| Unknown nutrients | Nullable per nutrient, and a total is unknown when any contributing entry is unknown | A tracker that reports "0 g of fat" for an unreported nutrient is stating something false |
| Day bucketing | Store `day_epoch_millis` (local midnight) alongside the timestamp, computed through a platform `DayBoundaryProvider` | Grouping by day must not depend on the device time zone at read time, and stepping days must survive a 23- or 25-hour day |
| Goals | Singleton `NutritionGoal` row, user-editable from More | Progress without a target is not progress |
| Weekly summary | Computed from the diary table, not stored | Derived data must not be stored twice |
| Add-food flow | A nested graph with a source step and a quantity step | The flow is short and linear, but it is more than one screen, and a nested graph keeps the back stack honest |
| Meal picker | A modal bottom sheet, not a route | A modal is not navigation and does not need a back-stack entry |
| Feature modules | `feature:diary` and `feature:summary` filled, `feature:scan` given the shared barcode resolution, `feature:assistant` added | The modules already exist; the restructure fills the reserved slots and adds one for the new concern |
| Module rename | `feature:savedproducts` is **not** renamed to `feature:library` | The rename would churn the dependency allowlist, the framework exports and every import, for no product value. The user chose the UI name "Library" and left the module name alone |
| Barcode decoding | CameraX plus the bundled ML Kit barcode model, in `androidApp` only | The bundled model decodes offline and does not need Play services on the handset. ADR-0017 |
| Scanner testability | The decoder sits behind a project-owned `BarcodeDecoder` interface | The scanner screen must be testable without a camera |
| Assistant transport | A remote LLM reached through a first-party AI gateway, over the already-approved Ktor client | The provider key must never ship inside the APK. ADR-0016 |
| Assistant actions | Structured, validated in shared code, applied only after the user confirms | Free text from a model is never executed |
| Assistant privacy | The user's message plus a minimal diary context, behind a first-use consent | The existing no-telemetry guarantee must stay true for everything except this explicit, consented path |
| Purchase model | One premium product, re-pointed away from per-food codes, reachable only from More | Per-food pricing on a nutrition tracker is not a credible commercial model. ADR-0018 |
| iOS | Not rebuilt | The brief scopes iOS to the scanner and diary screens and marks the rest out of scope; a full SwiftUI rebuild of five screens is a separate feature |

## 7. The assistant, the scanner and the purchase: what the second round changed

A second clarification round settled the three items that were still open after the
information architecture was chosen.

### The AI assistant is a remote LLM, reached through a first-party gateway

The user chose a remote LLM over an on-device deterministic engine, because free-form
natural language is the thing a rule-based parser cannot do.

That choice has one hard consequence that must be engineered around: **the LLM
provider key cannot live in the app.** A key compiled into an APK is extractable by
anyone who unzips it, and it becomes an unmetered liability billed to the publisher.
So the app talks to a thin gateway that the publisher deploys, which holds the key,
enforces rate limits and returns a provider-agnostic JSON envelope.

Good news for the repository: **Ktor 3.6.0 is already approved (ADR-0005) and
already used** for the catalogue client, so the gateway client needs no new
third-party dependency. The whole assistant therefore costs no new licence
obligation and no new HTTP stack.

The costs are real and are recorded rather than hidden:

- A server-side component now exists. It is a deployment prerequisite, not a
  repository deliverable, and until it is deployed the assistant is explicitly
  unavailable rather than silently broken.
- The app is no longer fully offline. Everything except the assistant stays
  offline-first, and the assistant says so when it has no connection.
- There is a per-request cost to the publisher, which is what makes the premium
  unlock the right thing to gate on.
- The existing privacy guarantee needs an explicit, documented exception. The app
  sends the user's own message and a minimal diary context, behind a first-use
  consent, and never the catalogue, the recent search terms or the purchase
  records.

The shared action model is deliberately provider-agnostic: the same
`AssistantAction` types could be served by an on-device engine later, so this
decision does not lock the app to a vendor.

### The scanner decodes on device

The user confirmed barcode scanning is in scope, which reverses the earlier
deferral. The implementation constraint that decides the design is the
dependency policy: the camera stack and the decoder are **new third-party
dependencies**, so they need an ADR (constitution rule III).

The chosen stack is CameraX plus the **bundled** ML Kit barcode model. The bundled
variant matters: it decodes with no network and no Play services on the handset,
so the core logging flow never fails because a model has not downloaded yet. The
unbundled variant is smaller on disk but makes the first scan depend on the
network, which is the wrong trade for the app's most important offline moment.

The alternatives were considered and rejected: the Play Services code scanner
needs Play services on the handset and cannot be themed or exercised on a
standard emulator; ZXing over CameraX means hand-writing detection, rotation and
focus handling; and a cloud resolution API would leak the user's shopping context
for something the device can do locally.

The scanner dependencies are declared in `androidApp` only, so the iOS framework
is unaffected by construction.

### The purchase is re-pointed to one premium product

Per-food pricing is not a credible commercial model for a nutrition tracker, and
showing a purchase button on a bag of oats is worse than showing none. The
purchase surface moves to More as a single unlock, and every per-food affordance
is removed. The existing `PurchaseClient` contract is code-keyed already, so the
change is a change of code and of placement, not of architecture.

What the unlock grants is the assistant, because it is the only feature with a
per-use cost to the publisher. The entitlement check on the client is advisory;
the gateway is the place where it can actually be enforced.

## 8. Risks

| Risk | Mitigation |
| --- | --- |
| Scope is large (new domain, new data, new screens, new navigation, new design system, an assistant and a scanner) | Deliver in phases behind the spec's task list; every phase keeps the app compiling and the suite green |
| Schema migration breaks existing installs | The v2 to v3 migration is additive only (`CREATE TABLE`), so v2 databases migrate without data loss, and the chain is verified from v1 |
| The navigation rewrite breaks existing instrumented tests | Navigation tests are rewritten as part of the same change, and the new tab model is covered by new tests |
| New shared code must satisfy the 80 % coverage gate on `core:domain` and `core:data` | Unit tests are written with the implementation, not after |
| The design system change touches every screen | The theme is changed first, then screens are migrated one at a time, verifying compilation after each |
| `feature:summary`, `feature:diary` and `feature:assistant` are new modules and the module dependency policy is enforced | The policy allowlist is updated in the same change that adds the dependency |
| The assistant cannot be demonstrated because no gateway is deployed | The disabled state is explicit and honest; the shared domain is provider-agnostic, so an on-device engine could serve the same contract |
| The assistant sends diary data to a third party | The payload is restricted and asserted by a test, consent is explicit, and the More destination discloses it |
| The scanner dependencies inflate the APK | They are `androidApp`-only and R8-shrinkable; the size is measured and recorded in ADR-0017 |
| The scanner cannot be exercised on a headless emulator | The decoder sits behind an interface, so the screen is tested with a fake decoder |

## 9. References

- Repository brief: `docs/Project brief for Spec Kit KMP nutrition diary.md` (F1-F5)
- Existing feature specs: `specs/002-product-search`, `specs/004-saved-products-navbar`, `specs/005-ui-redesign`
- Architecture decisions: `docs/adr/0001`-`0019`
- The brainstorming record for this feature: [brainstorming.md](brainstorming.md)
- Category references: MyFitnessPal, Cronometer, Lose It!, Lifesum, YAZIO
