# Phase 1: Scraper Foundation - Context

**Gathered:** 2026-04-03
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase delivers a multi-language scraper with genre-configurable interfaces. It refactors the existing CardMarket scraper to support all CardMarket languages (not just German), splits the monolithic content parser into focused components, introduces typed error handling, and establishes the genre configuration pattern that will enable Yu-Gi-Oh and MTG support in v2. This phase does NOT add new genres — it prepares the architecture for them.

</domain>

<decisions>
## Implementation Decisions

### Language Handling
- **D-01:** Supported CardMarket locales are defined as a sealed class or enum in the domain layer — type-safe, validated at boundaries
- **D-02:** Locale enum covers all CardMarket languages (de, en, fr, it, es, pt, nl, pl)
- **D-03:** Locale is passed through the scraper port interface and validated at the adapter boundary

### Content Parser Architecture
- **D-04:** Monolithic CardMarketContentParser (286 lines) is split into two focused parsers: one for gallery/search pages, one for product detail pages
- **D-05:** Language-specific labels (German "Rarität", "Erschienen", "ab", "Preis-Trend", pagination "von/of/de") are replaced with a translation map
- **D-06:** Translation map is implemented as a Kotlin data class with nested maps per language — simple, no Spring dependency, easy to test
- **D-07:** Translation map covers both search page labels (pagination) and detail page labels (rarity, release date, price, etc.)
- **D-08:** Parser returns `Result<Dto>` with typed errors (MissingElement, UnexpectedFormat) — caller decides how to handle failures

### Genre Configuration
- **D-09:** Replace CardMarketConstants with a genre-configurable structure that maps genre to URL patterns, default locale, and parser selection
- **D-10:** Genre configuration is extensible — adding Yu-Gi-Oh or MTG requires only adding a new genre config entry, no changes to core scraping logic

### Error Handling
- **D-11:** Typed parse errors (MissingElement, UnexpectedFormat) allow callers to distinguish between transient issues (missing element due to layout change) and permanent failures (unexpected format)
- **D-12:** Scraper adapter maps parse errors to appropriate responses — MissingElement may trigger retry, UnexpectedFormat may trigger circuit breaker

### the agent's Discretion
- Exact enum naming conventions and package placement for the locale sealed class
- Specific structure of the translation map data class (flat vs nested)
- Whether to use Kotlin sealed interfaces or sealed classes for parse errors

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scraper Architecture
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/port/out/CardMarketScraperPort.kt` — Current scraper port interface (needs locale/genre updates)
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt` — Monolithic parser to be split (286 lines, hardcoded German labels)
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt` — Playwright-based web fetcher
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/Dtos.kt` — Current DTO definitions
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/CardMarketConstants.kt` — Current constants to be replaced with genre config
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/CardMarketConfig.kt` — Spring configuration for CardMarket base URL

### Domain Models
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/Product.kt` — Product domain model
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/SearchResult.kt` — Search result domain model

### Codebase Context
- `.planning/codebase/CONCERNS.md` — Known concerns: hardcoded German labels, massive content parser, auth.json committed
- `.planning/codebase/ARCHITECTURE.md` — Hexagonal architecture, data flows, entry points
- `.planning/codebase/STACK.md` — Tech stack: Kotlin 2.2.20, Spring Boot 4.0.2, Playwright 1.58.0, Jsoup 1.22.1

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CardMarketWebFetcher` — Playwright fetcher with Resilience4j retry/circuit breaker annotations, can be reused with genre-aware URL construction
- `PlaywrightManager` — Browser lifecycle management, no changes needed
- `CardMarketProductMapper` — Maps DTOs to domain Product models, may need updates for multi-language fields
- `CardMarketConfig` — Spring `@ConfigurationProperties` for base URL, can be extended for genre config

### Established Patterns
- Hexagonal architecture: domain ports → adapter implementations → application orchestration
- OpenAPI-first: REST interfaces generated from `contract/openapi.yaml`
- `Result<T>` for expected failures in web fetcher, exceptions for unexpected
- Caffeine caching at application layer (`@Cacheable`)
- Hibernate Envers for entity auditing

### Integration Points
- `CardMarketScraperPort` interface in domain layer — the contract that search/details use cases depend on
- `CollectablesAdapter` (REST controller) → `SearchUseCase` → `CardMarketScraperPort` chain
- SQLite quicksearch import for product catalog reference (series, sets, cards with de/en/fr translations)

</code_context>

<specifics>
## Specific Ideas

- The pagination regex already handles multiple languages: `\\b(?:von|of|de) (\\d+)\\b` — extend this pattern for other languages
- Product detail parser has FIXME comment: "this works only for german details pages" — this is the primary target for the translation map
- Sell offer parsing extracts language from icon title/aria-label attributes — this already works for multiple languages, no changes needed there
- The `parseLink` function already extracts language from URL path segments — this can be leveraged for locale validation

</specifics>

<deferred>
## Deferred Ideas

- Genre-specific URL patterns for Yu-Gi-Oh and MTG — v2 work, but config structure should support it
- Additional CardMarket filter/sort parameter handling — Phase 2 (Search Completion)
- Product detail enrichment beyond CardMarket data — explicitly out of scope

</deferred>

---

*Phase: 01-scraper-foundation*
*Context gathered: 2026-04-03*
