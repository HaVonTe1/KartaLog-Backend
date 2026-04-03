---
phase: 01-scraper-foundation
plan: 02
subsystem: webscraper
tags: [cardmarket, parser, translation, multi-language, kotlin]

# Dependency graph
requires:
  - phase: 01-01
    provides: TranslationMap, Locale, GenreConfig
provides:
  - CardMarketGalleryParser - Gallery/search page parser with TranslationMap support
  - CardMarketDetailsParser - Product details page parser with TranslationMap support
  - ParseErrors - Typed parse error types (MissingElement, UnexpectedFormat)
affects: [01-scraper-foundation, 02-search-completion, 03-details-completion]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Parser returns Result<Dto> with typed errors
    - TranslationMap used for language-specific labels instead of hardcoded strings
    - Jsoup for HTML parsing with error handling

key-files:
  created:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/ParseErrors.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketGalleryParser.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketDetailsParser.kt
  modified: []

key-decisions:
  - ParseError implements RuntimeException to work with Result.failure()
  - GalleryParser extracts product tiles from a tags with class "card"
  - DetailsParser uses TranslationMap for label-based lookups (rarity, release date, price, price trend)
  - Sell offer parsing preserves existing logic while using translation map for label lookups

requirements-completed: [SCRAP-01, SCRAP-02, SCRAP-03]

# Metrics
duration: 35 min
completed: 2026-04-03
---

# Phase 01: Scraper Foundation - Plan 02 Summary

**Split monolithic CardMarketContentParser into two focused parsers with typed error handling and TranslationMap integration**

## Performance

- **Duration:** 35 min
- **Started:** 2026-04-03T17:00:00Z
- **Completed:** 2026-04-03T17:35:00Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Created ParseErrors.kt with sealed interface ParseError and MissingElement/UnexpectedFormat implementations
- Implemented CardMarketGalleryParser with parse() method using TranslationMap for pagination labels
- Implemented CardMarketDetailsParser with parse() method using TranslationMap for all label lookups
- All parsers return Result<Dto> with typed errors instead of throwing exceptions
- No hardcoded German labels in new parsers - all use TranslationMap

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ParseErrors.kt with typed error types** - `4db9107` (test)
2. **Task 2: Create CardMarketGalleryParser.kt** - `0aa8ab6` (feat)
3. **Task 3: Create CardMarketDetailsParser.kt** - `150eea5` (feat)

**Plan metadata:** Complete

## Files Created/Modified
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/ParseErrors.kt` - Typed parse error types with sealed interface and two implementations
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketGalleryParser.kt` - Gallery/search page parser with TranslationMap support
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketDetailsParser.kt` - Product details page parser with TranslationMap support

## Decisions Made
- ParseError implements both sealed interface and RuntimeException to work with Result.failure()
- GalleryParser preserves existing parsing logic from CardMarketContentParser
- DetailsParser uses TranslationMap for all label-based lookups (rarity, release date, price, price trend)
- Sell offer parsing preserves existing logic while using translation map for label lookups

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed Result.failure() type mismatch**
- **Found during:** Task 1 (ParseErrors.kt creation)
- **Issue:** Result.failure() expects Throwable, but ParseError is a sealed interface
- **Fix:** Made MissingElement and UnexpectedFormat implement both ParseError and RuntimeException
- **Files modified:** ParseErrors.kt
- **Verification:** Code compiles without type errors
- **Committed in:** 4db9107

**2. [Rule 3 - Blocking] Fixed GenreConfig naming conflict**
- **Found during:** Plan 01 execution (pre-existing)
- **Issue:** GenreConfig data class and object had same name causing redeclaration error
- **Fix:** Renamed data class to GenreConfigData while keeping object as GenreConfig
- **Files modified:** GenreConfig.kt (Plan 01)
- **Verification:** Code compiles successfully
- **Note:** Plan 01 task verification expects `data class GenreConfig` - this is a pre-existing deviation

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking)
**Impact on plan:** Both auto-fixes necessary for compilation. No scope creep.

## Issues Encountered
- GenreConfig data class and object naming conflict - resolved by renaming data class to GenreConfigData
- Multi-line function signature verification - adjusted verification to use grep patterns that match across lines

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Parser foundation complete with multi-language support via TranslationMap
- Typed error handling enables proper error propagation to callers
- Ready for Phase 2 (Search Completion) to build on these parsers
- No blockers - all parsers compile and pass existing tests

---

*Phase: 01-scraper-foundation*
*Completed: 2026-04-03*
