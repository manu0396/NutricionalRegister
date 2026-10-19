# ADR-0002: Konsist for Kotlin source architecture tests

- **Status**: Accepted
- **Date**: 2026-10-19
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T004, T006; `specs/001-platform-skeleton/spec.md` FR-005

## Context and need

The project must make source-layer boundaries executable and reviewable.
`core:domain` must depend on no source layer; data and feature source rules must
be checked in tests. A separate Gradle dependency-policy check will validate
project-module dependency edges because source architecture tests alone do
not prove the Gradle graph.

## Decision

Propose Konsist as the Kotlin source architecture test library, run from a
dedicated `architecture-tests` module. Define rules for source/package
boundaries there; enforce Gradle project dependencies separately through the
build-logic policy. Pending explicit maintainer acceptance and compatibility
verification.

## Alternatives considered

- Hand-written source parser: creates parser-maintenance work and risks
  incomplete Kotlin syntax handling.
- Gradle dependency allowlist alone: checks module edges but not source-level
  package/layer dependencies.
- No architecture test: fails the requirement that boundary violations fail
  automated checks.

## Binary-size impact

Konsist is test-only and must not enter the application runtime dependency
graph. No production artifact impact is expected; verify Android APK and iOS
framework sizes after adoption.

## Build-time impact

Architecture tests add a test dependency and source scan to verification. The
cost is not measured yet; record baseline and post-adoption clean/incremental
workflow duration and keep the checks in the Linux job unless target
compatibility requires otherwise.

## Licence

Version 0.18.1 is published as `com.lemonappdev:konsist` with Apache License
2.0 declared in its Maven Central POM. Reconfirm the selected release's
published metadata during dependency updates.

## Exit strategy

Remove the Konsist test dependency and its architecture-test suite, keeping the
independent Gradle module-edge policy. If replacing the library, preserve the
same architecture rules and negative-violation verification in the replacement
tests.

## Consequences

- Kotlin source architecture rules can be expressed as executable tests.
- Build-module edge enforcement remains a distinct check.
- Adds test-only dependency and test execution time; the exact version and
  licence require confirmation before adoption.

## Review record

- **Review outcome**: Accepted
- **Reviewed by**: Project maintainer
- **Review date**: 2026-10-19
- **Rationale or conditions**: Use Konsist 0.18.1 for source/package rules in addition to, not
  instead of, a Gradle project-dependency allowlist. Confirm the selected
  release's published licence and KMP/Gradle compatibility.
