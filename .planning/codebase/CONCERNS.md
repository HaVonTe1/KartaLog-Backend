# Codebase Concerns

**Analysis Date:** 2026-04-05

## Technical Debt

### Hardcoded German Labels in Content Parser
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt:167-182`
- **Issue:** `parseProductDetails` uses hardcoded German labels (`"Rarität"`, `"Erschienen"`, `"ab"`, `"Preis-Trend"`) to locate HTML elements. This breaks for any non-German locale.
- **Impact:** Product detail scraping only works for German CardMarket pages. All other locales return incorrect or empty data.
- **Fix approach:** Build a locale-to-label mapping table or use CSS selectors/data attributes instead of text-based element matching.

### Translation Map Returns Default for All Locales
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketScraperAdapter.kt:12-23`
- **Issue:** `getTranslationMap(locale)` returns `DEFAULT_TRANSLATION_MAP` regardless of locale parameter. The locale argument is completely ignored.
- **Impact:** Pagination parsing uses German labels for all locales - will fail on non-German locales since CardMarket page text varies by language.
- **Fix approach:** Create locale-specific TranslationMap instances or load from locale-aware configuration.

### N+1 Query Pattern in Import Runner
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/QuicksearchImportRunner.kt`
- **Issue:** `importSeries`, `importSets`, and `importCards` save entities one-by-one in tight loops (lines 96, 182, 338). Each `save()` triggers a separate database round-trip. For thousands of cards this is extremely slow.
- **Impact:** Import performance degrades linearly with data volume. A full import of 10k+ cards takes minutes instead of seconds.
- **Fix approach:** Use `saveAll()` with batched inserts, or use JDBC batch operations directly.

### SQLite Dependency for Data Import
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/QuicksearchImportRunner.kt`
- **Issue:** Direct JDBC connection to SQLite (`DriverManager.getConnection("jdbc:sqlite:$dbPath")`) bypasses Spring Data entirely. Raw SQL queries are embedded as strings with no compile-time validation.
- **Impact:** Schema changes to the SQLite source require manual SQL updates. No type safety.
- **Fix approach:** Migrate to a proper ETL pipeline or use a typed SQLite client.

### CSV Import Without Proper Parsing
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/QuicksearchImportRunner.kt:225`
- **Issue:** CSV parsing uses naive `line.split(';')` without handling quoted fields, escaped semicolons, or encoding issues.
- **Impact:** Malformed CSV rows silently produce incorrect data or are skipped.
- **Fix approach:** Use a proper CSV library (e.g., Apache Commons CSV or OpenCSV).

### Ktlint Disabled for Main Source
- **Files:** `build.gradle.kts:119-125`
- **Issue:** `runKtlintCheckOverMainSourceSet` and `runKtlintFormatOverMainSourceSet` are explicitly disabled. Only `ktlintFormatOverKotlinScripts` runs.
- **Impact:** Main source code is not automatically formatted or linted by ktlint, leading to inconsistent style.
- **Fix approach:** Re-enable ktlint for main source after fixing existing violations.

### Detekt Baseline Suppresses Real Issues
- **Files:** `detekt-baseline.xml`
- **Issue:** 54 detekt issues are suppressed including `CyclomaticComplexMethod`, `LongMethod`, `TooGenericExceptionCaught`, `MagicNumber`, and `ForbiddenComment`. The baseline masks real code quality problems.
- **Impact:** New violations of the same type go undetected. The baseline grows instead of issues being fixed.
- **Fix approach:** Address suppressed issues incrementally. Remove entries from baseline as they are fixed.

### Genre/Locale Fallback Returns Defaults Instead of Errors
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/Genre.kt:11`, `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/Locale.kt:16`
- **Issue:** `Genre.fromId()` returns `Genre.POKEMON` for unknown IDs, `Locale.fromId()` returns `Locale.GERMAN` for unknown codes instead of throwing or returning null.
- **Impact:** Invalid API parameters silently become valid defaults - clients receive unexpected data without knowing input was invalid.
- **Fix approach:** Return null or throw a proper validation exception for unknown values.

### Inconsistent Genre Lookup in REST Adapter
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesAdapter.kt` (lines 51-58 vs 91-93)
- **Issue:** `listCollectables()` uses `Genre.entries.find { it.identifier == }` and throws exceptions for invalid values, while `getProductDetails()` uses `Genre.fromId()` which returns defaults silently.
- **Impact:** Inconsistent validation across endpoints - some throw, others return wrong data.
- **Fix approach:** Use consistent validation approach (throw on invalid) across all endpoints.

## Code Smells

### Massive Content Parser (286 lines)
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt`
- **Issue:** `parseProductDetails` is 148 lines of dense HTML parsing with deeply nested null-safe chains. `parseGalaryPage` is similarly complex. Both have high cyclomatic complexity (suppressed in detekt baseline).
- **Impact:** Fragile to CardMarket HTML changes. Difficult to test and maintain.
- **Fix approach:** Extract smaller parsing functions per data section (price, rarity, sell offers). Use a builder pattern for DTO construction.

### Playwright Browser Options Initialized at Class Level
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/PlaywrightManager.kt:17-43`
- **Issue:** `BrowserType.LaunchOptions()` is created as a property initializer with a long list of hardcoded browser flags. This runs during bean construction before `executablePath` is fully resolved.
- **Impact:** Browser configuration is not easily testable or configurable. Adding/removing flags requires code changes.
- **Fix approach:** Move to a factory method or configuration bean.

### Mutable Collections in JPA Entities
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/ProductEntity.kt:68,81`, `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/ProductSetEntity.kt:53`
- **Issue:** Entities expose `MutableSet` for relations (`nameTranslations`, `sellOffers`). External code can modify collections without going through entity methods, bypassing JPA dirty tracking.
- **Impact:** Unexpected side effects and potential data corruption.
- **Fix approach:** Expose read-only collections and provide explicit add/remove methods.

### Hardcoded "dummy" Fallback Values
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/repository/SearchResultRepositoryAdapter.kt:95`, `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/repository/ProductRepositoryAdapter.kt:27`
- **Issue:** When `cmCode` is null, the string `"dummy"` is used as a fallback. This creates meaningless database entries.
- **Impact:** Database pollution with invalid records. Queries matching against `"dummy"` produce incorrect results.
- **Fix approach:** Use `Option`/`Result` types or throw a validation exception.

### Eager Fetching on ProductEntity Relations
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/ProductEntity.kt:66,79`
- **Issue:** `nameTranslations` and `sellOffers` use `FetchType.EAGER`. Every product query loads all translations and sell offers regardless of need.
- **Impact:** Unnecessary database load and memory usage, especially for list queries that only need basic product info.
- **Fix approach:** Change to `FetchType.LAZY` and use `@EntityGraph` for queries that need the relations.

### Duplicated parseLink Logic
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt:76-118`, `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketProductMapper.kt:78-93`
- **Issue:** URL/path parsing is implemented twice with different logic. `CardMarketContentParser.parseLink` handles search result URLs while `CardMarketProductMapper.parseLink` handles detail page URLs.
- **Impact:** Inconsistent parsing behavior. Changes to CardMarket URL structure require updates in two places.
- **Fix approach:** Extract shared URL parsing into a single utility class.

### auth.json Written to Working Directory
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt:64,86`
- **Issue:** Browser storage state is persisted to `auth.json` in the current working directory. This file is committed to git (exists at project root).
- **Impact:** Session cookies/tokens may be leaked via version control. Multiple instances overwrite each other's state.
- **Fix approach:** Use a configurable path outside the repository, or avoid persisting auth state entirely.

### Web Scraper Silently Swallows Errors
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketScraperAdapter.kt` (lines 46-47, 73-76, 88-91)
- **Issue:** `search()` and `fetchProductDetails()` return empty lists/nulls on any fetch or parse failure instead of propagating errors to callers.
- **Impact:** API consumers cannot distinguish between "no results found" and "service unavailable" - no way to retry or show appropriate error messages.
- **Fix approach:** Create a proper Result type that distinguishes between "not found" and "error" cases, expose error details to callers.

## Security Concerns

### Committed auth.json with Browser Session State
- **Files:** `auth.json` (project root)
- **Issue:** The `auth.json` file contains Playwright browser storage state (cookies, local storage). This file is tracked in git and contains session data from CardMarket.
- **Impact:** Session hijacking if the repository is public or compromised.
- **Recommendations:** Add `auth.json` to `.gitignore`. Use environment-specific paths for session storage.

### No Input Validation on API Parameters
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesAdapter.kt`
- **Issue:** All API parameters (`query`, `genre`, `type`, `locale`, `cmId`, `setname`, `lang`) are passed directly to the scraper without sanitization or validation.
- **Impact:** Malicious input could be injected into CardMarket URLs, potentially causing SSRF-like behavior or unexpected scraping targets.
- **Recommendations:** Add `@Valid` constraints and whitelist allowed values for `genre`, `type`, and `locale`.

### CloudFlareException Extends HttpServerErrorException
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CloudFlareException.kt`
- **Issue:** `CloudFlareException` extends `HttpServerErrorException` which is designed for HTTP client responses, not as a general exception type.
- **Impact:** Misleading exception hierarchy. Code catching `HttpServerErrorException` may inadvertently catch CloudFlare blocks.
- **Recommendations:** Create a standalone `RuntimeException` subclass instead.

### SQLite Path Configurable via Property
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/QuicksearchImportRunner.kt:62-63`
- **Issue:** `sqlitePath` is configurable via `app.data.import.sqlite.path` property. If set to an arbitrary path, it could read any SQLite database accessible to the JVM.
- **Impact:** Path traversal risk if the property is user-controllable.
- **Recommendations:** Restrict to a specific directory or validate the path against an allowed list.

### Hardcoded User Agent
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt:24`
- **Issue:** Hardcoded Chrome user agent (`"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36..."`) is easily detectable and blockable.
- **Impact:** CardMarket may detect and block scraping attempts based on signature.
- **Fix approach:** Rotate user agents or use realistic pool from configuration.

## Performance Issues

### No Pagination in Web Scraping
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt:15-67`, `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/CollectablesService.kt:24-45`
- **Issue:** Only the first page of search results is scraped. The `parseGalaryPage` method accepts a `page` parameter but it is never used to fetch additional pages. `SearchResultsPageDto` includes `totalPages` but this information is discarded.
- **Impact:** Users only see the first page of results (~30 products), missing potentially relevant items on subsequent pages.
- **Fix approach:** Implement multi-page scraping with configurable page limits.

### Single Browser Instance for All Requests
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/PlaywrightManager.kt:57-64`
- **Issue:** A single Chromium browser instance is shared across all requests. Each request creates a new context but they all share the same browser process.
- **Impact:** Under concurrent load, requests queue up waiting for browser resources. A single crashed page can affect all subsequent requests.
- **Fix approach:** Implement a browser context pool or use separate browser instances per request type.

### New Browser Context Per Request
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt` (lines 63-94)
- **Issue:** `fetchUrl()` creates a new `BrowserContext` for every single fetch operation. Browser context creation is expensive (~500ms each).
- **Impact:** Significant latency overhead per request, limits throughput even with concurrent browser instances.
- **Fix approach:** Reuse browser contexts with proper session management, implement context pooling.

### No Database Indexes
- **Files:** Entity classes in `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/`
- **Issue:** No explicit indexes defined for frequently queried columns (`cm_id`, `external_id`, `source_id`, `query` in SearchResult).
- **Impact:** Queries slow significantly as data grows, especially `findByCmId` and search result lookups.
- **Fix approach:** Add `@Index` annotations on entity fields or create Liquibase index migration.

### Cache Eviction Not Implemented
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/CacheConfig.kt`
- **Issue:** Caffeine cache has `maximumSize(1000)` and `expireAfterWrite(1, TimeUnit.HOURS)` but no mechanism to invalidate cache when underlying data changes (e.g., after a successful scrape update).
- **Impact:** Stale data served for up to 1 hour even when fresh data is available.
- **Fix approach:** Implement `@CacheEvict` on write operations or use cache refresh patterns.

### Cache Key Missing Locale/Genre in Details
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/CollectablesService.kt:51-92`
- **Issue:** `fetchProductDetails` uses `@Cacheable("detailsCache")` but cache key only uses cmId - locale and genre changes return stale data for different locales.
- **Impact:** Products cached for wrong locale - name/price displayed in wrong language to users.
- **Fix approach:** Include locale, genre, and type in cache key calculation.

### Excessive Debug Logging in ProductRepositoryAdapter
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/repository/ProductRepositoryAdapter.kt`
- **Issue:** 12+ debug log statements in `save()` and `saveAll()` methods, logging internal state at every step.
- **Impact:** Log volume grows significantly under load. Debug-level logging should be used sparingly in production paths.
- **Fix approach:** Reduce to entry/exit logging. Remove intermediate state logs.

## Maintainability

### No Unit Tests for Persistence Layer
- **Files:** `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/` (missing)
- **Issue:** No unit tests exist for `SearchResultRepositoryAdapter`, `ProductRepositoryAdapter`, or `ProductMapper`. Only `QuicksearchImportRunnerTest` exists for the persistence layer.
- **Impact:** Changes to entity mapping, upsert logic, or transaction boundaries are not covered by fast unit tests. Bugs only caught by slow integration tests.
- **Priority:** High

### No Tests for CollectablesAdapter (REST Controller)
- **Files:** `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesAdapterIT.kt`
- **Issue:** Only integration tests exist (tagged `@Tag("integration")`). No unit tests with mocked service layer.
- **Impact:** Slow test feedback loop. Cannot test error paths or edge cases without spinning up full Spring context.
- **Priority:** Medium

### No Tests for CollectablesMapper
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesMapper.kt`
- **Issue:** The domain-to-DTO mapper has zero test coverage.
- **Impact:** Mapping bugs (null handling, field mismatches) go undetected.
- **Priority:** Medium

### No Tests for SearchResultMapper
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/mapper/SearchResultMapper.kt`
- **Issue:** Zero test coverage for the search result entity mapper.
- **Impact:** Mapping errors between domain and entity models are not caught.
- **Priority:** Medium

### No Tests for CacheConfig and Resilience4jConfig
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/CacheConfig.kt`, `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/Resilience4jConfig.kt`
- **Issue:** Configuration classes have no tests verifying cache TTL, max size, or circuit breaker settings.
- **Impact:** Configuration regressions go unnoticed (e.g., cache TTL accidentally changed).
- **Priority:** Low

### No Tests for Error Paths in Web Scraper
- **Files:** Tests in `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/`
- **Issue:** Web scraper tests only cover happy paths, no tests for network failures, parsing errors, CloudFlare blocks, or empty results.
- **Impact:** Error handling code is untested and may not work correctly in production.
- **Priority:** High

### No Integration Tests for Multi-Page Search
- **Files:** Test coverage gaps
- **Issue:** No tests that verify pagination works correctly or handles large result sets.
- **Impact:** Pagination implementation is not validated - may silently fail.
- **Priority:** Medium

### Test Files Reference Relative Paths
- **Files:** `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParserTest.kt:17`, `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/CollectablesServiceIT.kt:38-39`
- **Issue:** Test fixtures loaded via `File("src/test/resources/...")` instead of classpath resources. Tests fail when run from a different working directory (e.g., IDE, CI).
- **Impact:** Flaky tests in different execution environments.
- **Fix approach:** Use `javaClass.classLoader.getResource("...")` or `javaClass.getResource("...")`.

### Comment Policy Contradiction
- **Files:** `AGENTS.md` states "DO NOT ADD ANY COMMENTS! No JavaDoc. No inline Comments. Nothing." but existing codebase has KDoc comments on entities, mappers, and repositories.
- **Issue:** The no-comments rule conflicts with existing code patterns and makes the codebase harder to understand.
- **Impact:** New contributors receive conflicting guidance.
- **Recommendation:** Clarify whether the rule applies only to generated code or all code.

## Known Issues

### Product Details Parsing Only Works for German Locale
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketContentParser.kt:146-147`
- **Symptoms:** When fetching product details with non-German locale, the parser fails to find elements because it searches for German text labels (`"Rarität"`, `"Erschienen"`, `"ab"`, `"Preis-Trend"`).
- **Trigger:** Any `fetchProductDetails` call with `lang != "de"`.
- **Workaround:** Always use German locale for detail pages.

### ProductEntity No-Arg Constructor Creates Invalid Instance
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/ProductEntity.kt:99`
- **Issue:** The JPA no-arg constructor `constructor() : this(0, 0, null, null)` creates an entity with `externalId = 0` which violates the `nullable = false, unique = true` constraint if accidentally persisted.
- **Impact:** Potential constraint violation if the no-arg constructor is used incorrectly.
- **Fix approach:** Use a safer sentinel value or make the constructor private with a factory method.

### SellOfferEntity No-Arg Constructor Creates Circular Reference
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/SellOfferEntity.kt:68-78`
- **Issue:** The no-arg constructor creates a new `ProductEntity()` as the `product` field, creating an orphaned product entity in memory.
- **Impact:** Confusing debugging sessions. Potential memory leaks if the orphaned entity is accidentally persisted.
- **Fix approach:** Set `product` to `null` in the no-arg constructor (requires making the field nullable).

### Circuit Breaker Only Configured for NotFoundException
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/Resilience4jConfig.kt`
- **Issue:** Only `NotFoundException` is ignored by the circuit breaker. `CloudFlareException` (403 responses) will trip the circuit breaker, which may be undesirable since CloudFlare blocks are transient.
- **Impact:** Legitimate CloudFlare challenges could permanently disable scraping until the circuit breaker resets.
- **Fix approach:** Add `CloudFlareException` to the ignored exceptions list or configure a separate retry strategy.

### Missing Retry Configuration
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/Resilience4jConfig.kt`
- **Issue:** `@Retry(name = "cardMarketRetry")` is used on fetcher methods but no retry configuration bean exists. Relies entirely on default/external configuration.
- **Impact:** Retry behavior is undefined if not configured in `application.yml`.
- **Fix approach:** Add explicit retry configuration bean or document required `application.yml` settings.

### No Rate Limiter Configuration
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/` (missing rate limiter config)
- **Issue:** `@RateLimiter(name = "apiRateLimiter")` is used on controller methods but no rate limiter configuration exists in the codebase.
- **Impact:** Rate limiting is either not active or relies on external configuration.
- **Fix approach:** Add rate limiter configuration bean.

### QuicksearchImportRunner Has No Transaction/Rollback
- **Files:** `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/QuicksearchImportRunner.kt` (lines 44-60)
- **Issue:** Import runner runs a series of operations (importSeries, importSets, importCards) without transaction handling. If import fails mid-way, database has partial data with no easy rollback.
- **Impact:** Database may have partial/inconsistent data requiring manual cleanup after failed imports.
- **Fix approach:** Wrap entire import in proper transaction or implement compensating transactions for rollback.

### Missing YGO/MTG Genre Support
- **Files:** Implied in `GenreConfig` - only Pokemon has proper search path patterns
- **Issue:** Only Pokemon genre has fully configured search URLs. YGO and MTG have placeholder or missing path patterns.
- **Impact:** Only Pokemon card search works via API, other genres fail to build proper URLs.
- **Fix approach:** Add complete URL patterns for YGO and MTG genres in `GenreConfig`.

---

*Concerns audit: 2026-04-05*
