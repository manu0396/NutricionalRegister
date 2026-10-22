# ADR-0017: CameraX and Bundled ML Kit Barcode Scanning for the Android Scanner

- **Status**: Proposed
- **Date**: 2026-10-21
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: `specs/007-app-restructure/` (spec.md, plan.md, tasks.md — the scanner tasks are to be numbered in the same change set); ADR-0011, ADR-0013, ADR-0014, ADR-0015

## Context and need

Feature `007-app-restructure` turns the app into a sellable nutrition tracker.
The maintainer has explicitly decided that **barcode scanning is in scope for
007**, which supersedes the "Out of Scope - Barcode scanning" entry currently
written in `specs/007-app-restructure/spec.md`; that entry must be amended in
the same change set as this ADR, and the corresponding tasks added to
`specs/007-app-restructure/tasks.md`.

The pieces are in place except the scanner itself:

- `feature/scan` is an **empty module**. Its entire build file is
  `implementation(project(":core:domain"))`
  (`feature/scan/build.gradle.kts`). It has no sources.
- It is already **exported from the shared module** into the `Shared.xcframework`
  (`shared/build.gradle.kts`, `export(project(":feature:scan"))`), and
  `:androidApp` already depends on `:feature:scan`
  (`androidApp/build.gradle.kts`).
- `androidApp/src/main/AndroidManifest.xml` declares only `INTERNET` and
  `ACCESS_NETWORK_STATE`. There is **no `CAMERA` permission**.
- The catalogue is Open Food Facts reached through Search-a-licious, and the
  **only** product data source is `ProductSearchRepository` in `core:domain`.
  The lookup entry point is
  `public suspend fun getProduct(code: String): Product?`
  (`core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/product/ProductSearchRepository.kt`).
  A scanned barcode must be resolved through this repository and through no
  other data source.

The scanner must therefore satisfy these requirements:

1. **Decode a barcode from the camera** and turn it into a catalogue product.
2. **Work offline** for the decode step: the core logging flow must not depend
   on a network round trip or on a model download to be usable.
3. **Fail honestly, never silently**: when the product is not available, the
   user must be told and offered a way forward.
4. **Degrade safely**: no crash when the camera permission is refused or when
   the device has no usable camera.
5. **Be testable**: the scan flow must be verifiable without camera hardware,
   because a headless emulator has no camera.
6. **Not disturb the iOS target**: navigation and UI are Android-only
   (ADR-0015) and the shared modules hold all business logic (constitution
   principle I). The iOS target keeps compiling and is not rebuilt in this
   feature.

Constitution rule III requires an ADR for every new third-party dependency
before it enters the version catalog. **This is that ADR.**

## Decision

**Adopt `androidx.camera` 1.6.2 (`camera-core`, `camera-camera2`,
`camera-lifecycle`, `camera-view`) and `com.google.mlkit:barcode-scanning`
17.3.0, bundled model, as `androidApp`-only implementation dependencies.**

### Version rationale (verified 2026-10-21)

- **CameraX 1.6.2.** Google Maven lists `1.7.0-alpha03` as the newest version
  but it is a pre-release; **1.6.2 is the newest stable release** of the
  1.6 line. Its published AAR metadata declares
  `minCompileSdk=36` and `minAndroidGradlePluginVersion=8.9.1`, both satisfied
  by this project (`compileSdk 37`, AGP 9.1.1). Its POM floors
  `kotlin-stdlib` at 2.1.20, `kotlinx-coroutines` at 1.9.0, `androidx.core` at
  1.1.0 and `androidx.lifecycle` at 2.1.0 — all below what this project
  already resolves (Kotlin 2.4.21, coroutines 1.11.0, androidx-core 1.19.1,
  androidx-lifecycle 2.11.0), so **no new version line is introduced** in the
  resolved graph. All four artifacts exist at 1.6.2 on Google Maven.
- **Bundled ML Kit barcode scanning 17.3.0.** 17.3.0 is the current release of
  `com.google.mlkit:barcode-scanning` and is the version Google's own
  documentation pins for the bundled model. The bundled variant **statically
  links the barcode model at build time**, so decoding needs no network and no
  Google Play services. The unbundled variant
  (`com.google.android.gms:play-services-mlkit-barcode-scanning`, 18.3.1, which
  is the version the bundled artifact itself depends on) would make the core
  logging flow depend on a first-use model download and would fail outright on
  devices without Play services. The app already requires Google Play Billing
  on Android (ADR-0014), but the scanner must not inherit that dependency for
  a core flow. ML Kit's bundled barcode API requires `minSdk 23`; this project
  is on `minSdk 24`.
- **Deliberately not added.** `androidx.camera:camera-compose` is not used:
  the project owns its own Compose scanner screen and `camera-view` is enough
  to host the preview surface. `androidx.camera:camera-mlkit-vision` is not
  used either: its `MlKitAnalyzer` bridge would tie the analyzer directly to
  ML Kit types and would replace the project-owned `BarcodeDecoder` boundary
  this ADR requires.

### Version catalog and build changes

`gradle/libs.versions.toml` — add the versions and the libraries:

```toml
[versions]
androidx-camera = "1.6.2"
mlkit-barcode = "17.3.0"

[libraries]
androidx-camera-core = { module = "androidx.camera:camera-core", version.ref = "androidx-camera" }
androidx-camera-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "androidx-camera" }
androidx-camera-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "androidx-camera" }
androidx-camera-view = { module = "androidx.camera:camera-view", version.ref = "androidx-camera" }
mlkit-barcode-scanning = { module = "com.google.mlkit:barcode-scanning", version.ref = "mlkit-barcode" }
```

`androidApp/build.gradle.kts` — add to `dependencies { }`:

```kotlin
implementation(libs.androidx.camera.core)
implementation(libs.androidx.camera.camera2)
implementation(libs.androidx.camera.lifecycle)
implementation(libs.androidx.camera.view)
implementation(libs.mlkit.barcode.scanning)
```

**No `settings.gradle.kts` change is needed.** The `google()` repository is
restricted with
`mavenContent { includeGroupAndSubgroups("androidx"); includeGroupAndSubgroups("com.android"); includeGroupAndSubgroups("com.google") }`,
which covers `androidx.camera:*` and `com.google.mlkit:*`.

### Scope: `androidApp`, not `core:data`

Both dependencies are declared **only in `androidApp`**. The camera preview is
UI, and the shared modules are consumed by the iOS build, so keeping CameraX
and ML Kit out of `core:data` and out of `core:domain` keeps the
`Shared.xcframework` free of them. The default is `androidApp` and this ADR
does not deviate from it.

### Platform split

- **Android-only** (`androidApp/src/main/kotlin/com/example/nutricionalregister/scan/`):
  the camera preview surface, the `ImageAnalysis` analyzer, the ML Kit decoder
  implementation, and the runtime permission handling. None of this is in a
  shared source set.
- **Shared** (`feature:scan`, `commonMain`): the reusable business logic that
  turns a scanned barcode into a catalogue product, and the "not found in the
  catalogue" path. This lives in the shared module so it is unit-testable in
  common tests and reusable by a future iOS SwiftUI screen.

`feature:scan` changes from an empty module to a filled one. Its build file
gains the shared dependencies it needs (at minimum `api(project(":core:domain"))`,
so the resolution result types are visible to `androidApp` and through the
exported framework; today it declares only `implementation`, which does not
expose them).

**`feature:scan` must contain no Android-only API.** Its shared API stays
platform-neutral — plain data (a barcode code string, a resolution outcome) —
so that a future iOS implementation built on `AVCaptureSession` can be added
behind the same boundary without changing the shared contract.

### Testability boundary

**Frame-to-barcode decoding sits behind a project-owned interface**,
`BarcodeDecoder`, in `androidApp`. The screen takes the decoder as a parameter;
the single class that imports ML Kit types (`MlKitBarcodeDecoder`) is its only
implementation.

Consequences, stated explicitly:

- The Compose scanner screen is exercised in **instrumented tests with a fake
  decoder**, so every screen state (permission denied, no camera, decoding,
  resolved, not found) is verifiable on an emulator **without a camera**.
- The shared resolution logic in `feature:scan` is verified in **common tests**
  against a fake `ProductSearchRepository`, with no Android dependency at all.
- The camera preview and the real ML Kit model remain the only parts that
  cannot be exercised on a headless emulator; they require hardware and are
  recorded as such in the Validation record.

### Permission handling

- `android.permission.CAMERA` is **declared** in
  `androidApp/src/main/AndroidManifest.xml` (a declared permission, requested at
  runtime, not an install-time permission).
- `<uses-feature android:name="android.hardware.camera" android:required="false" />`
  is declared so the app still installs on devices without a camera and can
  offer the manual fallback instead of being filtered out of the store.
- The scanner screen **requests the permission at runtime** and renders an
  explicit, actionable state when it is refused: an explanation of why the
  camera is needed plus a **manual barcode entry** field, so the flow is still
  completable. The screen **must not crash** when the permission is refused
  (including the "do not ask again" case, where the guidance points at the app
  settings) and **must not crash** when no camera is available.
- The scanner is **reachable only from the add-food flow**. It is not a
  bottom-navigation destination in this feature; the 007 shell keeps four
  destinations (Today, Search, Library, More).

### Product resolution and offline behaviour

- The scanned code is resolved through the existing repository:
  `ProductSearchRepository.getProduct(code)`. **No second data source is
  introduced.**
- **Verified constraint (2026-10-21).** `getProduct` is implemented in
  `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/ProductSearchRepositoryImpl.kt`
  as a **cache-only** lookup: it selects the product from the local SQLDelight
  table and returns `null` when the code was never cached, and
  `ProductCatalogue` exposes only `search(term)` (a name search), with no
  lookup by barcode code. Resolving a barcode the user has never searched
  therefore requires the repository to gain a by-code catalogue lookup over
  the **same** Search-a-licious / Open Food Facts backend. That is an
  implementation task inside the existing repository contract, not a new
  dependency and not a new data source; this ADR does not decide the wire
  format. If it is not added, the scanner can only resolve already-cached
  products and the feature appears broken on first use.
- With the bundled ML Kit model, **decoding works offline**. Resolving the
  barcode to a product still needs the catalogue cache or the network.
- When the product is not in the offline cache and the catalogue cannot be
  reached, the app **must say so honestly** and offer two explicit actions:
  **retry**, or **search by name** instead. It must never fail silently and
  must not show a generic error in place of the specific reason.

### iOS

The iOS target **keeps compiling and is not rebuilt** in this feature. The
`feature:scan` change is shared Kotlin code only, so the `Shared.xcframework`
grows by the small resolution logic and by nothing else. A platform-specific
scanner on iOS is a **separate decision**; the shared API is kept
platform-neutral precisely so that a future `AVCaptureSession` implementation
can be added behind the same boundary.

## Alternatives considered

- **Google code scanner (`com.google.code.scanner:play-services-code-scanner`).**
  Rejected: far less code and a much smaller APK because Play services already
  ships on the device, but it requires Google Play services on the handset, it
  cannot be exercised on a standard emulator image, it cannot be themed to the
  app's design, and it gives no control over the scan UX. In exchange for an
  owned, testable, offline-capable scanner this is a fair trade. Its version
  was not verified from the repository at authoring time and must be confirmed
  if it is ever reconsidered.
- **ZXing (`com.google.zxing:core`, current release 3.5.4) decoded from
  CameraX frames.** Rejected: the decoder is pure Java and small, but it means
  implementing barcode detection, rotation, cropping and focus handling by
  hand — exactly the fragile work ML Kit already does, and exactly the work
  that would have to be re-tested on every camera and every device.
- **Unbundled ML Kit model
  (`com.google.android.gms:play-services-mlkit-barcode-scanning` 18.3.1).**
  Rejected: the library itself is small (roughly 200 KB per Google's
  documentation), but the core logging flow would then need network on first
  use to fetch the model and would fail on devices without Play services. The
  bundled variant is chosen for that reason, not for size.
- **A cloud barcode-resolution API.** Rejected: it would leak the user's
  shopping context to a third party for something the device can do locally,
  and it adds a per-scan cost and a per-scan dependency.
- **Shipping the scanner inside `core:data`.** Rejected: it would pull
  CameraX and ML Kit into the `Shared.xcframework` that iOS consumes, for no
  benefit.
- **Manual barcode entry only, with no camera at all.** Rejected as the only
  path — it defeats the purpose of the feature and is far slower for the user
  than a scan. It is **kept as the fallback** for the permission-denied and
  no-camera states, and as the fallback when a barcode is not in the catalogue.

## Binary-size impact

Both estimates below are **estimates from published sources, not
measurements**, and they must be **measured after integration** against the
recorded release APK baseline and recorded in this ADR's Validation record.

- **ML Kit bundled barcode scanning.** Google's documentation states an app
  size increase of "about 2.4 MB" for the bundled model, against "about 200 KB"
  for the unbundled library. That figure is the source-supported basis for the
  estimate.
- **CameraX.** The four published artifacts are, uncompressed:
  `camera-core` 1,213,194 B, `camera-camera2` 1,184,080 B, `camera-lifecycle`
  54,987 B, `camera-view` 115,157 B — roughly 2.5 MB of AAR before dexing and
  shrinking. R8 removes the unused CameraX paths (video capture, extensions,
  effects, the Camera2 interop layer) in release builds; the release
  measurement captures that.
- **Native payload caveat (important).** The bundled ML Kit artifact ships
  `libbarhopper_v3.so` for four ABIs (arm64-v8a 4,946,720 B, armeabi-v7a
  3,244,440 B, x86 6,122,368 B, x86_64 5,909,280 B) plus roughly 880 KB of
  `.tflite` model assets. This project configures **no ABI splits and no
  `ndk.abiFilters`** (`build-logic/src/main/kotlin/nutrition.android-application.gradle.kts`),
  so its release APK is universal and carries every ABI the artifact
  provides. The measured delta will therefore likely be **larger** than the
  "+2 to +3 MB" estimate, and the Validation record must state the ABI
  configuration of the measured artifact.
- **Expected combined order of magnitude**: roughly **+2 to +3 MB** for an
  arm64-style release artifact, and materially more for the current universal
  release APK. Estimate only; to be measured.
- **Baseline for the measurement**: the recorded unsigned release APK is
  **12,520,644 bytes** (ADR-0014 Validation record, confirmed by ADR-0015 after
  navigation and billing). The measurement must be taken against that baseline.
- The iOS `Shared.xcframework` is **unaffected by construction**, because both
  dependencies are declared only in `androidApp`, which is not part of the
  XCFramework. It grows only by the small shared resolution logic added to
  `feature:scan`.

## Build-time impact

- **Two new dependency resolutions** per configuration, from Google Maven, with
  no repository or `settings.gradle.kts` change.
- **Extra dexing and merging work**: the CameraX and ML Kit class trees add
  to the dex and resource-merge steps, and the bundled ML Kit artifact brings
  native libraries and model assets that must be packaged and stripped per ABI.
  This is the dominant part of the expected build-time increase.
- R8 shrinks the unused CameraX paths in release builds; the release
  measurement captures the effect, so the build-time and size figures must be
  taken from the same build.
- Clean and incremental durations are measured after integration against the
  repository's CI baseline; the hosted PR pipeline must stay under the
  project's 10-minute target (ADR-0011, ADR-0015).
- The scanner adds an instrumented-test path that needs camera hardware (or a
  device emulator with a camera). This is precisely why the `BarcodeDecoder`
  boundary exists: the screen is tested with a fake decoder so the ordinary
  emulator run does not depend on camera hardware.

## Licence

Verified from the **published POM metadata** on 2026-10-21.

- **CameraX 1.6.2** (`androidx.camera:camera-core`, `camera-camera2`,
  `camera-lifecycle`, `camera-view`) is published by the Android Open Source
  Project under the **Apache License 2.0**, with a second, explicitly declared
  **BSD-3-Clause** entry covering the libyuv-derived code inside `camera-core`.
  Apache-2.0 is compatible with this public repository; it imposes no copyleft
  and requires only licence and notice preservation, which the artifact
  metadata satisfies.
  Source: <https://developer.android.com/jetpack/androidx/releases/camera>
- **ML Kit `com.google.mlkit:barcode-scanning` 17.3.0** is **not** published
  under Apache-2.0. Its POM declares the licence as
  **"ML Kit Terms of Service"**
  (<https://developers.google.com/ml-kit/terms>), and the unbundled
  `play-services-mlkit-barcode-scanning` 18.3.1 declares the same. The ML Kit
  Terms of Service incorporate the Google APIs Terms of Service and are terms
  of use rather than an OSI open-source licence. They require that ML Kit
  processing stays on-device (image data is not sent to Google), that the app
  informs users about Google's collection of ML Kit usage metrics as required
  by applicable law, and they prohibit reverse engineering of the APIs or of the
  machine learning models, which the terms treat as related software. There is
  **no copyleft, no attribution obligation on the UI, and no licence fee**.
  The bundled artifact additionally ships its own
  `third_party_licenses.txt` / `third_party_licenses.json` inside the AAR
  (Apache-2.0-family notices for Abseil, the AndroidX libraries and others);
  those notices must be preserved and surfaced in the app's About section.
  Source: <https://developers.google.com/ml-kit/terms>
- **Correction against the original brief.** The brief for this ADR assumed
  that ML Kit barcode scanning is Apache-2.0. The published POM metadata says
  otherwise, and the recorded value is the published one. The licence of the
  resolved artifacts must be **reconfirmed from the POMs that Gradle actually
  resolves** at adoption time and recorded in the Validation record, because
  the terms of a bundled model may change with the artifact version.

## Exit strategy

- **Swapping the decoder.** Decoding sits behind the project-owned
  `BarcodeDecoder` interface. ML Kit can be replaced by ZXing, by a Play
  Services scanner, or by any other implementation by swapping **one
  implementation class**; the Compose scanner screen and the shared resolution
  logic in `feature:scan` do not change.
- **Removing the camera entirely.** The `CAMERA` permission can be dropped
  from the manifest, the runtime request and the preview surface removed, and
  `feature:scan` reduced to a catalogue lookup driven by a typed code (or by a
  name search). No schema, no persisted data and no migration depends on the
  scanner: the barcode is a transient input and nothing in the SQLDelight
  schema or the diary model references it.
- **Nothing generated or shared depends on the libraries.** No shared, native
  or persisted artifact references CameraX or ML Kit, so the exit is a
  version-catalog deletion plus the removal of the `androidApp` scan package.
  The `feature:scan` shared API is designed to survive the removal, so it can
  be kept as a plain catalogue lookup.

## Consequences

- The app gains a scanner that **works offline**, is **owned and themed** by
  the project, and is **testable without a camera** through the
  `BarcodeDecoder` boundary.
- The **app now declares a camera permission**. The store listing needs a
  **camera-permission disclosure**, and the scanner screen needs an in-app
  rationale shown before the runtime request.
- The ML Kit terms require that users be informed about Google's ML Kit metrics
  collection as applicable law requires; this is a **Play data-safety
  disclosure** obligation, recorded here so it is not missed at release time.
- The **release APK grows** by an estimated +2 to +3 MB in an arm64-style
  artifact, and by more in the project's current universal release APK because
  the bundled model ships four ABIs. To be measured; see the Validation
  record. An ABI-split or arm64-only packaging decision would reduce this
  materially, but that is a packaging decision and is **out of scope for this
  ADR**.
- The scanner is **the one part of the logging flow that cannot be exercised
  on a headless emulator**. The camera preview and the real ML Kit model need
  hardware; the screen is verified with a fake decoder instead, and the
  hardware path is recorded as not verifiable in this environment.
- The project owns the upgrade path for two Google artifacts with independent
  release cadences. The exposure is bounded to the `androidApp` scan package.
- `feature:scan` stops being an empty module. Its shared API **must stay
  platform-neutral** and free of Android-only APIs, otherwise the iOS build
  and the `Shared.xcframework` break. This is the constraint that makes a
  future iOS scanner possible rather than a new migration.
- The resolution path depends on the repository gaining a by-code catalogue
  lookup over the existing backend. Until it does, only already-cached products
  resolve, and the scanner would appear broken on first use. This is recorded
  as an implementation requirement, not as a new dependency.
- The "Out of Scope - Barcode scanning" entry in
  `specs/007-app-restructure/spec.md` and the task list must be amended in the
  same change set, so that the specification and the implementation agree.

## Review record

- **Review outcome**: Pending
- **Reviewed by**: —
- **Review date**: —
- **Rationale or conditions**: The maintainer confirmed in the 007 clarification
  round that **barcode scanning is in scope for this feature**, which is what
  brings this ADR forward. The artifact selection itself - CameraX 1.6.2 and the
  bundled ML Kit barcode-scanning 17.3.0, scoped to `androidApp` - is proposed
  here and still awaits the maintainer's sign-off, together with the conditions
  below.

  Conditions: the dependencies must not enter `core:data`, `core:domain` or the
  shared module; no second catalogue data source may be introduced;
  `feature:scan` must stay free of Android-only APIs; the 007 specification must
  be amended in the same change set; and the measured release APK size must be
  recorded against the 12,520,644 B baseline with the ABI configuration stated.

## Validation record

To be completed after integration.

- **Dependency resolution**: `androidx.camera:camera-core`, `camera-camera2`,
  `camera-lifecycle`, `camera-view` at 1.6.2 and
  `com.google.mlkit:barcode-scanning` at 17.3.0 resolve from Google Maven
  under the existing restricted `google()` content filter, with no
  `settings.gradle.kts` change. Record the resolved POM licence entries.
- **Version compatibility**: the app compiles and packages against Kotlin
  2.4.21, AGP 9.1.1, `compileSdk 37`, `targetSdk 37` and `minSdk 24`; no new
  version line appears in the resolved dependency graph.
- **Module scope**: the dependencies are absent from `core:data`,
  `core:domain` and `shared`; the `Shared.xcframework` build is unchanged.
- **Binary size**: the measured debug and release APK sizes, the delta against
  the **12,520,644 B** release baseline, and the ABI configuration of the
  measured artifact; the `Shared.xcframework` size.
- **Build time**: clean and incremental durations before and after, against the
  CI baseline, with the hosted PR pipeline under the 10-minute target.
- **Tests**: the shared resolution logic in `feature:scan` verified in common
  tests against a fake `ProductSearchRepository`; the scanner screen verified
  in instrumented tests with a fake `BarcodeDecoder`, covering the permission
  denied state, the no-camera state, the decoding state, the resolved state
  and the not-found state with its retry and search-by-name actions.
- **Exit strategy check**: still holds; the decoder is behind
  `BarcodeDecoder` and the `CAMERA` permission can be dropped without a
  migration.
- **Not verifiable in this environment**: decoding a real barcode from a real
  camera (requires camera hardware); the Play data-safety and
  camera-permission disclosures (store-side actions).
