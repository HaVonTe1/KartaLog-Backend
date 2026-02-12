# TCGWatcher Backend - Critical Issues & Improvement Todos

---

## 🐛 SEVERE ISSUES

### 1. Product Audit Trail Not Persisted
- already fixed
---

### 2. Multi-Language Product Names Lost During Caching
**Problem:** When caching search results, product name translations are cleared in `SearchResultRepositoryAdapter.upsertProducts()` because the `clear()` call removes all existing translations before adding new ones.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/repository/SearchResultRepositoryAdapter.kt:32`
**Solution:** Update translation mapping to preserve existing languages or implement proper merge logic that doesn't discard non-duplicate locales.

---

### 3. Race Condition in Product Upsert
**Problem:** `upsertProducts()` reads existing products, then saves back without transaction isolation. Concurrent requests scraping same products can create duplicates or overwrite prices incorrectly.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/repository/SearchResultRepositoryAdapter.kt:41-57`
**Solution:** Use database-level upsert (INSERT ... ON CONFLICT) via JPA `merge()` or native query with proper locking.

---

### 4. Missing Database Constraints
**Problem:** Liquibase changelog creates products table without unique constraint on `external_id`, allowing duplicate products from different search queries.
**Location:** `src/main/resources/db/changelog/20260207-init.xml:10-33`
**Solution:** Add `unique="true"` to `external_id` column definition and add indexes for commonly queried fields (`setName`, `rarity`, `genre`, `type`).

---

### 5. Hardcoded Credentials and URLs
**Problem:** PostgreSQL password is hardcoded as "admin" in compose.yml, and CardMarket URL uses hardcoded German locale.
**Location:** 
- `deployment/compose.yml:7`
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt:50`
**Solution:** 
- Use environment variable substitution or Docker secrets
- Make locale/game configurable via application properties

---

### 6. Playwright Memory Leak Risk
**Problem:** Creates new browser instance per request without connection pooling, causing memory pressure under load.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt:29-66`
**Solution:** Implement a singleton or scoped bean that reuses Playwright browser context with proper cleanup on shutdown.

---

### 7. No Search Query Validation
**Problem:** Empty strings and potentially malicious search strings are accepted without validation, triggering expensive scraping operations.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/SearchUseCase.kt:10-12`
**Solution:** Add `@Size(min=1, max=255)` constraint on parameter and implement sanitization to prevent abuse.

---

## ⚠️ ARCHITECTURAL FLAWS

### 8. Broken Hexagonal Architecture
**Problem:** `SearchResult` entity contains `List<Product>` directly instead of using references IDs, creating circular dependency between entities and violating hexagonal architecture principles.
**Location:** 
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/SearchResult.kt:13`
- `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/SearchResultEntity.kt:47`
**Solution:** Change to use product IDs in cache, or flatten SearchResult to only store query metadata without embedded products.

---

### 9. Synchronous Blocking Call in Coroutine
**Problem:** `runBlocking` blocks the thread in `CollectablesService.search()` despite using coroutines elsewhere.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/CollectablesService.kt:50`
**Solution:** Remove `runBlocking`, make `SearchUseCase.search()` a suspend function, and let Spring Web handle async execution.

---

### 10. No Error Handling for External Services
**Problem:** `CardMarketWebFetcher` has no retry logic, timeout configuration, or fallback if CardMarket HTML structure changes.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt:27-66`
**Solution:** 
- Add configurable timeouts (Playwright's `setTimeout`)
- Implement retry with exponential backoff
- Add circuit breaker pattern using Resilience4j
- Return `Result<List<Product>>` to propagate failures

---

### 11. Incorrect Cache TTL Logic
**Problem:** Cache expiration check uses wrong logic: `cachedAt.isAfter(now.minus(ttl))` returns true when cache should be EXPIRED.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/CollectablesService.kt:39`
**Solution:** Change to: `now.isBefore(cachedAt.plus(ttl))` or `Duration.between(cachedAt, now).abs().toHours() < ttlHours`

---

### 12. Pagination Boundary Bug
**Problem:** `subList(from, to)` throws exception when `from >= to`, which happens with empty results or invalid page numbers.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesAdapter.kt:35`
**Solution:** Add boundary checks: validate page/size parameters, handle edge cases where from > results.size.

---

## 📊 TESTING ISSUES

### 13. All Integration Tests Disabled
**Problem:** All integration tests use `Assumptions` that fail when HTML fixtures are missing, rendering tests useless.
**Location:** 
- `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/SearchResultProductBehaviorTest.kt:78-82`
- `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/CollectablesServiceIntegrationTest.kt`
**Solution:** Create proper test HTML fixtures in `src/test/resources` with descriptive names, or mock the web scraper interface completely.

---

### 14. Missing Core Business Logic Tests
**Problem:** No direct unit tests for `SearchUseCase`, `CollectablesService.search()`, or repository adapter methods.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/`
**Solution:** Add unit tests with mocked dependencies for:
- Cache hit/miss scenarios
- TTL boundary conditions (exactly at expiry)
- Empty search results
- Null/empty query handling

---

### 15. No REST Contract Testing
**Problem:** OpenAPI spec exists but no tests validate that actual endpoints match the contract.
**Location:** `contract/openapi.yaml`
**Solution:** Add `@AutoConfigureMockMvc` tests for `/collectables/` endpoint covering:
- Pagination parameters
- Search query handling
- Response structure validation

---

## 🐛 CODE QUALITY ISSUES

### 16. Inconsistent Field Naming Conventions
**Problem:** Multiple names for similar concepts: `cmId`, `externalId`, `id`; `imgLink` vs expected `imageUrl`.
**Location:** All domain models and entities
**Solution:** Establish naming convention:
- Use `id` only for database primary key
- Use `externalId` for external system identifiers
- Rename `imgLink` to `imageUrl` throughout

---

### 17. Magic Strings in Code
**Problem:** Language codes ("de"), HTML selectors, and regex patterns scattered without constants.
**Location:** Multiple files including parsers and mappers
**Solution:** Create constants file with:
- Supported language codes as enum
- CSS selectors as strings
- Regex patterns as `private val` constants

---

### 18. Over-Engineered Link Parsing
**Problem:** Complex regex logic in `parseLink()` when JSoup's built-in methods could simplify.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt:63-85`
**Solution:** Refactor to use JSoup URL resolution or simpler string splitting instead of complex regex.

---

### 19. Missing Null Safety Checks
**Problem:** Multiple places access `.text()` on potentially missing DOM elements, returning empty strings that cause downstream errors.
**Location:** `CardMarketContentParser.kt:28-57`
**Solution:** Add null checks or default values before accessing element properties.

---

## 🔒 SECURITY ISSUES

### 20. No Rate Limiting
**Problem:** REST endpoint accepts unlimited requests without throttling, enabling DDoS attacks.
**Location:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesAdapter.kt:24-38`
**Solution:** Add `@RateLimiter` or Spring Web `HandlerInterceptor` to limit requests per IP.

---

### 21. SQL Injection Risk via Search String
**Problem:** Although JPA prevents direct SQL injection, the dynamic search string is directly concatenated into CardMarket URL without sanitization.
**Location:** `CardMarketWebFetcher.kt:50`
**Solution:** URL-encode the search parameter before constructing the URL.

---

## 📦 DEPLOYMENT ISSUES

### 22. Dockerfile Runs as Root
**Problem:** Container runs with root user, violating security best practices.
**Location:** `deployment/Dockerfile:1-5`
**Solution:** Add non-root user and switch to it:
```dockerfile
USER nobody
```

---

### 23. No Health Checks in Docker
**Problem:** Docker container has no healthcheck configured, making orchestration unreliable.
**Location:** `deployment/Dockerfile`
**Solution:** Add `HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health || exit 1`

---

### 24. Missing JVM Memory Configuration
**Problem:** Docker container doesn't limit JVM memory, potentially causing OOM in containers.
**Location:** `deployment/Dockerfile`
**Solution:** Add JVM flags: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`

---

## 📈 PERFORMANCE ISSUES

### 25. N+1 Query Risk
**Problem:** Loading `SearchResult` with eagerly fetched products may cause N+1 queries if not optimized.
**Location:** `SearchResultEntity.kt:37-46`
**Solution:** Use `@Query` with JOIN FETCH in repository or batch fetch strategy.

---

### 26. No Connection Pooling
**Problem:** Each scraping request creates new Playwright browser context, no reuse of connections.
**Location:** `CardMarketWebFetcher.kt:29-35`
**Solution:** Implement singleton Playwright manager that reuses browser instances.

---

## 🧪 TEST COVERAGE GAPS

### 27. Missing Mapper Null Tests
**Problem:** No tests for mapper behavior with null/missing values.
**Location:** `ProductMapper`, `SearchResultMapper`
**Solution:** Add unit tests for:
- Null entity fields
- Empty collections
- Missing optional properties

---

### 28. Entity equals/hashCode Not Tested
**Problem:** `ProductEntity.equals()` and `hashCode()` logic not verified.
**Location:** `ProductEntity.kt:87-99`
**Solution:** Add tests for:
- Same ID equality
- ExternalId-based equality when IDs null
- Null handling in hash calculation

---

### 29. Missing Integration Test Coverage
**Problem:** No tests for database persistence with Liquibase migrations.
**Location:** `deployment/postgres/initdb.d/init-postgres.sql`
**Solution:** Add integration test that:
1. Starts with empty DB
2. Runs migrations
3. Tests CRUD operations

---

## 🔧 REFACTORING PRIORITIES (HIGH TO LOW)

### Priority 1 - Critical Bugs
- [ ] Fix cache TTL logic (Issue #11)
- [ ] Add database constraints (Issue #4)
- [ ] Remove runBlocking blocking calls (Issue #9)
- [ ] Fix pagination boundary bug (Issue #12)

### Priority 2 - Architecture & Design
- [ ] Decide on Product audit fields (Issue #1)
- [ ] Fix SearchResult entity structure (Issue #8)
- [ ] Add proper error handling with retries (Issue #10)
- [ ] Implement circuit breaker pattern (Issue #10)

### Priority 3 - Security
- [ ] Remove hardcoded credentials (Issue #5)
- [ ] Add rate limiting (Issue #20)
- [ ] Run Docker as non-root (Issue #22)

### Priority 4 - Testing
- [ ] Fix disabled integration tests (Issue #13)
- [ ] Add SearchUseCase unit tests (Issue #14)
- [ ] Add mapper null safety tests (Issue #27)

### Priority 5 - Code Quality
- [ ] Standardize field naming conventions (Issue #16)
- [ ] Extract magic strings to constants (Issue #17)
- [ ] Simplify link parsing logic (Issue #18)

---

## 📝 NOTES

- All issues are ordered by severity and impact
- Issues with "SEVERE" prefix should be fixed before next release
- Architecture issues (#8, #9) require design review
- Testing gaps (#13, #14, #27, #28, #29) indicate need for test strategy
