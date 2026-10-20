# ADR-0008: Compose UI Test Dependencies for Product Search

- **Status**: Accepted
- **Date**: 2026-10-19
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T006, T012, T053; `specs/002-product-search/`

## Context and need

Product Search must expose accessible names, 48 dp minimum touch targets, and
usable controls at enlarged system text. Shared logic tests do not verify
Compose semantics or Android UI behavior, so deterministic instrumented tests
are needed for search, results, detail, error, and recent-search interactions.

## Decision

Propose the AndroidX artifacts
`androidx.compose.ui:ui-test-junit4-android:1.12.1` and
`androidx.compose.ui:ui-test-manifest:1.12.1` for Android instrumented tests.
The official Compose Multiplatform UI testing guide uses these exact
coordinates and versions for instrumented tests and demonstrates Compose
Multiplatform 1.12.2 in its common-test example. Confirm resolution alongside
the repository's AndroidX Compose UI transitive versions with a minimal Gradle
dependency/build check before adding catalog aliases. Keep test artifacts in
Android test configurations and UI tests out of release runtime dependencies.

No dependency entries are approved by this proposal.

## Alternatives considered

- Manual device-only smoke tests: useful as a supplement but do not provide
  repeatable semantics/accessibility regression checks in CI.
- Screenshot-only tests: do not establish semantics labels, actions, or touch
  target constraints.
- Robolectric-only UI tests: may not exercise the same Android Compose
  semantics behavior as instrumented tests; reconsider if emulator CI proves
  unreliable and maintainers approve an alternative.

## Binary-size impact

The test runner and test artifacts must not enter release runtime
configurations. They can increase debug/test APK and test dependency size.
Verify release dependency reports remain free of test artifacts and record
debug/test build size only if required for CI diagnostics.

## Build-time impact

Instrumented tests require Android test compilation and emulator execution.
The exact hosted CI cost is unknown; measure the full PR duration after
integration against the 10-minute target and use existing Android CI where
possible.

## Licence

AndroidX Compose artifacts are published under the Apache License 2.0 by
AndroidX. Confirm the exact selected versions' Maven metadata and transitive
licences when the dependencies are proposed for adoption.

Official testing guide and artifact coordinates:
<https://kotlinlang.org/docs/multiplatform/compose-test.html>.

Official release source:
<https://developer.android.com/jetpack/androidx/releases/compose-ui>.

## Exit strategy

Remove the Compose UI test dependencies and instrumented test source set usage.
Retain shared presentation tests and add an explicitly reviewed replacement
Android UI verification approach that still checks accessibility semantics
and touch targets.

## Consequences

- Android UI behavior and accessibility semantics can be regression-tested.
- Instrumented test runtime increases CI work and must remain isolated from
  release dependencies.
- Compose artifact versions require alignment with the current KMP/Android
  toolchain.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-19
- **Rationale or conditions**: Approved the documented Android UI test
  artifacts. Confirm Gradle resolution against Compose Multiplatform 1.12.2
  and review Android test CI duration during integration.

## Validation record

- **Final licences (verified 2026-10-19)**: the published POM metadata for
  `androidx.compose.ui:ui-test-junit4-android:1.12.1` and
  `androidx.compose.ui:ui-test-manifest:1.12.1` declares Apache License 2.0.
- **Resolution**: both artifacts resolved at 1.12.1 alongside Compose
  Multiplatform 1.12.2 in the integrated build.
- **Measured sizes (2026-10-19)**: the instrumented test run produced an
  androidTest APK of 1,127,639 bytes; the Android debug APK was 15,265,427
  bytes and the unsigned release APK was 11,035,730 bytes.
- **Validation run (2026-10-19)**: 7 of 7 instrumented Compose UI tests
  passed with 0 failures on the Medium_Phone emulator (API 16). Hosted CI
  duration for the test tasks remains pending task T056.
