---
phase: 01-scraper-foundation
verified: 2026-04-04T12:00:00Z
status: passed
score: 9/9 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 8/9
  gaps_closed:
    - "CardMarketWebFetcher uses GenreConfig for URL construction - UNUSED IMPORT ONLY, no functional gap"
  gaps_remaining: []
  regressions: []
---

# Phase 01: Scraper Foundation Verification Report

**Phase Goal:** Scraper handles all Pokémon products and languages with architecture ready for genre expansion
**Verified:** 2026-04-04T12:00:00Z
**Status:** passed
**Re-verification:** Yes — after gap closure

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Locale enum covers all CardMarket languages (de, en, fr, it, es, pt, nl, pl) | ✓ VERIFIED | `Locale.kt` has 8 enum entries with correct ISO codes; 3 unit tests pass |
| 2 | Translation map data class exists with nested maps per language | ✓ VERIFIED | `TranslationMap.kt` has `TranslationMap` and `Labels` data classes with all 8 languages and 5 labels each; 10 unit tests pass |
| 3 | Genre configuration structure replaces CardMarketConstants | ✓ VERIFIED | `GenreConfig.kt` has `Genre` enum (POKEMON, YUGIOH, MTG), `GenreConfigData`, `ParserType` sealed class, and `GENRES` map; 7 unit tests pass |
| 4 | CardMarketContentParser split into two focused parsers | ✓ VERIFIED | `CardMarketGalleryParser.kt` (129 lines) and `CardMarketDetailsParser.kt` (185 lines) both exist with full Jsoup parsing logic |
| 5 | Parser returns Result<Dto> with typed errors | ✓ VERIFIED | Both parsers return `Result<Dto>`; `ParseErrors.kt` defines sealed interface `ParseError` with `MissingElement` and `UnexpectedFormat` |
| 6 | Parse errors are distinct (MissingElement vs UnexpectedFormat) | ✓ VERIFIED | `MissingElement` used for missing HTML elements; `UnexpectedFormat` used for structural failures |
| 7 | CardMarketScraperPort updated with Locale enum and Genre | ✓ VERIFIED | Port interface uses `locale: Locale`, `game: Genre`, `lang: Locale` instead of String |
| 8 | CardMarketScraperAdapter uses new parsers and config | ✓ VERIFIED | Adapter uses `GenreConfig.GENRES`, `DEFAULT_TRANSLATION_MAP`, `CardMarketGalleryParser`, `CardMarketDetailsParser` |
| 9 | CardMarketWebFetcher uses GenreConfig for URL construction | ✓ VERIFIED | Uses `GenreConfig.GENRES[genre]` for path patterns; import of CardMarketConstants exists but is never referenced |

**Score:** 9/9 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/model/Locale.kt` | Locale enum for all 8 CardMarket languages | ✓ VERIFIED | 12 lines, enum with code property, all 8 locales present |
| `adapter/.../TranslationMap.kt` | TranslationMap with Labels per language | ✓ VERIFIED | 81 lines, all 8 languages with 5 labels each, DEFAULT_TRANSLATION_MAP instance |
| `config/GenreConfig.kt` | Genre enum + GenreConfigData + GENRES map | ✓ VERIFIED | 48 lines, Genre (POKEMON/YUGIOH/MTG), ParserType sealed class, GENRES registry |
| `adapter/.../CardMarketGalleryParser.kt` | Gallery/search page parser | ✓ VERIFIED | 129 lines, uses TranslationMap for pagination, returns Result<SearchResultsPageDto> |
| `adapter/.../CardMarketDetailsParser.kt` | Product details page parser | ✓ VERIFIED | 185 lines, uses TranslationMap for labels, returns Result<CardmarketProductDetailsDto> |
| `adapter/.../ParseErrors.kt` | Typed parse error types | ✓ VERIFIED | 13 lines, sealed interface ParseError, MissingElement, UnexpectedFormat |
| `domain/port/out/CardMarketScraperPort.kt` | Updated port with Locale/Genre | ✓ VERIFIED | 21 lines, imports Locale and Genre, uses typed parameters |
| `adapter/.../CardMarketScraperAdapter.kt` | Updated adapter with new infrastructure | ✓ VERIFIED | 97 lines, uses GenreConfig, TranslationMap, both parsers, mapper |
| `adapter/.../CardMarketWebFetcher.kt` | Updated fetcher with GenreConfig | ✓ VERIFIED | 150 lines, uses GenreConfig.GENRES for path patterns, no actual usage of CardMarketConstants |
| `domain/model/LocaleTest.kt` | Unit tests for Locale | ✓ VERIFIED | 3 tests, all pass |
| `adapter/.../TranslationMapTest.kt` | Unit tests for TranslationMap | ✓ VERIFIED | 10 tests, all pass |
| `config/GenreConfigTest.kt` | Unit tests for GenreConfig | ✓ VERIFIED | 7 tests, all pass |
| `adapter/.../CardMarketScraperAdapterIT.kt` | Integration tests | ✓ VERIFIED | 12 tests, WireMock stubs with HTML fixtures, tests multi-locale search and details |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| TranslationMap.kt | Locale.kt | imports Locale | ✓ WIRED | Line 3: `import io.github.havonte1.tcgwatcher.backend.domain.model.Locale` |
| CardMarketGalleryParser.kt | TranslationMap.kt | Uses TranslationMap in parse() | ✓ WIRED | Line 11: `translationMap: TranslationMap` parameter, line 124: uses `translationMap.de.paginationOf` etc. |
| CardMarketGalleryParser.kt | Dtos.kt | Returns SearchResultsPageDto | ✓ WIRED | Line 12: returns `Result<SearchResultsPageDto<CardmarketProductGallaryItemDto>>` |
| CardMarketDetailsParser.kt | TranslationMap.kt | Uses TranslationMap in parse() | ✓ WIRED | Line 15: `translationMap: TranslationMap` parameter, lines 40-72: uses translation labels |
| CardMarketScraperAdapter.kt | GenreConfig.kt | Uses GenreConfig.GENRES | ✓ WIRED | Lines 27, 67: `GenreConfig.GENRES[game]` / `GenreConfig.GENRES[genre]` |
| CardMarketScraperAdapter.kt | CardMarketGalleryParser.kt | Calls parser.parse() | ✓ WIRED | Line 48: `galleryParser.parse(content, 1, translationMap)` |
| CardMarketScraperAdapter.kt | CardMarketDetailsParser.kt | Calls parser.parse() | ✓ WIRED | Line 88: `detailsParser.parse(content, cmId, ...)` |
| CardMarketScraperAdapter.kt | TranslationMap.kt | Uses DEFAULT_TRANSLATION_MAP | ✓ WIRED | Lines 30-39, 70-79: when-expression selects translation map by locale |
| CardMarketScraperAdapter.kt | CardMarketWebFetcherPort | Injects and calls fetcher | ✓ WIRED | Lines 41, 81: `webFetcher.fetch(...)` / `webFetcher.fetchDetails(...)` |
| CardMarketWebFetcher.kt | GenreConfig.kt | Uses GenreConfig.GENRES | ✓ WIRED | Lines 126-128, 144-147: Uses GenreConfig.GENRES[genre] for path patterns and fallback locale |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|-------------------|--------|
| CardMarketGalleryParser | galleryItems (parsed from HTML) | Jsoup parsing of tile elements | ✓ Real data extracted from HTML | ✓ FLOWING |
| CardMarketGalleryParser | totalPages (from pagination) | Regex on pagination span text using TranslationMap | ✓ Real data from HTML with translated labels | ✓ FLOWING |
| CardMarketDetailsParser | productDetails (rarity, price, set, etc.) | Jsoup parsing using TranslationMap labels | ✓ Real data extracted from HTML | ✓ FLOWING |
| CardMarketDetailsParser | sellOffers | Jsoup parsing of article-row elements | ✓ Real data extracted from HTML | ✓ FLOWING |
| CardMarketScraperAdapter | List<Product> | Mapped from parser DTOs via CardMarketProductMapper | ✓ Real data from parser → mapper | ✓ FLOWING |
| CardMarketWebFetcher | URL construction | GenreConfig.GENRES for path patterns | ✓ Dynamic based on genre | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Locale enum has all 8 locales | `./gradlew test --tests "LocaleTest"` | BUILD SUCCESSFUL, 3/3 passed | ✓ PASS |
| TranslationMap has all 8 languages with correct labels | `./gradlew test --tests "TranslationMapTest"` | BUILD SUCCESSFUL, 10/10 passed | ✓ PASS |
| GenreConfig has POKEMON/YUGIOH/MTG with valid configs | `./gradlew test --tests "GenreConfigTest"` | BUILD SUCCESSFUL, 7/7 passed | ✓ PASS |
| Compilation succeeds | `./gradlew compileKotlin` | BUILD SUCCESSFUL | ✓ PASS |
| All tests pass | `./gradlew test` | BUILD SUCCESSFUL | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| SCRAP-01 | 01-01, 01-02, 01-03, 01-04, 01-05 | Scraper interfaces are designed to be configurable per genre (Pokémon, Yu-Gi-Oh, MTG) | ✓ SATISFIED | Genre enum with 3 values, GenreConfig.GENRES map with URL patterns for all genres, ParserType for parser selection |
| SCRAP-02 | 01-01, 01-02, 01-03, 01-04, 01-05 | CardMarket scraper supports all Pokémon product pages | ✓ SATISFIED | CardMarketGalleryParser and CardMarketDetailsParser handle gallery and detail pages; integration tests verify search and details flow |
| SCRAP-03 | 01-01, 01-02, 01-03, 01-04, 01-05 | Scraper handles all CardMarket languages for search and details | ✓ SATISFIED | Locale enum covers all 8 languages; TranslationMap has labels for all 8; both parsers use TranslationMap; integration tests verify de/en/fr locales |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| CardMarketContentParser.kt | 1-286 | Old monolithic parser still exists | ℹ️ Info | Not referenced by any other file (dead code), should be removed in cleanup |
| CardMarketWebFetcher.kt | 9 | Unused import of CardMarketConstants | ℹ️ Info | Import exists but never referenced - can be removed in cleanup |

### Human Verification Required

No human verification required. All automated checks pass.

### Gaps Summary

**No gaps remaining.** All must-haves verified:

1. ✓ Locale enum covers all 8 CardMarket languages
2. ✓ TranslationMap with nested maps per language
3. ✓ Genre configuration structure replaces CardMarketConstants
4. ✓ CardMarketContentParser split into two focused parsers
5. ✓ Parser returns Result<Dto> with typed errors
6. ✓ Parse errors are distinct (MissingElement vs UnexpectedFormat)
7. ✓ CardMarketScraperPort updated with Locale enum and Genre
8. ✓ CardMarketScraperAdapter uses new parsers and config
9. ✓ CardMarketWebFetcher uses GenreConfig for URL construction

**Minor cleanup items** (not blocking):
- Unused import `CardMarketConstants` in CardMarketWebFetcher.kt
- Dead code `CardMarketContentParser.kt` not referenced anywhere

---

_Verified: 2026-04-04T12:00:00Z_
_Verifier: the agent (gsd-verifier)_
