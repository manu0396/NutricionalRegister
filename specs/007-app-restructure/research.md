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

`ProductDatabase` (SQLDelight, schema version 1) holds `Product`,
`ProductNutrition`, `SearchSnapshot`, `SearchSnapshotProduct`, `RecentSearch`,
`RecentSearchSequence`, `SavedProduct`, `PurchaseRecord`. There is **no diary
table**, so a schema migration is required.

### Screens

| Screen | Role today | Role after the restructure |
| --- | --- | --- |
| `ProductSearchScreen` | root, catalogue browser | Search tab, and the search step of the add-food flow |
| `ProductDetailScreen` | overlay inside Search/Saved | Child route of the tab that opened it, with an "Add to diary" action |
| `SavedProductsScreen` | tab of saved products | Library tab, "my foods" with one-tap logging |
| `DiaryPlaceholderScreen` | "coming soon" placeholder | **Today tab, the real diary** |
| `PurchaseSection` | inline in Saved | Inline in the More tab, as the premium upsell |

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

Decision: **Option A**. Reasons:

- The Search tab keeps a fully built, fully tested feature (F1) reachable in one
  tap instead of burying it; the add flow *also* offers search, so nothing is
  lost.
- Option B leaves the bottom bar thin and the search feature feeling hidden.
- Option C needs a real barcode scanner. `feature:scan` is empty, scanning needs a
  camera dependency, a new ADR under constitution rule III, camera permission
  handling and its own hardware testing. That is a separate feature, not part of
  a restructure. It is explicitly out of scope here and recorded as follow-up
  work; the empty module is left untouched.
- Four tabs is the sweet spot for a bottom bar: every destination is reachable,
  labelled, and the bar does not overflow.

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
| Diary persistence | New `DiaryEntry` table, SQLDelight schema v1 -> v2 with a `.sqm` migration | Reuses the existing offline-first store; `verifyMigrations` stays enabled |
| Nutrition values on an entry | Snapshot the nutrient values at log time | The catalogue can change; a logged day must not |
| Day bucketing | Store `day_epoch_millis` (local midnight) alongside the timestamp | Grouping by day must not depend on the device time zone at read time |
| Goals | Singleton `NutritionGoal` row, user-editable | Goals are the premium hook and the anchor of the daily summary |
| Weekly summary | Computed from the diary table, not stored | Derived data must not be stored twice |
| Add-food flow | One screen with an explicit step state, not a multi-destination graph | The flow is short and linear; a single screen keeps the back stack honest |
| Feature modules | `feature:diary` and `feature:summary` filled, `feature:scan` left empty | The modules already exist; the restructure fills the reserved slots |
| iOS | Not rebuilt | The brief scopes iOS to the scanner and diary screens and marks the rest out of scope; a full SwiftUI rebuild of five screens is a separate feature |

## 7. Risks

| Risk | Mitigation |
| --- | --- |
| Scope is large (new domain, new data, new screens, new navigation, new design system) | Deliver in phases behind the spec's task list; every phase keeps the app compiling and the suite green |
| Schema migration breaks existing installs | Version 2 migration is additive only (`CREATE TABLE`), so version 1 databases migrate without data loss |
| Navigation rewrite breaks existing instrumented tests | Navigation tests are rewritten as part of the same change, and the new tab model is covered by new tests |
| New shared code must satisfy the 80 % coverage gate on `core:domain` and `core:data` | Unit tests are written with the implementation, not after |
| The design system change touches every screen | The theme is changed first, then screens are migrated one at a time, verifying compilation after each |
| `feature:summary` and `feature:diary` are new modules and the module dependency policy is enforced | The policy allowlist is updated in the same change that adds the dependency |

## 8. References

- Repository brief: `docs/Project brief for Spec Kit KMP nutrition diary.md` (F1-F5)
- Existing feature specs: `specs/002-product-search`, `specs/004-saved-products-navbar`, `specs/005-ui-redesign`
- Architecture decisions: `docs/adr/0001`-`0015`
- Category references: MyFitnessPal, Cronometer, Lose It!, Lifesum, YAZIO
