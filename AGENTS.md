# AGENTS.md – KartaLog Backend

**Generated:** 2026-07-07
**Commit:** `f737334` (main)
**Stack:** Spring Boot 4.0.2 · Kotlin 2.2.x · Java 17+ · Gradle 9.0 · PostgreSQL 18 · Playwright 1.61.0

## Quick Start
- **Build:** `./gradlew build`
- **Full check (unit + integration):** `./gradlew check`
- **Build without tests (fast compile):** `./gradlew build -x test`
- **Run unit tests only:** `./gradlew test --exclude-tags integration`
- **Run integration tests only:** `./gradlew integrationTest`
- **Single test class:** `./gradlew test --tests "io.github.havonte1.kartalog.backend.<package>.<TestClass>"`
- **Single test method:** `./gradlew test --tests "ClassName.\`method description\`"`
- **Format (ktlint):** `./gradlew ktlintFormat` (triggers OpenAPI codegen first)
- **Codegen (OpenAPI):** `./gradlew compileKotlin` (triggers `openApiGenerate` first)
- **Coverage report:** `./gradlew test jacocoTestReport`
- **Parallel build:** `./gradlew build -T 1C`
- **Run app (local):** `./gradlew bootRun` → `http://localhost:8083`, Swagger at `/swagger-ui.html`
- **Run app (Docker):** `SPRING_PROFILES_ACTIVE=compose ./gradlew bootRun` → `http://localhost:8080`

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Entry point / main | `KartaLogApplication.kt` | `@SpringBootApplication` |
| REST controllers | `adapter/inbound/rest/` | Implements OpenAPI-generated interfaces |
| Domain models | `domain/model/` | Pure Kotlin, no framework annotations |
| Port interfaces | `domain/port/out/` | Hexagonal boundary contracts |
| Use cases / services | `application/` | Orchestrates domain + ports |
| JPA entities | `adapter/out/persistence/entity/` | Hibernate Envers audited |
| Persistence adapters | `adapter/out/persistence/repository/` | Adapter → JpaRepository pattern |
| Web scraping | `adapter/out/webscraper/cardmarket/` | Playwright + Jsoup pipeline |
| Scraping strategies | `adapter/out/webscraper/strategy/` | 6 runtime-switchable strategies + selector |
| Scraper management API | `adapter/inbound/rest/strategy/` | `/actuator/scraper/*` |
| Scraping config | `config/` | `ScrapingStrategyConfig` bean wiring |
| Configuration | `config/` | Cache, Resilience4j, Health, Genre |
| OpenAPI contract | `contract/openapi.yaml` | API-first source of truth |
| DB migrations | `src/main/resources/db/changelog/` | Liquibase changeLogs |
| Deployment | `deployment/` | Dockerfile, compose, nginx, scripts |

## Architecture
- **Pattern:** Hexagonal — `domain` (pure models + port interfaces) → `application` (use-case services) → `adapter/inbound` (REST controllers, security) → `adapter/out` (persistence + webscraper implementations)
- **Entry point:** `KartaLogApplication.kt`
- **API-first:** `contract/openapi.yaml` → OpenAPI Generator (kotlin-spring, coroutines mode) → `build/generated/src/main/kotlin/`. Controllers in `adapter/inbound/rest/` implement generated interfaces. **Do not edit generated code.**
- **Database:** PostgreSQL (runtime) + SQLite (embedded, for quicksearch import). Liquibase migrations. Hibernate Envers for entity auditing. Separate DB users: `watcher_mig` (migrations), `watcher_app` (app), `watcher_readonly` (read-only).
- **Scraping:** Playwright (1.61.0) + Jsoup (1.22.1) → `adapter/out/webscraper/`. Coroutines-based (`suspend` functions). Resilience4j (2.3.0) circuit breaker (50% failure threshold, 60-call sliding window, 30s open) + retry (3 attempts, 10s base, 2x backoff). `NotFoundException` is ignored by circuit breaker via Java config (YAML `ignore-exceptions` is bugged).
- **Caching:** Caffeine (1h expiry, 1000 max entries) + HTTP ETag/If-None-Match.
- **Actuator:** Management port 8084 (local) / 8081 (Docker). Endpoints: `health,info,metrics,prometheus,loggers,env,heapdump,threaddump`. Spring Boot Admin client auto-registers to `http://localhost:9090`. Scraper management at `/actuator/scraper/*`.
- **Scraping strategies (6 total):**
  - In-process Java Playwright: `chromium` (lazy init), `camoufox` (lazy init)
  - Out-of-process sidecar: `puppeteer-worker` (port 3000), `playwright-extra-worker` (port 3001), `camoufox-python-worker` (port 3002)
  - Out-of-process real Chrome CDP: `chrome-cdp` (port 9222 host → 9223 socat)
  - Switch at runtime: `PUT /actuator/scraper/strategy {"strategy":"chrome-cdp"}`
- **Sidecar workers:** Each self-contained Docker container with its own browser. App delegates via HTTP.
- **BrowserContextPool:** Accepts `suspend` lambdas for coroutine-friendly context management.
- **CloudflareChallengeSolver.kt:** Detects Turnstile via JS + polling, attempts checkbox click in iframe. Only works for interactive checkbox variant; non-interactive (`/dark/fbE/new/normal`) cannot be clicked.
- **Camoufox Python worker:** FastAPI + Camoufox (Stealth Firefox fork) + playwright-captcha ClickSolver. Dodges initial Cloudflare block (200 vs 403 from other strategies) but Turnstile widget fails to initialize (`turnstile` global undefined). Challenge page persists indefinitely. Identified issue: Turnstile API v0/b/80a697ecdece fails silently in Camoufox Firefox.
- **Chrome CDP strategy (`chrome-cdp`):** Connects to a real Chrome instance running on the Docker host via Playwright's `connectOverCDP`. This is the only strategy that fully bypasses Cloudflare Bot Management because it uses the user's real Chrome with its full profile, trusted fingerprint, cookies, and browsing history. All headless browsers (Camoufox, Playwright Chromium, puppeteer-extra-stealth) are detected by Cloudflare and given a "managed" JavaScript proof-of-work challenge that never completes for headless browsers. The user's real Chrome works because the IP itself is clean — fingerprint mismatch in headless browsers triggers the harder challenge type. Chrome 150+ ignores `--remote-debugging-address=0.0.0.0` on Linux, so `scripts/start-chrome-cdp.sh` uses `socat` to bridge `0.0.0.0:9223` ↔ `127.0.0.1:9222`. The strategy uses a 60s navigation timeout (not 15s like headless strategies) because real Chrome loads CardMarket without Cloudflare but pages with images/JS can take longer. The 15s timeout was causing Resilience4j retries (3 attempts) that triggered duplicate page loads. `PLAYWRIGHT_BROWSERS_PATH=/app/playwright-data` is set in the Dockerfile to persist Playwright browser binaries on the `playwright-data` volume across restarts, avoiding hundreds-of-MB re-downloads on every rebuild. Chrome DevTools HTTP server rejects requests with non-IP `Host` headers, so the CDP URL uses `172.17.0.1` (Docker gateway IP) instead of `host.docker.internal`.

## CODE MAP
| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `KartaLogApplication` | class | `backend/` | Spring Boot entry point |
| `CollectablesAdapter` | class | `adapter/inbound/rest/` | REST controller (implements generated API) |
| `CollectablesMapper` | object | `adapter/inbound/rest/` | API schema ↔ domain mapping |
| `CollectablesService` | class | `application/` | Use-case service |
| `SearchUseCase` | interface | `application/` | Search contract |
| `SearchResponse` | data class | `application/` | Search result DTO |
| `Product` | data class | `domain/model/` | Core domain model |
| `SearchResult` | data class | `domain/model/` | Domain search result |
| `SellOffer` | data class | `domain/model/` | Pricing data |
| `ProductSet` | data class | `domain/model/` | Set info |
| `ProductSeries` | data class | `domain/model/` | Series info |
| `LanguagePricing` | data class | `domain/model/` | Multi-language pricing |
| `Genre` | enum class | `domain/model/` | Card game genre |
| `Locale` | enum class | `domain/model/` | Language locale |
| `ProductType` | enum class | `domain/model/` | Product type |
| `CardMarketScraperPort` | interface | `domain/port/out/` | Scraping port |
| `ProductRepository` | interface | `domain/port/out/` | Product persistence port |
| `SearchResultRepository` | interface | `domain/port/out/` | Search result persistence port |
| `ProductEntity` | data class | `adapter/out/persistence/entity/` | JPA entity |
| `SearchResultEntity` | class | `adapter/out/persistence/entity/` | JPA entity |
| `SellOfferEntity` | data class | `adapter/out/persistence/entity/` | JPA entity |
| `ProductRepositoryAdapter` | class | `adapter/out/persistence/repository/` | Port → JPA adapter |
| `SearchResultRepositoryAdapter` | class | `adapter/out/persistence/repository/` | Port → JPA adapter |
| `CardMarketScraperAdapter` | class | `adapter/out/webscraper/cardmarket/` | Scraping implementation |
| `CardMarketGalleryParser` | class | `adapter/out/webscraper/cardmarket/` | Parses gallery HTML |
| `CardMarketDetailsParser` | class | `adapter/out/webscraper/cardmarket/` | Parses details HTML |
| `CardMarketWebFetcher` | class | `adapter/out/webscraper/cardmarket/` | URL building + delegates to active strategy |
| `ScrapingStrategy` | interface | `adapter/out/webscraper/strategy/` | Strategy contract: id, fetch(), close() |
| `ScrapingStrategySelector` | class | `adapter/out/webscraper/strategy/` | Runtime switch via AtomicReference |
| `ScrapingStrategyRegistry` | class | `adapter/out/webscraper/strategy/` | Strategy lookup by id |
| `ChromiumPlaywrightStrategy` | class | `adapter/out/webscraper/strategy/` | In-process: Java Playwright + Chromium |
| `CamoufoxPlaywrightStrategy` | class | `adapter/out/webscraper/strategy/` | In-process: Java Playwright + Camoufox |
| `PuppeteerWorkerStrategy` | class | `adapter/out/webscraper/strategy/` | Out-of-process: HTTP → scraper-worker |
| `PlaywrightExtraWorkerStrategy` | class | `adapter/out/webscraper/strategy/` | Out-of-process: HTTP → playwright-extra worker |
| `CamoufoxPythonWorkerStrategy` | class | `adapter/out/webscraper/strategy/` | Out-of-process: HTTP → camoufox-python-worker |
| `ChromeCdpStrategy` | class | `adapter/out/webscraper/strategy/` | Out-of-process: real Chrome via CDP |
| `ScraperManagementController` | class | `adapter/inbound/rest/strategy/` | /actuator/scraper/* endpoints |
| `ScrapingStrategyConfig` | class | `config/` | Bean wiring for all 6 strategies |
| `start-chrome-cdp.sh` | script | `scripts/` | Launches Chrome + socat on host for CDP |
| `export-cookies.py` | script | `scripts/` | Exports Chrome cookies for other strategies |
| `PlaywrightManager` | class | `adapter/out/webscraper/` | Legacy browser lifecycle (not used by strategies) |
| `BrowserContextPool` | class | `adapter/out/webscraper/` | Browser context reuse |
| `CacheConfig` | class | `config/` | Caffeine cache setup |
| `Resilience4jConfig` | class | `config/` | Circuit breaker Java config |
| `CardMarketHealthIndicator` | class | `config/` | Actuator health check |
| `GenreConfig` | object | `config/` | Genre → CardMarket mapping |

## Critical Constraints
- **NO COMMENTS:** No JavaDoc, no KDoc, no inline comments. Exception: `TODO` must reference an issue key.
- **Null-safety:** Prefer non-nullable types. Use `Result<T>` for expected failures; `throw` for unexpected.
- **WebMVC, not WebFlux:** Despite `reactive: true` in OpenAPI generator config, the runtime is Spring WebMVC.
- **Import ordering:** Third-party imports first, blank line, then project imports. Alphabetical within each group. No wildcard imports.
- **Code style:** 4-space indentation, 120 char line length, PascalCase classes, camelCase functions/variables, UPPER_SNAKE_CASE constants.

## Testing
- **Mocking:** `mockk` only. Use `com.ninjasquad.springmockk.MockkBean` for Spring Boot test bean replacement.
- **Unit tests:** `src/test/kotlin/`, exclude `integration` and `e2e` JUnit tags.
- **Integration tests:** `*IT` classes, include `integration` tag. Use `@SpringBootTest` + Testcontainers (PostgreSQL `postgres:18.1-alpine`). Testcontainers container reuse enabled (`.testcontainers.json`).
- **Test profile (`test`):** `lazy-initialization: true`, Liquibase context `test`, app data import disabled.
- **Parallel execution:** Disabled globally (`junit-platform.properties`).
- **Naming:** Backtick descriptions preferred: `` `search returns one product` ``. Test classes: `ClassNameTest` (unit), `ClassNameIT` (integration).
- **Async tests:** Wrap `suspend` function calls in `runBlocking`.
- **Fixtures:** HTML snapshots in `src/test/resources/` (e.g., `pikachu_gallery_size30_v2.html`).
- **Verbose debugging:** Append `--info` or `-i` to any Gradle command.

## Workflow
- **Branching:** `feature/<ticket-id>-description` or `bugfix/<ticket-id>-description`.
- **Commits:** Atomic, prefixed `fix:`, `feat:`, `refactor:`.
- **Lint note:** Detekt main task is disabled in CI (`build.gradle.kts` disables it). Detekt config at `config/detekt/detekt.yml` is for IDE use only. Ktlint runs only on test sources and Gradle scripts.

## Config & Ops
- **Docker Compose:** `deployment/compose.yml` — profiles: `local` (postgres via `spring-boot-docker-compose`), `deployment` (full stack: app, nginx, admin-server, postgres, ofelia, scraper-worker, scraper-worker-playwright, scraper-worker-python).
- **Docker build:** `deployment/Dockerfile` — multi-stage, JDK 17 build → JRE 24 runtime. Chromium + Node.js installed in runtime image.
- **Env vars:** `.env` file (gitignored). Required: `WATCHER_READONLY_PWD`, `POSTGRES_PASSWORD`, `MIGRATION_PWD`, `WATCHER_MIG_PWD`, `WATCHER_APP_PWD`, `APPLICATION_PWD`, `JWT_SECRET`. JWT has hardcoded fallback secret in `application.yml`.
- **CI:** Self-hosted runner, runs `./gradlew check` on push/PR to `main`.
- **Gradle:** Configuration cache enabled. JVM args: `-Xmx2048m`. `gradle.properties` enables parallel execution and build caching.
- **Boot quirk:** `compileKotlin` and `ktlintFormat` both depend on `openApiGenerate`, so codegen always runs before compilation or formatting.

## Module Guidelines

Sub-modules with their own AGENTS.md:
- `adapter/out/persistence/` — JPA entities, mappers, repository adapters
- `adapter/out/webscraper/` — Playwright/JSoup scraping pipeline
- `deployment/` — Docker, compose, CI, backup scripts
- `scraper-worker-python/` — Camoufox Python sidecar worker
