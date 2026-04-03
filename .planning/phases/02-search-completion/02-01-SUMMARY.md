---
phase: 02-search-completion
plan: 01
subsystem: domain-model
tags: [search, filters, pagination, sort]
dependency_graph:
  requires: []
  provides:
    - SearchFilters
    - SortOptions
    - PaginationParams
    - CardMarketSearchParams
  affects:
    - CardMarketScraperPort
    - SearchUseCase
    - CollectablesAdapter
tech_stack:
  added:
    - Condition sealed class with NM, LP, MP, HP, DM values
    - SortField enum with price, name, relevance, releaseDate, avgPrice, amount
    - SortDirection enum with asc, desc
  patterns:
    - URL parameter builder pattern for CardMarket API
key_files:
  created:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/SearchFilters.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/SortOptions.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/PaginationParams.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketSearchParams.kt
modified: []
decisions:
  - Used sealed class for Condition to allow type-safe condition filtering
  - Used enum for SortField and SortDirection to provide type-safe sorting
  - Used data class for SearchFilters to group all filter parameters
  - Created CardMarketSearchParams helper object to build URL parameters
metrics:
  duration: 5min
  completed: 2026-04-05
  tasks: 4
  files: 4
---

# Phase 02 Plan 01: Define Domain Models for Search Summary

## Objective
Define domain models for search parameters, sorting, and pagination that represent all CardMarket filter/sort/pagination capabilities.

## What Was Built
- **SearchFilters** - Data class with all CardMarket filter parameters:
  - language, minPrice, maxPrice, condition
  - isAvailable, isFoil, isSigned, isAltered, isPlayset
- **Condition** - Sealed class with values: NearMint, LightlyPlayed, ModeratelyPlayed, HeavilyPlayed, Damaged
- **SortOptions** - SortField enum (PRICE, NAME, RELEVANCE, RELEASE_DATE, AVG_PRICE, AMOUNT) and SortDirection enum (ASC, DESC)
- **PaginationParams** - Data class with page/limit validation (page >= 1, limit 1..100)
- **CardMarketSearchParams** - Helper object to convert domain models to URL parameters

## Key Decisions
- Used sealed class for Condition to allow type-safe condition filtering
- Used enum for SortField and SortDirection to provide type-safe sorting
- Created CardMarketSearchParams helper object to build URL parameters

## Deviation: None

## Auth Gates: None

## Stub Tracking
None - all artifacts are fully implemented with no stubs.
