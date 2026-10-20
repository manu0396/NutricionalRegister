# Tasks: Modern UI Refresh with Product Images

**Feature**: `003-ui-refresh` | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

Legend: `[P]` = can run in parallel with other `[P]` tasks in the same phase.

## Phase 1: Documentation and decisions

- [X] T057 Record the product-image decision (source, licence, attribution,
  alternatives, exit strategy) in `docs/adr/0009-product-images-open-food-facts.md`.
- [X] T058 Write the feature spec `specs/003-ui-refresh/spec.md`.
- [X] T059 Write the implementation plan `specs/003-ui-refresh/plan.md`.
- [X] T060 Update `README.md` to replace the "images are excluded" statement with
  the image-and-attribution policy; note the redundant `specs/002-product-search`
  statement is superseded by ADR-0009.
- [X] T061 If a Compose image-loading library is adopted, record its ADR
  (need, alternatives, size/build impact, licence, exit strategy) before adding a
  version-catalog entry.

## Phase 2: Shared model and mapping

- [X] T062 [P] Add optional image URL fields to `Product` in
  `core/domain/.../product/Product.kt` with tests in the domain module.
- [X] T063 [P] Extend `ProductResponseMapper` to read the thumbnail and front
  image fields and coerce blank values to `null`; add mapper tests for present,
  absent, and blank values.
- [X] T064 Add the image fields to `SearchAliciousClient.REQUESTED_FIELDS`.

## Phase 3: Theme

- [X] T065 Add the `androidApp` theme (`ui/theme/Color.kt`, `Type.kt`,
  `Shape.kt`, `Theme.kt`) and apply it in `App.kt`.

## Phase 4: Modern UI

- [X] T066 Build the reusable product image composable with placeholder and
  failure fallback; expose accessible content descriptions.
- [X] T067 Build the Nutri-Score badge composable for supported grades.
- [X] T068 Redesign `ProductResultCard` with a thumbnail, typography hierarchy,
  and Nutri-Score badge.
- [X] T069 Redesign `ProductSearchScreen` (themed search surface, result list,
  spacing) while preserving behavior and test tags.
- [X] T070 Redesign `ProductDetailScreen` (image header, facts, nutrition cards,
  badge, back affordance). [P] Restyle `SearchOutcomeMessage`, `RecentSearches`,
  and `OpenFoodFactsAttribution` for image attribution.

## Phase 5: Verification

- [X] T071 Add instrumented Compose tests for image-present, image-absent
  (placeholder), and Nutri-Score badge; keep the existing 7 UI tests green.
- [X] T072 Re-run the full quickstart validation (clean + incremental
  verification, coverage gate, ktlint, architecture tests, UI tests, release APK,
  XCFramework, xcodebuild) and record the results in `AI-LOG.md`.
- [X] T073 Re-measure artifact sizes and hosted PR duration; record deltas in
  ADR-0009 and `AI-LOG.md`. (Hosted CI duration remains pending T056.)

## Phase 6: Post-review UI fixes (2026-10-20)

- [X] T074 Diagnose live Search-a-licious field shapes; fix mapper to read
  `brands`/`quantity` as string-or-array and normalize `"unknown"` Nutri-Score to
  null (+ mapper tests).
- [X] T075 Replace the full-width detail back button with a compact back bar
  (48 dp `IconButton`, `contentDescription = "Back to search"`, screen title).
- [X] T076 Format nutrient amounts to remove floating-point noise
  (`formatAmount`, unit-tested).
- [X] T077 Show a single graceful message when a product has no nutrition values;
  keep per-value "Unavailable" only for partially-missing data.
- [X] T078 Render a deterministic generated gradient placeholder for every
  product that has no catalogue image (stable per product code).
- [X] T079 Re-run instrumented tests (20/20), full clean verification, release
  APK, XCFramework, and iOS build; record results in ADR-0010 and AI-LOG.

## Phase 7: Professional design-system refactor (2026-10-20)

- [X] T080 Add reusable app shell (`ui/NutriLogScaffold.kt`) with a Material 3
  `TopAppBar` so content clears the system bars (was drawing under the status bar).
- [X] T081 Add shared UI primitives: `NutriLogSpacing` scale, `SectionHeader`,
  `InfoChip`, `StatRow`/`StatRowDivider`.
- [X] T082 Fix the detail divider pattern (dividers only between rows; none between
  a heading and its first row).
- [X] T083 Right-align `StatRow` values into a clean value column; label the
  Nutri-Score chip; replace the em-dash empty-state tile with an accent rule.
- [X] T084 De-duplicate Brand/Quantity (title block only) and reserve a fixed badge
  slot so result-card right edges align; detail hero to 4:3.
- [X] T085 Verify on device (status-bar clearance, value column, empty state),
  re-run instrumented tests (20/20), clean verification, release APK, XCFramework,
  iOS build; record in ADR-0010 and AI-LOG.

## Phase 8: Documentation completion (2026-10-20)

- [X] T086 Write `specs/003-ui-refresh/research.md`: image-source survey with
  rejected options, Android image-loader survey, live-verified field shapes,
  measured presentation findings, accessibility/privacy findings, open questions.
- [X] T087 Write `specs/003-ui-refresh/data-model.md`: the `Product` image-field
  change, mapping table, persistence decision (no schema change), UI component
  inventory, and requirement traceability.
- [X] T088 Write `specs/003-ui-refresh/quickstart.md`: build, verification gate,
  instrumented tests, iOS check, manual walkthrough, diagnostics, troubleshooting.
- [X] T089 Write `specs/003-ui-refresh/checklists/requirements.md` and the
  image-source contract `specs/003-ui-refresh/contracts/open-food-facts-images.md`.
- [X] T090 Update `specs/003-ui-refresh/plan.md` to link the new research, data
  model and contract documents and to record the measured layout requirements.
- [X] T091 Mark the spec status as implemented and confirm all 003 docs are
  consistent with the shipped code.

## Phase 9: Material icons (2026-10-20)

- [X] T092 Write `docs/adr/0011-compose-material-icons.md` recording the need
  (typographic glyphs instead of designed icons), the dependency choice,
  alternatives, licence, size/build impact and exit strategy.
- [X] T093 Add `androidx.compose.material:material-icons-core` (pinned in the
  version catalog) to `androidApp`; verify the JetBrains mirror does not resolve
  at the pinned Compose line and record that finding in the ADR.
- [X] T094 Replace the `←`, `✕` and `⌕` text glyphs in `ProductDetailScreen` and
  `RecentSearches` with `Icons.AutoMirrored.Filled.ArrowBack`, `Icons.Filled.Close`
  and `Icons.Filled.Search`, preserving content descriptions and 48 dp targets.
- [X] T095 Re-run instrumented tests (20/20), clean verification (exit 0), release
  APK, XCFramework (unchanged) and iOS build; record measured sizes in ADR-0011.
