# Requirements: TCGWatcher Backend

**Defined:** 2026-04-03
**Core Value:** Accurate, fast search and product detail retrieval for TCG cards — if the search doesn't return the right products with correct pricing, nothing else matters.

## v1 Requirements

### Search

- [ ] **SEARCH-01**: User can search Pokémon products on CardMarket by free-text query
- [ ] **SEARCH-02**: User can filter search results by language (all CardMarket languages)
- [ ] **SEARCH-03**: User can filter search results by price range (min/max)
- [ ] **SEARCH-04**: User can filter search results by condition
- [ ] **SEARCH-05**: User can filter search results by availability
- [ ] **SEARCH-06**: User can filter search results by all CardMarket-supported filter parameters
- [ ] **SEARCH-07**: User can sort search results by price (asc/desc)
- [ ] **SEARCH-08**: User can sort search results by name (asc/desc)
- [ ] **SEARCH-09**: User can sort search results by release date (asc/desc)
- [ ] **SEARCH-10**: User can sort search results by relevance
- [ ] **SEARCH-11**: User can sort search results by all CardMarket-supported sort options
- [ ] **SEARCH-12**: Search results are paginated with offset-based pagination (page/limit)
- [ ] **SEARCH-13**: Search response includes total count and pagination metadata

### Product Details

- [x] **DETAIL-01**: User can retrieve detailed information for a specific Pokémon product by cmId
- [x] **DETAIL-02**: Product details include language-specific pricing and data
- [x] **DETAIL-03**: Product details include all sell offers with language information
- [x] **DETAIL-04**: Product details include all CardMarket-supported product attributes
- [x] **DETAIL-05**: Product details response includes ETag for conditional requests

### API Infrastructure

- [ ] **API-01**: All endpoints return data directly (no envelope wrapper)
- [ ] **API-02**: Pagination metadata returned in response headers
- [ ] **API-03**: API is publicly accessible without authentication
- [ ] **API-04**: Server-side rate limiting protects CardMarket scraper from overuse
- [ ] **API-05**: Circuit breaker provides fallback behavior when CardMarket is unavailable
- [ ] **API-06**: Spring Boot Admin monitors scraper health and API status

### Scraper Architecture

- [x] **SCRAP-01**: Scraper interfaces are designed to be configurable per genre (Pokémon, Yu-Gi-Oh, MTG)
- [x] **SCRAP-02**: CardMarket scraper supports all Pokémon product pages
- [x] **SCRAP-03**: Scraper handles all CardMarket languages for search and details

## v2 Requirements

### Yu-Gi-Oh Support

- **YGO-01**: User can search Yu-Gi-Oh products on CardMarket
- **YGO-02**: User can retrieve Yu-Gi-Oh product details
- **YGO-03**: All v1 search filters and sort options work for Yu-Gi-Oh

### Magic: The Gathering Support

- **MTG-01**: User can search MTG products on CardMarket
- **MTG-02**: User can retrieve MTG product details
- **MTG-03**: All v1 search filters and sort options work for MTG

## Out of Scope

| Feature | Reason |
|---------|--------|
| User accounts / authentication | Public API, no user-specific features needed |
| Price history / trend analysis | Pass through CardMarket data only, no local enrichment |
| Other marketplaces (TCGPlayer, etc.) | CardMarket is the sole source |
| Cursor-based pagination | Offset-based is sufficient and CardMarket compatible |
| Client-side rate limiting | Server-side only to protect scraper |
| Multi-source scraping in v1 | CardMarket only initially, other sources added per genre later |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SCRAP-01 | Phase 1 | Complete |
| SCRAP-02 | Phase 1 | Complete |
| SCRAP-03 | Phase 1 | Complete |
| SEARCH-01 | Phase 2 | Pending |
| SEARCH-02 | Phase 2 | Pending |
| SEARCH-03 | Phase 2 | Pending |
| SEARCH-04 | Phase 2 | Pending |
| SEARCH-05 | Phase 2 | Pending |
| SEARCH-06 | Phase 2 | Pending |
| SEARCH-07 | Phase 2 | Pending |
| SEARCH-08 | Phase 2 | Pending |
| SEARCH-09 | Phase 2 | Pending |
| SEARCH-10 | Phase 2 | Pending |
| SEARCH-11 | Phase 2 | Pending |
| SEARCH-12 | Phase 2 | Pending |
| SEARCH-13 | Phase 2 | Pending |
| DETAIL-01 | Phase 3 | Complete |
| DETAIL-02 | Phase 3 | Complete |
| DETAIL-03 | Phase 3 | Complete |
| DETAIL-04 | Phase 3 | Complete |
| DETAIL-05 | Phase 3 | Complete |
| API-01 | Phase 4 | Pending |
| API-02 | Phase 4 | Pending |
| API-03 | Phase 4 | Pending |
| API-04 | Phase 4 | Pending |
| API-05 | Phase 4 | Pending |
| API-06 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 27 total
- Mapped to phases: 27
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-03*
*Last updated: 2026-04-03 after initial definition*
