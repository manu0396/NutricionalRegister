<!--
Sync Impact Report
Version change: 1.1.0 -> 1.2.0
Modified principles: none
Added sections: Principle V. Plain-Text Documentation and Code (no emojis)
Modified sections: none
Removed sections: none
Follow-up TODOs: remove existing emoji glyphs from documentation and code
  authored before this amendment
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

### V. Plain-Text Documentation and Code

Documentation, specifications, commit messages, code comments, and source files
MUST be plain text. Do NOT use emoji glyphs or decorative pictographic
characters in repository files. This keeps the repository readable in terminals,
diffs, code review, and accessibility tooling that do not render emoji
consistently. Express emphasis with Markdown (bold, headings, lists) instead of
pictographs. Typographic punctuation already used across the project (for
example the arrow character in explanatory prose) is out of scope for this rule.
Automated checks MAY enforce this; if one exists, its violations are treated as
build failures.

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

**Version**: 1.2.0 | **Ratified**: 2026-10-19 | **Last Amended**: 2026-10-20
