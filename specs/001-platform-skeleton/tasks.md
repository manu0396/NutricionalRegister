---

description: "Dependency-ordered implementation tasks for the platform skeleton"
---

# Tasks: Platform Skeleton

**Input**: Design documents from `specs/001-platform-skeleton/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, and
`quickstart.md`

**Tests**: Required by FR-005, FR-008, FR-009 and the acceptance criteria.
Architecture, module-boundary, build, lint, and coverage checks are included.

**Organization**: All implementation tasks belong to User Story 1 because the
specification defines one independently testable platform-foundation journey.

## Format

Every implementation task uses `- [ ] Tnnn [P?] [US1?] Description with exact
repository file paths`. `[P]` appears only for tasks that can edit separate
files after their listed prerequisites are complete.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish reviewable records and approve any new third-party
tooling before adding it to the build.

- [X] T001 Create the dependency decision-record template with all six required fields (need, alternatives, binary-size impact, build-time impact, licence, exit strategy) in `docs/adr/0000-template.md`.
- [X] T002 [P] Create the AI-assisted change review, execution, failure, and correction log in `AI-LOG.md`.
- [X] T003 [P] Draft the Kover adoption ADR using `docs/adr/0000-template.md` in `docs/adr/0001-kover-coverage.md`.
- [X] T004 [P] Draft the Konsist adoption ADR using `docs/adr/0000-template.md` in `docs/adr/0002-konsist-architecture-tests.md`.
- [X] T005 [P] Draft the ktlint adoption ADR using `docs/adr/0000-template.md` in `docs/adr/0003-ktlint-kotlin-lint.md`.
- [X] T006 Review and explicitly accept or reject each proposed tool ADR in `docs/adr/0001-kover-coverage.md`, `docs/adr/0002-konsist-architecture-tests.md`, and `docs/adr/0003-ktlint-kotlin-lint.md`; do not add any rejected tool to the build.
- [X] T007 Verify a mutually compatible stable Kotlin, Compose, Android Gradle Plugin, Gradle, and Java toolchain set; update the pins in `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, and `gradle/gradle-daemon-jvm.properties`, recording the compatibility evidence in `docs/toolchain.md`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Set up the included Gradle build and the project layout required by
the platform-foundation story.

**Checkpoint**: The multi-project build configures successfully and each module
applies its designated convention before story-specific verification is added.

- [X] T008 Create the included convention-plugin build in `build-logic/settings.gradle.kts` and `build-logic/build.gradle.kts`, then register `includeBuild("build-logic")` and the planned core, feature, architecture-tests, app, and framework-facade projects in `settings.gradle.kts`.
- [X] T009 Create reusable KMP library, Android application, and test convention plugins in `build-logic/src/main/kotlin/nutrition.kmp-library.gradle.kts`, `build-logic/src/main/kotlin/nutrition.android-application.gradle.kts`, and `build-logic/src/main/kotlin/nutrition.kotlin-test.gradle.kts`.
- [X] T010 [P] Create the standalone common KMP module and its applied convention in `core/common/build.gradle.kts`.
- [X] T011 [P] Create the dependency-free domain KMP module and its applied convention in `core/domain/build.gradle.kts`.
- [X] T012 Create the data KMP module with only `core:domain` as a project dependency in `core/data/build.gradle.kts`.
- [X] T013 [P] Create the search presentation KMP module with only `core:domain` as a project dependency in `feature/search/build.gradle.kts`.
- [X] T014 [P] Create the scan presentation KMP module with only `core:domain` as a project dependency in `feature/scan/build.gradle.kts`.
- [X] T015 [P] Create the diary presentation KMP module with only `core:domain` as a project dependency in `feature/diary/build.gradle.kts`.
- [X] T016 [P] Create the summary presentation KMP module with only `core:domain` as a project dependency in `feature/summary/build.gradle.kts`.
- [X] T017 Configure `androidApp/build.gradle.kts` to use the Android application convention and native Compose dependencies without depending on the shared UI.
- [X] T018 Retain `shared/build.gradle.kts` as an Apple framework facade that exports the KMP modules required by iOS while excluding the facade from domain-layer dependency rules.

---

## Phase 3: User Story 1 - Establish a Verifiable Project Foundation (Priority: P1) 🎯 MVP

**Goal**: A contributor can build the native Android and iOS applications from
a clean checkout, and pull requests receive deterministic, reviewable checks
for the platform, module architecture, code quality, and dependency boundaries.

**Independent Test**: Follow `specs/001-platform-skeleton/quickstart.md` from a
clean checkout; both application targets, applicable tests, lint, architecture
checks, coverage verification, and the review-activity gate pass. A deliberately
forbidden module edge fails architecture verification.

### Tests and verification rules for User Story 1

- [X] T019 [P] [US1] Add allowed-edge and forbidden-edge Gradle TestKit cases in `build-logic/src/test/kotlin/com/nutrition/build/ModuleDependencyPolicyTest.kt`; verify the forbidden-edge case fails before implementing the policy.
- [X] T020 [P] [US1] Add source-layer dependency rules for domain, data, and feature packages in `architecture-tests/src/test/kotlin/com/nutrition/architecture/ModuleArchitectureTest.kt`.
- [X] T021 [P] [US1] Document test rules requiring no live network, injected time and IDs, seeded randomness, and acceptance-criterion traceability in `docs/testing.md`.

### Platform and module implementation for User Story 1

- [X] T022 [US1] Implement the Gradle dependency allowlist and actionable failure reporting in `build-logic/src/main/kotlin/com/nutrition/build/ModuleDependencyPolicy.kt`, register `architectureTest`, and make it fail for the forbidden-edge case from T019.
- [X] T023 [US1] Configure `architecture-tests/build.gradle.kts` to run the T020 architecture rules using the accepted Konsist ADR and register its test task with `architectureTest`.
- [X] T024 [US1] Apply the accepted Kover ADR and enforce at least 80% JVM line coverage for `core:domain` and `core:data` only when the respective module contains production code in `build.gradle.kts`, `core/domain/build.gradle.kts`, and `core/data/build.gradle.kts`.
- [X] T025 [US1] Move the native Android Compose screen and preview out of shared UI into `androidApp/src/main/kotlin/com/example/nutricionalregister/App.kt` and update `androidApp/src/main/kotlin/com/example/nutricionalregister/MainActivity.kt` to launch it.
- [X] T026 [US1] Replace the Compose hosting wrapper with native SwiftUI in `iosApp/iosApp/ContentView.swift`, retain the SwiftUI app entry in `iosApp/iosApp/iOSApp.swift`, and remove the Compose `MainViewController` bridge from `shared/src/iosMain/kotlin/com/example/nutricionalregister/MainViewController.kt`.
- [X] T027 [US1] Remove shared UI-only Compose dependencies and resources from `shared/build.gradle.kts` and `shared/src/commonMain/kotlin/com/example/nutricionalregister/App.kt`, retaining shared platform-independent logic and the Apple framework facade.

### CI, contribution gates, and reports for User Story 1

- [X] T028 [US1] Add required Ubuntu pull-request verification for Android assembly, shared tests, Android Lint, architecture checks, and Kover verification in `.github/workflows/android.yml`.
- [X] T029 [US1] Add required macOS pull-request verification for iOS simulator tests, native SwiftUI app build, and `swift format lint` in `.github/workflows/ios.yml`.
- [X] T030 [US1] Add the submitted-review status gate in `.github/workflows/review-gate.yml`; on pull-request and submitted-review events require at least one recorded review from a user other than the author, without requiring an approval, and preserve a stable required status check for the pull-request head commit.
- [X] T031 [US1] Add a merge-to-main metrics workflow in `.github/workflows/metrics.yml` that records Android release APK size, iOS release framework size, clean and incremental build durations, and labels the first observations as baselines without an unset size budget.
- [X] T032 [US1] Configure the accepted ktlint ADR and repository-wide Kotlin lint tasks in `build-logic/build.gradle.kts`, `build-logic/src/main/kotlin/nutrition.kotlin-lint.gradle.kts`, and `.github/workflows/android.yml`.
- [X] T033 [US1] Add the dependency inventory, ADR process, native-platform build/test commands, review gate, test policy, and CI metrics description to `README.md`.
- [X] T034 [US1] Enforce explicit public API types in `build-logic/src/main/kotlin/nutrition.kmp-library.gradle.kts` and audit `gradle/libs.versions.toml`, `androidApp/src/main`, `shared/src`, `core/`, and `feature/` for analytics/tracking dependencies, diary-data network transmission, unresolved TODOs, and dead code.

---

## Phase 4: Polish & Cross-Cutting Verification

**Purpose**: Reconcile the run guide with implemented task paths and verify the
entire foundation against its acceptance criteria.

- [X] T035 [P] Update the Android, iOS, architecture, lint, and coverage commands and expected outputs in `specs/001-platform-skeleton/quickstart.md` to match the implemented Gradle tasks and CI job names.
- [ ] T036 [US1] Run every scenario in `specs/001-platform-skeleton/quickstart.md`, including the forbidden-edge negative case, review gate, and clean-checkout Android/iOS builds; fix failures in their owning files.
- [ ] T037 [US1] Verify the required pull-request workflow completes in under 10 minutes and report actual elapsed time and first artifact-size baselines in `.github/workflows/metrics.yml` and `README.md`; if the target is missed, optimize the workflow and record any remaining gap rather than reporting success.
- [X] T038 [US1] Complete the task-linked change, review, command, and outcome record in `AI-LOG.md`, including failures and corrections encountered while implementing this feature.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 and T002 can start immediately. T003-T005 require
  T001. T006 requires T003-T005. T007 requires accepted ADR outcomes from T006.
- **Foundational (Phase 2)**: Begins after T007. T008 establishes the included
  build and project paths; T009 depends on T008. Module build tasks T010-T018
  depend on the conventions in T009; T012-T016 also depend on `core:domain`
  existing.
- **User Story 1 (Phase 3)**: Begins after the module foundation. T019-T021
  can run in parallel after T008/T009 and the accepted ADRs. T022 depends on
  T019; T023 depends on T020; T024 depends on T007 and T019's coverage scope.
  T025-T027 depend on T017/T018 and are ordered to remove shared UI only after
  native entry points are configured. CI jobs depend on the modules and tests
  they invoke.
- **Polish (Phase 4)**: T035 follows the final task and workflow names; T036
  depends on all implementation and CI tasks. T037 depends on successful
  baseline runs; T038 records completed implementation and test outcomes.

### User Story Dependencies

- **User Story 1 (P1)**: The only story in this feature. It starts after
  dependency decisions, toolchain alignment, and the Gradle module foundation.
  Its completion is the platform-skeleton MVP; nutrition product features are
  explicitly excluded.

### Parallel Opportunities

- T002 may run alongside T001; after T001, the three independent ADR drafts
  T003-T005 can be prepared in parallel.
- After T009, T010 and T011 can proceed in parallel; after T011, T013-T016 can
  be implemented in parallel because each feature module has its own build file.
- Once T008/T009 and ADRs are complete, T019-T021 touch separate files and can
  be developed in parallel.
- Android UI migration T025 and native SwiftUI migration T026 touch separate
  platforms; perform them after the application/facade configurations exist.
- Android CI T028 and iOS CI T029 use separate workflow files and can be
  developed in parallel after module/test task names are confirmed.

## Parallel Example: User Story 1

```text
After T001:
Task: Draft the Kover ADR in docs/adr/0001-kover-coverage.md
Task: Draft the Konsist ADR in docs/adr/0002-konsist-architecture-tests.md
Task: Draft the ktlint ADR in docs/adr/0003-ktlint-kotlin-lint.md

After T008 and T009:
Task: Create core/common/build.gradle.kts and core/domain/build.gradle.kts
Task: Create feature/search/build.gradle.kts
Task: Create feature/scan/build.gradle.kts
Task: Create feature/diary/build.gradle.kts
Task: Create feature/summary/build.gradle.kts

After module task paths are confirmed:
Task: Add .github/workflows/android.yml
Task: Add .github/workflows/ios.yml
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and accept the required tool ADRs.
2. Complete Phase 2 and confirm Gradle configures every module.
3. Complete the single User Story 1 increment.
4. Run the quickstart and prove the forbidden-dependency test fails when
   intentionally exercised.
5. Confirm both platforms, review activity, and CI metrics are verified before
   considering the foundation complete.

### Incremental Delivery

1. Approve tool decisions and pin a compatible toolchain.
2. Establish Gradle conventions and module boundaries.
3. Migrate the app to native Compose and SwiftUI entry points.
4. Add architecture, coverage, lint, and platform CI gates.
5. Publish initial size/build-time baselines, then update documentation and
   `AI-LOG.md`.

## Notes

- Every implementation task has a unique ID, an exact file path, and follows
  the required checkbox format.
- `[P]` means the task edits distinct files and has no unfinished dependency.
- `[US1]` maps to the sole user story in `spec.md`.
- Keep one task per task-ID commit, referencing the task ID in its commit
  message; submit changes through a pull request with a recorded review.
- Do not add Kover, Konsist, or ktlint until their ADRs are accepted.
- No task implements search, scanning, diary storage, synchronization, or
  nutrition calculations.
