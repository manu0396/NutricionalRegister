# ADR-0019: Tracker Shell Navigation Model

- **Status**: Proposed
- **Date**: 2026-10-21
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: `specs/007-app-restructure/`; [spec.md](../../specs/007-app-restructure/spec.md); ADR-0015; ADR-0016; ADR-0017

## Context and need

ADR-0015 chose `org.jetbrains.androidx.navigation:navigation-compose` 2.9.2,
scoped to the `androidApp` UI layer only. **That decision stands and is not
re-litigated here.** ADR-0015 also described a bottom bar of Search / Saved
products / Diary, and it made one promise that this ADR now exercises: the
destination model is platform-neutral plain data, so a future native SwiftUI
navigation on iOS can be built from the same model without depending on the
Android navigation library.

The restructure changes the shell, because the product changes. The app is a
nutrition **diary**, not a search box with a saved list beside it, and the
reported defect confirms it: navigating from the library to the search
destination shows the product detail instead of the search screen. The
underlying causes are structural, not cosmetic:

- The product detail is **view-model state held outside the `NavHost`**
  (`ProductSearchState.selectedProduct`), so a detail left open in one tab
  resurrects itself when the user returns to that tab, and the back control
  cannot tell the user where it will return to.
- The bar's visible destination is resolved by an **enum route lookup**
  (`NutriLogDestination.fromRoute(route)`), which silently returns "no
  destination" for any route inside a nested graph and blanks the bar.
- The bar's current destination switch uses `saveState`/`restoreState`, which
  preserves a child screen across a tab switch instead of returning to the tab
  root.

The specification for the restructure therefore calls for an explicit shell
model: how many destinations exist, what the start destination is, where the
search capability lives, how the bar is shown, and how back behaves. This ADR
records that model, and it records the one architectural change it forces: the
destination model moves from `androidApp` into the shared `core:domain` module,
so that the iOS SwiftUI shell can reuse the identical route and destination
definitions, exactly as ADR-0015 promised.

The constitution forbids shared **UI**, not shared **data**. A route string and
a destination identity are plain data; the navigation graph and the bar
composable stay in `androidApp`, so the no-shared-UI principle and the
Android-leads-iOS precedent (ADR-0013) are preserved.

## Decision

**Adopt a three-destination tracker shell with a shared, platform-neutral
destination model, and make the product detail a route.** This ADR **extends
ADR-0015** and does not change its navigation library choice, its version, or
its Android-only scope.

- **Three destinations and one primary action.** The bottom navigation has
  THREE destinations: `today`, `library`, `more`, plus a floating action
  button as the global primary action (spec FR-009). The app's start
  destination is `today`. **Search is NOT a top-level destination any more**:
  a catalogue is an input to the diary, not a peer of it. Search is reached
  through the add-food flow and from the Library empty state (spec FR-012).
- **The destination model becomes shared, platform-neutral plain data.** The
  destination identities and route constants move into `core:domain` (a
  `NutriLogDestination` enum carrying a route and a label, plus the route
  constants for the non-tab routes), so the Android Compose shell and a future
  SwiftUI shell share one source of truth. The `androidApp` module keeps
  owning the actual navigation graph and the bottom-bar composable, per
  ADR-0015.
- **The route graph.** The declared structure is:
  - `today` (tab), `library` (tab), `more` (tab)
  - `addfood` nested graph with `addfood/source` and `addfood/quantity`; the
    source step is also where the barcode entry of the add-food flow lives
    (spec FR-014, ADR-0017)
  - `product/{code}` (product detail, shared by every entry point)
  - `coach` (the AI assistant screen)
  - `weekly` (the seven-day summary)
- **Bar visibility is derived from the route hierarchy, not from an enum
  lookup.** The rule is explicit: the bar is visible only on the three
  top-level destination routes, and it is hidden on every child route (spec
  FR-031). An enum lookup cannot express nested graphs and is the exact
  mechanism that blanks the bar, so it is not used for visibility.
- **Selecting a destination navigates to its ROOT.** The selection pops every
  child above the start destination with `popUpTo(start) { inclusive = false }`
  and navigates with `launchSingleTop = true` (spec FR-010). Re-selecting the
  current destination is a no-op. Back from a child returns to the screen that
  opened it. Back from a destination that is not `today` returns to `today`.
- **The product detail is a ROUTE (`product/{code}`), not shared view-model
  state held outside the `NavHost`** (spec FR-011). This is the fix for the
  reported defect where a detail left open in one tab resurrected itself when
  the user returned to that tab, and for the back control that lied about where
  it would return to. The detail is a back-stack entry owned by the stack that
  pushed it, so it cannot outlive the context that opened it.
- **The meal picker is a MODAL BOTTOM SHEET, not a route** (spec FR-030). A
  modal is not navigation and does not need a back-stack entry.
- **The floating action button opens the add-food flow.** It is present on
  `today` and `library`.
- **The `product/{code}` route resolves the product from the local catalogue
  cache** through the existing `ProductSearchRepository.getProduct(code)`
  lookup, so the detail works offline and from every entry point.

The specification was amended for exactly this model in its second
clarification session: FR-009 now states three destinations plus a floating
action button, and clarifications Q3 and Q4 are marked as superseded by Q13
("Three destinations - Today, Library, More - plus a floating action button.
Search is no longer a top-level destination; it is a step of the add-food
flow"). **This ADR is the architectural record of that model**: it states how
the shell is built, not only how many tabs it has.

## Alternatives considered

- **Keep Search as a fourth top-level destination.** Rejected: the complaint
  that was filed against the app was precisely that it opened on a search box.
  A nutrition tracker that starts with a catalogue is a search tool with a
  diary attached, not a diary. It also duplicates the add-food flow's search
  step in the bar, so the same action appears in two places.
- **Keep the destination model in `androidApp`.** Rejected: it breaks the
  platform-neutral promise ADR-0015 made, and iOS would have to re-declare the
  same route strings, which is how route names silently drift apart.
- **Derive bar visibility from the destination enum.** Rejected: it cannot
  express nested graphs, so it returns "no destination" for a route inside the
  `addfood` graph, and it is the exact mechanism that blanks the bar today.
- **Keep the product detail as view-model state.** Rejected: that is the root
  cause of the reported navigation defect. Any fix that only adjusts the tab
  switch leaves the shared state in place, and the detail would still
  resurrect itself on a tab return.
- **Make the meal picker a route.** Rejected: it is a modal. Giving it a
  back-stack entry would make the system back button dismiss a sheet that is
  not a navigation step, and would pollute the stack that the back rules
  above depend on.
- **A persistent meal rail instead of a bottom bar.** Rejected: it removes the
  tab structure the user asked for. A meal rail is a contextual control on the
  Today screen, not a global navigation structure, and it does not replace the
  top-level destinations.

## Binary-size impact

No new dependency is introduced. The shared destination model adds a small
shared type to `core:domain` (an enum plus a handful of route constants), and
that type is compiled into both the Android APK and the `Shared.xcframework`.
The `navigation-compose` 2.9.2 dependency stays in `androidApp` and is
unchanged (ADR-0015), so the iOS framework gains no navigation code; the
expected change is a small, non-material increase of a few hundred bytes per
artifact, partly offset by the retirement of the old destination entries and
the Diary placeholder.

The sizes are re-measured after implementation and compared against the
recorded baselines: the unsigned release APK measured 12,520,644 bytes and the
debug APK 17,167,130 bytes (ADR-0014 and ADR-0015, 2026-10-20), and
`Shared.xcframework` 93,836 KB (ADR-0013, 2026-10-21). The new numbers are
recorded in this ADR's validation record.

## Build-time impact

Negligible. No new dependency, no new Gradle module and no source-set change.
The change moves an existing type from `androidApp` to `core:domain` and adds
a few route constants, so the shared module compiles a slightly larger surface
for the Android and iOS targets, and the `androidApp` module compiles the
navigation graph and the bar against the shared model. Clean and incremental
durations are re-measured after implementation; hosted PR CI must stay within
the project's 10-minute target.

## Licence

No new dependency is introduced, so no new licence obligation arises. The
navigation library remains the JetBrains `navigation-compose` 2.9.2 under the
**Apache License 2.0** (ADR-0015), compatible with this public repository,
with no copyleft, no attribution obligation on the UI and no licence fee. The
shared destination model is first-party code.

## Exit strategy

The route constants are plain data, so the exit is contained:

- **Swap the navigation library.** Replacing `navigation-compose` with AndroidX
  Navigation, Voyager or hand-rolled routing touches only the `androidApp`
  shell, exactly as ADR-0015 already guarantees. The shared model contains no
  library types, so it survives the swap unchanged.
- **Retire the shared model.** Deleting the shared destination type touches
  only its file in `core:domain` and its references in `androidApp`; nothing
  in `core:data`, nothing in the database schema and nothing in the iOS build
  depends on it, because the iOS shell does not exist yet.
- **Add a route.** A new route is a constant in the shared model plus a
  `composable` in the Android graph; the SwiftUI shell picks the constant up
  for free when it is built.

## Consequences

- The Android shell and a future SwiftUI shell can no longer diverge on route
  names: the graph is now the single place where the app's structure is
  declared.
- The reported defect ("search shows a product detail") becomes structurally
  impossible rather than merely fixed: the detail is a back-stack entry, and
  a tab selection pops every child above the start destination.
- The bar can no longer be blanked by a route inside a nested graph, because
  visibility is a rule over the hierarchy, not a lookup that can fail.
- Back behaviour is predictable and stated: back from a child returns to the
  screen that opened it; back from a non-`today` destination returns to
  `today`.
- The bottom bar has three items and a floating action button, so the tab
  structure stays legible and the primary action has one unambiguous home.
- The shell grows. Three real destinations replace the previous mix of a
  search screen, a saved list and a placeholder; the Diary placeholder is
  retired; the add-food flow, the coach and the weekly summary become real
  routes.
- The route names become a shared contract. Renaming a route is a
  cross-platform change, which is the point, and the cost is paid on purpose
  rather than by accident.
- The specification already carries this model (FR-009, FR-010, FR-011,
  FR-012, FR-030, FR-031 and clarification Q13), so there is no outstanding
  specification work; the remaining work is implementation and the navigation
  tests.
- iOS is not rebuilt by this ADR. The shared model is what makes that rebuild
  cheap later, and this ADR does not pre-commit to a shared-UI navigation
  approach, consistent with ADR-0015.

## Review record

- **Review outcome**: Pending
- **Reviewed by**: —
- **Review date**: —
- **Rationale or conditions**: —

## Validation record

- **Pre-implementation state (verified 2026-10-21)**: the current shell
  resolves its visible destination with `NutriLogDestination.fromRoute(route)`
  in `androidApp/.../AppShell.kt`, which returns null for any route inside a
  nested graph, and the product detail is view-model state
  (`ProductSearchState.selectedProduct` in
  `feature/search/.../ProductSearchState.kt`) held outside the `NavHost`.
  Both are the mechanisms this ADR replaces. `ProductSearchRepository.getProduct(code)`
  already exists in `core:domain` and is the lookup the `product/{code}` route
  uses.
- **Pending after implementation**: the navigation instrumented tests must
  cover the new shell, specifically that a tab selection always shows the tab
  root (the reported defect cannot be reproduced), that a nested route hides
  the bar, that re-selecting the current destination is a no-op, that back from
  a child returns to its opener and that back from a non-`today` destination
  returns to `today`. The release and debug APKs and `Shared.xcframework` are
  re-measured against the recorded baselines, and the shared module's Kover
  gate must still pass with the new shared type.
