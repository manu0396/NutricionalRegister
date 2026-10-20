# ADR-0010: Compose Image Loading for Android Product Images

- **Status**: Accepted
- **Date**: 2026-10-20
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T061, T066, T069, T070, T071, T073; `specs/003-ui-refresh/`

## Context and need

ADR-0009 decides *which* images are shown (Open Food Facts catalogue URLs,
CC BY-SA) and requires the app to fetch and cache image bytes itself, degrade to
a placeholder on failure, and never let image work block or delay search or
nutrition content. It explicitly defers *how* the bytes are fetched, decoded, and
cached, and requires that any image-loading library be recorded in its own ADR
before a version-catalog entry is added. This is that ADR.

The Android Compose UI (feature 003) must display:

- a small thumbnail per search-result card (`image_front_small_url`), and
- a larger header image on the product-detail view (`image_front_url`).

The mechanism must satisfy the project constraints:

- **Platform scope.** Images are only rendered by the native Android Compose UI
  (`androidApp`). The iOS UI is explicitly out of scope for 003. A multiplatform
  image loader therefore buys nothing here that the Android target does not.
- **Governance.** Every production dependency needs an ADR, an explicit licence
  record, and a version-catalog entry. Compose Multiplatform 1.12.2, Kotlin
  2.4.21, Android `minSdk 24` / `compileSdk 37`, and Gradle 9.5.1 are fixed.
- **Coverage / lint.** New runtime code is subject to the ≥80% Kover gate and
  ktlint; third-party runtime libraries must be excluded or covered as the
  existing ADRs (0001, 0003) describe, and their internal behavior is not
  testable from this repository.
- **Modest scope.** One search screen and one detail view. No list of hundreds
  of images, no infinite feed, no offline image requirement (images remain
  transient per `specs/003-ui-refresh/plan.md`).

Two options are realistic:

1. Adopt a Compose Multiplatform image-loading library (Coil 3 or Kamel).
2. Write a small custom Android-only async image loader in `androidApp`, with no
   new dependency.

## Decision

**Adopt Coil 3 (`io.coil-kt.coil3:coil-compose` + `io.coil-kt.coil3:coil-network-okhttp`)
at 3.6.3, as an Android-only (`androidApp`) implementation dependency.**

Coil 3 is the only candidate whose Kotlin and Compose Multiplatform compatibility
with this project's toolchain is verifiable from the library's own release
notes: the Coil 3.6.0 release (2026-08-26) states "Update Kotlin to 2.4.10",
"Update Compose to 1.12.0", and "Update the Android compile SDK to 37", and
3.6.3 (2026-09-18) is a patch release on that line. That places Coil 3.6.3 on
Compose Multiplatform 1.12.x / Kotlin 2.4.x / compileSdk 37, matching our fixed
1.12.2 / 2.4.21 / 37. Kamel's own documentation badges Kotlin 2.3.0 and Compose
Multiplatform 1.10.0, which are *behind* our pinned versions, so Kamel
compatibility is not verifiable and is not recommended.

It is used only from `androidApp`, which is the only module that renders images.
The composable wrapper (`androidApp/.../search/ProductImage.kt`) stays
project-owned: it maps an absent/failed URL to the neutral placeholder required
by ADR-0009, keeps the `contentDescription`/accessibility semantics, and hides
Coil behind a single composable so the exit strategy below stays cheap. Coil's
Android `ImageLoader` provides the in-memory LRU cache, disk cache, request
deduplication/cancellation, and scroll-safe async decoding that the plan's
performance goals ("must not delay text content and must not jank list
scrolling") call for.

This ADR approves the dependency, version, and catalog entries below. It does
**not** approve live image requests until the T071 instrumented tests use a
`FakeImageLoaderEngine` or local fixture resources and never touch the live CDN
(see plan.md *Testing*). Final artifact-size and build-time deltas must be
recorded in the validation record below and in ADR-0009 after integration.

### Version catalog and build changes (to be applied under T061)

`gradle/libs.versions.toml` — add to `[versions]`:

```toml
coil = "3.6.3"
```

`gradle/libs.versions.toml` — add to `[libraries]`:

```toml
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }
coil-test = { module = "io.coil-kt.coil3:coil-test", version.ref = "coil" }
```

`androidApp/build.gradle.kts` — add to `dependencies { }`:

```kotlin
implementation(libs.coil.compose)
implementation(libs.coil.network.okhttp)
androidTestImplementation(libs.coil.test)
```

`coil-test` is test-only (`FakeImageLoaderEngine`) and adds no production
artifact content; it exists so the T071 Compose tests are deterministic and
offline. No other module and no common source set changes.

## Alternatives considered

- **Kamel (`media.kamel:kamel-image-default` / `kamel-image` 1.0.9).** Rejected
  for now: the published compatibility metadata advertises Kotlin 2.3.0 and
  Compose Multiplatform 1.10.0, both behind this project's pinned Kotlin 2.4.21
  and Compose Multiplatform 1.12.2. Adopting it would mean guessing compatibility
  and risking a compiler/metadata mismatch against an AGP 9.1.1 / Gradle 9.5.1
  build. Its default Ktor-based fetcher would also add its own Ktor engine and
  disk-cache stack alongside the project's existing Ktor 3.6.0 usage. Reconsider
  only if a release explicitly targets Kotlin 2.4.x / Compose 1.12.x, and only
  via a superseding ADR.
- **Minimal custom Android-only async image loader (no new dependency).**
  Viable and the safe fallback, but not chosen. It would use
  `androidx.compose.foundation.Image` plus a `remember`-scoped coroutine that
  fetches bytes with the already-present OkHttp/`HttpURLConnection` stack,
  decodes to an `ImageBitmap`, and holds an `LruCache`. This is a
  compatibility-risk-free option and satisfies ADR-0009 (placeholder, failure
  fallback). Its cost is that we own, test to ≥80%, and maintain the decode
  path, cancellation on scroll, request deduplication, bitmap downsampling for
  memory safety, and lifecycle handling — precisely the code that a mature
  loader already hardens. It has no disk cache and no scroll-prefetch unless we
  build them. The project's performance goals and ≥80% coverage/lint gates make
  owning that surface more expensive than reviewing one well-scoped library
  ADR, so it is retained only as the documented fallback if Coil's measured
  size/build impact or licence review fails.
- **Coil 2.x (`io.coil-kt:coil`).** Rejected: it is Android-only, does not
  track Compose Multiplatform compatibility, and is superseded by Coil 3 for
  this toolchain.
- **Platform-native Android primitives beyond a small wrapper (e.g. Glide,
  Picasso).** Rejected: larger dependency surface, Java/annotation-processor
  oriented, and they do not fit the Compose-first style of this app for a
  two-screen need.

## Binary-size impact

Coil 3 is a production dependency used by `androidApp`, so it will grow the
Android APK. Coil's own project states it depends only on Kotlin, Coroutines,
and Okio and is designed to work with R8 shrinking; Coil uses native Android
`ImageDecoder`, and the Android `coil-network-okhttp` artifact reuses the OkHttp
already present transitively through Ktor 3.6.0, so no second HTTP stack is
introduced. No project-specific delta is measured yet: this ADR was written
without modifying build files, per task instructions. Measure the unsigned
release APK and debug APK before and after adoption with the existing metrics
workflow and record the deltas in the validation record below and in ADR-0009;
attribute the delta cautiously because it coincides with feature code.

## Build-time impact

Adding two production artifacts and one test artifact adds dependency resolution
and compilation work. There is no annotation processor and no compiler plugin.
Measure clean and incremental Gradle duration before and after integration and
confirm the hosted PR pipeline stays under the 10-minute target (T056/T073).

## Licence

Coil is published under the **Apache License 2.0**. The `io.coil-kt.coil3:coil-compose`
POM on Maven Central declares "The Apache License, Version 2.0"
(<https://central.sonatype.com/artifact/io.coil-kt.coil3/coil-compose>), and the
repository root `<https://github.com/coil-kt/coil/blob/main/LICENSE.txt>` is
Apache-2.0. Apache-2.0 is compatible with this public repository and consistent
with the other accepted runtime dependencies (Ktor, coroutines, serialization).

Before final acceptance, re-confirm from published POM/Gradle-module metadata:
`coil-compose`, `coil-compose-core`, `coil`, `coil-core`,
`coil-network-okhttp`, `coil-network-core`, test-only `coil-test`, and their
transitive artifacts (expected: Okio Apache-2.0; OkHttp Apache-2.0; Skiko
Apache-2.0 on non-Android targets, not shipped by this Android-only usage).
No content or image assets are bundled; the CC BY-SA obligations for the images
themselves remain governed by ADR-0009.

## Exit strategy

All Coil usage is confined to the project-owned `ProductImage` composable in
`androidApp` (plus the `coil-test` engine in instrumented tests). To remove or
replace Coil:

1. Reimplement the body of `ProductImage` with the custom Android loader (option
   above) or another approved implementation, preserving the placeholder,
   failure fallback, and `contentDescription` behavior.
2. Delete the `implementation(libs.coil.compose)`,
   `implementation(libs.coil.network.okhttp)`, and
   `androidTestImplementation(libs.coil.test)` lines from
   `androidApp/build.gradle.kts`.
3. Remove the `coil` version and the three `coil-*` library entries from
   `gradle/libs.versions.toml`.
4. Keep the T071 image-present/image-absent/failure tests; they assert the
   wrapper's behavior, not Coil's internals.

No data migration is required because image URLs and bytes are transient (no
schema change; see plan.md *Storage*).

## Consequences

- Search results and the detail view gain image decoding with memory/disk cache,
  cancellation, and deduplication without owning that code.
- A new production dependency and two new catalog entries are introduced and
  must pass the ≥80% coverage, ktlint, and licence gates; Coil internals are
  not covered here, so the coverage configuration may need to exclude
  `coil3` packages as it excludes other third-party code.
- The UI must still render the ADR-0009 placeholder for absent/failed images;
  Coil's error/fallback slots are wired in the project-owned wrapper.
- Instrumented tests must use `coil-test`'s `FakeImageLoaderEngine` (or local
  fixtures) so automated tests never call the live Open Food Facts CDN.
- If Coil 3.6.3 fails measured-budget or licence review, the custom loader
  remains the documented fallback and would supersede this ADR.
- Kamel is explicitly deferred pending a release that targets Kotlin 2.4.x and
  Compose Multiplatform 1.12.x.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-20
- **Rationale or conditions**: Approved adopting Coil 3.6.3 (`coil-compose`,
  `coil-network-okhttp`, test-only `coil-test`) for the Android-only UI. Applied
  to `gradle/libs.versions.toml` and `androidApp/build.gradle.kts` under T061.
  Coil must stay confined to the project-owned `ProductImage` composable;
  instrumented tests must never call the live Open Food Facts CDN.

## Validation record

- **Compatibility evidence (verified 2026-10-20)**: Coil 3.6.0 release notes
  (<https://coil-kt.github.io/coil/changelog/>) state "Update Kotlin to 2.4.10",
  "Update Compose to 1.12.0", and "Update the Android compile SDK to 37"; the
  3.6.3 patch (2026-09-18) targets AGP 9.x/R8 compatibility. `coil-compose`
  3.6.3 depends on `kotlin-stdlib` 2.4.10. This is consistent with Compose
  Multiplatform 1.12.2, Kotlin 2.4.21, and `compileSdk 37`. Kamel 1.0.9's
  advertised Kotlin 2.3.0 / Compose Multiplatform 1.10.0 are behind this
  project's pins, so Kamel compatibility is **not** verified.
- **Licence (verified 2026-10-20)**: Apache License 2.0 per the published
  `coil-compose` POM and the repository `LICENSE.txt`; transitive Okio and
  OkHttp are expected Apache-2.0 and must be reconfirmed from metadata at
  acceptance.
- **Not yet measured**: release/debug APK deltas, clean/incremental and hosted
  PR durations, and the final transitive-licence sweep are pending T061/T073.
  No build file, source file, or catalog entry was modified while writing this
  ADR.

### Post-integration measurements (applied 2026-10-20, T073)

- **Catalog/build changes applied**: `gradle/libs.versions.toml` gained
  `coil = "3.6.3"` and the `coil-compose`, `coil-network-okhttp`, and
  `coil-test` entries; `androidApp/build.gradle.kts` gained
  `implementation(libs.coil.compose)`,
  `implementation(libs.coil.network.okhttp)`, and
  `androidTestImplementation(libs.coil.test)`.
- **Transitive licence sweep (2026-10-20)**: the resolved graph shows Coil
  3.6.3 depends on `kotlin-stdlib` 2.4.10, Okio 3.18.x, and OkHttp 5.5.0 — all
  Apache-2.0 — and Okio/OkHttp are already present transitively via Ktor 3.6.0,
  so no second HTTP stack was introduced (`:androidApp:dependencies` verified).
- **Measured size deltas (2026-10-20, versus the 002 baseline)**: unsigned
  Android release APK 11,035,730 → **11,365,002 bytes** (+329,272, ≈ +3.0%);
  debug APK 15,265,427 → **15,709,515 bytes** (+444,088, ≈ +2.9%). The delta is
  feature-attributable to both the Coil dependency and the new UI code, and
  cannot be isolated per-dependency. `Shared.xcframework` (release) was
  **92,024 KB**, effectively unchanged from 92,000 KB, confirming Coil is
  Android-only and does not affect the iOS framework.
- **Build/verification (2026-10-20)**: clean verification (module unit tests,
  architecture tests, `coverageVerify`, `kotlinLint`, `assembleDebug`, Android
  lint) exited 0 in 24 s; `assembleRelease` exited 0;
  `:shared:assembleSharedXCFramework` exited 0; `xcodebuild` for the iOS
  Simulator destination succeeded with 0 errors; instrumented Compose tests were
  **14 of 14 passing** (7 pre-existing + 7 new image/badge tests). Hosted PR
  duration remains pending T056.

### Follow-up UI fixes (2026-10-20, post-review)

- **Mapping fix (live-verified)**: Search-a-licious returns `brands` (and
  sometimes `quantity`) as JSON **arrays** (`"brands":["Nutella"]`), which the
  original mapper read with a string-only helper and therefore dropped. A
  `stringOrArray` helper now joins array fields with `", "`, and
  `nutriscore_grade` values of `"unknown"`/blank normalize to `null` so no
  unsupported badge renders. `ProductResponseMapperTest` gained 8 cases
  (array/string/empty brands, array quantity, unknown/lowercased grade); all
  `core:data` tests pass with the `Product` model, SQL schema, and
  `REQUESTED_FIELDS` unchanged.
- **Detail-screen polish**: the full-width filled "Back to search" button was
  replaced with a compact back-bar (`←` `IconButton` + "Product details" title,
  48 dp target, `contentDescription = "Back to search"`). Nutrition amounts are
  formatted to remove floating-point noise (`7.6899999999999995` → `7.69`,
  `25.0` → `25`) via a tested `formatAmount` helper. The nutrition card now
  shows the single message "Nutrition information not available for this
  product" when a product has no values at all, while partial data still shows
  per-value "Unavailable" for the nutrients that are missing.
- **Every product has an image**: products without a catalogue photo (Open Food
  Facts genuinely lacks them for many entries — confirmed on the live index)
  render a deterministic generated gradient tile derived from the product code
  (stable per product via an FNV-1a fold over an 8-colour brand palette) with
  the product initial, so no product ever shows a blank or broken image area.
- **Verification (2026-10-20)**: `:androidApp:connectedDebugAndroidTest` 20 of
  20 passing (7 pre-existing + 7 image/badge + 2 nutrition empty/partial + 4
  `NutritionFormatTest`); clean verification exit 0 in 17 s; `assembleRelease`
  exit 0; `:shared:assembleSharedXCFramework` exit 0; `xcodebuild` BUILD
  SUCCEEDED with 0 errors. Emulator walkthrough against live data confirmed
  brand/quantity populated, real nutrient values displayed, the all-empty
  nutrition message, and generated placeholder tiles. Updated sizes: release APK
  11,365,002 bytes, debug APK 15,709,515 bytes, `Shared.xcframework` (release)
  92,024 KB.

### Professional design-system refactor (2026-10-20, second review)

The user reported that the UI was still not professional "in all screens, overall
the detail one". A measured on-device diagnosis (emulator-5556, 1080×2400 @
420 dpi, status-bar band ≈ 0–63 px) found three systemic causes, which were fixed:

- **Content drew under the system bars (the largest tell).** `MainActivity` calls
  `enableEdgeToEdge()` but there was no `Scaffold`, `TopAppBar` or `WindowInsets`
  handling anywhere, so the search title rendered at y=42 and the detail title at
  y=70 — inside the status-bar band, overlapping the system clock. A reusable
  `ui/NutriLogScaffold.kt` (Material 3 `Scaffold` + `TopAppBar`) now wraps the
  search root and hands `PaddingValues` down; the detail screen consumes
  `WindowInsets.statusBars`/`navigationBars` itself. Titles now render at y≈92 and
  y≈112 respectively, clear of the bars. The shell is *additive*: screens already
  rendered directly inside a bare `MaterialTheme {}` by the instrumented tests are
  unaffected because those insets are zero under the test host.
- **Broken detail layout / no visual system.** `DetailRow`/`NutritionRow` each drew
  a `HorizontalDivider()` *above* the row, so a rule sat between each heading and
  its first row while the last row had none. A shared `ui/StatRow.kt` (with
  `StatRowDivider` emitted only when `index > 0`) fixes the asymmetry. New shared
  primitives — `ui/theme/NutriLogSpacing.kt` (4/8/12/16/24/32 dp), `ui/SectionHeader.kt`,
  `ui/InfoChip.kt` — replace ad-hoc blocks so screens compose from a system.
- **Orphaned badge and floating values.** The Nutri-Score badge now sits in a
  labelled `InfoChip` in the title block. `StatRow` values are right-aligned in a
  bounded trailing column, so the nutrition table reads as a clean value column
  (verified on device: every value ends at x=996). The lone em-dash empty-state
  tile — which read as a stray typo — was replaced with an accent rule plus the
  message. `ProductInfoCard` no longer repeats Brand/Quantity already shown in the
  title block, and result cards reserve a fixed trailing slot so their right edges
  align whether or not a grade is present. The detail hero uses a standard 4:3
  ratio instead of 16:10.

Verification after the refactor: `:androidApp:connectedDebugAndroidTest` 20 of 20
passing (unchanged, no test assertions relaxed); clean verification exit 0 in 13 s;
`assembleRelease` exit 0; `:shared:assembleSharedXCFramework` exit 0; `xcodebuild`
BUILD SUCCEEDED with 0 errors. Sizes: release APK 11,381,386 bytes, debug APK
15,725,958 bytes, `Shared.xcframework` (release) 92,024 KB (unchanged).
