# Architecture

**Analysis Date:** 2026-04-05

## Pattern Overview

**Overall:** Hexagonal Architecture (Ports & Adapters) with Spring Boot 4.0.2

**Key Characteristics:**
- Domain layer defines port interfaces (`domain/port/out/`) with no external dependencies
- Adapters implement ports: inbound adapters (`adapter/inbound/`) handle REST requests, outbound adapters (`adapter/out/`) implement persistence and web scraping
- Application layer (`application/`) orchestrates use cases by coordinating ports
- OpenAPI-first API contract: `contract/openapi.yaml` generates Spring interfaces at compile time
- Coroutines-based reactive API (`suspend` functions) despite WebMVC runtime
- Caching at two levels: Caffeine in-memory cache (`@Cacheable`) and HTTP ETag/If-None-Match
- Resilience4j circuit breaker and retry for external CardMarket scraping calls

## Layers

**Domain Layer:**
- Purpose: Core business models and outbound port interfaces
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/`
- Contains: Domain models (`Product`, `SearchResult`, `SellOffer`, `ProductSet`, `ProductSeries`, `StringWithValidity`), port interfaces (`ProductRepository`, `SearchResultRepository`, `CardMarketScraperPort`)
- Depends on: Nothing external — pure Kotlin
- Used by: Application layer services

**Application Layer:**
- Purpose: Use case orchestration — implements `SearchUseCase` interface
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/`
- Contains: `CollectablesService` (implements `SearchUseCase`), `SearchUseCase` interface
- Depends on: Domain ports (`CardMarketScraperPort`, `ProductRepository`, `SearchResultRepository`)
- Used by: Inbound REST adapters

**Inbound Adapters (Driving):**
- Purpose: Expose API to external clients via REST
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/`
- Contains: `CollectablesAdapter` (REST controller implementing generated `CollectablesApi`), `CollectablesMapper` (domain-to-DTO mapping)
- Depends on: Application layer (`SearchUseCase`), generated OpenAPI interfaces (`CollectablesApi`, DTOs)
- Used by: External HTTP clients

**Outbound Adapters (Driven):**
- Purpose: Implement persistence and external web scraping
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/`
- Contains:
  - Persistence: `ProductRepositoryAdapter`, `SearchResultRepositoryAdapter`, JPA entities, JPA repositories, mappers
  - Web Scraper: `CardMarketScraperAdapter`, `CardMarketWebFetcher`, `CardMarketContentParser`, `CardMarketProductMapper`, `PlaywrightManager`
  - Data Import: `QuicksearchImportRunner` (SQLite/CSV → PostgreSQL bootstrap)
- Depends on: Spring Data JPA, Playwright, Jsoup, SQLite JDBC
- Used by: Application layer via port interfaces

**Configuration Layer:**
- Purpose: Spring configuration beans
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/`
- Contains: `CacheConfig` (Caffeine), `Resilience4jConfig` (circuit breaker), `CardMarketConfig` (base URL), `CardMarketConstants` (defaults)

## Data Flow

**Search Flow (`GET /collectables/`):**

1. HTTP request hits `CollectablesAdapter.listCollectables()` (generated `CollectablesApi` interface)
2. ETag check: compares `If-None-Match` header against `cachedAt` epoch seconds → returns 304 if matched
3. Calls `CollectablesService.search()` (annotated `@Cacheable("listCache")`)
4. Cache miss → calls `CardMarketScraperPort.search()` → `CardMarketScraperAdapter.search()`
5. `CardMarketWebFetcher.fetch()` navigates CardMarket via Playwright browser → returns HTML
6. `CardMarketContentParser.parseGalaryPage()` parses HTML with Jsoup → DTOs
7. `CardMarketProductMapper.toProducts()` maps DTOs → `List<Product>` domain models
8. `SearchResultRepository` persists/updates `SearchResult` with products
9. Results mapped to `List<ProductDTO>` via `CollectablesMapper.toDto()`
10. Response includes `Cache-Control: max-age=3600` and `ETag` header

**Product Details Flow (`GET /collectables/{cmId}`):**

1. HTTP request hits `CollectablesAdapter.getProductDetails()`
2. ETag check against `updatedAt` epoch seconds → returns 304 if matched
3. Calls `CollectablesService.fetchProductDetails()` (annotated `@Cacheable("detailsCache")`)
4. Checks existing product in `ProductRepository.findByCmId()`
5. If exists, scrapes fresh data via `CardMarketScraperPort.fetchProductDetails()`, compares with `hasChanges()`, updates only if changed
6. If new, scrapes and persists via `ProductRepository.save()`
7. Result mapped to `ProductDetailsDTO` with sell offers

**Persistence Flow:**

1. Domain `Product` → `ProductMapper.toEntity()` → `ProductEntity` + `SellOfferEntity` + `NameTranslationEntity`
2. `ProductSetEntity` upserted first (by `cmProductCode`), then product entity
3. JPA `save()` within `@Transactional` boundary
4. Hibernate Envers tracks all changes via `@Audited` entities
5. Read path: `ProductMapper.toDomain()` reconstructs domain model from entities

**Bootstrap Import Flow:**

1. `QuicksearchImportRunner` runs on startup (conditional on `app.data.import.enabled=true`)
2. Opens bundled SQLite database (`import/quicksearch.db`) via JDBC
3. Imports series → sets (SQLite + CSV) → cards into PostgreSQL
4. Creates `NameTranslationEntity` rows for each locale (de, en, fr)
5. Skips if already imported (checks `existsBySourceIdIsNotNull()`)

## Key Abstractions

**Port Interfaces (domain contracts):**
- `CardMarketScraperPort` — web scraping contract (`search`, `fetchProductDetails`)
- `ProductRepository` — product persistence contract (CRUD + `findByCmId`)
- `SearchResultRepository` — search result caching contract

**Use Case Interface:**
- `SearchUseCase` — application-level contract (`search`, `fetchProductDetails`, `getSearchCachedAt`, `getProductUpdatedAt`)

**Domain Models:**
- `Product` — aggregate root with nested `ProductSet`, `ProductSeries`, `SellOffer`, `StringWithValidity`
- `SearchResult` — cached search with query string and product list

## Entry Points

**Application Entry Point:**
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/TcgWatcherApplication.kt`
- Standard Spring Boot `@SpringBootApplication` with `runApplication<TcgWatcherApplication>()`

**REST API:**
- Generated from `contract/openapi.yaml` → `CollectablesApi` interface in `build/generated/`
- Implemented by `CollectablesAdapter` at `adapter/inbound/rest/CollectablesAdapter.kt`
- Endpoints: `GET /collectables/`, `GET /collectables/{cmId}`
- Actuator on port 8081: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`

**Data Import:**
- `QuicksearchImportRunner` — `ApplicationRunner` triggered at startup

## Error Handling

**Strategy:** Result<T> for expected failures, exceptions for unexpected

**Patterns:**
- Web fetcher returns `Result<String>` — scraper adapter uses `getOrElse` to return empty list/null on failure
- Spring `ResponseStatusException` for HTTP errors (e.g., 400 on missing setname)
- Resilience4j `@Retry` (3 attempts, exponential backoff 2x, 10s base) for transient scraping failures
- Resilience4j `@CircuitBreaker` (50% failure threshold, 60-call sliding window, 30s open state) for CardMarket outages
- `NotFoundException` ignored by circuit breaker (404 is expected, not a failure)
- Custom `CloudFlareException` for 403 responses from CardMarket

## Cross-Cutting Concerns

**Logging:** `io.github.oshai:kotlin-logging-jvm:7.0.14` via `KotlinLogging.logger {}` at class level. Log level: `info` for `io.github.havonte1` package.

**Caching:** Caffeine in-memory cache (`CacheConfig`) — 1 hour expiry, max 1000 entries. Two caches: `listCache` (search results), `detailsCache` (product details). HTTP ETag caching via `If-None-Match` / `304 Not Modified`.

**Validation:** Jakarta Validation API (`jakarta.validation`) + OpenAPI schema constraints (minLength, maxLength, enum).

**Auditing:** Hibernate Envers (`@Audited` on `ProductEntity`, `ProductSetEntity`, `SeriesEntity`, `SellOfferEntity`, `NameTranslationEntity`). Timestamps via `@PrePersist`/`@PreUpdate` lifecycle callbacks.

**Rate Limiting:** Resilience4j `@RateLimiter(name = "apiRateLimiter")` on both REST endpoints.

**Database:** PostgreSQL with `watcher` schema. Liquibase migrations (`db/changelog/db.changelog-master.yaml`). JPA batch size 50, `open-in-view: false`.

**Resilience:** Resilience4j retry + circuit breaker on `CardMarketWebFetcher` methods. Retry on `CloudFlareException`, `HttpServerErrorException`, `IOException`.

---

*Architecture analysis: 2026-04-03*
