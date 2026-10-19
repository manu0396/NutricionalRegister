# Feature Specification: Platform Skeleton

**Feature Branch**: `001-platform-skeleton`

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description: "Start with Step 0 (Platform skeleton) from the project brief in docs/Project brief for Spec Kit KMP nutrition diary.md."

## Clarifications

### Session 2026-10-19

- Q: Should every pull request require a successful iOS build, or may that check be non-blocking when the required workflow cannot use a macOS runner? → A: Require successful Android and iOS builds on every pull request; use a macOS runner for iOS.
- Q: What must happen before a change can be merged into `main` for its pull request to count as reviewed? → A: Require review activity, but do not require an approval; all required checks must pass.
- Q: The brief defines `core:common` for shared utilities, clocks, and dispatchers, but its strict dependency rule says `domain` depends on nothing. Which boundary should the plan enforce? → A: Keep `core:common` standalone; defer consumers until a need is specified.

## User Scenarios & Testing

### User Story 1 - Establish a Verifiable Project Foundation (Priority: P1)

As a contributor to this public mobile project, I need a reproducible foundation
for building and checking the Android and iOS applications so that I can make
future changes with confidence before product features are added.

**Why this priority**: The project brief makes the platform foundation the first
delivery step. It enables every later feature and demonstrates maintainable
cross-platform engineering.

**Independent Test**: A contributor can follow the repository instructions on a
clean checkout, build the supported applications, and see automated checks
complete successfully in the repository's pull request workflow.

**Acceptance Scenarios**:

1. **Given** a clean checkout and the documented prerequisites, **When** a
   contributor follows the build instructions, **Then** the Android and iOS
   application targets build successfully.
2. **Given** a proposed change on a pull request, **When** the automated
   verification workflow runs, **Then** build, test, lint, and architecture
   checks report their results for Android and iOS, and a failed check prevents
   the change from passing. At least one pull request review is recorded, but
   reviewer approval is not required.
3. **Given** a change that violates an allowed dependency boundary, **When** the
   architecture checks run, **Then** the violation is reported as a failure.
4. **Given** the initial platform foundation is complete, **When** a contributor
   reviews the repository, **Then** they can find the architecture decision
   record template, AI change log, and a report of build duration and artifact
   sizes.
5. **Given** a proposed dependency or task change, **When** a reviewer checks its
   contribution record, **Then** the dependency decision includes the required
   evaluation topics and each task commit identifies its task.
6. **Given** the core tests run in an environment without network access or a
   fixed wall clock, **When** the same tests run repeatedly, **Then** they
   produce the same results without contacting a live service.

### Edge Cases

- A required build or verification check fails: the workflow reports failure
  rather than presenting the pull request as passing.
- A module depends on a layer it is not allowed to depend on: an automated
  architecture check identifies and rejects the boundary violation.
- A new third-party dependency is proposed without a decision record: the
  contribution process makes the missing decision record detectable during
  review.
- An iOS-capable macOS runner is unavailable: the required iOS check fails and
  the pull request cannot pass verification.
- An initial size or build-time measurement has no established budget yet: the
  report records a baseline without claiming compliance with a budget that has
  not been agreed.

## Requirements

### Functional Requirements

- **FR-001**: The repository MUST provide documented, repeatable build and test
  instructions for its Android and iOS application targets.
- **FR-002**: The repository MUST separate user interface code from shared
  business rules, data handling, and networking behavior.
- **FR-003**: Android and iOS MUST each have a native user interface entry point;
  business rules MUST NOT be implemented in either user interface layer.
- **FR-004**: The project MUST define the agreed project modules and their
  permitted dependency direction: feature presentation depends on domain,
  domain has no project-module dependencies, and data implements domain
  interfaces. `core:common` MUST remain standalone and have no consumers until
  a later specification establishes a permitted dependency.
- **FR-005**: Automated architecture checks MUST fail when a project-module
  dependency violates the permitted direction.
- **FR-006**: Pull request verification MUST report build, automated test, lint,
  and architecture-check results for Android and iOS. Both platform builds are
  required on every pull request, using a macOS runner for iOS. Any required
  verification failure MUST result in a failing workflow.
- **FR-007**: The project MUST maintain a central inventory of dependency
  versions. A new third-party dependency MUST have a decision record describing
  its need, alternatives, binary-size and build-time impact, licence, and exit
  strategy before adoption.
- **FR-008**: Automated verification MUST enforce at least 80% line coverage for
  the domain and data modules when those modules contain production code.
- **FR-009**: Automated tests MUST be structured so acceptance criteria for
  future features can be mapped to tests, and tests MUST NOT depend on live
  network services, the current clock, or unseeded randomness.
- **FR-010**: Public APIs MUST declare their types, and the mainline branch MUST
  not contain unresolved placeholders, TODOs, or dead code.
- **FR-011**: The repository MUST include an architecture decision record
  template and an AI change log. The log MUST support recording AI-assisted
  changes, their review and test results, and any failures or corrections.
- **FR-012**: The contribution process MUST require one task per atomic commit
  with its task identifier, and changes MUST reach the mainline branch through
  pull requests with at least one recorded review. Reviewer approval is not
  required.
- **FR-013**: The project MUST not include analytics or tracking; food diary
  data MUST remain on the user's device.
- **FR-014**: The project MUST report build duration and Android and iOS
  artifact sizes in automated verification on changes merged to the mainline
  branch. Initial reports MUST establish a baseline before a size budget is set.
- **FR-015**: The initial foundation MUST not implement nutrition diary product
  features, user accounts, cloud sync, a custom backend, social features, meal
  recommendations, or manual product creation.

## Success Criteria

### Measurable Outcomes

- **SC-001**: A clean checkout can build both supported application targets
  using the instructions in the repository.
- **SC-002**: Every pull request receives a passing result for Android and iOS
  builds and a reported result for each required test, lint, and architecture
  check; any required check failure makes the verification workflow fail.
- **SC-003**: An automated architecture check rejects a test change that
  introduces a forbidden project-module dependency.
- **SC-004**: Domain and data modules with production code achieve at least 80%
  line coverage as measured by automated verification.
- **SC-005**: Each dependency introduced after the foundation has a decision
  record containing all six required topics: need, alternatives, binary-size
  impact, build-time impact, licence, and exit strategy.
- **SC-006**: Automated verification reports build duration and artifact size
  for both supported application targets on every change merged to the
  mainline branch.
- **SC-007**: No food diary data is transmitted off-device, and the project
  contains no analytics or tracking SDKs.
- **SC-008**: Every core test can run without a live network service, and
  repeated runs with the same inputs produce the same results.
- **SC-009**: Every change on the mainline branch is traceable to a task
  identifier and a reviewed pull request.
- **SC-010**: Public APIs declare their types, and the mainline branch contains
  no unresolved placeholders, TODOs, or dead code.
- **SC-011**: The required pull request verification workflow completes in
  under 10 minutes.

## Assumptions

- The existing repository is the starting point; this feature may restructure
  its generated starter application to meet the agreed project boundaries.
- The first platform delivery establishes size and build-time baselines; numeric
  budgets are set only after those measurements are reviewed.
- The Open Food Facts integration and nutrition diary behavior belong to later
  feature specifications, not this platform-foundation feature.
- A CI environment capable of building each supported platform is available or
  will be selected during planning.
- Exact module implementation, tooling choices, and dependency versions are
  decisions for the implementation plan and must follow the project brief and
  constitution.
