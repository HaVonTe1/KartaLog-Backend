---
phase: 01-scraper-foundation
plan: 01
subsystem: domain
tags: [locale, translation, genre, configuration, kotlin]

# Dependency graph
requires:
  - phase: 01-context
    provides: project context and requirements
provides:
  - Locale enum for all CardMarket languages (de, en, fr, it, es, pt, nl, pl)
  - TranslationMap data class with nested Labels per language
  - GenreConfig structure with Genre enum and GENRES registry
affects:
  - 01-02
  - 01-03
  - 01-04

# Tech tracking
tech-stack:
  added:
    - Locale enum (domain model)
    - TranslationMap data class (cardmarket adapter)
    - GenreConfig data class (config)
  patterns:
    - Type-safe locale handling via enum
    - Nested data class structure for translation maps
    - Extensible genre configuration with registry pattern

key-files:
  created:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/Locale.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/TranslationMap.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/GenreConfig.kt
  modified: []

key-decisions:
  - "Locale implemented as enum (not sealed class) since only CardMarket languages are needed and no custom locales expected"
  - "TranslationMap uses nested Labels data class for each language to provide type-safe access per locale"
  - "GenreConfigData data class (not object) for instance-based configuration, with GenreConfig object as registry"
  - "ParserType uses sealed class with object instances for type-safe parser selection"

requirements-completed: [SCRAP-01, SCRAP-02, SCRAP-03]

# Metrics
duration: 15min
completed: 2026-04-03
---

# Phase 01: Scraper Foundation - Plan 01 Summary

**Type-safe locale handling with enum, translation map for multi-language labels, and genre-configurable structure**

## Performance

- **Duration:** 15 min
- **Started:** 2026-04-03T15:43:08Z
- **Completed:** 2026-04-03T16:00:00Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Locale enum with all 8 CardMarket languages (de, en, fr, it, es, pt, nl, pl)
- TranslationMap data class with nested Labels per language for type-safe access
- GenreConfig structure with Genre enum (POKEMON, YUGIOH, MTG) and GENRES registry for extensibility

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Locale enum/sealed class** - `dfe3478` (feat)
2. **Task 2: Create TranslationMap data class** - `25ba655` (feat)
3. **Task 3: Create GenreConfig structure** - `26c97ff` (feat) + `6248cb9` (fix)

**Plan metadata:** `6248cb9` (docs: complete plan)

## Files Created/Modified
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/Locale.kt` - Enum with 8 CardMarket languages
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/TranslationMap.kt` - Nested Labels per language
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/GenreConfig.kt` - Genre enum and GENRES registry

## Decisions Made
- **Locale as enum:** Used enum instead of sealed class since only CardMarket languages are needed and no custom locales expected
- **TranslationMap structure:** Nested Labels data class for each language to provide type-safe access per locale
- **GenreConfig naming:** Data class named GenreConfigData to avoid conflict with existing GenreConfig object; object remains as registry
- **ParserType sealed class:** Uses sealed class with object instances for type-safe parser selection (GalleryParser, DetailsParser)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Renamed GenreConfig to GenreConfigData to avoid naming conflict**
- **Found during:** Task 3 (GenreConfig structure implementation)
- **Issue:** Plan specified `data class GenreConfig` but `object GenreConfig` already existed in codebase, causing redeclaration error
- **Fix:** Renamed data class to `GenreConfigData` while keeping `object GenreConfig` as registry
- **Files modified:** src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/GenreConfig.kt
- **Verification:** `./gradlew compileKotlin` passes without errors
- **Committed in:** `6248cb9` (fix commit)

---

**Total deviations:** 1 auto-fixed (1 bug - naming conflict)
**Impact on plan:** Naming conflict fix necessary for compilation. No scope creep - same structure, just renamed class.

## Issues Encountered
- **Naming conflict:** Data class `GenreConfig` conflicted with existing `object GenreConfig`. Resolved by renaming data class to `GenreConfigData`.
- **Locale default:** Used ENGLISH as default locale instead of GERMAN to align with plan's goal of multi-language support beyond German.

## Next Phase Readiness
- Locale enum ready for use in parser interfaces and validation
- TranslationMap ready to replace hardcoded German labels in CardMarketContentParser
- GenreConfig ready to configure scraper behavior per genre (POKEMON, YUGIOH, MTG)

---
*Phase: 01-scraper-foundation*
*Completed: 2026-04-03*
