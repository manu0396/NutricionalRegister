# Product Search Data Model

**Feature**: [Product Search](spec.md)
**Plan**: [plan.md](plan.md)

## Persisted Records

### Product

Represents one catalogue product that is locally available for result display
and detail inspection.

| Field | Type | Rules |
|---|---|---|
| `code` | String | Required opaque catalogue identifier; primary key. Do not parse it as a number because leading zeroes may be meaningful. |
| `name` | String | Required non-blank display name for a valid search result. A hit without a usable name is excluded without discarding other hits. |
| `brands` | String? | Optional identifying text. |
| `quantity` | String? | Optional catalogue quantity text; do not infer or convert units. |
| `nutritionValues` | Collection of NutritionValue | Zero or more independently optional measures. Missing or malformed measures remain unavailable, not zero. |
| `cachedAtEpochMillis` | Long | Required injected-clock time of the latest successful local update. |

The F1 mapper requests only identifying and nutrition fields needed by search
and product details. Product images are not persisted or rendered in this
feature because their licensing differs from the database licence.

### NutritionValue

One nutrient value as supplied by the catalogue.

| Field | Type | Rules |
|---|---|---|
| `nutrientKey` | String | Required stable key, for example energy, protein, carbohydrate, or fat. |
| `amount` | Decimal-compatible number | Required finite source value. A missing or malformed value is represented by the absence of that nutrient record, never by a fabricated zero. A genuine source value of zero remains valid. |
| `unit` | String? | Preserve the source unit when known; do not convert in F1. |
| `basis` | Enum | `PER_100_G`, `PER_SERVING`, or `UNKNOWN`. |

Values are descriptive only; F1 does not calculate serving totals or diary
macros. The detail UI labels the basis and unit for each available value and
marks absent values as unavailable.

### SearchSnapshot

Represents the latest successfully fetched catalogue response for a normalized
term. A snapshot can have zero results, which distinguishes a known online
no-match response from a query that has never been fetched.

| Field | Type | Rules |
|---|---|---|
| `normalizedTerm` | String | Required, non-empty trimmed query; part of the primary key. |
| `fetchedAtEpochMillis` | Long | Required injected-clock time of the last successful remote response. |
| `resultCodes` | Ordered collection of Product.code | May be empty. Each code refers to a cached Product; preserve server relevance order. |

A refresh transaction updates the snapshot and its ordered product links
together. A failed refresh does not erase a usable prior snapshot.

### RecentSearch

One locally persisted search-history entry, independent from product-cache
records.

| Field | Type | Rules |
|---|---|---|
| `normalizedTerm` | String | Required trimmed query; unique history key. |
| `displayTerm` | String | Required non-blank trimmed form displayed in history. |
| `lastCompletedAtEpochMillis` | Long | Required injected-clock completion time. |
| `completionSequence` | Long | Required monotonically increasing persisted sequence assigned when a submitted search completes; determines stable most-recent order and breaks timestamp ties. |

Every completed non-blank submitted search updates history, including
successful matches, successful no-results, and recoverable failures. Blank
submissions do not create history. Upsert a repeated normalized term to the
newest position and retain at most 10 entries. Order by
`completionSequence DESC`; `lastCompletedAtEpochMillis DESC` may be used as a
secondary presentation check but must not override sequence order. Removing a
`RecentSearch` must not delete a `Product` or `SearchSnapshot`.

## In-Memory Presentation State

### SearchScreenState

The immutable state owned by the shared search presentation logic includes:

- Current editable query and selected product code, if any.
- Current request identity, used to prevent stale requests from changing the
  visible state.
- An explicit phase: initial guidance, cached results refreshing, remote
  results, no results, offline with cached products, offline without cached
  products, or recoverable service failure.
- Ordered products, source/freshness indication, and an actionable retry/edit
  message where appropriate.
- Recent searches in most-recent order.

An empty local snapshot while an online refresh is pending is not presented as
the final no-results outcome. Only the latest submitted request may publish a
screen update.

## Relationships and Lifecycle

```text
RecentSearch                (independent, max 10)
    normalizedTerm

SearchSnapshot
    normalizedTerm ── ordered Product.code references ── Product
    fetchedAtEpochMillis                              cachedAtEpochMillis
```

- Search history and product results are stored locally and never sent to
  analytics or a tracking service.
- A network success replaces the snapshot for that term and upserts each
  independently valid product.
- A malformed product hit does not fail the entire response; skip a hit only
  when required identity/name data is unusable, and retain other valid hits.
- A network failure preserves any existing snapshot and marks its products as
  potentially stale.
- Assign a persisted, strictly increasing completion sequence to each history
  update so searches completing in the same millisecond remain deterministically
  ordered.
- Deleting history removes only its history row. Cached products remain
  available for other query snapshots and future offline use.
- A superseded response cannot commit a snapshot or publish visible state. A
  newer request for the same term must never be overwritten by an older
  response.
