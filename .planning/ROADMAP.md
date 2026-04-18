# Roadmap: TCGWatcher Backend

## Overview

Complete the TCGWatcher Backend from working PoC to production-ready v1.0. The journey starts with hardening the scraper foundation (multi-language support, genre-configurable interfaces), then completes the two core endpoints (search with all filters/sorts/pagination, product details with all languages/attributes), and finishes with production infrastructure (rate limiting, circuit breaker fallback, monitoring). Pokémon only — Yu-Gi-Oh and MTG are v2.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Scraper Foundation** - Multi-language scraper with genre-configurable interfaces
- [ ] **Phase 1.5: Series Data** - Extend ProductDTO with series information from CardMarket (INSERTED)
- [ ] **Phase 2: Search Completion** - Full CardMarket search with all filters, sorts, and pagination
- [ ] **Phase 3: Details Completion** - Complete product details with all languages and attributes
- [ ] **Phase 4: API Infrastructure** - Production-ready API with rate limiting, resilience, and monitoring

## Phase Details

### Phase 1: Scraper Foundation
**Goal**: Scraper handles all Pokémon products and languages with architecture ready for genre expansion
**Depends on**: Nothing (first phase)
**Requirements**: SCRAP-01, SCRAP-02, SCRAP-03
**Success Criteria** (what must be TRUE):
  1. Scraper can search and retrieve details for any Pokémon product on CardMarket
  2. Scraper correctly parses product data in all CardMarket languages (not just German)
  3. Scraper interfaces are structured so adding a new genre (Yu-Gi-Oh, MTG) requires no changes to core scraping logic
**Plans**: TBD

### Phase 1.5: Series Data (INSERTED)
**Goal**: Extend ProductDTO with series information from CardMarket scraping
**Depends on**: Phase 1
**Requirements**: SCRAP-02, SCRAP-03
**Success Criteria** (what must be TRUE):
  1. Search results include seriesId and seriesName for each product
  2. Product details include seriesId and seriesName
  3. Series data is populated from CardMarket pages during scraping
**Plans**: 5 plans

### Phase 1.5: Series Data (INSERTED)
**Goal**: Extend ProductDTO with series information from CardMarket scraping
**Depends on**: Phase 1
**Requirements**: SCRAP-02, SCRAP-03
**Success Criteria** (what must be TRUE):
  1. Search results include seriesId and seriesName for each product
  2. Product details include seriesId and seriesName
  3. Series data is populated from CardMarket pages during scraping
**Plans**: 1 plan

### Phase 2: Search Completion
**Goal**: Users can search Pokémon products with all CardMarket filters, sort options, and pagination
**Depends on**: Phase 1
**Requirements**: SEARCH-01, SEARCH-02, SEARCH-03, SEARCH-04, SEARCH-05, SEARCH-06, SEARCH-07, SEARCH-08, SEARCH-09, SEARCH-10, SEARCH-11, SEARCH-12, SEARCH-13
**Success Criteria** (what must be TRUE):
  1. User can search Pokémon products by free-text query and get relevant results
  2. User can filter results by language, price range, condition, availability, and all CardMarket-supported filter parameters
  3. User can sort results by price, name, release date, relevance, and all CardMarket-supported sort options (each asc/desc)
  4. Search results are paginated with page/limit parameters and response includes total count and pagination metadata
**Plans**: TBD

### Phase 3: Details Completion
**Goal**: Users can retrieve complete product details for any Pokémon product in any language
**Depends on**: Phase 1
**Requirements**: DETAIL-01, DETAIL-02, DETAIL-03, DETAIL-04, DETAIL-05
**Success Criteria** (what must be TRUE):
  1. User can retrieve detailed information for any Pokémon product by cmId
  2. Product details include language-specific pricing and all sell offers with language information
  3. Product details include all CardMarket-supported product attributes (rarity, release date, price trends, etc.)
  4. Product details response includes ETag header and returns 304 Not Modified for unchanged resources
**Plans**: TBD

### Phase 4: API Infrastructure
**Goal**: Production-ready API with rate limiting, circuit breaker fallback, and health monitoring
**Depends on**: Phase 2, Phase 3
**Requirements**: API-01, API-02, API-03, API-04, API-05, API-06
**Success Criteria** (what must be TRUE):
  1. All endpoints return data directly without envelope wrappers
  2. Pagination metadata is returned in response headers (X-Total-Count, X-Page, X-Limit, etc.)
  3. API is publicly accessible without any authentication requirement
  4. Server-side rate limiting throttles requests to protect CardMarket scraper from overuse
  5. Circuit breaker provides graceful fallback behavior when CardMarket is unavailable
  6. Spring Boot Admin displays scraper health status and API metrics
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 1.5 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Scraper Foundation | 0/0 | Not started | - |
| 1.5 Series Data | 1/1 | Complete | 2026-04-18 |
| 2. Search Completion | 0/0 | Not started | - |
| 3. Details Completion | 0/0 | Not started | - |
| 4. API Infrastructure | 0/0 | Not started | - |
