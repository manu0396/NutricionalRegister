# Tasks: Intuitive navigation (spec 008)

**Input**: [spec.md](spec.md), [plan.md](plan.md), [data-model.md](data-model.md), [contracts/navigation-flow-contracts.md](contracts/navigation-flow-contracts.md)
**Branch**: `008-intuitive-navigation`

**Format**: `[ID] [P?] [Story]` — **[P]** the task can run in parallel with the
tasks of other stories; **[Story]** the story it belongs to.
**Contribution rule**: one task is one commit, and a task's commit message
names the task's ids.

## Path Conventions

- Shared logic: `core/domain/src/.../domain/navigation/`,
  `core/domain/src/commonTest/`
- Shell and navigator: `androidApp/src/main/kotlin/com/example/nutricionalregister/`
- Instrumented tests: `androidApp/src/androidTest/`

## User Story 1 — a food is logged and the user goes on (P1)

- [x] **T001** [US1] Write ADR-0021 (the flow's landing and back: a confirm
      lands on the flow's own source step with the meal kept, a detail opened
      from a flow row carries the flow further), linked to ADR-0019
- [ ] **T002** [US1] Add the landing rule to the shared navigation model
      (`core/domain/.../domain/navigation/NutriLogRoutes.kt`): the flow's
      source step, with the meal as the flow's payload, is the confirm's
      landing, and no route string changes
- [ ] **T003** [US1] Cover the landing rule with domain tests
      (`core/domain/.../domain/navigation/NutriLogRoutesTest.kt`): the landing
      is a route, carries the meal, and is not the detail's
- [ ] **T004** [US1] Add the navigator's landing carrier
      (`androidApp/.../ui/NutriLogNavigator.kt`): `landAfterAddFood(meal)` pops
      the flow's graph to its own start, keeping the flow's session (one entry
      popped, not the whole stack)
- [ ] **T005** [US1] Rewire the flow's confirm to the landing rule
      (`androidApp/.../App.kt`): after a confirm the app lands on the search,
      with the meal still chosen, and the product detail the user came from is
      never presented
- [ ] **T006** [US1] Make the detail's "Add to diary" carry the flow further
      (ADR-0021): when a flow is open the action returns to its quantity step
      with the code, and when there is none it opens the flow as today;
      covered in the destination layer's test
- [x] **T007** [US1] Instrumented test of story 1 (the real app, emulator):
      open the flow from a meal, log a food, confirm, assert the search with
      the meal, the entry among the diary's sections, and no detail on the
      way out.

## User Story 2 — back means the previous step, never a detour (P1)

- [x] **T008** [US2] Instrumented test of the walk back at every step of the
      flow (the quantity step -> the source step with the choices; the source
      step -> the screen the flow came from)
- [x] **T009** [US2] Instrumented test of the detail's round trip inside a
      flow row: the flow's step, the query and the results kept, and a second
      opening never pushed
- [x] **T010** [US2] Instrumented test of More's inline goal editor and privacy
      surface: change a goal, open and close privacy, and assert More retains
      the edited goal and remains the active destination
- [x] **T011** [US2] Instrumented test of the system's back at the flow's
      first step, which returns to the screen the flow came from

## User Story 3 — only screens the user asked for, ever (P2)

- [x] **T012** [US3] Instrumented test: a repeated tap of the bottom bar's
      current tab is a no-op, and no entry is pushed (the bar's visibility
      unchanged)
- [x] **T013** [US3] Instrumented test: the coach opens over the screen it is
      asked from, closes back onto it, and the flow is exactly as it was
- [ ] **T014** [US3] Run the whole gates against the feature:
      `:core:domain:testAndroidHostTest :feature:diary:testAndroidHostTest
      :coverageVerify :androidApp:ktlintCheck :kotlinLint :architectureTest
      :plainTextCheck :androidApp:connectedDebugAndroidTest`, then review every
      changed screen in light and dark mode

## Completion

- [ ] **T015** Update the spec's status, record the work in AI-LOG.md, and
      open the pull request

Tasks grouped by story; each carries its tests with it (a task's commit is the
code and its test together), so a story is independently landable.
