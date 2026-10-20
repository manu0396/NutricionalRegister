# Open Food Facts Product Image Contract

**Status**: Public documentation reviewed on 2026-10-20. Field availability was
verified against a live response; licence terms were read from the published
terms of use. Presence per product is **not guaranteed** and this document does
not constitute permission for production traffic.
**Plan**: [../plan.md](../plan.md)

## Service

- **Code endpoint (primary, unchanged)**: `GET https://search.openfoodfacts.org/search`
  — the catalogue search already used by feature 002.
- **Image origin (delivered URLs)**: `https://images.openfoodfacts.org/...`
- **Purpose**: display a representative photo of the product the user selected.
- **Image dataset reference**: <https://registry.opendata.aws/openfoodfacts-images>

The image URL is **discovered through the existing search request**, not by a
separate lookup. Because Search-a-licious can return the image fields in the same
response, showing images adds **no additional catalogue round trip**.

## Request

The app does not construct image URLs itself. It requests two additional
document fields on the existing search call via the `fields` parameter:

| Field | Use |
|---|---|
| `image_front_small_url` | Small front image for list result-card thumbnails. |
| `image_front_url` | Larger front image for the product-detail header. |

Field names must be re-verified against the endpoint if the catalogue changes
them; they are not part of a versioned, guaranteed response schema.

## Response Shape

Verified live on 2026-10-20 against `GET /search?q=oat%20drink`:

```json
{
  "hits": [
    {
      "code": "5411788041157",
      "product_name": "Oat drink natural",
      "image_front_url": "https://images.openfoodfacts.org/images/products/541/178/804/1157/front_nl.77.400.jpg",
      "image_front_small_url": "https://images.openfoodfacts.org/images/products/541/178/804/1157/front_nl.77.200.jpg"
    },
    {
      "code": "7394376621482",
      "product_name": "Barista Edition Oat Drink"
    }
  ]
}
```

Key observations:

- Products **with** a photo carry both image URL fields.
- Products **without** a photo omit them entirely (second hit above). This is
  normal catalogue state, not an error.

### Related field shapes (verified live, same call)

| Field | Observed shape | Handling |
|---|---|---|
| `brands` | JSON **array**, e.g. `["Nutella"]` | Join array members with `", "`; also accept a plain string |
| `quantity` | String or array; often `null` | Same string-or-array handling; blank → `null` |
| `nutriscore_grade` | `"e"`, `"a"`, … or the literal `"unknown"` | Trim + lowercase; blank and `"unknown"` → `null` |
| `nutriments` | Object of doubles, e.g. `"proteins_100g": 7.6899999999999995` | Format for display to ≤2 decimals without a trailing `.0` |

## Client Obligations

1. **Optional by contract.** The app MUST treat image URLs as optional. A missing
   URL is not an error and MUST NOT produce an error or broken-image state.
2. **Placeholder on absence or failure.** When a URL is absent, blank, or the
   download fails, the UI MUST render the generated placeholder instead
   (a deterministic tile derived from the product identity).
3. **Never block content.** Image work MUST NOT delay, block, or replace product
   name, brand, quantity, code, or nutrition values. Text content is shown first;
   images fill in.
4. **Attribution.** Because product images are **CC BY-SA**, the existing Open
   Food Facts attribution MUST be present wherever images are shown, MUST credit
   Open Food Facts for images as well as data, MUST link to the catalogue, and
   MUST remain a 48 dp minimum touch target.
5. **Privacy.** Image requests MUST carry no user identity beyond the app
   User-Agent. No analytics, tracking, or search-term logging is permitted.
6. **No tests against the live CDN.** Automated tests MUST use a fake image
   loader or local fixtures. Live image traffic is confined to interactive use.

## Licence

Open Food Facts publishes (<https://world.openfoodfacts.org/terms-of-use>):

- Database: Open Database License (ODbL).
- Individual contents: Database Contents License (DbCL).
- **Product images: Creative Commons Attribution ShareAlike (CC BY-SA).**

Reuse therefore requires attribution and share-alike. This app satisfies
attribution by crediting Open Food Facts for data and images with a link to the
catalogue. It does not modify the images beyond proportional scaling for display,
so it creates no derivative works of the images. Full analysis and exit strategy
in [ADR-0009](../../../docs/adr/0009-product-images-open-food-facts.md).

## Caching Behaviour

- Image bytes are fetched by the Android image loader (Coil 3.6.3, see
  [ADR-0010](../../../docs/adr/0010-compose-image-loading.md)), which provides an
  in-memory cache and a disk cache.
- Image URLs are **transient** and are *not* persisted in the product cache, so
  no SQLDelight migration is required.
- Offline, already-cached images render normally; images that are not cached fall
  back to the generated placeholder. Offline-first search behaviour is otherwise
  unchanged.

## Exit Strategy

Remove the two image fields from the request, delete the image composable and the
image clause of the attribution, and restore text-only result and detail
rendering. No persisted schema change is involved because image URLs are never
stored.
