# Implementation Plan: Product Search

**Branch**: `002-product-search` | **Date**: 2026-10-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-product-search/spec.md`

## Summary

Add a shared, offline-first product-search capability backed by a local
SQLDelight cache and the Open Food Facts Search-a-licious full-text endpoint.
The search presentation state and use cases remain in `feature:search`;
`core:domain` owns product/search contracts; and `core:data` owns persistence,
HTTP, and mapping. `androidApp` supplies the initial native Compose search and
product-detail screens and composes the dependencies. Search results from the
device are emitted first and refreshed in the background. The feature does not
create diary entries and does not add iOS screens.

The selected endpoint is a planning decision, not permission for production
traffic. The public documentation review is recorded in
[research.md](research.md) and the [service contract](contracts/search-a-licious.md):
it confirms the root `/search` endpoint, publicly configured candidate fields,
and Open Food Facts' general data-reuse guidance, but does not establish
Search-a-licious-specific quotas, client-header or registration requirements,
or a live response-field guarantee. Do not assume limits documented for
separate Product Opener endpoints apply. Keep tests mocked and confirm any
remaining service requirements before production deployment.

## Technical Context

**Language/Version**: Kotlin Multiplatform, Kotlin 2.4.21 and Gradle 9.5.1
from the current project catalog; SwiftUI remains outside this feature's UI
scope.

**Primary Dependencies**: Existing KMP module conventions plus proposed
`kotlinx.coroutines` 1.11.0, `kotlinx.serialization` 1.11.0, Ktor 3.6.0,
SQLDelight 2.4.1, and Metro 1.4.5. These versions were checked against their
official release sources on 2026-10-19. They are planning candidates only:
create and review ADRs covering need, alternatives, measured or planned
binary-size/build-time impact, licence, and exit strategy before adding any
dependency or version-catalog entry. Verify Metro's Kotlin compiler
compatibility against the repository's Kotlin version before adoption.
Proposed Ktor engines are OkHttp on Android and Darwin on iOS; tests use
Ktor's MockEngine. Proposed SQLDelight drivers are Android and Native, with a
SQLite test driver where JVM integration tests require one.

**Storage**: On-device SQLDelight/SQLite. Persist products, normalized recent
terms, query-to-product relationships, and fetch timestamps. No account,
remote diary, analytics, or search-history upload.

**Testing**: Deterministic common tests for domain and MVI behavior; SQLDelight
repository tests using an in-memory/test driver; Ktor MockEngine fixtures for
success, empty, malformed, timeout, and server-error responses; Android
Compose semantics/UI tests for accessibility and navigation; Android-host and
iOS simulator compilation/tests for shared code. Inject time and coroutine
dispatchers. Never call the live catalogue from automated tests.

**Target Platform**: Android 24+ initial UI, with domain, data, and search
behavior implemented in KMP common code for later native iOS screens.

**Project Type**: Kotlin Multiplatform mobile application with a native Android
Compose UI and a native SwiftUI application.

**Performance Goals**: Show a matching local snapshot within 1 second; show
network results within 3 seconds on a normal connection. Submit only explicit
searches, cancel or reject superseded requests, and never hold local results
behind a refresh. In deterministic tests, assert the local emission occurs
before releasing a blocked network response; measure elapsed time in a
separate device smoke test rather than relying on a flaky CI wall-clock test.
The complete hosted pull-request CI pipeline must remain under 10 minutes;
measure the full hosted run after integrating this feature and record any
overrun and mitigation rather than assuming local build duration predicts CI.

**Constraints**: Respect the existing inward module graph and the constitution's
dependency-ADR gate. Only the latest submitted query may update screen state.
Recent history is a separate persisted concern from cached product data. Cache
and history remain on-device. Show missing nutrition values as unavailable,
not zero. Do not add diary behavior or iOS screens.
Do not display product images in this feature: that constraint was scoped to
`002-product-search` and is superseded by ADR-0009 and `specs/003-ui-refresh/`
(images from the Open Food Facts catalogue are now displayed with attribution).
Handle rate limits and server failures as recoverable outcomes; do not retry
unboundedly. Public documentation does not establish Search-a-licious-specific
operational limits or incoming User-Agent requirements. These remain
production-deployment checks; implementation must not assume Product Opener
limits apply.

**Scale/Scope**: One Android search screen, a product-detail view, shared
search/domain/data behavior, a bounded local result cache, and a maximum of 10
distinct recent terms. No barcode scan, recommendation, manual product entry,
account, analytics, synchronization, or diary write.

**Build Health**: Measure the full hosted Android/iOS pull-request checks after
feature integration. The target is under 10 minutes, matching the project
brief; if exceeded, optimize or report the actual remaining gap.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Result | Evidence |
|---|---|---|
| Shared logic and deliberate platform boundaries | PASS | Domain/data/search behavior stays in KMP; the initial UI is native Compose in `androidApp`; no iOS screen is added. |
| Cross-platform compatibility | PASS | Shared contracts and state are common Kotlin and are validated on Android-host and iOS simulator targets. |
| Verify behavior with tests | PASS | Cache/network ordering, state transitions, persistence, malformed records, query races, and accessibility have deterministic test coverage planned. |
| Focused, maintainable changes | PASS | The feature is limited to discovery and inspection; diary, scanning, and media rendering remain out of scope. |
| Module boundaries | PASS | `core:domain` has no project dependencies; `core:data` depends only on domain; `feature:search` depends only on domain; the Android composition root depends on data and search. |
| Governance and dependency review | PASS WITH REQUIRED SEQUENCE | Prepare/review ADRs for each new runtime and test dependency before changing build files or adding catalog entries. |
| External data requirements | PASS WITH DOCUMENTED LIMITATIONS | Public evidence and unresolved service-specific questions are recorded in the contract and research. Use explicit-submit requests and conservative error handling; production traffic still requires confirmation of Search-a-licious-specific policy. |

## Phase 0: Research

See [research.md](research.md). Official documentation confirms that
Search-a-licious supports full-text search with `GET /search?q=...`, whereas
the Open Food Facts v2 `/search` endpoint is filter-oriented and does not
provide the free-text behavior required by F1. The maintainer selected
Search-a-licious for this feature. Its published OpenAPI schema leaves each
hit as an untyped object and does not establish operational rate limits or
User-Agent requirements; those are explicit pre-implementation checks, not
assumptions.

## Phase 1: Design and Contracts

See [data-model.md](data-model.md), [contracts/search-a-licious.md](contracts/search-a-licious.md),
and [quickstart.md](quickstart.md).

### Project Structure

#### Documentation (this feature)

```text
specs/002-product-search/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── search-a-licious.md
└── checklists/
    └── requirements.md
```

#### Source Code (repository root)

```text
androidApp/
└── src/main/                    # Compose search/detail UI and app composition
core/
├── domain/src/commonMain/       # Product, nutrition, search repository contracts
└── data/
    ├── src/commonMain/          # Repository implementation, HTTP mapping, SQLDelight
    ├── src/androidMain/         # Android HTTP and SQLite drivers
    └── src/iosMain/             # Darwin HTTP and Native SQLite drivers
feature/
└── search/src/commonMain/       # Search state, intents, and use-case orchestration
shared/                          # Existing Apple framework facade; no new iOS UI
```

**Structure Decision**: Follow the existing `androidApp`, `core`, `feature`,
and `shared` layout. Put cross-platform business behavior and repository
contracts in common source sets. Keep platform engines and SQLite driver
construction in platform source sets and create the object graph at the
Android application boundary. `core:common` remains standalone. Do not add a
navigation library for this single feature; the Android app can hold the
selected-product route in its existing Compose root until the app navigation
scope is separately specified.

### Dependency and External-Service Decision Records

Before implementation adds dependencies, create and review ADRs for:

- Kotlin coroutines and serialization runtime/compiler support.
- Ktor client, selected Android/iOS engines, content negotiation, and MockEngine.
- SQLDelight runtime, compiler/plugin, platform drivers, and test driver.
- Metro runtime/compiler integration, conditional on Kotlin compatibility.
- Android Compose UI test artifacts required to verify semantics and
  accessibility.

Record measured binary-size and build-time deltas after integrating each
choice. If Metro is incompatible with Kotlin 2.4.21, stop before adding it;
document the compatibility evidence and present Koin as the project-brief
fallback for maintainer review.

### Design Re-check

The proposed design preserves shared KMP behavior, native Android UI, existing
module boundaries, device-only history, and deterministic tests. No constitution
exception is required. The remaining production-traffic limitations and ADR review sequence remain
mandatory before dependency adoption or production requests.

## Complexity Tracking

No constitution violations. The one external integration risk is bounded by
the pre-implementation verification gate documented above; no fallback endpoint
or unsupported rate-limit assumption is introduced.

## Requirement Traceability

Every functional requirement and success criterion maps to at least one task in
`tasks.md`, so no requirement is implemented without a tracking task.

| Requirement | Task IDs |
|---|---|
| FR-001 (enter and submit a term) | T029, T031 |
| FR-002 (distinguish products in results) | T028, T029 |
| FR-003 (inspect details without a diary entry) | T040, T048 |
| FR-004 (on-device results < 1 s; background refresh) | T020, T022, T051 |
| FR-005 (catalogue results < 3 s) | T019, T021 |
| FR-006 (distinct outcome states and next action) | T033-T038 |
| FR-007 (stale results survive a catalogue failure) | T036, T037 |
| FR-008 (persistent, ordered, de-duplicated, 10-cap history) | T015, T024, T030, T041 |
| FR-008a (non-blank searches recorded; blanks not) | T025, T030 |
| FR-009 (missing nutrition shown as unavailable, never zero) | T017, T040 |
| FR-010 (history on-device; no analytics) | T018, T051 |
| FR-011 (malformed data never crashes search) | T016, T023 |
| FR-012 (search never writes diary entries) | T012, T053 |
| FR-013 (only the latest query updates the screen) | T002, T014 |
| FR-014 (48 dp targets, accessible names, large text) | T012, T031, T034 |
| SC-001 (on-device results < 1 s) | T020, T022 |
| SC-002 (catalogue results < 3 s) | T019, T021 |
| SC-003 (distinguishable feedback per outcome) | T033-T038 |
| SC-004 (history survives restart offline; 10-cap; deletion) | T015, T024, T041 |
| SC-005 (no zero-for-missing; malformed-safe) | T017, T023, T040 |
| SC-006 (no diary mutation) | T012, T053 |
| SC-007 (accessible at largest text size) | T031, T034 |

Automated coverage note: SC-001 and SC-002 are timing criteria asserted through
deterministic repository/clock contracts rather than wall-clock measurements in
the test suite; the measurement approach is documented in `quickstart.md`.

