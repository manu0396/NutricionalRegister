# ADR-0015: Compose Multiplatform Navigation for the Bottom Navigation Bar

- **Status**: Accepted
- **Date**: 2026-10-20
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: `specs/004-saved-products-navbar/`; [contracts/navigation.md](../../specs/004-saved-products-navbar/contracts/navigation.md)

## Context and need

Feature 004 introduces a persistent bottom navigation bar with three destinations:
Search, Saved products, and Diary (spec FR-005, FR-006). The bar must:

- always indicate the current destination and switch between destinations (FR-005);
- let a detail view be opened from within a destination and, on return, land back
  on the destination that opened it — i.e. model a real back stack (FR-007);
- sit inside the app shell so it never obscures content or the system navigation
  area, including gesture navigation (FR-008);
- keep each destination's navigation state coherent on switch.

The project has no navigation dependency today. `androidApp` currently hosts a
single screen (`App.kt` → `ProductSearchScreen`). A decision is required on how
destinations are routed.

The constitution forbids **shared UI**: the bar is Android UI in this feature
(native SwiftUI navigation on iOS is a later step, consistent with the existing
Android-leads-iOS precedent). The dependency must therefore not force shared UI,
and it must not disturb the shared module or the iOS build.

## Decision

**Adopt `org.jetbrains.androidx.navigation:navigation-compose` version 2.9.2,
scoped to the Android UI layer (`androidApp`).**

- **Version rationale (verified 2026-10-20).** On Maven Central the JetBrains
  Compose Multiplatform navigation artifact has stable **2.9.2**; the 2.10.x line
  is currently alpha/beta/rc only. **2.9.2** is the newest stable release and sits
  on the same JetBrains Compose Multiplatform line the project already uses
  (Compose Multiplatform 1.12.2), so it does not introduce a second Compose
  version line — the exact problem ADR-0011 avoided for Material icons.
- **Scope.** The dependency is declared only where the Android UI lives. It is
  **not** added to `core:domain`, `core:data`, or the shared module, and it does
  not affect the iOS build or the `Shared.xcframework`.
- **Project-owned bar.** The bottom bar composable is written by the project
  (`androidApp/src/main/kotlin/com/example/nutricionalregister/ui/NutriLogBottomBar.kt`)
  rather than taken from a library, so the navigation dependency is confined to a
  single call site and is trivially swappable.
- **Destination model is platform-neutral.** Destination identities (`search`,
  `saved`, `diary`) are plain data, so a future native SwiftUI navigation on iOS
  can be built from the same model without depending on this library.

## Alternatives considered

- **AndroidX `navigation-compose` 2.10.2.** Rejected: it exists and is stable, but
  it is a plain AndroidX artifact on a different Compose version line than the
  JetBrains Compose Multiplatform artifacts the project uses; mixing the two
  reintroduces the multi-version-line problem ADR-0011 resolved for icons.
- **Hand-rolled tab state (`when (currentTab)`).** Rejected: a simple switch cannot
  express FR-007's detail→back-to-origin with a correct back stack, loses per-tab
  state, and would have to be re-implemented per platform. It would also make
  FR-008's inset handling a manual concern at every call site.
- **A third-party navigation library (e.g. Voyager, Decompose).** Rejected: both
  are additional non-Google dependencies that would need their own justification,
  and neither is required for a three-destination bar; the JetBrains artifact is
  the path already aligned with the project's Compose stack.
- **Shared Compose UI navigation for both platforms now.** Rejected: it violates
  the constitution's no-shared-UI principle and the established Android-leads-iOS
  precedent (spec Assumptions, ADR-0013).

## Binary-size impact

The artifact pulls `navigation-common`/`navigation-runtime` Compose code; it
contains no native libraries and is R8-shrinkable. The Android APK change is
measured after integration and recorded alongside the existing debug/release sizes
(003 recorded a release APK of 11,463,514 B). The iOS `Shared.xcframework` is
**unaffected by construction** because the dependency is Android-UI-only.

## Build-time impact

One additional Android-only artifact on the `androidApp` classpath, resolved from
Maven Central (`org.jetbrains.androidx.navigation`). No repository or
`settings.gradle.kts` change is needed (Maven Central is already configured). The
shared and iOS builds are untouched. Clean and incremental durations are measured
after integration; hosted PR CI must stay within the project's 10-minute target.

## Licence

Published by JetBrains under the **Apache License 2.0**, compatible with this
public repository. No copyleft; no attribution obligation on the UI; no licence
fee.

Source: <https://github.com/JetBrains/compose-multiplatform>

## Exit strategy

Because routing is reached through a single project-owned bar composable and the
destination model is plain data, replacing the library (with AndroidX Navigation,
Voyager, or hand-rolled routing) touches only the `androidApp` shell. No shared
contract, repository, or persisted schema references the library, so no migration
is required.

## Consequences

- The bottom bar gets correct back-stack behaviour (FR-007) and inset handling
  (FR-008) without bespoke code.
- The Android UI gains one small Apache-2.0 dependency; the shared module and the
  iOS build are unaffected.
- The project owns future upgrades of this artifact, bounded to the UI layer.
- Adding iOS navigation later is a separate, independent decision; this ADR
  deliberately does not pre-commit to a shared-UI navigation approach.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-20
- **Rationale or conditions**: Approved JetBrains `navigation-compose` 2.9.2
  scoped to the Android UI layer. Must remain Android-UI-only, must not enter the
  shared module, and must not change the iOS build or `Shared.xcframework`.

## Validation record

- **Version (verified 2026-10-20)**: `org.jetbrains.androidx.navigation:navigation-compose`
  2.9.2 present and stable on Maven Central; 2.10.x is pre-release.
- **Post-implementation validation (2026-10-20)**: the navigation dependency
  (`org.jetbrains.androidx.navigation:navigation-compose` 2.9.2) compiles against
  Kotlin 2.4.21 / Compose Multiplatform 1.12.2 and is scoped to the Android UI
  layer only (`androidApp`), so it does not enter the shared module or the iOS
  build. The bottom-bar instrumented tests pass on emulator-5556 (see
  `androidApp/src/androidTest/.../navigation/`), covering the three destinations,
  the current-destination indication, back-to-origin, the Diary placeholder, and
  accessibility at the largest text scale. The dark release APK measures
  12,520,644 bytes (11,365,002 bytes before navigation + billing, a combined delta
  of +1,155,642 bytes with ADR-0014); the `Shared.xcframework` is unchanged by this
  dependency.
