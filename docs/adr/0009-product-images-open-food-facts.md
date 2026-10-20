# ADR-0009: Product Images from the Open Food Facts Catalogue

- **Status**: Accepted
- **Date**: 2026-10-20
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T057-T070; `specs/003-ui-refresh/`

## Context and need

The initial Product Search UI intentionally rendered text only and downloaded
no catalogue images because their per-product licences had not been reviewed
(see the *Product search* section of `README.md` and the constraint in
`specs/002-product-search/plan.md`). Search results and the product-detail
view are therefore hard to scan: users cannot visually identify a product among
similar matches, and the detail view only lists raw nutrition rows.

A modern detail view needs a representative product image. Reusing product
images also keeps the feature within the catalogue the app already queries:
the product identifiers (`code`) returned by Search-a-licious are the same
Open Food Facts barcodes used for the image URLs, so no second product
identity or reconciliation step is introduced.

Two questions must be answered before images can be shown:

1. Are catalogue images licensed for reuse in this app?
2. Which image source is the most capable fit, and what attribution and
   caching behavior does it require?

## Decision

Display product images that the Open Food Facts catalogue already publishes,
using the image URL fields returned for each product:

- `image_front_small_url` for list thumbnails (small payload, grid/list use).
- `image_front_url` (or the largest available front image URL) for the
  product-detail header.

These URLs point at the Open Food Facts image CDN; the app fetches and caches
them itself. The image URL fields are added to the existing Search-a-licious
`fields` request parameter so the number of catalogue round trips does not
increase.

Attribution is extended, not replaced: the existing Open Food Facts
attribution on the search screen and detail view continues to credit the data
source and now also covers images. Because product images are CC-BY-SA, the
attribution text explicitly credits Open Food Facts for images and links to the
catalogue. The attribution remains a 48 dp minimum-touch-target control.

Images are optional. If a product has no image URL, or the download fails, the
UI renders a neutral placeholder (brand/name initial or a food glyph) rather
than a broken image or an error state. Image loading must never block, delay,
or replace search results or nutrition data.

## Alternatives considered

- **Generic stock-photo search APIs (Unsplash, Pexels) or food-category
  datasets (Foodish).** Rejected: images would not depict the actual product,
  which is misleading in a nutrition app, and they add a second vendor, API
  key, network dependency, and licence record for no accuracy gain.
- **Generate placeholder art only (no network images).** Rejected: meets none
  of the user's stated requirement to connect an image API and identify
  products visually.
- **Continue excluding images.** Rejected by the maintainer decision on
  2026-10-20; the licence question is now answered by Open Food Facts'
  published terms (see *Licence*).
- **Bundle images into the app.** Rejected: the catalogue is too large and
  changes continuously; it would bloat the APK and XCFramework and require an
  export pipeline.

## Binary-size impact

No new runtime dependency is required to fetch images if the platform HTTP
stack is reused. The image loader choice is recorded separately in the
implementation plan; a Compose image-loading library, if adopted, is a new
runtime dependency and requires its own size measurement and licence record
before it is added to the version catalog. Images are downloaded at runtime and
cached on device, so they do not materially change APK/XCFramework size beyond
any image-loader code. Measure the release APK and XCFramework before and after
adoption and record the deltas in the validation record below.

## Build-time impact

Adding fields to the existing request and mappings does not add build steps.
If an image-loading library is added, measure clean and incremental Gradle
duration before and after and confirm the hosted PR pipeline remains under the
10-minute target.

## Licence

Open Food Facts publishes three licences
(<https://world.openfoodfacts.org/terms-of-use>):

- Database: Open Database License (ODbL).
- Individual contents: Database Contents License (DbCL).
- **Product images: Creative Commons Attribution ShareAlike (CC BY-SA)**, and
  they "may contain graphical elements subject to copyright or other rights,
  that may in some cases be reproduced (quotation rights or fair use)".

Reuse requires attribution and share-alike. This app satisfies attribution by
crediting Open Food Facts for both data and images with a link to the catalogue
on the surfaces that show the content. The app does not modify the images
(beyond proportional scaling for display), so it creates no derivative works of
the images; where it does combine catalogue data, the ODbL share-alike
obligation is respected by keeping Open Food Facts-derived data separate from
app-owned data and by not relicensing it.

Authoritative sources:

- Terms of use: <https://world.openfoodfacts.org/terms-of-use>
- API documentation: <https://openfoodfacts.github.io/documentation/docs/Product-Opener/api>
- Images dataset (AWS Open Data): <https://registry.opendata.aws/openfoodfacts-images>

## Exit strategy

Remove the image URL fields from the request/mapping and delete the image
composable(s) and attribution clause covering images. Restore the text-only
search and detail rendering. No persisted schema changes are required if image
URLs are treated as transient catalogue fields; if URLs are cached, drop the
column/table in a forward migration.

## Consequences

- Products become visually identifiable in results and detail views.
- The app now fetches third-party image bytes from a CDN; failures must degrade
  to a placeholder and never affect search or nutrition data.
- The attribution control must remain visible wherever images are shown, so it
  can no longer be removed from the search/detail layout.
- The "images are excluded" statements in `README.md` and
  `specs/002-product-search/plan.md` become stale and must be updated.
- Hosted PR duration and artifact sizes need re-measurement after integration.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-20
- **Rationale or conditions**: Approved reusing Open Food Facts catalogue image
  URLs (CC-BY-SA) with explicit image attribution and placeholder-on-failure
  behavior. Any image-loading library must be recorded in its own ADR before
  being added to the version catalog.

## Validation record

- **Final licences (verified 2026-10-20)**: Open Food Facts product images are
  published under CC BY-SA per the terms of use; the database is ODbL and
  individual contents are DbCL.
- **Field availability**: image URL fields are served by the Open Food Facts
  product/image schema; presence per product is not guaranteed, so the UI must
  treat images as optional.
- **Measured sizes (2026-10-20, after integration)**: unsigned Android release
  APK 11,365,002 bytes and debug APK 15,693,131 bytes (up ≈3.0%/2.8% from the
  002 baseline of 11,035,730/15,265,427); `Shared.xcframework` release 92,008 KB
  (effectively unchanged, since images are Android-only). The image integer
  fields add no separate artifact impact beyond the Android UI/loader recorded
  in ADR-0010.
- **Validation run (2026-10-20)**: the full clean verification (unit tests,
  architecture tests, `coverageVerify`, `kotlinLint`, `assembleDebug`, lint)
  exited 0; `xcodebuild` succeeded with 0 errors; 14 of 14 instrumented Compose
  tests passed (7 pre-existing + 7 new image/badge/placeholder tests). Live
  image requests remain confined to non-test usage.
