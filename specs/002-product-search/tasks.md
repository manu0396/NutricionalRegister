# Tasks: Product Search

**Input**: Design documents from `specs/002-product-search/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`contracts/search-a-licious.md`, and `quickstart.md`

**Tests**: Automated tests are required by the project constitution and the
feature acceptance criteria. Use deterministic clocks, dispatchers, database
fixtures, and Ktor MockEngine; never call the live catalogue in automated tests.

**Organization**: Tasks are grouped by user story so each story can be
implemented and validated as an incremental delivery.

## Phase 1: Setup and Approval Gates

**Purpose**: Resolve external-service and dependency approval prerequisites
before adding dependencies or sending product-search requests.

- [X] T001 Review authoritative public Search-a-licious/Open Food Facts documentation for production/staging, terms, rate limits, User-Agent, registration, and indexed fields; record verified facts, explicit unknowns, and a conservative request policy in `specs/002-product-search/contracts/search-a-licious.md` and `specs/002-product-search/research.md`. This documentary review is complete but does not approve production traffic.
- [X] T002 [P] Prepare the coroutines and serialization dependency ADR, including need, alternatives, Apache-2.0 licence evidence, binary-size/build-time measurement plan, and exit strategy, in `docs/adr/0004-kotlin-coroutines-and-serialization.md`
- [X] T003 [P] Prepare the Ktor client, OkHttp/Darwin engines, and MockEngine dependency ADR, including need, alternatives, Apache-2.0 licence evidence, binary-size/build-time measurement plan, and exit strategy, in `docs/adr/0005-ktor-http-client.md`
- [X] T004 [P] Prepare the SQLDelight runtime, compiler/plugin, platform drivers, and test-driver dependency ADR, including need, alternatives, Apache-2.0 licence evidence, binary-size/build-time measurement plan, and exit strategy, in `docs/adr/0006-sqldelight-persistence.md`
- [X] T005 [P] Verify Metro's official Kotlin 2.4.21 compatibility matrix and prepare its KMP dependency ADR, including alternatives, licence, size/build-time measurement plan, and exit strategy, in `docs/adr/0007-metro-dependency-injection.md`; if incompatible, stop and document the evidence and Koin fallback for maintainer review
- [X] T006 [P] Prepare the Android Compose UI-test dependency ADR, including exact compatible artifacts, alternatives, licence, size/build-time measurement plan, and exit strategy, in `docs/adr/0008-compose-ui-testing.md`
- [X] T007 Obtain maintainer review for ADRs `docs/adr/0004-kotlin-coroutines-and-serialization.md` through `docs/adr/0008-compose-ui-testing.md`; record each accepted/rejected status and rationale in its ADR before adding any corresponding dependency. ADRs 0004–0006 and 0008 accepted; Metro rejected and manual constructor composition selected in ADR-0007.

## Phase 2: Foundational Infrastructure

**Purpose**: Add only approved dependencies and shared testable infrastructure
that blocks all user-story implementation.

**⚠️ CRITICAL**: T007 is an approval gate for dependency adoption. T001's
public-documentation review is complete, but its documented unknowns do not
authorize production traffic; do not send production requests until
Search-a-licious-specific operational requirements and a representative
response contract are confirmed. Automated tests must remain mocked.

- [X] T008 Add only maintainer-approved coroutine and serialization versions/plugins to `gradle/libs.versions.toml`, `build-logic/build.gradle.kts`, and the relevant KMP convention files under `build-logic/src/main/kotlin/`
- [X] T009 Add only the approved Ktor core, content-negotiation/serialization, OkHttp, Darwin, and MockEngine artifacts to `gradle/libs.versions.toml` and configure their common/platform/test source-set dependencies in `core/data/build.gradle.kts`
- [X] T010 Add only the approved SQLDelight Gradle plugin, runtime, Android/Native drivers, and test driver versions to `gradle/libs.versions.toml`, configure the plugin in `build-logic/build.gradle.kts`, and configure `core/data/build.gradle.kts`
- [X] T011 Apply the approved manual constructor-composition decision from `docs/adr/0007-metro-dependency-injection.md`; do not add Metro or Koin plugin/runtime dependencies.
- [X] T012 Add approved Compose UI-test dependencies to `gradle/libs.versions.toml` and `androidApp/build.gradle.kts` for instrumented semantics tests
- [X] T013 Create module-local deterministic test clocks and coroutine-dispatcher fixtures in `core/domain/src/commonTest/kotlin/com/example/nutricionalregister/domain/testing/TestClock.kt`, `core/data/src/commonTest/kotlin/com/example/nutricionalregister/data/testing/TestClock.kt`, and `feature/search/src/commonTest/kotlin/com/example/nutricionalregister/feature/search/testing/TestClock.kt`; do not add consumers to standalone `core:common`
- [X] T014 Create a MockEngine fixture client and reusable HTTP response fixtures in `core/data/src/commonTest/kotlin/com/example/nutricionalregister/data/remote/SearchAliciousTestClient.kt` and `core/data/src/commonTest/kotlin/com/example/nutricionalregister/data/remote/SearchAliciousResponseFixtures.kt`
- [X] T015 Configure the in-memory SQLDelight test database and test driver setup in `core/data/src/androidHostTest/kotlin/com/example/nutricionalregister/data/local/InMemoryProductDatabase.kt`

## Phase 3: User Story 1 - Search and Inspect Products (Priority: P1) 🎯 MVP

**Goal**: Submit a food-name search, see local results before an online
refresh, view remote matches when no useful cache exists, and inspect a
product's available identifying/nutrition details without writing a diary
entry.

**Independent Test**: With a deterministic local database and MockEngine,
verify a cached result is emitted before a blocked network response is
released; verify uncached search maps ordered service results within the
contract fixture; open a result and inspect available details; assert a
whitespace-only submission makes no request and no diary data changes.

### Tests for User Story 1

- [X] T016 [P] [US1] Add domain tests for trimmed non-blank search terms, blank-query no-op behavior, product identity, optional identifying fields, and nutrient basis/unit in `core/domain/src/commonTest/kotlin/com/example/nutricionalregister/domain/product/ProductTest.kt`
- [X] T017 [P] [US1] Add Search-a-licious contract-fixture tests for request method/path/query encoding, language/page/field parameters, response ordering, and empty hits in `core/data/src/commonTest/kotlin/com/example/nutricionalregister/data/remote/SearchAliciousClientTest.kt`
- [X] T018 [P] [US1] Add SQLDelight repository tests for product upsert, ordered term-to-product associations, and snapshot replacement in `core/data/src/androidHostTest/kotlin/com/example/nutricionalregister/data/local/ProductSearchRepositoryTest.kt`
- [X] T019 [P] [US1] Add search presentation tests proving cached results emit before a blocked refresh, successful remote results replace the snapshot, and blank submission makes no repository/network call in `feature/search/src/commonTest/kotlin/com/example/nutricionalregister/feature/search/ProductSearchViewModelTest.kt`
- [X] T020 [P] [US1] Add Compose semantics tests for query entry/submission, result identification, product-detail navigation, and no diary mutation in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/search/ProductSearchScreenTest.kt`

### Implementation for User Story 1

- [X] T021 [US1] Create typed product, nutrition value, search term, and search snapshot domain models in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/product/Product.kt`, `NutritionValue.kt`, and `ProductSearch.kt`; require a non-blank product name and opaque string code, preserve optional unit/basis, and represent missing nutrition by absence rather than fabricated zero
- [X] T022 [US1] Define product-search repository and catalogue-search contracts in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/product/ProductSearchRepository.kt`; keep `core:domain` free of project-module dependencies
- [X] T023 [US1] Define the SQLDelight product, nutrient, query snapshot, and ordered snapshot-product tables and queries in `core/data/src/commonMain/sqldelight/com/example/nutricionalregister/data/ProductSearch.sq`; use a non-empty normalized-term key and opaque string product code
- [X] T024 [US1] Implement Search-a-licious request construction, response-envelope parsing, and valid-hit product mapping in `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/remote/SearchAliciousClient.kt` and `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/remote/ProductResponseMapper.kt`; use the approved contract policy, send only explicit non-blank queries, preserve hit order, and request only verified fields
- [X] T025 [US1] Implement platform HTTP engine and SQLDelight driver factories in `core/data/src/androidMain/kotlin/com/example/nutricionalregister/data/PlatformDataFactories.kt` and `core/data/src/iosMain/kotlin/com/example/nutricionalregister/data/PlatformDataFactories.kt`
- [X] T026 [US1] Implement the SQLDelight-backed local-first product repository in `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/ProductSearchRepositoryImpl.kt`; emit the stored snapshot before refreshing, transactionally persist successful results, preserve cached data on refresh failure, and prevent an older same-term refresh from replacing a newer snapshot
- [X] T027 [US1] Implement the shared search state, explicit submit/detail intents, loading/result/empty phases, and latest-request identity guard in `feature/search/src/commonMain/kotlin/com/example/nutricionalregister/feature/search/ProductSearchViewModel.kt` and `feature/search/src/commonMain/kotlin/com/example/nutricionalregister/feature/search/ProductSearchState.kt`
- [X] T028 [US1] Add native Compose search and result-list UI in `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductSearchScreen.kt`; provide query editing/submission, ordered product identification, initial guidance, and selection of a product
- [X] T029 [US1] Add native Compose product-detail UI in `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductDetailScreen.kt`; show available identifying/nutrition values with units and basis, mark missing values unavailable, and do not add diary actions
- [X] T030 [US1] Compose the approved dependency graph and connect the search/detail screens from the application root in `androidApp/src/main/kotlin/com/example/nutricionalregister/App.kt` and `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductSearchDependencies.kt`
- [X] T031 [US1] Add visible Open Food Facts attribution and catalogue link to the Android search/detail UI in `androidApp/src/main/kotlin/com/example/nutricionalregister/search/OpenFoodFactsAttribution.kt` and render it from `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductSearchScreen.kt`

**Checkpoint**: US1 is independently demonstrable with fixture-backed service
behavior and a working local cache; verify the user can inspect products
without any diary mutation.

## Phase 4: User Story 2 - Understand No-Result, Offline, and Failure States (Priority: P1)

**Goal**: Distinguish blank input, known no-results, offline-with-cache,
offline-without-cache, recoverable service failure, and malformed catalogue
data while retaining valid cached products.

**Independent Test**: Drive the same search state machine using deterministic
empty, timeout, offline, HTTP error, malformed-hit, mixed-validity, and cached
fixtures. Verify each produces its specified distinct, actionable state; a
cached result survives refresh failure; valid hits survive malformed peers.

### Tests for User Story 2

- [X] T032 [P] [US2] Add mapper tests for missing/blank identifiers and names, malformed optional nutrients, non-finite values, genuine zero values, and mixed valid/invalid hits in `core/data/src/commonTest/kotlin/com/example/nutricionalregister/data/remote/ProductResponseMapperTest.kt`
- [X] T033 [P] [US2] Add domain/presentation tests for initial, no-results, offline-with-cache, offline-without-cache, retryable failure, stale-data messaging, and query-edit actions in `feature/search/src/commonTest/kotlin/com/example/nutricionalregister/feature/search/SearchOutcomeTest.kt`
- [X] T034 [P] [US2] Add state-machine race tests where an older request completes after a newer query and where two requests for the same normalized term complete out of order in `feature/search/src/commonTest/kotlin/com/example/nutricionalregister/feature/search/LatestQueryWinsTest.kt`
- [X] T035 [P] [US2] Add Compose semantics tests for distinct empty/offline/error messages and visible stale-result/retry/edit actions in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/search/SearchOutcomeScreenTest.kt`

### Implementation for User Story 2

- [X] T036 [US2] Harden product-hit mapping in `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/remote/ProductResponseMapper.kt`; skip only hits without usable identity/name, preserve other valid hits, and represent missing/malformed nutrients as unavailable rather than zero
- [X] T037 [US2] Add typed repository outcomes and recoverable network/service/malformed-response error mapping in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/product/ProductSearchOutcome.kt` and `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/remote/SearchAliciousClient.kt`
- [X] T038 [US2] Add connectivity status to the shared search contract and Android connectivity provider in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/product/ConnectivityStatusProvider.kt` and `androidApp/src/main/kotlin/com/example/nutricionalregister/search/AndroidConnectivityStatusProvider.kt`
- [X] T039 [US2] Implement distinct no-results, offline, stale-cache, and recoverable-failure transitions plus retry/edit behavior in `feature/search/src/commonMain/kotlin/com/example/nutricionalregister/feature/search/ProductSearchViewModel.kt` and `feature/search/src/commonMain/kotlin/com/example/nutricionalregister/feature/search/ProductSearchState.kt`
- [X] T040 [US2] Render accessible no-results, offline, stale, and recoverable-error UI with actionable controls in `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductSearchScreen.kt` and `androidApp/src/main/kotlin/com/example/nutricionalregister/search/SearchOutcomeMessage.kt`

**Checkpoint**: US2 is independently testable by selecting deterministic
repository outcomes; no test requires a live catalogue or loses cached data
after a failed refresh.

## Phase 5: User Story 3 - Return to Recent Searches Offline (Priority: P2)

**Goal**: Persist the 10 most recent distinct submitted terms, show them after
restart/offline, reuse cached product results, retry uncached offline terms,
and remove an individual term without deleting product data.

**Independent Test**: Complete more than 10 fixture-backed searches, recreate
the database/repository, and verify history order, uniqueness, cap, and cached
offline selection. Remove one term and assert its history row is gone while
its product and snapshot remain.

### Tests for User Story 3

- [X] T041 [P] [US3] Add SQLDelight tests for most-recent ordering including equal-millisecond completions, distinct normalized terms, cap of 10, persistence after reopening, and deletion without product/snapshot deletion in `core/data/src/androidHostTest/kotlin/com/example/nutricionalregister/data/local/RecentSearchRepositoryTest.kt`
- [X] T042 [P] [US3] Add presentation tests for offline history display, selecting cached and uncached recent terms, repeat-term promotion, and individual term deletion in `feature/search/src/commonTest/kotlin/com/example/nutricionalregister/feature/search/RecentSearchViewModelTest.kt`
- [X] T043 [P] [US3] Add Compose semantics tests for most-recent ordering and individual history deletion without selecting or deleting cached products in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/search/RecentSearchScreenTest.kt`

### Implementation for User Story 3

- [X] T044 [US3] Add the distinct recent-search table and ordered-list/upsert/trim/delete queries in `core/data/src/commonMain/sqldelight/com/example/nutricionalregister/data/RecentSearch.sq`; persist normalized terms, non-blank display terms, completion timestamps, and a strictly increasing sequence for deterministic timestamp ties, with at most 10 unique terms
- [X] T045 [US3] Add recent-search domain records and repository operations in `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/product/RecentSearch.kt` and `RecentSearchRepository.kt`
- [X] T046 [US3] Implement SQLDelight recent-history persistence in `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/RecentSearchRepositoryImpl.kt`; promote repeated terms, trim to 10, and delete history independently of cached products/snapshots
- [X] T047 [US3] Integrate recent-history observation, every completed non-blank search outcome (matches, no-results, or recoverable failure), recent-term selection, and individual deletion in `feature/search/src/commonMain/kotlin/com/example/nutricionalregister/feature/search/ProductSearchViewModel.kt`; do not record blank submissions
- [X] T048 [US3] Render persistent recent terms, offline cached selection, uncached offline retry guidance, and individual delete controls in `androidApp/src/main/kotlin/com/example/nutricionalregister/search/RecentSearches.kt` and `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductSearchScreen.kt`

**Checkpoint**: US3 remains independently verifiable after database recreation
and airplane-mode selection; deletion affects only the recent-term record.

## Phase 6: Polish and Cross-Cutting Validation

**Purpose**: Validate accessibility, cross-platform compilation, licensing
attribution, performance expectations, and the full approved quickstart.

- [X] T049 [P] Verify search, result, detail, recent, retry, and delete controls expose accessible names and at least 48 dp touch targets in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/search/ProductSearchAccessibilityTest.kt`
- [X] T050 [P] Verify enlarged system text preserves essential query, result, detail, error, and recent-search actions in `androidApp/src/androidTest/kotlin/com/example/nutricionalregister/search/ProductSearchLargeTextTest.kt`
- [X] T051 [P] Document Open Food Facts attribution, the approved Search-a-licious endpoint, offline/cache behavior, and image-license exclusion in `README.md`
- [X] T052 Run the approved automated scenarios from `specs/002-product-search/quickstart.md`; record actual Android device cache/network timings and any unmet performance target in `AI-LOG.md`
- [X] T053 Verify the approved dependencies' final licences and record measured Android APK/iOS framework size plus clean/incremental build deltas in the corresponding `docs/adr/0004-kotlin-coroutines-and-serialization.md` through `docs/adr/0008-compose-ui-testing.md`
- [X] T054 Verify Android-host and iOS simulator compilation/tests, Android assembly/UI tests, architecture checks, coverage, and Kotlin lint; record exact results in `AI-LOG.md`
- [X] T055 [P] Verify that search terms and product queries are not sent to analytics/tracking and are not written to logs; add a regression test or documented source/dependency audit in `feature/search/src/commonTest/kotlin/com/example/nutricionalregister/feature/search/SearchPrivacyTest.kt` and record the audit result in `AI-LOG.md`
- [X] T056 Measure the complete hosted pull-request CI duration after integrating the feature, record actual elapsed time against the project's under-10-minute target in `AI-LOG.md`, and optimize or document the remaining gap if it exceeds 10 minutes; **completed 2026-10-20 via PR #2** (Android Verification 268 s, iOS Verification 467 s, both under the 10-minute target)

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup and approval)**: T001's public evidence review is complete.
  Keep production Search-a-licious traffic blocked until its documented
  operational unknowns and live response shape are confirmed. Complete T007
  before corresponding dependency integration. Independent ADR drafts T002–
  T006 may be prepared in parallel; dependency additions remain blocked until
  each relevant ADR is accepted.
- **Phase 2 (Foundational)**: Depends on required Phase 1 approvals. T013–T015
  establish deterministic test infrastructure; all story work depends on the
  appropriate approved libraries and test fixtures.
- **US1 (P1)**: Depends on Phases 1–2 and delivers the first independently
  useful search/detail experience. Tasks T016–T020 are test-first; implement
  the contracts, cache, and search state before the Compose integration.
- **US2 (P1)**: Depends on US1's repository and presentation contracts so it
  can expose real error/cache states; its tests T032–T035 remain fixture-only.
  US1 and US2 together make the P1 MVP because both are priority P1 stories.
- **US3 (P2)**: Depends on the shared search repository and SQLDelight database
  established by US1; its history table is independent from product cache rows.
- **Polish (Phase 6)**: Depends on all three story checkpoints.

### User Story Dependencies

- **US1 (P1)**: Starts after the external-service and dependency approval
  gates; independent of other user stories.
- **US2 (P1)**: Uses US1 search outcomes and cache; it may be implemented after
  US1 and can be fixture-tested independently.
- **US3 (P2)**: Uses the shared search repository/database from US1; it can be
  developed as a separate increment once those interfaces stabilize.

### Within Each User Story

- Tests are specified because the constitution requires automated tests for
  business behavior; write them before implementation and verify expected
  failure before code changes.
- Implement domain contracts before repository/state consumers.
- Implement database schema and query operations before SQLDelight repository
  methods.
- Implement mapper and repository behavior before UI wiring.
- Keep platform-specific connectivity, HTTP engines, and database drivers in
  platform source sets.

## Parallel Opportunities

- ADR drafts T002–T006 are independent files and may be prepared in parallel;
  approval still gates dependency additions.
- After Phase 2, US1 domain tests T016, API contract tests T017, persistence
  tests T018, state tests T019, and UI tests T020 are independently authored
  fixture-driven tasks.
- US2 mapper tests T032, outcome/state tests T033–T034, and UI semantics tests
  T035 target separate test files.
- US3 persistence T041, presentation T042, and UI T043 tests target separate
  files.
- Final accessibility tests T049–T050 and README attribution T051 can be
  performed in parallel after the relevant UI and service policy are stable.

## Parallel Example: User Story 1

```text
After the approval gates and test infrastructure are complete:
Task: T016 Product/search domain tests
Task: T017 Search-a-licious request/response contract tests
Task: T018 Product cache repository tests
Task: T019 Cache-first and latest-query presentation tests
Task: T020 Compose search/detail semantics tests
```

## Implementation Strategy

### MVP First (P1 User Stories 1 and 2)

1. Keep the production-service gate in place for unresolved Search-a-licious
   operational policy; obtain maintainer approval for each dependency ADR.
2. Complete foundational dependency, platform factory, and deterministic test
   infrastructure.
3. Complete US1 and US2, both P1: explicit search, cached-first/network
   refresh, product identification/detail inspection, blank-query no-op,
   distinct empty/offline/error states, stale-cache handling, and no diary
   mutation.
4. Validate both P1 stories independently with fixture-backed tests and an
   Android device smoke test. Do not claim live-service behavior or network
   timing targets until verified against an approved service.

### Incremental Delivery

1. Phase 1 + Phase 2 → approved and testable foundation.
2. US1 + US2 → complete P1 search/detail and resilient state-handling MVP.
3. US3 → persistent, bounded, individually deletable offline history.
4. Polish → accessibility, privacy, licensing, platform, and performance
   verification.

## Notes

- `[P]` marks tasks that touch separate files and have no prerequisite on
  another incomplete task.
- `[US1]`, `[US2]`, and `[US3]` map directly to the stories in `spec.md`.
- Preserve the existing inward dependency policy; do not make `core:common` a
  dependency of the product-search modules.
- Do not log search terms, product details, or user data.
- Do not add a live-network test, a new navigation dependency, product images,
  diary operations, or iOS UI in this feature.
- Task descriptions preserve the data-model constraints for required product
  identity/name, optional nutrient values/units/basis, normalized non-empty
  terms, independent history deletion, and the maximum of 10 recent terms.

## Phase 7: Convergence

> Task IDs in this phase are local to the 002 feature and do not overlap in
> meaning with the 003 `T057`-`T095` range in `specs/003-ui-refresh/tasks.md`.

- [X] T057 Resolve the unsatisfiable `review-activity` pull-request gate: the
  check in `.github/workflows/review-gate.yml` requires a submitted review from a
  login *different* from the PR author, but the repository has a single
  collaborator, so the gate cannot pass even after a submitted `COMMENTED`
  self-review (verified on PR #2, run 39391340341). Record the solo-maintainer
  constraint in a new ADR and amend the workflow so a self-review is honoured or
  a configured reviewer list is used, keeping the gate meaningful (partial,
  Constitution II / verification gates) — CRITICAL.
- [X] T058 Close T056 with measured hosted-CI evidence: record PR #2's actual
  durations (Android Verification 268 s, iOS Verification 467 s, both under the
  10-minute target) in `AI-LOG.md` and mark T056 complete (partial, T056) — HIGH.
- [X] T059 Update `specs/002-product-search/spec.md` from `Status: Draft` to
  `Status: Implemented`, since all 55 tasks are complete and hosted CI is green
  (partial, spec.md front-matter) — HIGH.
- [X] T060 Add an FR-/SC-to-task traceability table to
  `specs/002-product-search/plan.md`, matching the traceability table already
  present in `specs/003-ui-refresh/data-model.md` (partial, plan.md) — MEDIUM.
- [X] T061 Record in `AI-LOG.md` that the product-image surface now present in
  002's screens is owned by feature 003 (ADR-0009/0010), so 002's "no product
  images" scope note is superseded and traceable rather than contradictory
  (unrequested, tasks.md scope note) — LOW.
