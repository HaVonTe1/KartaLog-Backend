# Integrations

**Analysis Date:** 2026-04-03

## External APIs

**CardMarket (Web Scraping):**
- **Service:** CardMarket (`https://www.cardmarket.com`) - European TCG marketplace
- **Integration method:** Headless browser scraping via Playwright (Chromium) + Jsoup HTML parsing
- **Config:** `cardmarket.basePath` property (defaults to `https://www.cardmarket.com`) in `CardMarketConfig.kt`
- **Auth:** None - public web scraping (subject to Cloudflare protection)
- **Resilience:** Circuit breaker (`cardMarketCircuitBreaker`) + retry (`cardMarketRetry`) via Resilience4j
- **Adapters:**
  - `CardMarketScraperAdapter` - Main scraper implementation
  - `CardMarketWebFetcher` - HTTP fetching via Playwright
  - `CardMarketContentParser` - HTML parsing with Jsoup
  - `CardMarketProductMapper` - DTO-to-domain mapping
- **Error handling:** `CloudFlareException` for Cloudflare challenges, `NotFoundException` for missing products
- **Supported locales:** de, en, fr, es, it, pt, pl, ru (default: de)
- **Supported genres:** Pokemon, Magic, Yu-Gi-Oh, FFI (default: Pokemon)

## Database

**PostgreSQL:**
- **Version:** 18.1 (via Docker compose)
- **Connection:** Port 51915 (mapped from 5432) in local/dev
- **Database name:** `tcgwatcherdb`
- **Schema management:** Liquibase (`src/main/resources/db/changelog/`)
  - Master changelog: `db.changelog-master.yaml`
  - Initial migration: `20260207-init.xml`
- **Multiple DB users:**
  - `watcher_readonly` - Read-only access
  - `watcher_mig` - Migration/Liquibase user
  - `watcher_app` - Application user
- **Auditing:** Hibernate Envers tracks entity revisions via `RevisionInfoEntity`
- **JPA Repositories:**
  - `ProductJpaRepository` - Product entities
  - `SearchResultJpaRepository` - Search result entities
  - `SeriesJpaRepository` - Series entities
  - `ProductSetJpaRepository` - Product set entities
  - `SellOfferJpaRepository` - Sell offer entities
  - `NameTranslationJpaRepository` - Name translation entities
  - `RevisionInfoJpaRepository` - Audit revision entities
- **Port adapters:**
  - `ProductRepository` (domain port) → `ProductRepositoryAdapter` (JPA implementation)
  - `SearchResultRepository` (domain port) → `SearchResultRepositoryAdapter` (JPA implementation)

**SQLite (Import):**
- **Purpose:** Quicksearch data import (`import/quicksearch.db`)
- **Driver:** `org.xerial:sqlite-jdbc:3.45.1.0`
- **Config:** `app.data.import.sqlite.path` property
- **Import runner:** `QuicksearchImportRunner` - loads data at startup when `app.data.import.enabled=true`
- **Data file:** `src/main/resources/import/quicksearch.db`
- **Reference data:** `src/main/resources/import/sets.csv`

## Caching

**In-Memory (Caffeine):**
- **Framework:** Spring Cache + Caffeine (`com.github.ben-manes.caffeine:caffeine:3.1.8`)
- **Config:** `CacheConfig.kt` - 1-hour expiry after write, 1000 max entries
- **HTTP Caching:** ETag/If-None-Match support on API endpoints (epoch seconds based)

**No distributed caching** (Redis, Memcached) detected.

## Message Queues / Event Systems

**None detected.** The application uses synchronous request-response patterns only.

## Third-party Services

**Spring Boot Admin:**
- **Service:** Spring Boot Admin Server (`de.codecentric:spring-boot-admin-starter-client:4.0.2`)
- **URL:** `http://localhost:9090` (local)
- **Purpose:** Application health monitoring, metrics, log management
- **Management endpoint:** `http://localhost:8081/actuator`
- **Auto-registration:** Enabled with auto-deregistration on shutdown

**Actuator Endpoints (port 8081):**
- `health` - Application health
- `info` - Application info
- `metrics` - Application metrics
- `prometheus` - Prometheus metrics export
- `loggers` - Runtime log level management
- `env` - Environment properties
- `heapdump` - JVM heap dump
- `threaddump` - JVM thread dump

## Internal Service Communication

**Architecture Pattern:** Hexagonal (Ports & Adapters)

**Inbound (REST API):**
- **Entry point:** `CollectablesAdapter` (`src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesAdapter.kt`)
- **Mapper:** `CollectablesMapper` - Domain-to-DTO conversion
- **Generated API interfaces:** From `contract/openapi.yaml` via OpenAPI Generator
  - `apiPackage`: `io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api`
  - `modelPackage`: `io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.model`
- **Endpoints:**
  - `GET /collectables/` - Search/list collectables (with ETag support)
  - `GET /collectables/{cmId}` - Get product details with sell offers (with ETag support)

**Application Layer:**
- `CollectablesService` - Orchestrates search and detail retrieval
- `SearchUseCase` - Search business logic

**Outbound Adapters:**
- **Persistence:** JPA repositories mapped to domain ports
- **Web Scraping:** Playwright-based CardMarket scraper

**Service Flow:**
```
Client → REST Controller (generated API) → CollectablesAdapter → CollectablesService
  → CardMarketScraperPort (search/fetch) → CardMarketScraperAdapter
    → CardMarketWebFetcher (Playwright) → CardMarket (external)
  → ProductRepository / SearchResultRepository → PostgreSQL
```

## Deployment Infrastructure

**Docker Compose** (`deployment/compose.yml`):
- **app:** Spring Boot application (ports 8080, 8081)
- **postgres:** PostgreSQL 18.1 (port 51915:5432)
- **nginx:** Reverse proxy (port 19123:80) - deployment profile only
- **admin-server:** Spring Boot Admin (port 9090:8080) - deployment profile only
- **ofelia:** Job scheduler for automated DB backups - deployment profile only

**Container Image:**
- Builder: `eclipse-temurin:17-jdk-alpine`
- Runtime: `eclipse-temurin:24-jre-alpine` with Chromium + Node.js

**Production Server:**
- URL: `https://havonte.ddns.net:8080` (from OpenAPI spec)

## Environment Configuration

**Required Environment Variables:**
- `WATCHER_READONLY_PWD` - Read-only DB user password
- `POSTGRES_PASSWORD` - PostgreSQL root password
- `MIGRATION_PWD` - Liquibase migration password
- `WATCHER_MIG_PWD` - Migration user password
- `WATCHER_APP_PWD` - Application user password
- `APPLICATION_PWD` - Application password

**Secrets Location:**
- `.env` file (gitignored) for local development
- Docker compose `env_file` references `../.env`

## Webhooks & Callbacks

**Incoming:** None detected.

**Outgoing:** None detected.

---

*Integration audit: 2026-04-03*
