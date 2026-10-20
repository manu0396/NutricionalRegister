# ADR-0005: Ktor Client for Product Search

- **Status**: Accepted
- **Date**: 2026-10-19
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T003, T009, T053; `specs/002-product-search/`

## Context and need

Product Search needs a shared HTTP client for the Search-a-licious full-text
endpoint, explicit request construction, status/error handling, and deterministic
fixtures. Platform engines are required for Android and iOS. Automated tests
must not make live network requests.

## Decision

Propose Ktor Client 3.6.0 with only the modules required for core HTTP,
serialization/content negotiation, OkHttp on Android, Darwin on iOS, and
MockEngine in tests. Keep the service origin and request policy behind the
data-layer client boundary. Do not send requests until Search-a-licious usage
policy is verified in T001.

No dependency or version-catalog entry is approved by this proposal. Verify
Ktor 3.6.0 and the selected engines against Kotlin 2.4.21 and the project's
Android/iOS targets before adoption.

## Alternatives considered

- Platform-native URL/HTTP clients: rejected because they duplicate request
  logic and make shared tests and error mapping harder.
- Another multiplatform HTTP client: viable, but Ktor is named by the project
  brief and provides a documented MockEngine for deterministic tests.
- Direct calls from Compose or shared UI: rejected because UI must not own
  networking or product-domain behavior.

## Binary-size impact

Ktor core and platform engines are production dependencies and will affect
Android/iOS artifacts. The exact delta depends on selected modules and
transitive dependencies; no project-specific measurement is available.
Measure release APK and XCFramework sizes before and after adoption and keep
the selection minimal.

## Build-time impact

The client adds dependency resolution and compilation work. MockEngine tests
add test compilation/execution work but no production artifact content.
Measure clean/incremental Gradle duration and full hosted PR duration after
integration; the latter must be compared with the 10-minute target.

## Licence

Ktor 3.6.0's versioned repository license is Apache License 2.0:
<https://github.com/ktorio/ktor/blob/3.6.0/LICENSE>.
Confirm the licences of OkHttp, Darwin-related transitive artifacts, and all
selected modules from their published metadata before adoption.

## Exit strategy

Replace the Ktor adapter behind the shared catalogue interface with another
approved HTTP implementation. Remove Ktor modules and engine factories while
retaining request-contract tests, cancellation, timeout/error semantics, and
offline behavior.

## Consequences

- Shared request and response handling can be exercised using MockEngine.
- Android/iOS engines remain isolated in platform source sets.
- Engine and serialization module selection affects artifact size and needs
  verification.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-19
- **Rationale or conditions**: Approved the proposed Ktor client and engines.
  Verify platform resolution and transitive licences. Live production requests
  remain blocked until Search-a-licious policy and response shape are
  confirmed.

## Validation record

- **Final licences (verified 2026-10-19)**: the published POM metadata for
  the resolved `io.ktor` 3.6.0 artifacts used here (`ktor-client-core`,
  `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`,
  `ktor-client-okhttp`, `ktor-client-darwin`, and test-only
  `ktor-client-mock`) declares Apache License 2.0, and the transitive
  `com.squareup.okhttp3:okhttp` POM declares Apache License 2.0.
- **Compatibility**: the client and both platform engines compile and pass
  the module tests under the repository toolchain (Kotlin 2.4.21,
  Gradle 9.5.1).
- **Measured sizes (2026-10-19, with all approved dependencies and feature
  code integrated)**: unsigned Android release APK 11,035,730 bytes; Android
  debug APK 15,265,427 bytes; `Shared.xcframework` approximately 92,000 KB on
  disk. These are feature-level totals, not per-dependency deltas; the
  engine/serialization selection cannot be measured in isolation from the
  feature code added alongside it.
- **Build-time deltas (2026-10-19)**: the full verification run (module unit
  tests, architecture tests, `coverageVerify`, `kotlinLint`, `assembleDebug`,
  and Android lint) exited 0; the warm-build-cache clean verification took
  6 s and the incremental verification took 1 s. Hosted pull-request duration
  remains pending task T056.
