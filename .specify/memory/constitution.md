<!--
Sync Impact Report
Version change: 1.0.0 -> 1.1.0
Modified principles: none
Added sections: none
Modified sections: Platform and Technology Constraints; clarified native UI boundary
Removed sections: none
Follow-up TODOs: none; project brief is authoritative for platform UI choices
-->

# NutricionalRegister Constitution

## Core Principles

### I. Shared Logic, Deliberate Platform Boundaries
Implement behavior that is common to Android and iOS in the Kotlin Multiplatform
shared source sets. Keep platform-specific integrations in their matching source
sets and preserve native UI entry points. This keeps shared behavior consistent
without hiding platform requirements.

### II. Cross-Platform Compatibility
Changes to shared behavior MUST be evaluated for both Android and iOS. Platform-
specific implementations MUST provide equivalent behavior where the feature is
shared, or document an intentional platform difference in its specification.

### III. Verify Behavior with Tests
New or changed business behavior MUST have automated tests at the narrowest
appropriate level. Shared behavior belongs in common tests where possible;
platform-specific behavior belongs in the relevant platform test source set.
Run the applicable project test tasks and report any target that could not be
verified.

### IV. Focused, Maintainable Changes
Implement only behavior required by the approved specification. Prefer existing
project patterns and dependencies; justify new dependencies and additional
abstractions in the plan. Keep code understandable and avoid unrelated changes.

## Platform and Technology Constraints

The application targets Android and iOS. Kotlin Multiplatform is used for shared
business logic, data, and networking, not for shared UI. Android UI uses
Jetpack Compose; iOS UI uses SwiftUI. Android-specific code belongs in Android
source sets; iOS-specific code belongs in iOS source sets and the existing
SwiftUI application entry point. Do not remove or change supported targets
without an approved specification.

## Development Workflow

Specifications define user-visible behavior and compatibility requirements
before planning implementation. Plans and tasks MUST align with the existing
source-set structure and project dependencies. Before completion, run relevant
tests; use `./gradlew :shared:testAndroidHostTest` for shared Android-host tests
and `./gradlew :shared:iosSimulatorArm64Test` for iOS simulator tests when the
environment supports those targets.

## Governance

This constitution guides specifications, plans, implementation, and reviews.
When a requested change conflicts with a principle, resolve the conflict in the
specification or amend the constitution before implementation. Amendments MUST
be reviewed with the project changes, use semantic versioning (MAJOR for
incompatible governance changes, MINOR for new or materially expanded
principles, PATCH for clarifications), and update the amendment date. Reviews
MUST check compliance with the principles and record justified exceptions in
the relevant specification.

**Version**: 1.1.0 | **Ratified**: 2026-10-19 | **Last Amended**: 2026-10-19
