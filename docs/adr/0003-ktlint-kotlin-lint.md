# ADR-0003: ktlint for Kotlin source linting

- **Status**: Accepted
- **Date**: 2026-10-19
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T005, T006; `specs/001-platform-skeleton/spec.md` FR-006

## Context and need

The project contains Kotlin shared logic, Android application code, and Gradle
Kotlin DSL. The foundation needs a repeatable Kotlin style/lint check in
pull-request CI in addition to Android Lint and Swift formatting.

## Decision

Propose ktlint as the Kotlin linter, integrated through a Gradle convention
plugin or pinned CLI so local and CI checks use the same version and
configuration. Pending explicit maintainer acceptance and compatibility
verification.

## Alternatives considered

- Android Lint only: does not lint all shared KMP sources or Gradle Kotlin DSL
  style.
- IDE-only formatting: not reproducible as a required pull-request check.
- Another Kotlin formatter/linter: evaluate if ktlint compatibility or
  maintenance does not meet the project requirements.

## Binary-size impact

ktlint is a build-time tool and must not be packaged into Android or iOS
runtime artifacts. No production artifact impact is expected; verify release
artifact reports after adoption.

## Build-time impact

Lint adds a source analysis task. Measure clean and incremental CI duration
before and after adoption; use Gradle caching and avoid duplicate executions
between platform jobs where possible.

## Licence

The ktlint repository is MIT licensed. The accepted Gradle integration version is 14.2.0 and the CLI version is
1.5.0; confirm the tool versions and transitive distribution during dependency
updates.

## Exit strategy

Remove the ktlint tool/plugin configuration, version-catalog entries, and CI
invocations. Keep Kotlin compilation and Android Lint; if replacing ktlint,
preserve a local command and required CI lint gate.

## Consequences

- Provides a reproducible Kotlin style check across modules.
- Requires a pinned version and configuration that remain compatible with
  Kotlin/Kotlin DSL sources.
- Adds build-time work but no application runtime dependency.

## Review record

- **Review outcome**: Accepted
- **Reviewed by**: Project maintainer
- **Review date**: 2026-10-19
- **Rationale or conditions**: Pin ktlint Gradle plugin 14.2.0 and CLI 1.5.0 for local and CI
  use. Confirm compatibility with the chosen Kotlin toolchain before configuring
  updates.
