# TCGWatcher Backend

## What This Is

A REST API for searching and retrieving detailed product information from trading card game marketplaces. Users can search for TCG products by query and get comprehensive product details including pricing, sell offers, and metadata. The API proxies and normalizes data from external marketplaces, starting with CardMarket for Pokémon cards.

## Core Value

Accurate, fast search and product detail retrieval for TCG cards — if the search doesn't return the right products with correct pricing, nothing else matters.

## Requirements

### Validated

- ✓ CardMarket search endpoint exists as PoC — existing
- ✓ CardMarket product details endpoint exists as PoC — existing
- ✓ Hexagonal architecture with OpenAPI-first contract — existing
- ✓ PostgreSQL persistence with Hibernate Envers auditing — existing
- ✓ Playwright-based web scraping with Jsoup parsing — existing
- ✓ Caffeine caching + HTTP ETag support — existing
- ✓ Resilience4j circuit breaker + retry for scraping — existing
- ✓ Docker Compose deployment with Spring Boot Admin — existing

### Active

- [ ] Complete CardMarket search with all filter parameters (price range, language, condition, availability, seller location, etc.)
- [ ] Complete CardMarket search with all sort options (price, name, release date, relevance, etc.)
- [ ] Support all CardMarket languages in search and product details
- [ ] Finish pagination implementation (offset-based with page/limit)
- [ ] Expose all CardMarket filters through REST API parameters
- [ ] Return CardMarket product data directly (no enrichment layer)
- [ ] Server-side rate limiting to protect against CardMarket bans
- [ ] Circuit breaker fallback behavior for CardMarket failures
- [ ] Design scraper interfaces configurable per genre (Pokémon, Yu-Gi-Oh, MTG)
- [ ] Add Yu-Gi-Oh card support via CardMarket
- [ ] Add Magic: The Gathering card support via CardMarket
- [ ] Public API with no authentication required
- [ ] Direct response format with pagination in headers
- [ ] Spring Boot Admin health monitoring for scraper status

### Out of Scope

- User accounts or authentication — public API, no user management needed
- Price history or trend analysis — pass through CardMarket data only, no local enrichment
- Multi-source scraping in v1 — CardMarket only initially, other sources added per genre later
- Cursor-based pagination — offset-based is sufficient and CardMarket compatible
- Client-side rate limiting — server-side only to protect scraper
- Other marketplaces (TCGPlayer, etc.) — CardMarket is the sole source

## Context

**Existing codebase:** Brownfield project with working PoC for Pokémon card search and details on CardMarket. Hexagonal architecture established, OpenAPI contract in `contract/openapi.yaml`, database schema managed by Liquibase, web scraping via Playwright + Jsoup.

**Current endpoints:** `GET /collectables/` (search) and `GET /collectables/{cmId}` (product details). Both have ETag caching and basic pagination stub.

**Data bootstrap:** SQLite quicksearch database imported at startup for product catalog reference (series, sets, cards with translations in de/en/fr).

**Known concerns:** Hardcoded German labels in content parser, N+1 queries in import runner, some Resilience4j configuration incomplete, `auth.json` with session state committed to repo, ktlint disabled for main source, detekt baseline masking 54 issues.

## Constraints

- **Tech Stack**: Kotlin 2.2.20, Spring Boot 4.0.2, PostgreSQL, Playwright — established stack, no changes
- **Data Source**: CardMarket only for v1 — no other marketplaces until v2
- **Genre Scope**: Pokémon only for v1 — Yu-Gi-Oh and MTG are v2
- **Response Format**: Direct CardMarket data passthrough — no enrichment or transformation layer
- **Deployment**: Docker Compose with PostgreSQL and Playwright browser
- **API Contract**: OpenAPI-first — all endpoints defined in `contract/openapi.yaml` and generated

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Public API, no auth | Price data is public, no user-specific features needed | — Pending |
| Offset-based pagination | Simpler, compatible with CardMarket's own pagination | — Pending |
| Direct response format | No envelope wrapper — cleaner API, pagination in headers | — Pending |
| Server-side rate limiting only | Protect scraper from bans, no need to limit API consumers | — Pending |
| Circuit breaker fallback | Graceful degradation when CardMarket is unavailable | — Pending |
| Configurable per genre | Each genre maps to its own scraper backend — simpler than multi-source abstraction | — Pending |
| CardMarket data only | No local enrichment — keep it simple, avoid stale derived data | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-03 after initialization*
