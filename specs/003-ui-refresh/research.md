# Research: Modern UI Refresh with Product Images

**Feature**: `003-ui-refresh` | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

**Date**: 2026-10-20

This document records the investigation behind the UI refresh. The two decisions
that needed external evidence were (1) where product images come from and under
what licence, and (2) how image bytes are fetched, decoded, and cached on
Android. Both are ratified in ADRs; this file records the wider survey, including
the options that were rejected.

## 1. Product image source

### Question

The app already queries Open Food Facts via Search-a-licious for product data.
Does that same catalogue publish per-product images that the app may legally
reuse, and are they identifiable from data the app already has?

### Findings (verified 2026-10-20)

- **Field availability.** A live `GET /search` against
  `https://search.openfoodfacts.org/search` returns `image_front_small_url` and
  `image_front_url` for products that have a photo, for example:

  ```json
  {
    "code": "5411788041157",
    "product_name": "Oat drink natural",
    "image_front_url": "https://images.openfoodfacts.org/images/products/541/178/804/1157/front_nl.77.400.jpg",
    "image_front_small_url": "https://images.openfoodfacts.org/images/products/541/178/804/1157/front_nl.77.200.jpg"
  }
  ```

  The fields are served by the catalogue's product/image schema, so they can be
  added to the existing `fields` request parameter without a second round trip.

- **Coverage is not guaranteed.** The same query returned a product with no
  image fields at all (`"Barista Edition Oat Drink"`, no `image_*` keys). Many
  catalogue entries simply have no photo. Images therefore **must** be treated as
  optional, and the "every product shows an image" requirement can only be met by
  falling back to a generated placeholder rather than by the catalogue alone.

- **Shared identity.** The `code` returned by the search endpoint is the Open
  Food Facts barcode embedded in the image URL path, so no product-identity
  reconciliation step is introduced.

- **Licence.** Open Food Facts publishes product images under **CC BY-SA**; the
  database is **ODbL** and individual contents are **DbCL**
  (<https://world.openfoodfacts.org/terms-of-use>). Reuse requires attribution
  and share-alike. The app already credits Open Food Facts on every surface that
  shows catalogue data, so attribution is extended rather than introduced.

### Options considered

| Option | Verdict | Reason |
|---|---|---|
| Open Food Facts catalogue image URLs | **Chosen** | Actual product photo; no second vendor; identity already known; licence already covered by existing attribution |
| Stock-photo APIs (Unsplash, Pexels) | Rejected | Image does not depict the actual product — misleading in a nutrition app; adds a vendor, API key and licence record |
| Food-category APIs (e.g. Foodish) | Rejected | Generic category image, not the product; still a second dependency for no accuracy gain |
| Bundled offline image set | Rejected | Catalogue is large and changes continuously; bloats APK/XCFramework and needs an export pipeline |
| Generated placeholders only | Rejected | Meets none of the "identify products visually" requirement |

**Decision**: reuse Open Food Facts catalogue image URLs. Full rationale,
alternatives, licence analysis and exit strategy in
[ADR-0009](../../docs/adr/0009-product-images-open-food-facts.md).

## 2. Android image loading

### Question

Coil 3, Kamel, or a small hand-written Android-only loader? Images are rendered
only by the native Android Compose UI, so a multiplatform loader buys nothing the
Android target does not already need.

### Findings (verified 2026-10-20)

- **Coil 3.6.3** release notes state "Update Kotlin to 2.4.10", "Update Compose
  to 1.12.0" and "Update the Android compile SDK to 37". This project pins Kotlin
  **2.4.21**, Compose Multiplatform **1.12.2** and `compileSdk 37`, so Coil 3.6.3
  sits on a matching line.
- **Kamel 1.0.9** advertises Kotlin **2.3.0** and Compose Multiplatform
  **1.10.0** — both *behind* the pinned versions — so compatibility cannot be
  verified and would be guessed.
- Coil already depends on **OkHttp**, which the project already ships transitively
  via Ktor's Android engine, so adopting Coil does **not** introduce a second HTTP
  stack.
- Coil provides in-memory LRU caching, a disk cache, request
  deduplication/cancellation, and scroll-safe async decoding — the behaviours the
  plan's performance goals require ("must not delay text content", "must not jank
  list scrolling").

### Options considered

| Option | Verdict | Reason |
|---|---|---|
| Coil 3.6.3 (`coil-compose` + `coil-network-okhttp`, test-only `coil-test`) | **Chosen** | Verifiable compatibility with pinned Kotlin/Compose/compileSdk; reuses OkHttp; caching and async decoding for free |
| Kamel 1.0.9 | Rejected | Published compatibility metadata is behind the project's pinned versions; adds its own Ktor engine and disk cache alongside the existing Ktor usage |
| Hand-written Android-only async loader | Rejected (safe fallback) | No compatibility risk, but the project would own, test to ≥80%, and maintain decode, cache, and cancellation logic |

**Decision**: adopt Coil 3.6.3 as an Android-only implementation dependency,
wrapped behind a single project-owned composable so the exit strategy stays
cheap. Details, version-catalog entries and licence record in
[ADR-0010](../../docs/adr/0010-compose-image-loading.md).

## 3. Live-verified catalogue field shapes (mapping defects found)

Reading the mapper against the real endpoint surfaced two defects that were not
visible from the published schema alone. Both were confirmed live on 2026-10-20
and fixed as part of this feature:

- **`brands` is a JSON array, not a string.** The endpoint returns
  `"brands":["Nutella"]`. The original mapper read strings only, so every brand
  was silently dropped. A `stringOrArray` helper now joins array fields with
  `", "` (and reads plain strings unchanged for backward compatibility).
- **`quantity` may also be an array**, and is frequently absent
  (`"quantity": null`). Handled by the same helper with blank/null coercion.
- **`nutriscore_grade` may be the literal string `"unknown"`,** which is not a
  valid grade. Blank and `"unknown"` both normalize to `null` so no unsupported
  badge is rendered.
- **`nutriments` values are noisy doubles,** e.g. `7.6899999999999995`. These are
  formatted for display to at most two decimals with no trailing `.0`
  (`formatAmount`), because the raw value reads as corrupt data.

## 4. Presentation findings (measured on device)

The UI refresh was validated by dumping real layout bounds from a running
emulator (1080×2400 @ 420 dpi, status-bar band ≈ 0–63 px) rather than by
inspection alone. That measurement found:

- **Content was drawing under the system bars.** `MainActivity` enables
  edge-to-edge, but the app had no `Scaffold`, `TopAppBar` or inset handling, so
  titles rendered at y=42 (search) and y=70 (detail) — inside the status-bar band,
  overlapping the system clock. Fixed with an app shell and per-screen insets.
- **Dividers were drawn *above* each row,** including the first, so a rule sat
  between every heading and its first row and none closed the list. Fixed so
  dividers appear only between rows.
- **Label/value rows did not align,** so nutrient values jittered horizontally.
  Values are now right-aligned in a bounded column (all value nodes end at the
  same x).

These are recorded here because they are the empirical basis for the layout
requirements added to the feature, and because they show the class of defect that
unit tests alone do not catch.

## 5. Accessibility and privacy findings

- **Touch targets.** All interactive controls were already at 48 dp minimum;
  the refresh preserves that and the existing accessibility test suite enforces
  it.
- **Image semantics.** The detail header image keeps a useful
  `contentDescription` ("Image of {name}"); card thumbnails stay decorative
  (`null`) because the card already describes the product, avoiding duplicate
  announcements.
- **Enlarged text.** Existing large-text tests were kept green; the new layout
  must not truncate essential actions.
- **Privacy.** Image requests carry only the app User-Agent. No analytics,
  tracking, or search-term logging is introduced, and automated tests never touch
  the live image CDN.

## 6. Open questions

- **Hosted CI duration** (task T056) remains unmeasured because it requires a
  pushed branch and pull request to trigger the hosted workflows. It is tracked
  against the project's under-10-minute target.
