# Codebase Structure

**Analysis Date:** 2026-04-05

## Directory Layout

```
TCGWatcher-Backend/
├── src/
│   ├── main/
│   │   ├── kotlin/io/github/havonte1/tcgwatcher/backend/
│   │   │   ├── TcgWatcherApplication.kt          # Spring Boot entry point
│   │   │   ├── config/                            # Spring configuration beans
│   │   │   ├── domain/                            # Core domain (models + ports)
│   │   │   │   ├── model/                         # Domain models
│   │   │   │   └── port/out/                      # Outbound port interfaces
│   │   │   ├── application/                       # Use cases / services
│   │   │   └── adapter/
│   │   │       ├── inbound/rest/                  # REST controllers + mappers
│   │   │       └── out/
│   │   │           ├── persistence/               # JPA entities, repos, mappers
│   │   │           │   ├── entity/                # JPA entities
│   │   │           │   ├── mapper/                # Entity ↔ Domain mappers
│   │   │           │   └── repository/            # Port implementations + JPA repos
│   │   │           └── webscraper/                # CardMarket scraping
│   │   │               ├── cardmarket/            # CardMarket-specific scraping
│   │   │               └── PlaywrightManager.kt   # Browser lifecycle management
│   │   └── resources/
│   │       ├── application.yml                    # Main Spring config
│   │       ├── application-compose.yml            # Docker compose profile config
│   │       ├── db/changelog/                      # Liquibase migrations
│   │       └── import/                            # Bootstrap data (SQLite + CSV)
│   └── test/
│       ├── kotlin/io/github/havonte1/tcgwatcher/backend/
│       │   ├── application/                       # Unit tests for services
│       │   └── adapter/
│       │       ├── inbound/rest/                  # Integration tests for REST
│       │       ├── out/persistence/               # Tests for data import
│       │       └── out/webscraper/cardmarket/     # Unit + IT tests for scraping
│       └── resources/                             # Test fixtures
├── contract/
│   └── openapi.yaml                               # API contract (code generation source)
├── deployment/
│   ├── Dockerfile                                 # Container build
│   ├── compose.yml                                # Docker Compose (PostgreSQL)
│   ├── nginx/nginx.conf                           # Reverse proxy config
│   ├── postgres/initdb.d/                         # DB initialization scripts
│   ├── scripts/                                   # Backup/restore scripts
│   └── backups/                                   # Database backup files
├── admin-server/                                  # Spring Boot Admin (separate module)
├── build.gradle.kts                               # Main build configuration
├── settings.gradle.kts                            # Gradle settings
├── gradle.properties                              # Gradle properties
├── detekt-baseline.xml                            # Detekt suppression baseline
├── auth.json                                      # Playwright browser auth state
└── .github/workflows/ci.yml                       # CI pipeline
```

## Directory Purposes

**`src/main/kotlin/.../domain/`:**
- Purpose: Core domain — pure Kotlin, zero external dependencies
- Contains: Domain models (`Product`, `SearchResult`, `SellOffer`, `ProductSet`, `ProductSeries`, `StringWithValidity`), outbound port interfaces (`ProductRepository`, `SearchResultRepository`, `CardMarketScraperPort`)
- Key files: `domain/model/Product.kt`, `domain/port/out/CardMarketScraperPort.kt`

**`src/main/kotlin/.../application/`:**
- Purpose: Use case orchestration — wires domain ports together
- Contains: `CollectablesService` (implements `SearchUseCase`), `SearchUseCase` interface
- Key files: `application/CollectablesService.kt`, `application/SearchUseCase.kt`

**`src/main/kotlin/.../adapter/inbound/rest/`:**
- Purpose: REST API layer — receives HTTP requests, delegates to application layer
- Contains: `CollectablesAdapter` (controller), `CollectablesMapper` (domain→DTO)
- Key files: `adapter/inbound/rest/CollectablesAdapter.kt`

**`src/main/kotlin/.../adapter/out/persistence/`:**
- Purpose: PostgreSQL persistence via JPA/Hibernate
- Contains: JPA entities (`entity/`), domain↔entity mappers (`mapper/`), port implementations + Spring Data JPA repositories (`repository/`), SQLite bootstrap importer (`QuicksearchImportRunner.kt`)
- Key files: `entity/ProductEntity.kt`, `mapper/ProductMapper.kt`, `repository/ProductRepositoryAdapter.kt`

**`src/main/kotlin/.../adapter/out/webscraper/`:**
- Purpose: CardMarket web scraping via Playwright + Jsoup
- Contains: Browser management (`PlaywrightManager.kt`), CardMarket adapter (`cardmarket/`)
- Key files: `webscraper/PlaywrightManager.kt`, `webscraper/cardmarket/CardMarketScraperAdapter.kt`, `webscraper/cardmarket/CardMarketWebFetcher.kt`, `webscraper/cardmarket/CardMarketContentParser.kt`

**`src/main/kotlin/.../config/`:**
- Purpose: Spring `@Configuration` classes
- Contains: `CacheConfig` (Caffeine), `Resilience4jConfig`, `CardMarketConfig`, `CardMarketConstants`

**`contract/`:**
- Purpose: OpenAPI specification — source of truth for REST API contract
- Key file: `contract/openapi.yaml`
- Generated to: `build/generated/src/main/kotlin/` (API interfaces + DTO models)

**`deployment/`:**
- Purpose: Production deployment artifacts
- Contains: Dockerfile, docker-compose, nginx config, PostgreSQL init scripts, backup/restore scripts

**`admin-server/`:**
- Purpose: Spring Boot Admin server (separate Gradle subproject for monitoring)

## Key File Locations

**Entry Points:**
- `src/main/kotlin/.../TcgWatcherApplication.kt`: Spring Boot main class
- `contract/openapi.yaml`: API contract (generates `CollectablesApi` interface)

**Configuration:**
- `src/main/resources/application.yml`: Main Spring configuration
- `src/main/resources/application-compose.yml`: Docker compose profile overrides
- `build.gradle.kts`: Gradle build (OpenAPI generation, test config, dependencies)
- `src/main/resources/db/changelog/db.changelog-master.yaml`: Liquibase changelog

**Core Logic:**
- `src/main/kotlin/.../application/CollectablesService.kt`: Primary service — search + product details
- `src/main/kotlin/.../adapter/out/webscraper/cardmarket/CardMarketScraperAdapter.kt`: Scraping orchestration
- `src/main/kotlin/.../adapter/out/webscraper/cardmarket/CardMarketContentParser.kt`: HTML parsing (Jsoup)
- `src/main/kotlin/.../adapter/out/persistence/repository/ProductRepositoryAdapter.kt`: Product persistence

**Testing:**
- `src/test/kotlin/.../application/CollectablesServiceTest.kt`: Unit tests (MockK)
- `src/test/kotlin/.../adapter/out/webscraper/cardmarket/CardMarketScraperAdapterTest.kt`: Unit tests
- `src/test/kotlin/.../adapter/out/webscraper/cardmarket/CardMarketScraperAdapterIT.kt`: Integration tests
- `src/test/kotlin/.../adapter/inbound/rest/CollectablesAdapterIT.kt`: REST integration tests
- `src/test/kotlin/.../CollectablesServiceIT.kt`: End-to-end service tests with Testcontainers

## Naming Conventions

**Files:**
- Domain models: `PascalCase.kt` (e.g., `Product.kt`, `SearchResult.kt`)
- Port interfaces: `*Port.kt` or `*Repository.kt` (e.g., `CardMarketScraperPort.kt`, `ProductRepository.kt`)
- Adapters: `*Adapter.kt` (e.g., `CollectablesAdapter.kt`, `CardMarketScraperAdapter.kt`)
- Entities: `*Entity.kt` (e.g., `ProductEntity.kt`, `SellOfferEntity.kt`)
- Mappers: `*Mapper.kt` (e.g., `ProductMapper.kt`, `CollectablesMapper.kt`)
- Config: `*Config.kt` or `*Constants.kt` (e.g., `CacheConfig.kt`, `CardMarketConstants.kt`)
- Use cases: `*UseCase.kt` or `*Service.kt` (e.g., `SearchUseCase.kt`, `CollectablesService.kt`)

**Test files:**
- Unit tests: `*Test.kt` (e.g., `CollectablesServiceTest.kt`)
- Integration tests: `*IT.kt` (e.g., `CollectablesAdapterIT.kt`, `CardMarketWebFetcherIT.kt`)

## Where to Add New Code

**New REST Endpoint:**
1. Add to `contract/openapi.yaml` (define path, operation, schemas)
2. Run `./gradlew compileKotlin` to regenerate interfaces
3. Implement generated interface in new `*Adapter.kt` under `adapter/inbound/rest/`
4. Add mapper if new DTOs: `adapter/inbound/rest/*Mapper.kt`
5. Add use case method to `application/` or create new service

**New Data Source / Scraper:**
1. Define port interface in `domain/port/out/*Port.kt`
2. Implement in `adapter/out/webscraper/<source>/` (adapter, fetcher, parser, mapper)
3. Add to `CollectablesService` or create new application service
4. Add config in `config/` if needed

**New Entity / Persistence:**
1. Create entity in `adapter/out/persistence/entity/*Entity.kt`
2. Create JPA repository in `adapter/out/persistence/repository/*JpaRepository.kt`
3. Create port interface in `domain/port/out/*Repository.kt`
4. Implement port in `adapter/out/persistence/repository/*RepositoryAdapter.kt`
5. Add mapper in `adapter/out/persistence/mapper/*Mapper.kt`
6. Add Liquibase migration in `src/main/resources/db/changelog/`

**New Configuration:**
- Add `*Config.kt` in `config/`
- Use `@ConfigurationProperties` for externalized config (pattern: `CardMarketConfig`)

**New Unit Test:**
- Place alongside source structure: `src/test/kotlin/.../` mirroring `src/main/kotlin/.../`
- Name: `*Test.kt`
- Use MockK for mocking

**New Integration Test:**
- Name: `*IT.kt`
- Use `@SpringBootTest` + Testcontainers (PostgreSQL)
- Tag with `@Tag("integration")`

## Special Directories

**`build/generated/`:**
- Purpose: OpenAPI-generated Kotlin interfaces and DTO models
- Generated: Yes (by `openapi-generator` plugin during `compileKotlin`)
- Committed: No (excluded from linting via ktlint filter)
- Package: `io.github.havonte1.tcgwatcher.backend.adapter.inbound.rest.api` + `.model`

**`src/main/resources/import/`:**
- Purpose: Bootstrap data for initial database population
- Contains: `quicksearch.db` (SQLite with series/sets/cards), `sets.csv` (additional sets)
- Consumed by: `QuicksearchImportRunner` on application startup

**`deployment/`:**
- Purpose: Production deployment configuration
- Contains: Dockerfile, docker-compose, nginx, PostgreSQL init, backup scripts
- Not part of application build — operational artifacts only

**`admin-server/`:**
- Purpose: Separate Spring Boot Admin module for monitoring
- Has own `build.gradle.kts` and `application.yml`
- Runs on port 9090

---

*Structure analysis: 2026-04-03*
