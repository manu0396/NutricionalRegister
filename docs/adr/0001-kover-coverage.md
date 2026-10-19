# ADR-0001: Kover for JVM-measurable shared-core coverage

- **Status**: Accepted
- **Date**: 2026-10-19
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T003, T006; `specs/001-platform-skeleton/spec.md` FR-008

## Context and need

The platform foundation requires at least 80% line coverage for `core:domain`
and `core:data` when they contain production code. The project uses Kotlin
Multiplatform and must distinguish measured JVM/Android-host coverage from
Kotlin/Native iOS tests.

## Decision

Propose Kover Gradle Plugin to generate and verify coverage for shared/core code
executed by JVM/Android-host tests. The threshold applies separately to
`core:domain` and `core:data`, and only when a module has production code. Do
not describe JVM coverage as iOS/native coverage. Pending explicit maintainer
acceptance and compatibility verification.

## Alternatives considered

- JaCoCo with custom KMP aggregation: more configuration and not a direct
  substitute for Kotlin/Native coverage.
- No automated coverage tool: does not enforce FR-008.
- Claim coverage across JVM and native targets from one report: unsupported and
  misleading.

## Binary-size impact

Kover instruments test execution and generates reports; it is a build/test
plugin, not an application runtime dependency. No production artifact impact is
expected. Measure Android release APK and iOS framework before and after
adoption to verify this assumption.

## Build-time impact

Coverage adds instrumentation and report/verification work to the JVM test
path. The impact is unknown until measured on the repository's CI baseline.
Record clean and incremental durations before and after enabling it, and keep
coverage reporting in parallel CI work where possible.

## Licence

Kover is an official JetBrains project, distributed under Apache License 2.0
according to its repository metadata. The accepted plugin version is 0.9.11;
reconfirm its compatibility with the pinned Kotlin/Gradle/AGP matrix during
implementation.

## Exit strategy

Remove the Kover plugin alias/application and its verification configuration,
the `koverVerify` CI invocation, and coverage-report links. Preserve the test
suites and replace the metric only if an accepted ADR chooses another
measurement tool.

## Consequences

- Enforces the specified 80% threshold for code covered by Kover's supported
  JVM/Android-host execution.
- Kotlin/Native iOS test coverage remains reported separately and is not part of
  the threshold.
- Adds plugin compatibility and incremental-build cost that must be monitored.

## Review record

- **Review outcome**: Accepted
- **Reviewed by**: Project maintainer
- **Review date**: 2026-10-19
- **Rationale or conditions**: Use Kover 0.9.11 for JVM/Android-host coverage only. Do not
  claim coverage of Kotlin/Native iOS tests. Reconfirm compatibility when
  selecting the plugin version.
