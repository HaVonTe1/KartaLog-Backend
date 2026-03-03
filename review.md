# Code Review: TCGWatcher-Backend

## 1. Dual Caching Mechanism Creates Confusion

**Summary:** The `CollectablesService` uses both Spring's `@Cacheable` annotation AND custom TTL logic. This creates redundant caching with different behaviors - Spring cache uses the method result as value while custom logic checks `cachedAt` timestamp.

**Suggested Solution:** Choose ONE caching strategy. Either use Spring's `@Cacheable` with proper cache configuration (Redis, Caffeine, etc.) OR implement custom caching with the `SearchResultRepository` but remove the `@Cacheable` annotation to avoid confusion.

**Criticality:** Medium

---

## 2. Unused Import in CardMarketWebFetcher

**Summary:** Line 22 imports `kotlin.math.log` which is never used in the file.

**Suggested Solution:** Remove the unused import `kotlin.math.log`.

**Criticality:** Low

---

## 3. Security Risk: Auth.json Storage in Working Directory

**Summary:** The `CardMarketWebFetcher` writes session state to `auth.json` in the working directory (`Path.of("auth.json")`). This could expose authentication tokens or session data if the application is deployed in a shared environment or if the working directory is world-readable.

**Suggested Solution:** Store the auth state in a secure, application-controlled location like `/var/run/tcgwatcher/auth.json` or use environment-specific paths with proper file permissions. Consider encrypting the stored state.

**Criticality:** High

---

## 4. EAGER Fetching Causes N+1 Query Problems

**Summary:** Both `ProductEntity` (lines 71-77, 79-85) and `SearchResultEntity` (lines 35-45) use `FetchType.EAGER` for collections. This loads all related data immediately, causing potential performance issues with large datasets.

**Suggested Solution:** Change to `FetchType.LAZY` and use `JOIN FETCH` queries when needed. For `SearchResultEntity`, consider paginating or limiting the number of products loaded.

**Criticality:** High

---

## 5. Missing Database Index on Search Results Query

**Summary:** The `SearchResultEntity` table has a unique constraint on `query` (line 22), but there's no explicit index for lookups. The query column should be indexed for faster retrieval.

**Suggested Solution:** Add an index on the `query` column in the Liquibase migration: `<createIndex indexName="idx_search_results_query" tableName="search_results" unique="true">`.

**Criticality:** Medium

---

## 6. Reserved SQL Keyword as Column Name

**Summary:** `SellOfferEntity` uses `condition` (line 45) as a column name, which is a reserved keyword in most SQL dialects including PostgreSQL.

**Suggested Solution:** Rename the column to `condition_type` or `item_condition` and update the corresponding Liquibase migration.

**Criticality:** Medium

---

## 7. Inconsistent Error Handling in Scraper

**Summary:** The `CardMarketScraperAdapter` returns empty list on fetch failure (line 31) and null on details fetch failure (line 49). No distinction is made between "not found" and "error occurred" - both result in silent failures logged as warnings.

**Suggested Solution:** Introduce a sealed class or Result type that properly distinguishes between: `Success`, `NotFound`, `NetworkError`, `ParsingError`. Propagate these errors to the service layer for appropriate HTTP responses.

**Criticality:** Medium

---

## 8. Hardcoded Geolocation in Web Fetcher

**Summary:** The `CardMarketWebFetcher` hardcodes Berlin coordinates (lines 26-27: `BERLIN_LAT = 52.5200`, `BERLIN_LONG = 13.4050`). This is not configurable.

**Suggested Solution:** Move to configuration properties and make it configurable via `application.yml` with sensible defaults.

**Criticality:** Low

---

## 9. No Pagination in Repository Methods

**Summary:** `ProductRepository.findAll()` and `SearchResultRepository` methods load all entities into memory without pagination support. With large datasets, this will cause `OutOfMemoryError`.

**Suggested Solution:** Add paginated variants: `findAll(pageable: Pageable)`, `findByQuery(query: String, pageable: Pageable)`.

**Criticality:** High

---

## 10. Entity equals/hashCode Implementation Is Problematic

**Summary:** Both `ProductEntity` (lines 105-111) and `SearchResultEntity` (lines 50-57) have custom `equals` that compares by ID if present, otherwise by business key. This can cause issues in Sets and when JPA merges entities.

**Suggested Solution:** Use JPA-generated IDs only for equality after the entity is persisted, or use a stable business key. Consider using Hibernate's `@NaturalId` for business keys.

**Criticality:** Medium

---

## 11. Redundant Transaction Annotation on Delete Operations

**Summary:** `SearchResultRepositoryAdapter.deleteAll()` (line 106) has `@Transactional` but `ProductRepositoryAdapter.deleteAll()` (line 18) does not. This inconsistency may cause issues if delete operations need to be rolled back.

**Suggested Solution:** Add `@Transactional` to all methods that modify data in both repository adapters.

**Criticality:** Low

---

## 12. OpenAPI Contract Mismatch with Implementation

**Summary:** The OpenAPI spec marks `setname` as required (line 111) but the implementation has it as optional parameter. Similarly, `genre`, `type`, and `lang` have defaults but aren't marked as required=false properly.

**Suggested Solution:** Update the OpenAPI contract to match implementation: mark `setname` as required=true in path but optional in query parameters, or adjust implementation to match the contract.

**Criticality:** Low

---

## 13. No Input Sanitization for Search Query

**Summary:** The search endpoint accepts any string without sanitization. While Playwright handles some escaping, malicious input could potentially cause issues.

**Suggested Solution:** Add `@Size(max = 255)` validation and sanitize special characters that might cause issues in URL encoding or HTML parsing.

**Criticality:** Medium

---

## 14. Test Coverage Has Conditional Skips

**Summary:** Tests like `CardMarketScraperAdapterTest` use `Assumptions.assumeTrue()` to skip if fixture files are missing. This means tests may silently pass in CI without actually testing.

**Suggested Solution:** Include test fixtures in the repository or fail the build if fixtures are missing. Consider embedding test HTML as string constants or using a test resources generator.

**Criticality:** Medium

---

## 15. Web Fetcher Creates New Context Per Request

**Summary:** In `CardMarketWebFetcher.fetchUrl()` (lines 66-94), a new browser context is created for every request. While this is good for isolation, it's expensive. The PlaywrightManager already maintains a singleton browser but doesn't reuse contexts.

**Suggested Solution:** Consider using a context pool or reusing contexts with proper state management to reduce resource consumption.

**Criticality:** Low

---

## 16. Missing @Transactional on Service Save Operations

**Summary:** `CollectablesService.save()` calls `searchResultRepository.save()` (line 58) without explicit transaction handling. While JPA repositories are transactional by default, the service layer should have explicit `@Transactional` for better control and consistency.

**Suggested Solution:** Add `@Transactional` to the `search()` and `fetchProductDetails()` methods in `CollectablesService`.

**Criticality:** Medium

---

## 17. Race Condition in Product Upsert

**Summary:** In `SearchResultRepositoryAdapter.upsertProducts()` (lines 46-68), there's a potential race condition between checking for existing products and inserting new ones. The comment mentions transaction isolation but the implementation doesn't explicitly set isolation level.

**Suggested Solution:** Use database-level UPSERT (`ON CONFLICT DO UPDATE`) or add `@Transactional(isolation = Isolation.SERIALIZABLE)` to prevent race conditions.

**Criticality:** Medium

---

## 18. TimeLimiter Configuration Never Used

**Summary:** The `application.yml` configures `cardMarketTimeLimiter` (lines 71-75) but the `@TimeLimiter` annotation is not applied to any method in `CardMarketWebFetcher`.

**Suggested Solution:** Either apply `@TimeLimiter(name = "cardMarketTimeLimiter")` to the fetch methods or remove the configuration.

**Criticality:** Low

---

## 19. Missing Circuit Breaker on Scraper Adapter

**Summary:** While `CardMarketWebFetcher` has circuit breaker annotations (lines 36-37), the `CardMarketScraperAdapter` doesn't have any resilience4j annotations. If the web fetcher fails fast, the adapter will propagate errors without circuit breaker protection at that level.

**Suggested Solution:** Add `@CircuitBreaker` to `CardMarketScraperAdapter.search()` and `fetchProductDetails()` methods.

**Criticality:** Medium

---

## 20. Product Details Always Triggers Scraping

**Summary:** In `CollectablesService.fetchProductDetails()` (lines 74-95), even when a product exists in the database, the code ALWAYS calls the scraper to get fresh data, then compares. This defeats the purpose of caching and will hit CardMarket on every request.

**Suggested Solution:** Implement a proper cache with configurable TTL for product details. Only scrape if cache is stale or expired.

**Criticality:** High

---

## 21. No Graceful Shutdown for Playwright

**Summary:** The `PlaywrightManager` closes browser and playwright in `@PreDestroy`, but there's no timeout or force-kill mechanism. If browser operations are hanging, the application may not shutdown cleanly.

**Suggested Solution:** Add a timeout to the shutdown process and use `CompletableFuture.orTimeout()` to force termination if cleanup takes too long.

**Criticality:** Low

---

## 22. Verbose Logging in Production

**Summary:** The `application.yml` sets log levels to `trace` for the application and web (lines 76-81), which will generate enormous log volume in production.

**Suggested Solution:** Use profile-based logging: set `trace` for development profile only, use `info` or `warn` for production.

**Criticality:** Medium

---

## 23. Incomplete Product Details Mapping

**Summary:** `CollectablesMapper.toDetailDto()` (line 32) constructs `detailsUrl` manually without proper URL encoding. Special characters in parameters could produce invalid URLs.

**Suggested Solution:** Use `UriComponentsBuilder` from Spring Web for proper URL construction with encoding.

**Criticality:** Low

---

## 24. SellOffer Not Audited but Product Is

**Summary:** `SellOfferEntity` has `@Audited` (line 19) but its parent `ProductEntity` also has auditing. However, the relationship might not properly track sell offer changes in the audit history.

**Suggested Solution:** Review Hibernate Envers configuration to ensure related entities are properly audited together.

**Criticality:** Low

---

## 25. No API Versioning Strategy

**Summary:** The API is at version 0.1.0 but there's no versioning strategy (URL path, header, or query param). Future breaking changes will be difficult to introduce.

**Suggested Solution:** Implement API versioning from the start, e.g., `/api/v1/collectables/`.

**Criticality:** Low
