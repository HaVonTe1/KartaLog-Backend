---
phase: 01-5-series-data
plan: 01
subsystem: scraper/cardmarket
tags: [scraper, series, cardmarket]
dependency_graph:
  requires: []
  provides: [SCRAP-02, SCRAP-03]
  affects: [Product, API]
tech_stack:
  added: [SeriesDto]
  patterns: [series scraping from product page]
key_files:
  created: []
  modified:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/Dtos.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketProductMapper.kt
decisions: []
metrics:
  duration: 2 minutes
  completed_date: "2026-04-18T17:50:00Z"
---

# Phase 1.5 Plan 01: Series Data Summary

## One-Liner

CardMarket scraper now extracts and maps series information from product pages.

## Objective

Extend CardMarket scraping to include series information for products.

## Completed Tasks

| Task | Name | Status |
|------|------|--------|
| 1 | Extend DTOs with series field | ✓ |
| 2 | Parse series from CardMarket HTML | ✓ |
| 3 | Map series in CardMarketProductMapper | ✓ |
| 4 | Verify series flows to API | ✓ |

## Implementation Details

### DTOs Extended

- Added `SeriesDto` data class with: `seriesId: Long`, `name: String`, `languageCode: String`
- Extended `CardmarketProductGallaryItemDto` with optional `series: SeriesDto?`
- Extended `CardmarketProductDetailsDto` with optional `series: SeriesDto?`

### Content Parser Changes

- Parser locates "Serie" label in product page info list
- Extracts series link and title from adjacent element
- Converts series link to ID via `substringAfterLast("/").toLongOrNull()`
- Creates `SeriesDto` only when seriesId > 0 and name is non-null

### Product Mapper Changes

- Imported `ProductSeries` domain class
- Maps `dto.series` to `ProductSeries(seriesId = it.seriesId, names = mapOf(it.languageCode to it.name))`
- Handles null series gracefully via `?.let {}` pattern
- Both `toProducts()` and `toProductDetails()` updated

### Verification

- `CollectablesMapper` already maps `seriesId` and `seriesName` to API (lines 22-23, 52-53 in existing code)
- No changes needed to API layer

## Deviation Documentation

**None** - Plan executed exactly as written.

## Build Verification

- [x] `./gradlew compileKotlin` - PASS
- [x] `./gradlew detekt` - PASS
- [x] `./gradlew ktlintFormat` - PASS

## Auth Gates

No authentication gates encountered.

## Known Stubs

None.

---

## Checkpoint Verification

All tasks complete and committed. Build passes. Series data now flows from CardMarket HTML → DTOs → Product domain → API.

**Commit:** 230a0ef

**Plan Complete.**