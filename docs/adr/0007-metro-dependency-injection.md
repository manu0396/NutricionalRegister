# ADR-0007: Manual Composition for Product Search Dependencies

- **Status**: Accepted — Metro rejected
- **Date**: 2026-10-19
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T005, T011, T053; `specs/002-product-search/`

## Context and need

The Android application composition root must construct the Ktor client,
platform database driver, repositories, and shared search logic without placing
business logic in UI code. Metro 1.4.5 was proposed because the project brief
prefers Metro, with Koin as a possible fallback.

The official compatibility document checked on 2026-10-19 lists Kotlin 2.4.20
as tested with Metro 1.2.0 and newer, but does not list the repository's Kotlin
2.4.21. Its forward-compatibility statement is best-effort, not a guarantee.
The applicable Metro licence was also not confirmed from the default repository
license location.

## Decision

The maintainer rejected Metro for this feature and selected manual constructor
composition. Do not add Metro or Koin runtime/compiler dependencies.

## Alternatives considered

- Metro: rejected because compatibility with Kotlin 2.4.21 and the applicable
  licence were not established.
- Koin: not selected; this bounded graph does not require another runtime.
- Service locator: rejected because it obscures dependencies and weakens
  testability.

## Binary-size impact

Manual constructor composition adds no third-party production artifact.

## Build-time impact

Manual composition adds no compiler plugin or DI-specific build work.

## Licence

No DI dependency is adopted, so no third-party DI licence applies.

Compatibility evidence:
<https://github.com/ZacSweers/metro/blob/main/docs/compatibility.md>.

## Exit strategy

If the dependency graph becomes too large for straightforward constructor
wiring, introduce a DI library only through a new reviewed ADR. Replace the
application-root factory while preserving explicit constructor-based test
seams.

## Consequences

- The product uses an explicit, dependency-free composition root.
- No Metro/Koin plugin or runtime dependency is introduced.
- Compatibility evidence remains documented should Metro be reconsidered.

## Review record

- **Review outcome**: Metro rejected; manual constructor composition accepted
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-19
- **Rationale or conditions**: Avoid adopting an unverified compiler plugin.
  Revisit DI only if the graph grows enough to justify a new dependency and
  reviewed ADR.

## Validation record

- **Final licence/dependency check (2026-10-19)**: `gradle/libs.versions.toml`
  contains no Metro or Koin entry and no dependency-injection plugin or
  runtime is applied, so no third-party DI licence applies.
- **Binary size and build time**: manual composition adds no artifact or
  compiler-plugin work; the feature-level measurements recorded in ADR-0004
  through ADR-0006 apply to this decision unchanged.
