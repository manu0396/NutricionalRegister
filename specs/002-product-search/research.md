# Product Search Research

**Date**: 2026-10-19
**Feature**: [Product Search](spec.md)
**Plan**: [plan.md](plan.md)

## Decisions

### Full-text catalogue search

- **Decision**: Target the Open Food Facts Search-a-licious full-text endpoint
  at `GET https://search.openfoodfacts.org/search` with the normalized search
  phrase in `q`. This is the maintainer-selected endpoint for F1.
- **Rationale**: Its published OpenAPI describes plain words as full-text
  search across indexed fields. The Open Food Facts API overview identifies
  v3 as the current API and v2 as deprecated; the v2 `/api/v2/search` endpoint
  is for structured filters and does not support the free-text query behavior
  needed by this specification.
- **Alternatives considered**:
  - Open Food Facts v2 `/api/v2/search`: rejected for F1 name search because
    its documented search parameters are tag/nutrient filters, not general
    product-name text search.
  - Open Food Facts legacy `/cgi/search.pl`: not selected; the platform API
    overview includes it in the 10-search-requests/minute/IP limit and warns
    against search-as-you-type. Search-a-licious has a direct full-text
    contract.
  - Open Food Facts v3: recommended by the API overview for new integrations,
    but the available full-text contract found for F1 is Search-a-licious.
    Revisit if Open Food Facts documents a v3 full-text endpoint that satisfies
    the same requirement.
- **Operational evidence review (2026-10-19)**: Reviewed the public
  Search-a-licious OpenAPI, deployment workflow, route and Open Food Facts
  index configuration, Open Food Facts API guide, and terms. The documented
  facts and unresolved service-specific questions are recorded in the
  [Search-a-licious contract](contracts/search-a-licious.md). No Search-specific
  rate limit, incoming User-Agent requirement, registration rule, or live
  response-field guarantee was found. The Open Food Facts 10 requests/minute/IP
  limit is explicitly scoped to Product Opener `/api/v*/search` and
  `/cgi/search.pl`; it must not be applied to Search-a-licious. Public index
  configuration is candidate-field evidence, not a live response guarantee.
  This research closes the documentation-review task only; it is not
  operational approval for production requests.

### Search request and response handling

- **Decision**: Make a request only on explicit submission or selection of a
  recent term, pass the user's supported language list, request only the
  necessary result fields, and cap each response page to a small result set.
- **Rationale**: The published endpoint supports `q`, `langs`, `page_size`,
  `page`, and `fields`; explicit submission avoids unnecessary catalogue
  traffic and matches the F1 interaction.
- **Alternatives considered**: Search-as-you-type was rejected because it
  generates unnecessary requests and the Open Food Facts API documentation
  explicitly warns against it for its v2/legacy search endpoints.
- **Response robustness**: The published `hits` items are generic JSON
  objects. Decode each hit independently; validate required product identity
  and name; treat optional identity/nutrition fields as absent when missing or
  malformed; continue displaying other valid hits. The public repository
  index configuration lists candidate full-text and nutrition fields, but
  does not guarantee the currently deployed index or response nesting. Keep
  requests explicit-submit only, avoid automatic retries, and do not rely on
  fields until validated against an authoritative response fixture.

### Local-first persistence and query races

- **Decision**: Persist products and their query associations separately from
  the recent-term list. Emit a local snapshot immediately, then update it only
  with a successful remote refresh. Use a monotonically increasing request
  identity and cancellation/guarding so an older search cannot publish after
  a newer submission.
- **Rationale**: This directly supports the offline-first acceptance criteria,
  individual history deletion without product deletion, and latest-query-wins.
- **Alternatives considered**: A remote-only search cannot satisfy offline
  results or persistent history. Coupling cached products to history rows
  would make individual history deletion risk deleting still-useful product
  data.

### Technology choices (proposed, subject to ADR review)

Official releases and platform documentation checked on 2026-10-19 support
these candidate versions. They are not yet added to the version catalog.

| Concern | Proposed decision | Rationale and trade-offs |
|---|---|---|
| Concurrency | `kotlinx.coroutines` 1.11.0 and Flow | KMP support fits repository contracts, cancellation, and observable cache updates. Keep coroutine dispatchers and time injectable for deterministic tests. |
| JSON | `kotlinx.serialization` 1.11.0; compiler plugin follows the Kotlin toolchain | KMP-compatible; unknown properties can be ignored, while known optional values need tolerant parsing so one bad field/hit does not fail the whole response. |
| HTTP | Ktor 3.6.0; OkHttp engine on Android, Darwin engine on iOS, MockEngine in tests | KMP client and test engine keep request construction and response mapping shared. Selecting separate platform engines adds artifacts; measure the Android APK and iOS framework after adoption. |
| Local database | SQLDelight 2.4.1 with Android and Native drivers | Typed SQL and generated APIs support query-cache joins and history limits. Generated code and platform drivers add build work and binary size; measure after the first baseline. |
| Dependency injection | Metro 1.4.5 if its compatibility matrix includes Kotlin 2.4.21 | The brief prefers Metro. It supports KMP/Apple targets, but its compiler compatibility must be checked before implementation. Use Koin only after documenting incompatibility and obtaining maintainer review. |
| Android UI verification | AndroidX Compose UI test artifacts, exact stable versions to be recorded in an ADR/catalog | Needed to assert semantics, touch targets, and accessibility-facing labels. Keep platform UI checks separate from shared logic tests. |

The cited project repositories publish Apache-2.0 licenses for the proposed
Ktor, SQLDelight, Metro, kotlinx.coroutines, and kotlinx.serialization
dependencies. Confirm exact artifact metadata when preparing the ADRs. No
reliable project-specific binary-size or build-time delta was found; establish
those measurements before and after dependency adoption rather than estimating
them.

### Product data reuse and presentation

- **Decision**: Store only the product attributes needed for search and detail
  inspection; keep nutrition measures optional and retain their basis/unit.
  Do not show a missing value as zero.
- **Rationale**: Open Food Facts data is community-contributed and may be
  incomplete or inaccurate. Product images have a separate license from the
  database and are not required to satisfy the F1 specification.
- **Operational requirements**: Open Food Facts' general API guide asks
  clients to use a descriptive `AppName/Version (ContactEmail)` User-Agent and
  requests submission of its API-usage form. The sources reviewed do not say
  whether those requirements apply specifically to Search-a-licious. Send a
  descriptive app name/version conservatively, include a contact only if
  maintainers provide one, and do not claim registration is either required
  or complete for this service. Follow Open Food Facts' attribution/reuse
  terms; product images are excluded until their separate rights are reviewed.
- **Alternatives considered**: Treating absent nutrients as zero was rejected
  because it fabricates data. Displaying images by default was rejected for
  this feature because it expands licensing scope without being required.

## Sources

1. Open Food Facts API overview and usage guidance, pinned server documentation
   revision: <https://github.com/openfoodfacts/openfoodfacts-server/blob/f7f60fb43a8fa54d30d748589265a19f4b27d1d3/docs/api/index.md>
   (API version status, limits for documented v2/legacy search, User-Agent,
   staging guidance, usage form, and data reliability caveat).
2. Open Food Facts v2 OpenAPI specification, same revision:
   <https://github.com/openfoodfacts/openfoodfacts-server/blob/f7f60fb43a8fa54d30d748589265a19f4b27d1d3/docs/api/ref/api.yaml>
   (structured search and product response fields).
3. Search-a-licious live OpenAPI:
   <https://search.openfoodfacts.org/openapi.json>
   (GET `/search`, full-text `q`, `langs`, pagination, fields, and generic
   `hits` response items).
4. Search-a-licious staging OpenAPI:
   <https://search.openfoodfacts.net/openapi.json>.
5. Search-a-licious deployment workflow, route, and Open Food Facts index
   configuration, pinned revision:
   <https://github.com/openfoodfacts/search-a-licious/blob/0a2c69c2ca2e306285927a33472c3d8b7066f985/.github/workflows/container-deploy.yml>,
   <https://github.com/openfoodfacts/search-a-licious/blob/0a2c69c2ca2e306285927a33472c3d8b7066f985/app/api.py>,
   <https://github.com/openfoodfacts/search-a-licious/blob/0a2c69c2ca2e306285927a33472c3d8b7066f985/data/config/openfoodfacts.yml>
   (staging/production deployment, root search route, configured full-text
   and product/nutrition fields; configuration is not a live index guarantee).
6. Open Food Facts terms and reuse:
   <https://world.openfoodfacts.org/terms-of-use>
   (database reuse and attribution).
7. Ktor releases and client testing:
   <https://github.com/ktorio/ktor/releases/tag/3.6.0>,
   <https://ktor.io/docs/client-supported-platforms.html>,
   <https://ktor.io/docs/client-testing.html>.
8. SQLDelight release and KMP SQLite setup:
   <https://github.com/sqldelight/sqldelight/releases/tag/2.4.1>,
   <https://github.com/sqldelight/sqldelight/blob/main/docs/multiplatform_sqlite/index.md>.
9. Metro release, multiplatform guide, and compatibility matrix:
   <https://github.com/ZacSweers/metro/releases/tag/1.4.5>,
   <https://github.com/ZacSweers/metro/blob/main/docs/multiplatform.md>,
   <https://github.com/ZacSweers/metro/blob/main/docs/compatibility.md>.
10. Kotlin serialization and coroutines releases:
   <https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.11.0>,
   <https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.11.0>.
11. Koin KMP fallback reference:
   <https://github.com/InsertKoinIO/koin/releases/tag/4.2.2>,
   <https://insert-koin.io/docs/reference/koin-core/kmp-setup/>.
