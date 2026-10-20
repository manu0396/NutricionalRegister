# ADR-0004: Kotlin Coroutines and Serialization for Product Search

- **Status**: Accepted
- **Date**: 2026-10-19
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T002, T008, T053; `specs/002-product-search/`

## Context and need

Product Search needs cancellable shared asynchronous work for cached-first
search, background refresh, latest-query-wins behavior, and observable state.
It also needs a Kotlin Multiplatform JSON parser for Search-a-licious responses.
Time and dispatchers must remain injectable so behavior tests do not depend on
the real clock or scheduling.

## Decision

Propose `kotlinx.coroutines` 1.11.0 and `kotlinx.serialization` 1.11.0 for
shared asynchronous behavior and typed JSON decoding. Align the serialization
compiler plugin with the repository's Kotlin compiler version; the runtime
library version is independent. Keep public API types explicit and tolerate
missing or malformed optional product fields without fabricating nutrient
values.

No dependency or version-catalog entry is approved by this proposal. Verify
both versions against Kotlin 2.4.21, Gradle 9.5.1, and the project's Android
and Apple targets before adoption.

## Alternatives considered

- Kotlin standard-library callbacks and hand-written JSON parsing: rejected
  because they do not provide the required shared cancellation/Flow patterns
  and create unnecessary parser maintenance.
- Platform-specific async and JSON libraries: rejected because they duplicate
  behavior and complicate equivalent Android/iOS results.
- A different serialization library: reconsider if official compatibility
  checks fail or tolerant per-product decoding cannot be implemented safely.

## Binary-size impact

Both are runtime dependencies when used by production code and can affect the
Android APK and iOS framework. No project-specific size delta is measured.
After approval, record release artifact sizes before and after dependency
integration using the existing metrics workflow; attribute the delta cautiously
because dependency adoption may coincide with feature code.

## Build-time impact

The serialization compiler plugin adds compilation work and code generation;
coroutines add runtime classes but no compiler plugin. Measure clean and
incremental builds before and after adoption and report the full hosted PR
duration against the 10-minute project target.

## Licence

The versioned Kotlin repositories publish Apache License 2.0:

- Coroutines 1.11.0: <https://github.com/Kotlin/kotlinx.coroutines/blob/1.11.0/LICENSE.txt>
- Serialization 1.11.0: <https://github.com/Kotlin/kotlinx.serialization/blob/v1.11.0/LICENSE.txt>

Reconfirm selected artifact metadata and transitive dependency licences when
the versions are accepted.

## Exit strategy

Replace coroutine-based repository and presentation boundaries with another
approved shared asynchronous abstraction, remove coroutine runtime references,
and replace serialization annotations/generated serializers with a reviewed
parser. Preserve cancellation, deterministic injected dispatchers, and
per-product malformed-data tolerance in tests.

## Consequences

- Shared search orchestration can use cancellable coroutine and Flow APIs.
- Typed serializers can keep remote decoding in common code.
- Compiler compatibility and additional production artifact size require
  verification.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-19
- **Rationale or conditions**: Approved the proposed coroutine and
  serialization dependencies. Verify selected artifact metadata and
  transitive licences, and record measured size/build impacts as planned.

## Validation record

- **Final licences (verified 2026-10-19)**: the published POM metadata for
  the resolved `org.jetbrains.kotlinx` artifacts at these versions
  (`kotlinx-coroutines-core`, `kotlinx-coroutines-test`,
  `kotlinx-serialization-json`, `kotlinx-serialization-core`, and their
  platform targets) declares `Apache-2.0`.
- **Compatibility**: both versions compile and pass the module tests under
  the repository toolchain (Kotlin 2.4.21, Gradle 9.5.1).
- **Measured sizes (2026-10-19, with all approved dependencies and feature
  code integrated)**: unsigned Android release APK 11,035,730 bytes; Android
  debug APK 15,265,427 bytes; `Shared.xcframework` approximately 92,000 KB on
  disk. These are feature-level totals, not per-dependency deltas, so this
  pair's individual impact cannot be isolated from the concurrent feature
  code.
- **Build-time deltas (2026-10-19)**: the full verification run (module unit
  tests, architecture tests, `coverageVerify`, `kotlinLint`, `assembleDebug`,
  and Android lint) exited 0; the warm-build-cache clean verification took
  6 s and the incremental verification took 1 s. Hosted pull-request duration
  remains pending task T056.
