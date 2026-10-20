# Implementation Plan: Modern UI Refresh with Product Images

**Branch**: `003-ui-refresh` | **Date**: 2026-10-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-ui-refresh/spec.md`

## Summary

Refresh the Android Compose search and product-detail surfaces into a coherent
modern UI and add catalogue product images. Images reuse the Open Food Facts
catalogue already queried by Product Search: the image URL fields are added to
the existing Search-a-licious `fields` request and carried through the existing
`Product` model. A shared Material 3 theme (colour, typography, shape, spacing)
is introduced in the Android app, result cards gain thumbnails, the detail view
gains an image header and a Nutri-Score badge, and all states are restyled.
Existing search behavior (offline-first, latest-query-wins, recent history) is
unchanged. The iOS UI remains out of scope; shared model/mapping changes remain
in common code.

This work introduces one external-resource decision (product-image licensing),
recorded in [ADR-0009](../../docs/adr/0009-product-images-open-food-facts.md).
If an image-loading library is adopted it must be recorded in its own ADR before
a version-catalog entry is added.

## Technical Context

**Language/Version**: Kotlin Multiplatform, Kotlin 2.4.21, Gradle 9.5.1.

**Primary Dependencies**: Existing stack (Compose Multiplatform 1.12.2,
Material 3 1.12.0-alpha03, Ktor 3.6.0, kotlinx.serialization 1.11.0). A Compose
image-loading library is a candidate only; alternatives are assessed below and
any choice requires an ADR before adoption.

**Storage**: No schema change required if image URLs are transient. If offline
images are required, cache image bytes on device (keyed by URL, bounded size)
and extend the SQLDelight schema in a forward migration. The initial scope keeps
image URLs transient and relies on the platform image cache.

**Testing**: Extend `ProductResponseMapper` tests for image-field extraction and
absence; add Compose instrumented tests for image-present, image-absent
(placeholder), Nutri-Score badge, and preserved accessibility semantics; keep
existing 7 UI tests green. Never call the live image CDN from automated tests —
use local fixture resources or a fake image loader.

**Target Platform**: Android 24+ native Compose UI. Shared model changes apply
to all KMP targets.

**Project Type**: Kotlin Multiplatform mobile app with native Android Compose UI.

**Performance Goals**: Preserve existing goals (snapshot < 1 s, network results
< 3 s). Image loading must not delay text content and must not jank list
scrolling; thumbnails use the small image URL and the detail view uses the
larger one.

**Constraints**: Respect module boundaries (`core:domain` no project deps,
`core:data` → domain only, `feature:search` → domain only, `androidApp`
composes). Preserve accessibility (labels, 48 dp targets, enlarged text).
Preserve privacy (no analytics/tracking/logging). Show missing nutrition as
unavailable, never zero. Keep raw Open Food Facts values display-ready in
common code; keep Android-only styling in `androidApp`.

**Scale/Scope**: One Android search screen, one product-detail view, a theme,
and shared model/mapping additions. No new feature module, navigation library,
account, analytics, or iOS screen.

**Build Health**: Re-measure clean/incremental build duration and re-run the
full quickstart validation after integration; hosted PR duration (T056) remains
pending.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Result | Evidence |
|---|---|---|
| Shared logic and deliberate platform boundaries | PASS | Model/mapping change lives in common code; theme, composables, and image loading stay in `androidApp`. |
| Cross-platform compatibility | PASS | `Product` gains optional common fields; no platform-specific type leaks into `core:domain`. |
| Verify behavior with tests | PASS | Mapper tests for image fields; instrumented tests for image present/absent/failure and Nutri-Score; existing tests preserved. |
| Focused, maintainable changes | PASS | Scope limited to presentation polish and an image field; no new behavior or module. |
| Module boundaries | PASS | No new cross-module dependency; `core:data` still depends only on domain. |
| Governance and dependency review | PASS WITH REQUIRED SEQUENCE | ADR-0009 records the image-licence decision; any image-loading library requires its own ADR before catalog changes. |
| External data requirements | PASS WITH DOCUMENTED LIMITATIONS | Image URLs come from the already-used catalogue; presence is not guaranteed and failures degrade to placeholders. |

## Phase 0: Research

Full survey, including rejected options and the live-verified field shapes, is in
[research.md](research.md). Summarised findings (2026-10-20):

- Open Food Facts product images are licensed CC BY-SA; the database is ODbL and
  individual contents are DbCL (<https://world.openfoodfacts.org/terms-of-use>).
  Reuse requires attribution and share-alike.
- Open Food Facts publishes image URLs per product (thumbnail and full/front
  images) documented via its product schema and image APIs, and as an AWS Open
  Data dataset (<https://registry.opendata.aws/openfoodfacts-images>).
- Reusing the catalogue's own image URLs avoids a second product identity: the
  Search-a-licious `code` is the Open Food Facts barcode used by the image URLs.
- Generic stock-photo and food-category APIs return images that do not depict the
  actual product and were rejected as misleading for a nutrition app.

Decision: use Open Food Facts catalogue image URLs. Full rationale, alternatives,
licence analysis, and exit strategy are in
[ADR-0009](../../docs/adr/0009-product-images-open-food-facts.md).

## Phase 1: Design and Contracts

The full data model is in [data-model.md](data-model.md); the image-source
dependency contract is in
[contracts/open-food-facts-images.md](contracts/open-food-facts-images.md).

### Data model change

`Product` (in `core:domain`) gains two optional fields:

- `imageThumbnailUrl: String?` — small front image for list thumbnails.
- `imageUrl: String?` — larger front image for the detail header.

`ProductResponseMapper` reads the corresponding catalogue fields and coerces
blank/missing values to `null`. `SearchAliciousClient.REQUESTED_FIELDS` gains the
image fields.

### UI structure (androidApp)

```text
androidApp/src/main/kotlin/com/example/nutricionalregister/
├── App.kt                         # applies NutriLogTheme
├── ui/
│   ├── NutriLogScaffold.kt        # app shell: Scaffold + TopAppBar, system-bar insets
│   ├── SectionHeader.kt           # section title + optional trailing action
│   ├── StatRow.kt                 # label/value row; dividers only between rows
│   ├── InfoChip.kt                # labelled pill giving standalone values context
│   └── theme/                     # Color.kt, Type.kt, Shape.kt, Theme.kt, NutriLogSpacing.kt
└── search/
    ├── ProductSearchScreen.kt     # modern search scaffold + result list
    ├── ProductResultCard.kt       # thumbnail + name/brand/quantity + fixed badge slot
    ├── ProductImage.kt            # image + generated placeholder + failure fallback
    ├── NutriScoreBadge.kt         # grade badge (a-e)
    ├── ProductDetailScreen.kt     # hero image, title block, facts, nutrition cards
    ├── SearchOutcomeMessage.kt    # themed state messages
    ├── RecentSearches.kt          # themed recent-search rows
    └── OpenFoodFactsAttribution.kt# data + image attribution
```

Layout requirements established by on-device measurement (see
[research.md](research.md) §4): content MUST clear the system bars, dividers MUST
appear only between rows, and label/value values MUST align in a bounded
right-aligned column.

**Structure Decision**: Keep all styling and composables in `androidApp`; keep
only display-ready data in common code. Do not add a navigation library; the app
root continues to hold the selected-product route.

### Dependency decision

Preferred: reuse the platform/Compose facilities already available. If a
Compose image loader (e.g. Coil 3 / Kamel) is adopted for robust caching and
async decoding, create an ADR recording need, alternatives, binary-size and
build-time impact, licence, and exit strategy before adding a catalog entry.

## Complexity Tracking

No constitution violations. The one external-resource risk (third-party image
bytes and their licence) is bounded by ADR-0009 and the placeholder-on-failure
requirement; no unsupported availability assumption is introduced.
