# Tasks: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Spec**: [spec.md](spec.md)
**Plan**: [plan.md](plan.md)

## Phase 0: Spec Kit artifacts

- [ ] **T001** Commit the spec kit artifacts (spec, research, plan, data model, contracts, tasks, quickstart, checklist) for feature 007
- [ ] **T002** Record the restructure in `AI-LOG.md`, including the rejected feature 005 direction and the root cause of the navigation defect

## Phase 1: Design system

- [ ] **T003** Replace the colour palette in `ui/theme/Color.kt` with a neutral base and one restrained accent, for light and dark schemes
- [ ] **T004** Rebuild the type scale in `ui/theme/Type.kt` with stronger weight contrast and a proper display size
- [ ] **T005** Reduce the shape radii in `ui/theme/Shape.kt` to a restrained 8-14 dp scale
- [ ] **T006** Review and consolidate the spacing scale in `ui/theme/NutriLogSpacing.kt`
- [ ] **T007** Update `ui/theme/Theme.kt` to the new colour schemes, including the surface roles used by the cards and the app bar
- [ ] **T008** Add the shared UI components: section header, progress bar, macro row, stat tile, list item and empty state

## Phase 2: Shared domain (core:domain)

- [ ] **T009** Add `MealType` with the display name and the display order
- [ ] **T010** Add `DiaryEntry` with the validation rules
- [ ] **T011** Add `NutritionTotals` with `plus` and `scaledBy`
- [ ] **T012** Add `NutritionGoal` with the defaults
- [ ] **T013** Add `DayKey` with the local-day helpers
- [ ] **T014** Add the `DiaryRepository` and `NutritionGoalRepository` interfaces
- [ ] **T015** Write the domain unit tests for the meal ordering, the day bucketing, the totals arithmetic and the goal defaults

## Phase 3: Persistence (core:data)

- [ ] **T016** Add the `DiaryEntry` and `NutritionGoal` tables with their queries in a new `.sq` file
- [ ] **T017** Add the additive `2.sqm` migration and regenerate the schema snapshot
- [ ] **T018** Implement `DiaryRepositoryImpl` over the new table
- [ ] **T019** Implement `NutritionGoalRepositoryImpl` over the singleton table
- [ ] **T020** Wire the two repositories into `ProductSearchRuntime` and the Android and iOS platform factories
- [ ] **T021** Write the data-layer tests: insert, update, remove, day filtering, ordering and goal persistence
- [ ] **T022** Update the module dependency policy allowlist for the new module edges

## Phase 4: Shared presentation logic

- [ ] **T023** Implement `SummaryCalculator` with the daily and the weekly summaries in `feature:summary`
- [ ] **T024** Write the summary tests, including the "empty day is not zero intake" rule
- [ ] **T025** Implement `DiaryState` and `DiaryViewModel` in `feature:diary`
- [ ] **T026** Write the diary view-model tests: load, remove, goal update and day change

## Phase 5: Navigation model

- [ ] **T027** Change `NutriLogDestination` to Today, Search, Library and More with Today as the start destination
- [ ] **T028** Rework `AppShell` into a route-based shell with the four destinations and the three child routes
- [ ] **T029** Implement the tab-switch rule that resets to the destination root and the no-op on re-selection
- [ ] **T030** Hide the bottom bar on the full-screen child routes
- [ ] **T031** Move the product detail from view-model state to the `product/{code}` route
- [ ] **T032** Update the bottom bar to the four destinations with icons and visible labels

## Phase 6: Screens

- [ ] **T033** Implement the Today screen: the summary card, the four meal sections, the entry rows and the weekly card
- [ ] **T034** Implement the add-food flow: the meal step, the food step and the quantity step
- [ ] **T035** Implement the Library screen with the list, the quick-add action and the empty state
- [ ] **T036** Implement the More screen with the goals, the premium section and the about section
- [ ] **T037** Implement the weekly summary screen
- [ ] **T038** Restyle the search screen and the product result card to the new design system
- [ ] **T039** Restyle the product detail and add the "Add to diary" action
- [ ] **T040** Restyle the purchase section and the saved-product list rows
- [ ] **T041** Remove the diary placeholder screen and the superseded saved-products list screen
- [ ] **T042** Wire everything in `App.kt` with the new dependencies and the new screens

## Phase 7: Tests and validation

- [ ] **T043** Write the instrumented tests for the Today screen, the add-food flow, the Library and the More screen
- [ ] **T044** Rewrite the navigation instrumented tests for the new model, including the regression test for the reported defect
- [ ] **T045** Update the existing instrumented tests for the new destinations and routes
- [ ] **T046** Run the full unit-test suite and fix the failures
- [ ] **T047** Run the Android instrumented suite and fix the failures
- [ ] **T048** Run the coverage gate, the architecture tests, ktlint, lint and the plain-text gate
- [ ] **T049** Review every screen in light and dark mode against the design rules
- [ ] **T050** Mark the spec as implemented, update the tasks and the AI log, and open the pull request
