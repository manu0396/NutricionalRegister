# Implementation Plan: Platform Skeleton

**Branch**: `001-platform-skeleton` | **Date**: 2026-10-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-platform-skeleton/spec.md`

## Summary

Reshape the generated Kotlin Multiplatform starter into the explicitly agreed
project foundation: native Jetpack Compose and SwiftUI entry points, independent
shared KMP logic modules, an inward-only module dependency graph, convention
plugins, and reproducible CI checks. Keep the existing Gradle wrapper, version
catalog, Android app, and Xcode-to-Gradle framework integration as the migration
starting point. Add architecture and coverage enforcement, an ADR template,
and an AI change log only after documenting any new build/test dependencies.
Do not implement nutrition product behavior.

## Technical Context

**Language/Version**: Kotlin Multiplatform and Swift; use the repository's
Gradle wrapper and Java 21 toolchain, then verify the pinned Kotlin, Android
Gradle Plugin, and Compose versions against current stable compatibility data.

**Primary Dependencies**: Existing Gradle version catalog and KMP plugins.
Android UI uses Jetpack Compose; iOS UI uses SwiftUI. Core runtime dependencies
for later features (coroutines, serialization, Ktor, SQLDelight, Metro) are
deferred until their feature modules require them. Any new verification plugin
or test library requires an ADR before adoption.

**Storage**: None in this foundation. Persistence is deferred to the data-layer
feature.

**Testing**: Kotlin common/Android-host tests, iOS simulator tests, Android
Lint, ktlint for Kotlin, `swift format lint` for Swift, module-dependency
architecture checks, and Kover verification for JVM-testable shared/core code.
Kover does not collect Kotlin/Native test coverage; iOS tests validate
compilation and behavior separately.

**Target Platform**: Android (min API 24 per current version catalog) and iOS
simulator/device targets defined by the existing KMP project. Android and iOS
build checks are both required for every pull request; iOS runs on macOS.

**Project Type**: Kotlin Multiplatform mobile application with native platform
UIs and shared business logic.

**Performance Goals**: Required pull-request verification finishes within
10 minutes, with independent Linux and macOS jobs running in parallel. Record
clean and incremental build durations; establish artifact-size baselines before
setting budgets.

**Constraints**: No shared UI, food-log network transmission, tracking SDKs,
or product features in this step. `core:domain` has no project-module
dependencies; feature modules depend on domain; data implements domain
interfaces; `core:common` remains standalone and has no consumers until a later
specification authorizes them. Do not add a dependency before its ADR is
reviewed. Pull requests require passing checks and a recorded review; an
approval is not required.

**Scale/Scope**: One Android application, one iOS application, three core
modules, four feature-logic modules, one KMP framework facade, one included
Gradle build for convention plugins, and automated workflows. No user-facing
nutrition features.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Result | Evidence |
|---|---|---|
| Shared logic and deliberate platform boundaries | PASS | Shared Kotlin modules contain logic; Android Compose and iOS SwiftUI remain native UI layers. |
| Cross-platform compatibility | PASS | Pull requests build Android and iOS; common behavior has shared tests and iOS simulator validation. |
| Verify behavior with tests | PASS | CI runs host/common tests, iOS simulator tests, architecture checks, and applicable coverage verification. |
| Focused, maintainable changes | PASS | Scope is the platform skeleton only; product behavior and unused runtime dependencies are deferred. |
| Technology and source-set constraints | PASS | KMP modules retain Android and Apple targets; iOS Xcode framework integration is preserved. |
| Governance and dependency review | PASS WITH REQUIRED SEQUENCE | Create and review an ADR before adding each new third-party build/test dependency. |

## Project Structure

### Documentation (this feature)

```text
specs/001-platform-skeleton/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md                 # Created by /speckit-tasks
```

No external application API is introduced by this foundation, so no
`contracts/` directory is needed.

### Source Code (repository root)

```text
androidApp/
├── src/main/kotlin/         # Native Android Compose UI and navigation
└── build.gradle.kts
build-logic/
├── build.gradle.kts         # Included build for convention plugins
└── src/main/kotlin/         # Shared module, Android app, and test conventions
core/
├── common/                  # Standalone shared abstractions; no consumers yet
├── data/                    # Repository implementations; implements domain
└── domain/                  # Pure shared model and interfaces; no project deps
feature/
├── diary/                   # Shared state/intents/reducer logic
├── scan/
├── search/
└── summary/
shared/                      # KMP framework facade exported to the iOS app
iosApp/
├── iosApp/                  # Native SwiftUI application and screens
└── iosApp.xcodeproj/        # Embeds the KMP framework through Gradle
gradle/
└── libs.versions.toml       # Central plugin and dependency versions
specs/
└── 001-platform-skeleton/
```

**Structure Decision**: Use a Gradle multi-project build with the existing
`androidApp` and `iosApp` entry points, feature logic grouped under `feature/`,
core modules under `core/`, and an included `build-logic/` build for reusable
convention plugins. The four feature modules depend on `core:domain`; `core:data`
implements domain interfaces and is available to the app composition root;
`core:domain` and `core:common` have no project-module dependencies.
`core:common` has no downstream dependents in this step. Keep `:shared` as the
Apple framework facade and export the core and feature modules needed by iOS
through it. It is a platform interoperability boundary, not a domain layer.
The iOS app continues to embed this framework through its existing Xcode build
phase.

## Complexity Tracking

No constitution violations or unjustified architecture exceptions.
