# ADR-0011: Compose Material Icons for Android UI Controls

- **Status**: Accepted
- **Date**: 2026-10-20
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T092-T095; `specs/003-ui-refresh/`

## Context and need

The 003 UI refresh introduced several icon-bearing controls: the product-detail
back affordance, the recent-search delete action, and the recent-search leading
glyph. The app could not render real icons because no Material icons artifact was
on the classpath — verified by compiling a probe file that imported
`androidx.compose.material.icons.Icons`, which failed with
`Unresolved reference 'icons'`.

The controls therefore rendered **typographic characters** as icon substitutes:

| Control | Current glyph | Intended icon |
|---|---|---|
| Product-detail back | `←` (U+2190) | `Icons.AutoMirrored.Filled.ArrowBack` |
| Recent-search delete | `✕` (U+2715) | `Icons.Filled.Close` |
| Recent-search leading glyph | `⌕` (U+2315) | `Icons.Filled.Search` |

This is a cosmetic-but-real gap. Text glyphs are not designed iconography: they
vary by platform font, do not mirror in right-to-left layouts, have inconsistent
optical weight against Material text styles, and their glyph metrics do not match
the 24 dp Material icon box, which makes padding and touch-target alignment
approximate rather than exact.

The refresh's own goal is a coherent, professional visual language, so real icons
are in scope. This is a new production dependency, and this project requires every
production dependency to be recorded in its own ADR before a version-catalog entry
is added (the process established by ADRs 0001-0010). This is that ADR.

## Decision

**Adopt `androidx.compose.material:material-icons-core` at a pinned catalog version
(1.7.8), as an `androidApp`-only implementation dependency.**

Two points drove the choice of coordinates and artifact:

1. **AndroidX coordinates, matching the resolved Compose line.** This project
   consumes Compose through the Compose Multiplatform artifacts
   (`org.jetbrains.compose.runtime`, `org.jetbrains.compose.foundation`,
   `org.jetbrains.compose.material3`, …), but on Android those JetBrains
   artifacts **map onto AndroidX Compose** — Compose Multiplatform 1.12.2 resolves
   `androidx.compose.runtime` to **1.12.1** in the Android runtime classpath. The
   icon classes are imported as `androidx.compose.material.icons.*`, so the
   AndroidX artifact is the natural coordinate and shares the resolved Compose
   graph rather than introducing an independently-versioned line.

2. **Core, not extended.** The three icons needed (`ArrowBack`, `Close`,
   `Search`) all live in `material-icons-core`. The `material-icons-extended`
   artifact carries thousands of icons at a substantial size cost and is not
   required.

> **Coordinate note (verified 2026-10-20).** The JetBrains mirror of this
> artifact (`org.jetbrains.compose.material:material-icons-core`) was considered
> first for consistency with the other `org.jetbrains.compose.*` entries, but it
> is **no longer published at the pinned Compose line**: resolution fails with
> `Could not find org.jetbrains.compose.material:material-icons-core:1.12.2`
> (its last published release is 1.7.3; the Maven Central POM returns 404 for
> 1.12.2). The AndroidX artifact, by contrast, resolves and compiles cleanly.
> Because the icons are Android-only and the AndroidX artifact joins the existing
> resolved Compose graph, AndroidX coordinates are the correct and only working
> choice.

Scope is restricted to `androidApp`: it is the only module that renders UI. No
common source set, no shared module, and no iOS target changes, so the
XCFramework is unaffected.

### Version catalog and build changes (applied under T093)

`gradle/libs.versions.toml` — add the version and the library:

```toml
[versions]
androidx-materialIcons = "1.7.8"

[libraries]
androidx-material-icons-core = { module = "androidx.compose.material:material-icons-core", version.ref = "androidx-materialIcons" }
```

`androidApp/build.gradle.kts` — add to `dependencies { }`:

```kotlin
implementation(libs.androidx.material.icons.core)
```

A dedicated `[versions]` entry is required (unlike the originally-planned
`composeMultiplatform` reference), because the AndroidX icon artifact is frozen
at the `1.7.x` line and does not track the JetBrains Compose Multiplatform
version.

## Alternatives considered

- **Keep the text glyphs (no dependency).** Viable and fully functional — the
  current code works, is accessible, and passes every test. Rejected because the
  glyphs do not mirror in RTL, do not align to the Material 24 dp icon box, and
  render inconsistently across platform fonts, which conflicts with the
  professional-consistency goal that motivated the refresh.
- **Hand-draw the three icons as `ImageVector`s in `androidApp`.** Rejected: it
  avoids a dependency but means hand-maintaining path data and forfeits the
  standard icon set, including the AutoMirrored RTL variant of the back arrow.
  Three icons today would likely grow, and each would need hand-drawn paths.
- **`material-icons-extended`.** Rejected: carries thousands of unused icons for a
  large binary-size cost when only three core icons are needed.
- **JetBrains `org.jetbrains.compose.material:material-icons-*`.** Attempted first
  for coordinate consistency with the other `org.jetbrains.compose.*` entries, but
  **rejected because it no longer resolves** at the pinned Compose line (last
  published at 1.7.3; 404 for 1.12.2). The AndroidX artifact resolves and
  compiles, and shares the Android Compose graph the JetBrains artifacts already
  map onto.

## Binary-size impact

`material-icons-core` contains only the small built-in icon set and is a code-only
Kotlin/Compose artifact (no native libraries). Its contribution to the APK is
limited to the icons actually referenced, which R8 shrinks to the three used.

Measured for the validation record below. The iOS `Shared.xcframework` is
**unaffected by construction**, because the dependency is declared only in
`androidApp`, which is not part of the XCFramework.

## Build-time impact

An additional Compose artifact on the `androidApp` compile and runtime classpath.
It is a small, already-cached artifact on the same Compose release line, so no new
repository or toolchain is introduced. Clean and incremental build durations are
measured for the validation record below; the hosted PR pipeline must remain under
the project's 10-minute target.

## Licence

The AndroidX Compose icon artifacts are published by the Android Open Source
Project under the **Apache License 2.0**, matching the rest of the Compose stack
already used by this project. The icon path data derives from Google's Material
Icons, also Apache-2.0. Apache-2.0 is compatible with this public repository; it
imposes no copyleft and requires only licence and notice preservation, which is
satisfied by the dependency metadata.

Authoritative sources:

- AndroidX Compose Material: <https://developer.android.com/jetpack/androidx/releases/compose-material>
- Material Icons: <https://fonts.google.com/icons>
- Apache License 2.0: <https://www.apache.org/licenses/LICENSE-2.0>

Unlike ADR-0009 (Open Food Facts images, CC BY-SA), this dependency introduces
**no attribution obligation on the UI** — no user-visible credit is required.

## Exit strategy

The dependency is used from exactly three call sites inside `androidApp`. To
remove it: delete the three `Icon(...)` usages and restore the previous
`Text`-based glyphs (or substitute hand-drawn `ImageVector`s), then delete the
catalog entry and the `implementation` line. No shared, generated, or persisted
artifact depends on it, and no migration is required.

To keep this cheap, the icons are referenced only where needed and are not wrapped
in project-owned abstractions that would have to be rewritten.

## Consequences

- Back, delete, and search affordances render as designed vector icons, aligned to
  the Material 24 dp icon box and consistent across platform fonts.
- The back arrow uses the **AutoMirrored** variant, so it flips correctly in
  right-to-left locales.
- `androidApp` gains one small Apache-2.0 Compose dependency on the already-pinned
  Compose line; no second Compose version line is introduced.
- No UI attribution obligation is created, and no iOS or shared-module impact
  follows.
- Accessibility is preserved: each icon keeps its existing `contentDescription`
  (or stays decorative where an enclosing node already describes the control), and
  the containing controls keep their 48 dp minimum touch targets.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-20
- **Rationale or conditions**: Approved adding the core icon artifact on the
  pinned Compose Multiplatform version, scoped to `androidApp`, to replace
  typographic glyphs with designed vector icons. Must remain
  `material-icons-core` (not `-extended`), must not change any shared or iOS
  module, and must preserve existing accessibility semantics and touch targets.

## Validation record

- **Artifact resolution (verified 2026-10-20)**: the JetBrains coordinate
  `org.jetbrains.compose.material:material-icons-core:1.12.2` **failed** to
  resolve (`Could not find ...:1.12.2`; its last published release is 1.7.3).
  Switching to `androidx.compose.material:material-icons-core:1.7.8` resolved and
  compiled cleanly.
- **Removal of the trigger issue**: the earlier probe compiled a file importing
  `androidx.compose.material.icons.Icons` and failed with
  `Unresolved reference 'icons'`. After this change the same import compiles.
- **Compatibility (verified 2026-10-20)**: the AndroidX icon artifact joins the
  existing resolved Compose graph (Compose Multiplatform 1.12.2 maps
  `androidx.compose.runtime` to 1.12.1). Its own runtime reference to
  `androidx.compose.ui:ui:1.6.0` is upgraded to the already-resolved 1.12.1; no
  conflict and no second Compose line is introduced.
- **Measured sizes (2026-10-20, after integration)**:
  - debug APK: **15,857,238 B** (was 15,725,958 B; **+131,280 B**)
  - release APK: **11,463,514 B** (was 11,381,386 B; **+82,128 B**)
  - `Shared.xcframework` (release): **92,024 KB** — **unchanged**, confirming the
    `androidApp`-only scope and zero iOS impact.
- **Validation run (2026-10-20, exit 0)**: full clean verification (unit tests,
  architecture tests, `coverageVerify`, `kotlinLint`, `assembleDebug`, lint),
  release APK, XCFramework; **20/20** instrumented Compose tests on
  `emulator-5556`; `xcodebuild` **BUILD SUCCEEDED** with 0 errors.
