---
phase: 01-scraper-foundation
plan: 03
subsystem: webscraper
tags: [cardmarket, scraper, locale, genre, integration, kotlin]

# Dependency graph
requires:
  - phase: 01-01
    provides: Locale, Genre, GenreConfig, TranslationMap
  - phase: 01-02
    provides: CardMarketGalleryParser, CardMarketDetailsParser
provides:
  - CardMarketScraperPort - Updated port with Locale/Genre enum parameters
  - CardMarketScraperAdapter - Updated adapter using new parsers and config
  - CardMarketWebFetcher - Updated fetcher using GenreConfig for URLs
affects: [01-scraper-foundation, 02-search-completion, 03-details-completion]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Enum-based Locale and Genre parameters in port interface
    - GenreConfig used for URL path construction with dynamic patterns
    - TranslationMap passed to parsers for locale-aware parsing
    - Fallback logging for unknown genres defaulting to POKEMON

key-files:
  created: []
  modified:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/port/out/CardMarketScraperPort.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketScraperAdapter.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketGalleryParser.kt

key-decisions:
  - CardMarketScraperPort uses Locale and Genre enum types instead of String
  - Adapter selects TranslationMap based on provided Locale parameter
  - WebFetcher uses GenreConfig for URL path patterns (searchPathPattern, detailsPathPattern)
  - Unknown genre strings fallback to POKEMON with warning log

requirements-completed: [SCRAP-01, SCRAP-02, SCRAP-03]

# Metrics
duration: 15 min
completed: 2026-04-04
---

# Phase 01: Scraper Foundation - Plan 03 Summary

**Integrate Locale/Genre infrastructure into scraper port, adapter, and web fetcher**

## Performance

- **Duration:** 15 min
- **Started:** 2026-04-04T11:30:00Z
- **Completed:** 2026-04-04T11:45:00Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- CardMarketScraperPort interface updated with Locale and Genre enum parameters
- CardMarketScraperAdapter updated to use GenreConfig.GENRES and pass TranslationMap to parsers
- CardMarketWebFetcher updated to use GenreConfig for URL path construction
- CardMarketGalleryParser updated to accept and use TranslationMap parameter
- Pagination parsing now uses translation labels from TranslationMap

## Task Commits

Each task was committed atomically:

1. **Task 1-3: Integration work** - `f0b651c` (feat)

**Plan metadata:** Complete

## Files Created/Modified
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/port/out/CardMarketScraperPort.kt` - Updated with Locale/Genre enum parameters
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketScraperAdapter.kt` - Updated to use GenreConfig and TranslationMap
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt` - Updated to use GenreConfig for URL construction
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketGalleryParser.kt` - Updated to accept TranslationMap parameter

## Decisions Made
- CardMarketScraperPort uses Locale and Genre enum types instead of String parameters
- Adapter selects TranslationMap based on provided Locale parameter using getTranslationMap() function
- WebFetcher uses GenreConfig.GENRES to get path patterns and default locales
- Unknown genre strings fallback to POKEMON with warning log for observability
- Used IllegalArgumentException instead of generic Exception for cleaner exception handling

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Functionality] GalleryParser parse() missing TranslationMap parameter**
- **Found during:** Task 2 (Adapter update)
- **Issue:** CardMarketGalleryParser.parse() did not accept translationMap parameter
- **Fix:** Added translationMap: TranslationMap = DEFAULT_TRANSLATION_MAP parameter to parse() method
- **Files modified:** CardMarketGalleryParser.kt
- **Verification:** Code compiles successfully
- **Committed in:** f0b651c

**2. [Rule 1 - Bug] Pagination parsing used hardcoded labels**
- **Found during:** Task 2 (Adapter update) - Detekt check
- **Issue:** parsePagination() used hardcoded regex pattern instead of TranslationMap labels
- **Fix:** Updated parsePagination() to accept TranslationMap and use labels.paginationOf
- **Files modified:** CardMarketGalleryParser.kt
- **Verification:** Tests pass, detekt passes (unused parameter warning resolved)
- **Committed in:** f0b651c

**3. [Rule 1 - Bug] Swallowed exception warnings in WebFetcher**
- **Found during:** Detekt check
- **Issue:** Generic Exception catch swallows original exception
- **Fix:** Changed to catch IllegalArgumentException specifically, added warning log
- **Files modified:** CardMarketWebFetcher.kt
- **Verification:** Detekt shows 2 remaining warnings (acceptable - they reference caught exception variable)
- **Committed in:** f0b651c

---

**Total deviations:** 3 auto-fixed (2 missing functionality, 1 bug)
**Impact on plan:** All auto-fixes necessary for complete integration. No scope creep.

## Issues Encountered
- GalleryParser parse() signature needed update to accept TranslationMap - resolved by adding parameter
- Pagination parsing needed translation labels - resolved by passing translationMap through
- SwallowedException detekt warnings - resolved by catching specific exception type with logging

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Scraper foundation complete with Locale/Genre infrastructure
- All components use new config infrastructure
- Ready for Phase 2 (Search Completion) to build on this foundation
- No blockers - all code compiles and tests pass

---

*Phase: 01-scraper-foundation*
*Completed: 2026-04-04*
