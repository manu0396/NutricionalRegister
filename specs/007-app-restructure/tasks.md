# Tasks: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Spec**: [spec.md](spec.md)
**Plan**: [plan.md](plan.md)

**Rules for every task**
- One task, one commit. Use a Conventional Commit subject.
- A task is only complete when the build compiles and the task's own tests pass.
- No new third-party dependency without an ADR already accepted.
- Business logic belongs in the shared KMP modules, never in `androidApp`.
- Documentation is plain text: no emojis.

## Phase 0: Spec kit and ADRs

- [x] **T001** Add the shared diary domain: `MealType`, `DiaryEntry`, `DiaryDay`, `NutritionTotals`, `NutritionGoal`, `NutritionCalculator`, `DayBoundaryProvider`, the two repository interfaces and their tests (`5fd7f6d`)
- [ ] **T002** Rewrite the spec kit for the Tracker Shell: `spec.md`, `research.md`, `plan.md`, `data-model.md`, `contracts/`, `checklists/`, `quickstart.md`, `tasks.md`
- [ ] **T003** Write ADR-0016 (remote LLM assistant behind a first-party AI gateway), ADR-0017 (barcode scanning with CameraX and ML Kit), ADR-0018 (single premium product, superseding the per-food model in ADR-0014) and ADR-0019 (Tracker Shell navigation model, extending ADR-0015)
- [ ] **T004** Record the restructure in `AI-LOG.md`: the four navigation defects and their root causes, the rejected IA and design directions, the purchase re-point and the assistant's deployment prerequisite

## Phase 1: Shared domain (core:domain)

- [ ] **T005** Add the platform-neutral navigation model to `core:domain`: `NutriLogDestination` (Today, Library, More) and `NutriLogRoutes`, with the route constants and `fromRoute` resolution
- [ ] **T006** Write the navigation model tests: the start destination, the route resolution and the round trip
- [ ] **T007** Add the assistant domain: `AssistantMessage`, `AssistantRole`, `AssistantContext`, `AssistantReply` and `AssistantAction` (log a food, remove an entry, change an entry), with their validation rules
- [ ] **T008** Add the `AssistantClient` contract and the `AssistantActionExecutor` that validates an action against the diary repository before applying it
- [ ] **T009** Write the assistant domain tests: action validation, the rejection of an unknown food or a negative quantity, and that a rejected action writes nothing
- [ ] **T010** Add the single premium product code as one shared constant, and the entitlement check that reads it

## Phase 2: Persistence (core:data)

- [ ] **T011** Add the `DiaryEntry` and `NutritionGoal` tables with their queries in `DiaryEntry.sq`, and the additive `2.sqm` migration from schema v2 to v3
- [ ] **T012** Reconstruct and commit the `databases/2.db` snapshot from `1.db` plus `1.sqm`, and generate `databases/3.db`, so the migration chain verifies from v1 on a fresh checkout
- [ ] **T013** Implement `DiaryRepositoryImpl` over the new table, including the day filtering, the ordering within a day and the entry update
- [ ] **T014** Implement `NutritionGoalRepositoryImpl` over the singleton table
- [ ] **T015** Add `SystemDayBoundaryProvider` for Android and iOS, stepping days through the platform calendar APIs rather than by a fixed number of milliseconds
- [ ] **T016** Wire the two repositories and the day boundary provider into `ProductSearchRuntime` and the Android and iOS platform factories
- [ ] **T017** Write the data-layer tests: insert, update, remove, day filtering, ordering, the day boundary across a daylight-saving transition and goal persistence
- [ ] **T018** Add the AI gateway client over the already-approved Ktor client, and the `AssistantClient` implementation on top of it
- [ ] **T019** Write the AI gateway client tests using Ktor's `MockEngine`: the request shape, the response parsing, the action decoding and the failure mapping. No test may make a live network call
- [ ] **T020** Update the module dependency policy allowlist for the new module edges and the new `feature:assistant` module

## Phase 3: Shared presentation logic

- [ ] **T021** Implement `DiaryState` and `DiaryViewModel` in `feature:diary`, with the load, remove, goal-update and day-change intents
- [ ] **T022** Write the diary view-model tests: load, remove, goal update, the day change and the totals always matching the entries
- [ ] **T023** Implement `SummaryCalculator` with the daily and the weekly summaries in `feature:summary`
- [ ] **T024** Write the summary tests, including the "an empty day is not zero intake" rule and the "an unknown nutrient stays unknown" rule
- [ ] **T025** Create the `feature:assistant` module with `AssistantState` and `CoachViewModel`, covering the transcript, the pending proposal, the consent state and the availability state
- [ ] **T026** Write the assistant view-model tests: sending a message, the disabled state, the offline state and the confirmation of a proposed action
- [ ] **T027** Implement `BarcodeResolver` in `feature:scan`, resolving a scanned code through `ProductSearchRepository.getProduct` and modelling the "not in the offline cache" outcome
- [ ] **T028** Write the barcode resolver tests: a resolved code, an unknown code and an empty catalogue

## Phase 4: Design system

- [ ] **T029** Replace the colour palette in `ui/theme/Color.kt` with a cool neutral ramp and one deep evergreen accent, for the light and the dark scheme
- [ ] **T030** Rebuild the type scale in `ui/theme/Type.kt` so every Material 3 slot is defined, with distinct numeric roles and tabular figures for the quantities
- [ ] **T031** Reduce the shape radii in `ui/theme/Shape.kt` to a restrained 8-14 dp scale
- [ ] **T032** Update `ui/theme/Theme.kt` to the new colour schemes, including the surface roles used by the cards and the app bar
- [ ] **T033** Review and consolidate the spacing scale in `ui/theme/NutriLogSpacing.kt` and apply it everywhere
- [ ] **T034** Add the shared UI components: section header, progress bar, macro row, stat tile, list item, surface card and empty state
- [ ] **T035** Replace the saturated pastel product placeholder with a neutral monogram, so the lists read as product data rather than as a colour chart
- [ ] **T036** Replace the two inconsistent app bars with the single shared app bar

## Phase 5: Navigation model

- [ ] **T037** Rework `AppShell` into a route-based shell: the three top-level destinations, the nested `addfood` graph and the `product/{code}`, `coach` and `weekly` child routes
- [ ] **T038** Implement the destination-selection rule that resets to the destination root, and the no-op on re-selection
- [ ] **T039** Derive the bottom-bar visibility from the route hierarchy, so the bar is visible only on the three top-level routes
- [ ] **T040** Move the product detail from view-model state to the `product/{code}` route, resolved from the local catalogue cache
- [ ] **T041** Update the bottom bar to the three destinations, with icons and visible labels, and add the floating action button
- [ ] **T042** Remove the Android-local `NutriLogDestination`, which now lives in `core:domain`

## Phase 6: Screens

- [ ] **T043** Implement the Today screen: the summary card with the calorie ring, the four meal sections, the entry rows and the seven-day card
- [ ] **T044** Implement the add-food flow: the source step with search, my foods and scan, and the quantity step with the meal sheet
- [ ] **T045** Implement the Library screen with the list, the quick-add action and the empty state
- [ ] **T046** Implement the More screen with the goals editor, the coach and premium section, the privacy section and the about section
- [ ] **T047** Implement the Coach screen with the transcript, the composer, the consent disclosure, the "not configured" state and the confirmation sheet
- [ ] **T048** Implement the weekly summary screen
- [ ] **T049** Implement the scanner screen with CameraX and ML Kit, add the `CAMERA` permission to the manifest, and handle the denied and unavailable cases with a manual-entry fallback
- [ ] **T050** Re-skin the search screen and the product result card to the new design system
- [ ] **T051** Move the product detail to its route, add the "Add to diary" action, and make the back control state where it actually returns to
- [ ] **T052** Move the purchase surface into the More destination as a single premium unlock, and remove every per-food purchase affordance
- [ ] **T053** Remove the diary placeholder screen and the superseded saved-products list screen
- [ ] **T054** Wire everything in `App.kt` with the new dependencies and the new screens, and make the assistant address a build configuration value that defaults to empty

## Phase 7: Tests and validation

- [ ] **T055** Write the instrumented tests for the Today screen, the add-food flow, the Library, the More screen and the Coach screen
- [ ] **T056** Rewrite the navigation instrumented tests for the new model, including the regression test for the reported defect, the bar visibility and the back behaviour
- [ ] **T057** Update the existing instrumented tests for the new destinations and routes
- [ ] **T058** Write the scanner instrumented tests against a fake decoder, so no test needs a camera
- [ ] **T059** Run the full unit-test suite and fix the failures
- [ ] **T060** Run the Android instrumented suite and fix the failures
- [ ] **T061** Run the coverage gate, the architecture tests, ktlint, lint, the plain-text gate and the SQLDelight migration verification
- [ ] **T062** Review every screen in light and dark mode against the design rules, and confirm no screen is left on the pre-restructure visuals
- [ ] **T063** Measure the release APK and the `Shared.xcframework` sizes and record them in ADR-0014, ADR-0015 and ADR-0017
- [ ] **T064** Mark the spec as implemented, update the tasks and the AI log, and open the pull request

## Phase 8: Findings raised by the brainstorming

- [ ] **T065** Fix the Konsist architecture test so it matches the real package roots; the rule currently matches no package and can never fail
- [ ] **T066** Wire the SQLDelight migration verification into the Android CI workflow, which currently never invokes it
- [ ] **T067** Close specification 006 as superseded by 007, since the two overlap almost entirely and 006 was never implemented
