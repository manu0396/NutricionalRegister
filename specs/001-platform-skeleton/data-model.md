# Data Model: Platform Skeleton

This feature introduces no nutrition or user data. The model below describes the
project's architecture and verification artifacts.

## Gradle Module

- **Identity**: Gradle project path and source directory.
- **Kinds**: Android application, shared KMP library, feature-logic KMP library,
  or included build convention plugin.
- **Allowed project dependencies**:
  - `core:domain`: none.
  - `core:common`: none; no module consumes it in this step.
  - `core:data`: `core:domain` only; implements its repository contracts.
  - `feature:search`, `feature:scan`, `feature:diary`, `feature:summary`:
    `core:domain` only.
  - `androidApp`: feature modules needed by its native UI and `core:data` for
    application-level wiring when product features are introduced.
  - `shared`: Apple framework facade exporting the core and feature modules
    required by iOS; excluded from domain-layer dependency assertions.
  - `iosApp`: consumes the built `shared` KMP framework through the existing
    Xcode/Gradle integration; it is not a Gradle project module.
- **Validation**: A forbidden project dependency causes the architecture
  verification task to fail.

## Architecture Boundary Rule

- **Identity**: A named rule that constrains project-module or source-layer
  dependencies.
- **Attributes**: Rule identifier, source module/layer, allowed dependency
  targets, and an actionable failure message.
- **Validation**: Rules cover declared Gradle project dependencies and Kotlin
  source-layer dependencies. `core:domain` must remain dependency-free.

## Verification Workflow

- **Identity**: A required GitHub Actions workflow run for a pull request or
  merge to `main`.
- **Attributes**: Trigger, Linux/Android jobs, macOS/iOS jobs, required checks,
  job results, elapsed duration, and applicable artifact-size measurements.
- **State**: Queued → running → passed or failed.
- **Validation**: A failed required check fails the workflow. Pull-request
  validation includes both platform builds; iOS work runs on macOS. The target
  is under 10 minutes, with parallel jobs and Gradle caching.

## Dependency Decision Record

- **Identity**: An ADR identifier and title.
- **Required attributes**: Need, alternatives considered, binary-size impact,
  build-time impact, licence, and exit strategy.
- **Lifecycle**: Proposed → reviewed → accepted or rejected.
- **Validation**: A new third-party dependency is not added until its ADR is
  reviewed and accepted.

## AI Change Log Entry

- **Identity**: Task or change identifier.
- **Required attributes**: Change summary, AI assistance used, reviewer
  assessment, commands/tests run, outcomes, and any failures or corrections.
- **Validation**: Each AI-assisted change has a review and execution record;
  recurring correction patterns can be considered for constitution updates.

## Build Measurement

- **Identity**: Workflow run, platform, build variant/configuration, and commit.
- **Attributes**: Build duration and produced Android artifact or iOS framework
  size.
- **Validation**: Measurements are reported on merge to `main`. Initial values
  establish baselines; they are not presented as passing an unset size budget.

## Acceptance Verification Mapping

| Specification scenario | Verification |
|---|---|
| AC1: Clean checkout builds Android and iOS | Android assembly on Linux; iOS simulator app build on macOS; documented local commands. |
| AC2: Pull request gets platform checks and review activity | Required Linux/macOS workflow jobs plus a submitted-review status gate. |
| AC3: Forbidden module dependency fails | Negative architecture test introduces a forbidden project edge and expects task failure. |
| AC4: ADR template, AI log, and metrics are present | Repository artifact checks and merge-to-main report validation. |
| AC5: Dependency ADR and task-linked commits are reviewable | ADR required-field checklist and task/commit traceability review. |
| AC6: Tests are deterministic and offline | Test suite runs without live-service access; repeatability and injected time/random sources are checked in tests. |
