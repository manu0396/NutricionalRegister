# Search-a-licious Search Contract

**Status**: Public documentation reviewed on 2026-10-19. Search-a-licious
specific operational limits and a live response-field guarantee are not
published in the sources reviewed; this evidence does not constitute service
approval or permission for production traffic.
**Plan**: [../plan.md](../plan.md)

## Service

- **Origin**: `https://search.openfoodfacts.org`
- **Operation**: `GET /search`
- **Purpose**: Full-text product search for the explicit user-submitted phrase.
- **Specification**: <https://search.openfoodfacts.org/openapi.json>

The published API describes `q` as supporting Lucene query syntax and plain
words as full-text terms searched across fields. The app sends the normalized
user phrase as a query parameter and URL-encodes it using the HTTP client.

## Request Parameters

| Parameter | Use |
|---|---|
| `q` | Required by the application; trimmed non-empty search phrase. The API schema permits omission, but F1 does not. |
| `langs` | Supported user-language list, serialized as documented by the endpoint. Do not assume English-only locale behavior. |
| `page_size` | Bounded result count suitable for a mobile screen and a small local cache. |
| `page` | First page for the initial F1 search. Pagination behavior is outside F1 unless later specified. |
| `fields` | Restrict returned document fields to the identifiers, display details, and nutrition data needed by F1. Confirm exact indexed field names and response nesting before coding. |

No query is sent for a blank/whitespace-only term. Submit only on explicit
search or selection of a saved recent term; do not implement search-as-you-type.

## Response Shape

The published OpenAPI describes a successful response with:

- `hits`: an array of generic JSON objects (the schema does not type individual
  product fields);
- `page`, `page_size`, `page_count`, `count`, `is_count_exact`, `took`, and
  `timed_out`;
- optional `warnings`, facets, aggregations, charts, and debug information.

An error response can contain an `errors` array with a title and optional
description. Invalid requests may return HTTP 422 with validation details.
The API's generic `hits` schema does not guarantee exact field paths or the
deployed product-index mapping. The public Search-a-licious Open Food Facts
index configuration identifies these candidate full-text fields: `code`,
`product_name`, `generic_name`, `categories`, `labels`, `brands`, and
`emb_codes`. It also configures additional fields including `quantity`,
`product_quantity`, `product_quantity_unit`, `nutrition_data_per`,
`nutriscore_grade`, and selected nested `nutriments` values. The configuration
is evidence of the repository's intended index, not a guarantee that a live
deployment currently indexes or returns every field. The OpenAPI's generic
hit schema leaves response nesting and exact result shape unspecified. Treat
all these fields as candidates until verified against an authoritative live
fixture or service guarantee; keep mapper tests fixture-backed and tolerant.

## Mapping and Failure Rules

- Parse the response envelope and validate each hit independently.
- Require a usable product identifier and non-blank product name to present a
  result. Skip an invalid hit without discarding other valid hits.
- Treat absent, malformed, non-finite, or otherwise unusable nutrition values
  as unavailable; never coerce them to zero.
- Preserve nutrient unit and basis when supplied. Do not calculate diary
  quantities or totals.
- Preserve result order from the service.
- A successful empty `hits` array is a no-results response.
- Network failures, HTTP 429/5xx, timeouts, invalid response data, and service
  errors become recoverable outcomes; do not retry without a bounded policy.
- A failed refresh must not erase previously cached products for the query.
- The presentation layer must guard against a late response updating the
  visible state for a newer submitted query.

## Evidence-reviewed request policy and limitations

Official public sources reviewed:

| Topic | Authoritative evidence | Limitation and policy |
|---|---|---|
| Endpoint | Search-a-licious publishes a root `GET /search` and `POST /search`; its deployment source configures `.org` for production and `.net` for staging. | This is not Product Opener `/api/v1/search`. The Search-a-licious docs do not explicitly state a client staging requirement. Use mocks in automated tests; confirm the target environment before deployment. |
| Terms and reuse | Open Food Facts terms describe the database as ODbL and individual contents as DBCL, require attribution/linking for reuse, and note possible third-party rights and service interruption. | No separate Search-a-licious client terms were found. Preserve attribution and data-license review; F1 excludes images. |
| Rate limits | The API overview states 10 search requests/minute/IP for Product Opener `/api/v*/search` and `/cgi/search.pl`. | No Search-a-licious-specific numerical limit was found. Do not apply the Product Opener limit to this endpoint or infer an unlimited quota. |
| User-Agent | The general API guide asks API clients to send `AppName/Version (ContactEmail)` and describes no read authentication beyond the custom header. | Search-a-licious does not document an incoming client User-Agent requirement. Send a descriptive app name/version conservatively; only include a project contact address if maintainers provide one. |
| Registration | The general API guide requests developers to read the terms and submit its API-usage form. | Whether that form or any registration is required specifically for Search-a-licious is not established. Do not claim registration is complete or unnecessary. |
| Fields | The public Open Food Facts Search-a-licious index configuration identifies candidate search fields and configured product/nutrition fields (see above). | OpenAPI hits are generic objects; public index configuration is not a guarantee of live deployment content or response nesting. Confirm representative response shape before relying on fields. |

Until Search-a-licious-specific operational requirements and a representative
response are confirmed, keep requests explicit-submit only, do not implement
search-as-you-type or automatic retries, use a descriptive User-Agent without
inventing contact details, and handle 429/5xx/errors as recoverable outcomes.
This conservative behavior is not a substitute for a published quota or
service approval. Do not send production traffic based solely on this public
documentation review.

The Open Food Facts API documentation warns that product records are
community-contributed and may be inaccurate, incomplete, or unreliable.
Nutrition details must be presented as catalogue information, not medical
advice.

## Contract Sources

- Search-a-licious OpenAPI: <https://search.openfoodfacts.org/openapi.json>
- Search-a-licious staging OpenAPI: <https://search.openfoodfacts.net/openapi.json>
- Search-a-licious deployment workflow, pinned revision:
  <https://github.com/openfoodfacts/search-a-licious/blob/0a2c69c2ca2e306285927a33472c3d8b7066f985/.github/workflows/container-deploy.yml>
- Search-a-licious route and public index configuration, pinned revision:
  <https://github.com/openfoodfacts/search-a-licious/blob/0a2c69c2ca2e306285927a33472c3d8b7066f985/app/api.py>
  and
  <https://github.com/openfoodfacts/search-a-licious/blob/0a2c69c2ca2e306285927a33472c3d8b7066f985/data/config/openfoodfacts.yml>
- Open Food Facts API overview, pinned documentation revision:
  <https://github.com/openfoodfacts/openfoodfacts-server/blob/f7f60fb43a8fa54d30d748589265a19f4b27d1d3/docs/api/index.md>
- Open Food Facts terms of use:
  <https://world.openfoodfacts.org/terms-of-use>
