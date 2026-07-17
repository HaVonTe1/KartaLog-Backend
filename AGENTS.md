# AGENTS.md – KartaLog Backend

**Generated:** 2026-07-17
**Commit:** `main` (current)
**Stack:** Spring Boot 4.1.0 · Kotlin 2.3.21 · Java 17+ · Gradle 9.6.1 · PostgreSQL 18 · Playwright 1.61.0

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
- **Pipeline validation:** `./test-pipeline.sh` (nginx → app → workers → CardMarket)
- **Verbose debugging:** Append `--info` or `-i` to any Gradle command

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
| Scraping strategies | `adapter/out/webscraper/strategy/` | 7 runtime-switchable strategies + selector |
| Scraper management API | `adapter/inbound/rest/strategy/` | `/actuator/scraper/*` |
| Configuration | `config/` | Cache, Resilience4j, Health, Genre, strategy wiring |
| OpenAPI contract | `contract/openapi.yaml` | API-first source of truth |
| DB migrations | `src/main/resources/db/changelog/` | Liquibase changelogs (XML) |
| Deployment | `deployment/` | Dockerfile, compose, nginx, scripts |

## Architecture
- **Pattern:** Hexagonal — `domain` (pure models + port interfaces) → `application` (use-case services) → `adapter/inbound` (REST controllers) → `adapter/out` (persistence + webscraper)
- **API-first:** `contract/openapi.yaml` → OpenAPI Generator (kotlin-spring, coroutines mode) → `build/generated/src/main/kotlin/`. Controllers implement generated interfaces. **Do not edit generated code.**
- **Database:** PostgreSQL (runtime) + SQLite (embedded, for quicksearch import). Liquibase migrations. Hibernate Envers for entity auditing. Separate DB users: `watcher_mig` (migrations), `watcher_app` (app), `watcher_readonly` (read-only).
- **Scraping:** Playwright (1.61.0) + Jsoup (1.22.2) → `adapter/out/webscraper/`. Coroutines-based (`suspend` functions). Resilience4j (2.4.0) circuit breaker (50% failure threshold, 60-call sliding window, 30s open) + retry (3 attempts, 10s base, 2x backoff). `NotFoundException` is ignored by circuit breaker via Java config (YAML `ignore-exceptions` is bugged).
- **Caching:** Caffeine (1h expiry, 1000 max entries) + HTTP ETag/If-None-Match.
- **Actuator:** Management port 8084 (local) / 8081 (Docker). Endpoints: `health,info,metrics,prometheus,loggers,env,heapdump,threaddump`. Spring Boot Admin client auto-registers to `http://localhost:9090`.
- **Scraping strategies (7 total):**
  - In-process Java Playwright: `chromium` (lazy init), `camoufox` (lazy init)
  - Out-of-process sidecar: `puppeteer-worker` (port 3000), `playwright-extra-worker` (port 3001), `camoufox-python-worker` (port 3002)
  - Out-of-process real Chrome CDP: `chrome-cdp` (port 9222 host → 9223 socat)
  - Out-of-process Camoufox via Playwright Server: `camoufox-cdp` (WS → host Node.js bridge → port 9224/9225)
  - Switch at runtime: `PUT /actuator/scraper/strategy {"strategy":"chrome-cdp"}`
- **Sidecar workers:** Each self-contained Docker container with its own browser. App delegates via HTTP.
- **BrowserContextPool:** Accepts `suspend` lambdas for coroutine-friendly context management.
- **CloudflareChallengeSolver.kt:** Detects Turnstile via JS + polling, attempts checkbox click in iframe. Only works for interactive checkbox variant; non-interactive (`/dark/fbE/new/normal`) cannot be clicked.
- **Chrome CDP strategy (`chrome-cdp`):** Only strategy that fully bypasses Cloudflare — uses user's real Chrome with full profile. Chrome 150+ ignores `--remote-debugging-address=0.0.0.0` on Linux, so `scripts/start-chrome-cdp.sh` uses `socat` to bridge `0.0.0.0:9223` ↔ `127.0.0.1:9222`. Uses 60s navigation timeout (headless strategies use 15s). CDP URL uses `172.17.0.1` (Docker gateway IP) not `host.docker.internal` because Chrome DevTools rejects non-IP `Host` headers. `PLAYWRIGHT_BROWSERS_PATH=/app/playwright-data` persisted on `playwright-data` volume.
- **Camoufox Python worker**: 12-attempt recovery loop with homepage warmup, `location.reload()` at attempt 4, fresh navigation at attempt 8. Homepage warmup establishes session cookies before real request.

## CODE MAP
| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `CollectablesAdapter` | class | `adapter/inbound/rest/` | REST controller (implements generated API) |
| `CollectablesMapper` | object | `adapter/inbound/rest/` | API schema ↔ domain mapping |
| `CollectablesService` | class | `application/` | Use-case service |
| `SearchUseCase` | interface | `application/` | Search contract |
| `Product` | data class | `domain/model/` | Core domain model |
| `Genre` | enum class | `domain/model/` | Card game genre |
| `Locale` | enum class | `domain/model/` | Language locale |
| `ProductType` | enum class | `domain/model/` | Product type |
| `CardMarketScraperPort` | interface | `domain/port/out/` | Scraping port |
| `ProductRepository` | interface | `domain/port/out/` | Product persistence port |
| `SearchResultRepository` | interface | `domain/port/out/` | Search result persistence port |
| `CardMarketScraperAdapter` | class | `adapter/out/webscraper/cardmarket/` | Scraping port implementation |
| `CardMarketWebFetcher` | class | `adapter/out/webscraper/cardmarket/` | URL building + delegates to active strategy |
| `ScrapingStrategy` | interface | `adapter/out/webscraper/strategy/` | Contract: `id`, `fetch()`, `close()` |
| `ScrapingStrategySelector` | class | `adapter/out/webscraper/strategy/` | Runtime switch via `AtomicReference` |
| `ScrapingStrategyRegistry` | class | `adapter/out/webscraper/strategy/` | Strategy lookup by id |
| `ChromiumPlaywrightStrategy` | class | `adapter/out/webscraper/strategy/` | In-process: Java Playwright + Chromium |
| `CamoufoxPlaywrightStrategy` | class | `adapter/out/webscraper/strategy/` | In-process: Java Playwright + Camoufox |
| `ChromeCdpStrategy` | class | `adapter/out/webscraper/strategy/` | Out-of-process: real Chrome via CDP |
| `CamoufoxCdpStrategy` | class | `adapter/out/webscraper/strategy/` | Out-of-process: Camoufox via WS → host |
| `WorkerStrategy` | sealed class | `adapter/out/webscraper/strategy/` | Base for HTTP worker strategies |
| `PuppeteerWorkerStrategy` | class | `adapter/out/webscraper/strategy/` | HTTP → scraper-worker (port 3000) |
| `PlaywrightExtraWorkerStrategy` | class | `adapter/out/webscraper/strategy/` | HTTP → playwright-extra worker (port 3001) |
| `CamoufoxPythonWorkerStrategy` | class | `adapter/out/webscraper/strategy/` | HTTP → camoufox-python-worker (port 3002) |
| `ScraperManagementController` | class | `adapter/inbound/rest/strategy/` | `/actuator/scraper/*` endpoints |
| `ScrapingStrategyConfig` | class | `config/` | Bean wiring for all 7 strategies |
| `Resilience4jConfig` | class | `config/` | Circuit breaker Java config (NotFoundException ignored) |
| `CacheConfig` | class | `config/` | Caffeine cache setup |
| `CardMarketHealthIndicator` | class | `config/` | Actuator health check |
| `GenreConfig` | object | `config/` | Genre → CardMarket mapping |
| `start-chrome-cdp.sh` | script | `scripts/` | Launches Chrome + socat on host |
| `export-cookies.py` | script | `scripts/` | Exports Chrome cookies |

## Critical Constraints
- **NO COMMENTS:** No JavaDoc, no KDoc, no inline comments. Exception: `TODO` must reference an issue key. (See `RULES.md`)
- **Null-safety:** Prefer non-nullable types. Use `Result<T>` for expected failures; `throw` for unexpected.
- **WebMVC, not WebFlux:** Despite `reactive: true` in OpenAPI generator config, the runtime is Spring WebMVC.
- **Import ordering:** Third-party imports first, blank line, then project imports. Alphabetical within each group. No wildcard imports.
- **Code style:** 4-space indentation, 120 char line length, PascalCase classes, camelCase functions/variables, UPPER_SNAKE_CASE constants.

## Testing
- **Mocking:** `mockk` only. Use `com.ninjasquad.springmockk.MockkBean` for `@SpringBootTest` bean replacement.
- **Unit tests:** `src/test/kotlin/`, exclude `integration` and `e2e` JUnit tags (configured in `build.gradle.kts`).
- **Integration tests:** `*IT` classes, include `integration` tag. Use `@SpringBootTest` + Testcontainers (PostgreSQL `postgres:18.1-alpine`, `withReuse(true)`). Container reuse requires `testcontainers.reuse.enable=true` in user-level config.
- **Test profile (`test`):** `lazy-initialization: true`, Liquibase context `test`, app data import disabled. Config at `src/test/resources/application.yml`.
- **Naming:** Backtick descriptions preferred: `` `search returns one product` ``. Test classes: `ClassNameTest` (unit), `ClassNameIT` (integration).
- **Async tests:** Wrap `suspend` function calls in `runBlocking`.
- **Fixtures:** HTML snapshots in `src/test/resources/` (e.g., `pikachu_gallery_size30_v2.html`). Some tests use `Assumptions.assumeTrue()` to skip when fixtures missing.
- **Parallel execution:** Gradle-level parallel enabled (`gradle.properties`). JUnit parallel is NOT configured.

## Workflow
- **Branching:** `feature/<ticket-id>-description` or `bugfix/<ticket-id>-description`.
- **Commits:** Atomic, prefixed `fix:`, `feat:`, `refactor:`.
- **Lint/format:** Detekt main task is disabled in CI (`build.gradle.kts` disables it). Ktlint runs only on test sources and Gradle scripts (main source ktlint disabled). Run `./gradlew ktlintFormat` to format (triggers OpenAPI codegen).
- **CI:** Self-hosted runner, runs `./gradlew check` on push/PR to `main`. See `.github/workflows/ci.yml`.
- **Prefer executable truth:** If `docs/` prose conflicts with `build.gradle.kts` or `application.yml`, trust the executable source.

## Config & Ops
- **Docker Compose:** `deployment/compose.yml` — profiles: `local` (postgres via `spring-boot-docker-compose`), `deployment` (full stack: app, nginx, admin-server, postgres, ofelia, scraper-worker, scraper-worker-playwright, scraper-worker-python).
- **Docker build:** `deployment/Dockerfile` — multi-stage: JDK 17 build → JRE 24 runtime. Chromium + Node.js + Camoufox (135.0.1-beta.24) installed in runtime image.
- **Env vars:** `.env` file (gitignored). Required (from `application.yml` + `.env.example`): `WATCHER_READONLY_PWD`, `POSTGRES_PASSWORD`, `MIGRATION_PWD`, `WATCHER_MIG_PWD`, `WATCHER_APP_PWD`, `APPLICATION_PWD`. No JWT in the project — don't fabricate env vars.
- **Gradle:** Configuration cache enabled. JVM args: `-Xmx2048m`. `gradle.properties`: parallel execution and build caching enabled. `spring-boot-docker-compose` manages postgres for `local` profile only.
- **Boot quirk:** `compileKotlin` depends on `openApiGenerate`; `ktlintFormat` also depends on `openApiGenerate` (via `runKtlintFormatOverKotlinScripts`). Codegen always runs first.

## Module Guidelines

Sub-modules with their own AGENTS.md:
- `adapter/out/persistence/` — JPA entities, mappers, repository adapters
- `adapter/out/webscraper/` — Playwright/Jsoup scraping pipeline, 7 strategies
- `deployment/` — Docker, compose, CI, backup scripts
- `scraper-worker-python/` — Camoufox Python sidecar worker

Root-level instruction files:
- `RULES.md` — "No comments" constraint (source of truth for that rule)
- `CLAUDE.md` — May contain stale guidance (prefer AGENTS.md + executable config for facts)
