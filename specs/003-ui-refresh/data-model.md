# Data Model: Modern UI Refresh with Product Images

**Feature**: `003-ui-refresh` | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

**Date**: 2026-10-20

## Scope of the model change

This feature is a presentation refresh. Its only data-model change is the
addition of **two optional image URL fields** to the existing `Product` domain
type, plus the mapping that populates them from the catalogue response. There is
**no schema change**: the image URLs are transient catalogue fields and are not
persisted, so no SQLDelight migration is required.

## Changed entity: Product

`Product` (in `core:domain`, common source set) gains two nullable fields. All
existing fields are unchanged, and the constructor gains defaults so existing
call sites and tests remain source-compatible.

| Field | Type | Required | Notes |
|---|---|---|---|
| `code` | `String` | yes | Existing. Catalogue barcode; opaque, non-blank. |
| `name` | `String` | yes | Existing. Non-blank; the product's display name. |
| `brands` | `String?` | no | Existing. Now populated from a **string-or-array** catalogue field joined with `", "`. |
| `quantity` | `String?` | no | Existing. Now populated from a string-or-array field. |
| `nutriScoreGrade` | `String?` | no | Existing. Normalized to lowercase; blank and `"unknown"` become `null`. |
| `nutritionDataPer` | `String?` | no | Existing. Declared basis label from the catalogue. |
| `nutritionValues` | `List<NutritionValue>` | yes (possibly empty) | Existing. Empty list means "no nutrition data known". |
| `imageThumbnailUrl` | `String?` | **new, optional** | Small front image for list thumbnails (`image_front_small_url`). `null`/blank when the catalogue has no photo. |
| `imageUrl` | `String?` | **new, optional** | Larger front image for the detail header (`image_front_url`). `null`/blank when absent. |

### Invariants

- `code` is non-blank and opaque; it is the identity used for de-duplication and
  is also the value embedded in the image URL path, so no reconciliation between
  product identity and image identity is needed.
- `name` is non-blank (an existing invariant; hits without a usable name are
  skipped by the mapper).
- Image URL fields are **display-only and never required**. Absence is normal:
  many catalogue entries have no photo. Nothing in the domain layer may assume an
  image exists.
- Image URL fields are **never persisted**. They live only on the in-memory
  `Product` carried through search results and the selected product.
- Nutrition continues to represent missing values by **absence**, never by a
  fabricated zero.

## Mapping: catalogue response → Product

`ProductResponseMapper` (in `core:data`) reads the image fields and coerces
blank/missing values to `null`. Alongside the image change, the mapping was
corrected against the live endpoint (see [research.md](research.md) §3):

| Catalogue field | Product field | Handling |
|---|---|---|
| `image_front_small_url` | `imageThumbnailUrl` | Blank/missing → `null` |
| `image_front_url` | `imageUrl` | Blank/missing → `null` |
| `brands` | `brands` | String **or** array of strings; arrays joined with `", "`; blank → `null` |
| `quantity` | `quantity` | String **or** array; blank → `null` |
| `nutriscore_grade` | `nutriScoreGrade` | Trimmed + lowercased; blank and `"unknown"` → `null` |

`SearchAliciousClient.REQUESTED_FIELDS` is extended with the two image fields so
they arrive in the same request. The number of catalogue round trips does not
increase.

## Unchanged entity: NutritionValue

No change. Retained for context:

- `nutrientKey`: stable key (`energy`, `protein`, `carbohydrate`, `fat`, `fiber`,
  `sugars`, `salt`, `sodium`).
- `amount`: the catalogue's numeric value.
- `unit`: e.g. `g`, `mg`, `kcal`.
- `basis`: `PER_100_G`, `PER_SERVING`, or `UNKNOWN`.

Display formatting (`formatAmount`) is a **presentation** concern and lives in
`androidApp`, not in the domain type: catalogue doubles such as
`7.6899999999999995` are rounded to at most two decimals and lose a trailing
`.0` before display.

## Persistence

**No schema change.** The existing SQLDelight tables cache products, search
snapshots, and recent-search history. Image URLs are deliberately excluded:

- The columns already cached (identity, name, brand, quantity, Nutri-Score,
  nutrition) are unchanged.
- Image URLs are re-read from the catalogue on the next search. Because the
  platform image cache (Coil's memory + disk cache) holds decoded bytes,
  re-searching does not re-download images.
- If offline images are ever required, the exit path is a forward migration
  adding URL columns (or an image-bytes table keyed by URL with a bounded size).

This keeps offline-first search behaviour, latest-query-wins, and recent-history
semantics completely unchanged.

## UI components introduced (androidApp)

Not persisted, but part of the feature's structure:

| Component | Responsibility |
|---|---|
| `ui/theme/{Color,Type,Shape,Theme}.kt` | Material 3 brand palette, type scale, shape scale; applied in `App.kt`. |
| `ui/theme/NutriLogSpacing.kt` | Spacing scale (4/8/12/16/24/32 dp) replacing magic numbers. |
| `ui/NutriLogScaffold.kt` | App shell: Material 3 `Scaffold` + `TopAppBar`, consumes system-bar insets. |
| `ui/SectionHeader.kt` | Section title with optional trailing action. |
| `ui/StatRow.kt` / `StatRowDivider` | Label/value row with a right-aligned value column; dividers only between rows. |
| `ui/InfoChip.kt` | Labelled pill giving standalone values (e.g. Nutri-Score) context. |
| `search/ProductImage.kt` | Image with a deterministic generated placeholder and failure fallback. |
| `search/NutriScoreBadge.kt` | Colour-coded a–e badge; renders nothing for unsupported grades. |
| `search/ProductResultCard.kt` | Thumbnail + name/brand/quantity + fixed badge slot. |
| `search/ProductDetailScreen.kt` | Hero image, title block, labelled badge, info card, nutrition card. |

## Traceability to requirements

| Requirement | Model/surface |
|---|---|
| FR-001 card image area | `Product.imageThumbnailUrl` → `ProductImage` in `ProductResultCard` |
| FR-002 detail image header | `Product.imageUrl` → `ProductImage` in `ProductDetailScreen` |
| FR-003 placeholder, no error state | `ProductImage` placeholder + `AsyncImagePainter.State.Error` fallback |
| FR-004 images never block content | `ProductImage` paints placeholder first; text renders independently |
| FR-005 Nutri-Score indicator | `nutriScoreGrade` → `NutriScoreBadge` inside `InfoChip` |
| FR-006 missing nutrition not zero | `nutritionValues` absence → "Unavailable" / empty-state message |
| FR-007 single consistent theme | `ui/theme/*` applied in `App.kt` |
| FR-010 attribution credits images | `OpenFoodFactsAttribution` |
| FR-012 existing search behaviour preserved | No schema change; identity/latest-query-wins/recent-history untouched |
