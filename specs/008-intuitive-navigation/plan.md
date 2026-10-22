# Implementation Plan: Intuitive navigation in the app's main work

**Feature**: `008-intuitive-navigation`
**Branch**: `008-intuitive-navigation`
**Date**: 2026-10-22
**Spec**: [spec.md](spec.md)
**Data Model**: [data-model.md](data-model.md)
**Contracts**: [contracts/navigation-flow-contracts.md](contracts/navigation-flow-contracts.md)
**ADRs**: [0015](../docs/adr/0015-*.md), [0019](../docs/adr/0019-tracker-shell-navigation-model.md), [0021](../docs/adr/0021-flow-landing-and-back.md)

## Summary

The work is the landing rule of the flow (the add-food flow), which is the only
part of the navigation whose current behaviour confuses the user: after a food
is logged the app returns to a product detail the user already left, and a
detail opened from a flow row pushes a second opening of the flow, so the user
walks two screens back to continue.

The fix is one rule (ADR-0021) and its two consequences:

- a **confirm** lands on the flow's own source step, with the meal still
  chosen, whatever opening started the flow;
- a detail opened from a flow row that is then left with the flow's "Add to
  diary" **carries the flow further** instead of opening a second flow, so the
  walk back is one step at a time and never a detour.

Everything else the spec names (a repeated tab tap is a no-op, a child screen's
back control returns to the screen that opened it, a dialog closes over an
untouched screen) is the behaviour 007 already gives, and is locked in by
instrumented tests instead of by code.

## Technical Context

- **Framework**: Kotlin 2.4 (KMP shared core), Jetpack Compose (androidApp)
- **Navigation**: `org.jetbrains.androidx.navigation:navigation-compose` 2.9.2
  (ADR-0015), scoped to `androidApp`; the route model is shared domain data
  (ADR-0019) and is not changed by this feature
- **Flow state**: `feature:diary`'s `AddFoodViewModel` (two steps, the step in
  the route, the session id the nested graph's entry id) - unchanged in
  principle, its *session* semantics are what the fix makes exact
- **Tests**: kotlin-test (shared), Compose UI Test (androidTest), Konsist,
  Kover

## Constitution Check

- The business logic stays in the shared KMP modules: the landing rule's
  *decision* lives in the shared navigation model (`core:domain`), so the
  Android shell reads it and a future native SwiftUI shell can too (ADR-0019's
  promise). Platform code only carries it out.
- No new third-party dependency.
- The Android-only navigation stays in `androidApp` (ADR-0015).
- Plain text, one task per commit (constitution principle V and the
  contribution rule).

## Decisions

### 1. The landing of a confirm (ADR-0021)

A confirm pop's the flow's steps away and moves the user to the flow's **source
step**, not to whatever entry happens to be under it. The rule is in the
navigation layer (`NutriLogNavigator`'s landing), so a screen's confirm control
asks for a landing and does not pop a number of entries itself, because the
stack's shape is navigation's facts, not a screen's.

Consequences: the flow's graph is popped to its own start (the source step) and
the entries that were pushed on top of it (a detail, the quantity step) are
gone; the flow's state itself is kept by the view model, and the meal is
re-selected from the graph's arguments, so the search starts where the work
continues.

### 2. A detail opened from a flow row does not open a second flow

The detail's "Add to diary" action today navigates to a NEW add-food route with
the product's code, which pushes a second `addfood` graph entry with a new
session id: the flow's state resets and the back walk now has two flows on it.

The fix: the action sends the product's code to the flow that is already open -
the route of the quantity step is *returned to* rather than pushed, which keeps
one flow, one session, and one back walk. A detail opened outside a flow (from
the Today's entry rows, or from the Library) behaves as today: it opens the flow
from the beginning, with the food preselected.

### 3. A back press is the previous step, decided in one place

The back control of every child screen (the flow's steps, the product detail)
means "one step", and that meaning is carried out by the navigation layer in
one place: the shell's own rule says that popping is to the previous entry, and
a route that is left by a confirm is not "popped twice" but is the **landing**
decision above.

### 4. What already works is locked, not rewritten

- The bottom bar's visibility derives from the route hierarchy (ADR-0019), and
  a second tap of the same tab is a no-op: locked by an instrumented test.
- A dialog closes over an untouched screen: locked by an instrumented test.
- The Coach, More and their child routes stay as they are.

## Testing

- **Shared**: the landing rule and the flow's session rule are decided in
  domain: covered by unit tests in `core:domain`.
- **Instrumented**: story 1's walk (open the flow, log a food, confirm: the
  search is shown, the meal is kept, no detail is shown); story 2's walk (back
  at every step, the detail's round trip); story 3's walk (a repeated tab tap,
  a dialog's close). All against the real app, on an emulator, in
  `:androidApp:connectedDebugAndroidTest`.

## Risks

- The session id's meaning ("reopen reset, stepping keeps") must keep its
  promise when the flow is carried back into from a detail: the round trip
  must neither reset the flow (a new session) nor clone it (a second entry).
  This is the plan's most important risk and its covered unit tests are the
  reason it is acceptable.
- A pop that lands on a nested graph's start has to keep the graph entry's
  arguments, so the meal the flow was opened with can be re-selected: the rule
  is in the shell's own library of navigation facts (ADR-0019), and the tests
  cover it.
