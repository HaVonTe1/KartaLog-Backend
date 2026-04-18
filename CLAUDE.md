# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

- **Language:** Kotlin (JVM 17)
- **Build System:** Gradle Kotlin DSL (single‑module)
- **Package Structure:** `domain`, `application`, `adapter/inbound`, `adapter/out`
- **Testing:** JUnit 5 + Testcontainers (PostgreSQL) for integration tests
- **Static Analysis:** Detekt, Ktlint
- **API Generation:** OpenAPI Generator (kotlin‑spring) from `contract/openapi.yaml`

## Key Tech Stack

- Spring Boot 4.0.2 (WebMVC)
- Spring Data JPA + Hibernate Envers (auditing)
- Liquibase (database migrations)
- Playwright + Jsoup (web‑scraping)
- Kotlin Coroutines + Reactor integration
- Resilience4j (circuit‑breakers)

## Common Development Commands

| Action | Command |
|--------|---------|
| Clean | `./gradlew clean` |
| Build | `./gradlew build` |
| Build (skip tests) | `./gradlew build -x test` |
| Run **all** tests | `./gradlew test` |
| Run a **single test class** (fully‑qualified name) | `./gradlew test --tests "io.github.havonte1.tcgwatcher.backend.<package>.<TestClass>"` |
| Run a **single test method** (back‑ticked description) | `./gradlew test --tests "ClassName.\`method description\`"` |
| Run **integration tests only** (`*IT`) | `./gradlew test --tests "*IT"` |
| Lint (Detekt) | `./gradlew detekt` |
| Format (Ktlint) | `./gradlew ktlintFormat` |
| JaCoCo coverage report | `./gradlew test jacocoTestReport` |
| Parallel build | `./gradlew build -T 1C` |
| Regenerate OpenAPI client/server code | `./gradlew compileKotlin` (after editing `contract/openapi.yaml`) |

*Add `-i` to any Gradle command for verbose output (useful for debugging flaky tests).*

## High‑Level Architecture Overview

```
src/main/kotlin/io/github/havonte1/tcgwatcher/backend/
├── domain/          # Pure business‑logic models & port interfaces
│   ├── model/       # Core domain entities (e.g., Card, Set, MarketPrice)
│   └── port/        # Hexagonal “ports” that the application layer depends on
│       ├── repository/   # CRUD contracts for persistence
│       └── scraper/      # Contracts for external web‑scraping services
│
├── application/     # Use‑case / service layer
│   └── service/     # Orchestrates domain objects, calls ports
│
├── adapter/
│   ├── inbound/     # REST controllers, request/response DTOs, mappers
│   │   └── web/
│   │       └── controller/
│   └── out/         # Implementations of the domain ports
│       ├── persistence/   # JPA entities, Spring Data repositories,
│       │                     # Liquibase migrations, Envers auditing
│       └── webscraper/    # Playwright/Jsoup scrapers that fulfil
│                             the `scraper` port contract
│
└── TcgWatcherApplication.kt   # Spring Boot entry point (`@SpringBootApplication`)
```

### Architectural Highlights

1. **Hexagonal (Ports‑and‑Adapters) Design** –
   - `domain` contains *pure* business rules and **ports** (interfaces) that describe required capabilities (e.g., `CardRepository`, `MarketScraper`).
   - `adapter/out` supplies concrete implementations: JPA persistence for repository ports and Playwright‑based scrapers for the scraper port.
   - `adapter/inbound` exposes the **inbound** side (HTTP API) via Spring MVC controllers that map JSON to domain DTOs and invoke `application` services.

2. **Separation of Concerns** –
   - **Domain** has no Spring or framework annotations. It can be unit‑tested without bootstrapping the Spring context.
   - **Application** contains the transactional/use‑case logic; it wires together domain objects and ports.
   - **Adapters** handle external concerns (web, DB, scraping) and are the only layers that know about Spring, JPA, Playwright, etc.

3. **Database Management** –
   - Liquibase (`src/main/resources/db/changelog`) defines the schema evolution.
   - Hibernate Envers automatically tracks entity history for auditability.

4. **Resilience & Fault‑Tolerance** –
   - Calls to external web‑scrapers are wrapped with Resilience4j circuit‑breakers (configured in `application.yml`).
   - Coroutines (`suspend` functions) are used throughout the scraper adapters to avoid blocking threads.

5. **OpenAPI‑Driven API** –
   - The contract lives in `contract/openapi.yaml`.
   - Code generation (`openapi-generator`) produces the server stubs under `build/generated/src/main/kotlin`.
   - Controllers in `adapter/inbound/web/controller` delegate to application services, keeping the generated code thin.

6. **Testing Strategy** –
   - **Unit tests** (`src/test/kotlin`) focus on domain logic and application services using MockK.
   - **Integration tests** (`*IT` classes) spin up PostgreSQL via Testcontainers, load the Spring context, and execute end‑to‑end flows (including Liquibase migrations).
   - Test fixtures (HTML snapshots, static resources) reside under `src/test/resources`.

## Code Style Guidelines

### Imports
- No wildcard imports
- Order: third‑party imports first, blank line, then project imports
- Alphabetical within each group

### Formatting
- 4‑space indentation (no tabs)
- Max line length: 120 characters
- No trailing whitespace
- K&R brace style
- Files end with single newline

### Naming Conventions
| Element | Convention |
|--------|------------|
| Package | `lowercase.dotted` |
| Class/Interface | `PascalCase` |
| Function/Method | `camelCase` |
| Variable | `camelCase` |
| Constant | `UPPER_SNAKE_CASE` |
| Test class | `MyClassTest` |
| Test method | `shouldDoSomethingWhenCondition` or backtick descriptions |
| Integration test | `MyClassIT` |

### Types & Null‑Safety
- Prefer non‑nullable types
- Use `?.let {}` or explicit null checks for nullable values
- `data class` for DTOs, `sealed class` for algebraic types
- Prefer `Result<T>` for recoverable failures
- Use `suspend` functions for async operations (coroutines)

### Error Handling
- Use `Result<T>` for expected failures; `throw` for unexpected
- Log via `io.github.oshai.kotlinlogging.KotlinLogging`
- Never swallow exceptions
- For JPA entities, use `@PrePersist` and `@PreUpdate` lifecycle callbacks for timestamp management

### Documentation
- KDoc for public APIs (`@param`, `@return`, `@throws`)
- `TODO` comments must reference an issue key

## Kotlin Idioms
- Prefer `val` over `var`
- Use scoped functions (`apply`, `run`, `also`, `let`)
- Companion object for constants (`const val`)
- Coroutines for async (already configured in project)
- Use `suspend` functions for async operations
- `typealias` for complex generic signatures

## Project Structure
```
src/main/kotlin/io/github/havonte1/tcgwatcher/backend/
├── domain/           # Domain models and port interfaces
├── application/      # Use cases and services
├── adapter/inbound/  # REST controllers, mappers
└── adapter/out/      # Persistence, webscraper implementations
```

## Testing Conventions
- Unit tests: `src/test/kotlin/` with `mockk` for mocking
- Integration tests: Use `@SpringBootTest` and Testcontainers
- Test method names: descriptive with backticks (e.g., `` `search returns one product` ``)
- Use `runBlocking` for testing suspend functions
- Place test fixtures in `src/test/resources/`
- Use `Assumptions.assumeTrue()` to skip tests when fixtures are missing

## Key Dependencies
- Spring Boot 4.0.2 with WebMVC
- Playwright 1.58.0 for web scraping
- kotlinx‑coroutines 1.8.1 for async
- Kotlin‑logging (`io.github.oshai:kotlin-logging-jvm:7.0.14`)
- MockK 1.13.12 for mocking in tests
- Testcontainers for integration tests
- Hibernate Envers for auditing
- Liquibase for database migrations
- Resilience4j 2.2.0 for circuit breakers

## Git Conventions
- Feature branches: `feature/<ticket-id>-description`
- Bugfix branches: `bugfix/<ticket-id>-description`
- Atomic commits with concise messages prefixed by intent (`fix:`, `feat:`, `refactor:`)
- No amend after push unless explicitly requested

## CI/CD Tips
- Fast lint check on PRs: `./gradlew build -x test`
- Full test suite on pushes to `main`: `./gradlew test`
- Fail on any lint error: `./gradlew detekt ktlintFormat`

## OpenAPI Code Generation
- Generated from `contract/openapi.yaml` using `openapi-generator` (kotlin‑spring)
- Output: `build/generated/src/main/kotlin/`
- Configured with reactive coroutines mode and Spring Boot 3 compatibility
- Run `./gradlew compileKotlin` to regenerate after API changes

## Agent Workflow Tips
- Read files before editing
- Run relevant tests after changes
- Do NOT auto‑commit without explicit request
- Run lint before finalizing: `./gradlew detekt ktlintFormat`
- Use `./gradlew -T 1C` for parallel execution

## Spring Boot Specifics
- Web layer uses WebMVC (not WebFlux) despite reactive config in OpenAPI generator
- JPA entities use lifecycle callbacks (`@PrePersist`, `@PreUpdate`) for timestamp management
- Database schema managed by Liquibase, history tracked by Hibernate Envers
- Configuration via `application.yml` and `application-{profile}.yml` files

## Important Code Generation Notes
- DO NOT WRITE COMMENTS! No JavaDoc. No inline Comments. Nothing.
- When using the `edit` tool, remember the right parameter is `filePath` NOT `path`.

<!-- GSD:project-start source:PROJECT.md -->
## Project

**TCGWatcher Backend**

A REST API for searching and retrieving detailed product information from trading card game marketplaces. Users can search for TCG products by query and get comprehensive product details including pricing, sell offers, and metadata. The API proxies and normalizes data from external marketplaces, starting with CardMarket for Pokémon cards.

**Core Value:** Accurate, fast search and product detail retrieval for TCG cards — if the search doesn't return the right products with correct pricing, nothing else matters.

### Constraints

- **Tech Stack**: Kotlin 2.2.20, Spring Boot 4.0.2, PostgreSQL, Playwright — established stack, no changes
- **Data Source**: CardMarket only for v1 — no other marketplaces until v2
- **Genre Scope**: Pokémon only for v1 — Yu-Gi-Oh and MTG are v2
- **Response Format**: Direct CardMarket data passthrough — no enrichment or transformation layer
- **Deployment**: Docker Compose with PostgreSQL and Playwright browser
- **API Contract**: OpenAPI-first — all endpoints defined in `contract/openapi.yaml` and generated
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->
## Technology Stack

## Languages
- **Kotlin 2.2.20** (JVM) - All application code in `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/`
- **Java 17** (JVM target) - Runtime compatibility via `jvmToolchain(17)`
- **YAML** - Configuration (`application.yml`, OpenAPI spec, Liquibase changelogs)
- **SQL** - Database migrations (`src/main/resources/db/changelog/`)
## Runtime
- **JVM 17** (build), **JRE 24** (runtime container via `eclipse-temurin:24-jre-alpine`)
- **Spring Boot 4.0.2** - Application framework
- **Package Manager:** Gradle 9.x (Kotlin DSL)
- **Lockfile:** Not detected (Gradle uses version catalogs or direct version pins)
## Frameworks
- **Spring Boot 4.0.2** - Application framework with WebMVC (`spring-boot-starter-web`)
- **Spring Data JPA** - Database access layer (`spring-boot-starter-data-jpa`)
- **Hibernate Envers** - Entity auditing (`hibernate-envers`)
- **Liquibase** - Database schema migration (`spring-boot-starter-liquibase`)
- **Spring Boot Actuator** - Health/metrics endpoints (`spring-boot-starter-actuator`)
- **Spring Boot Admin Client 4.0.2** - Application monitoring (`de.codecentric:spring-boot-admin-starter-client`)
- **Playwright 1.58.0** - Headless browser automation for CardMarket scraping (`com.microsoft.playwright:playwright`)
- **Jsoup 1.22.1** - HTML parsing (`org.jsoup:jsoup`)
- **Resilience4j 2.3.0** - Circuit breaker + retry patterns (`resilience4j-spring-boot3`, `resilience4j-all`, `resilience4j-kotlin`)
- **Spring Cache** with **Caffeine 3.1.8** - In-memory caching (`com.github.ben-manes.caffeine:caffeine`)
- **SpringDoc OpenAPI 2.7.0** - Swagger UI (`springdoc-openapi-starter-webmvc-ui`)
- **OpenAPI Generator 7.19.0** - Generates Kotlin Spring interfaces from `contract/openapi.yaml`
- **Detekt 2.0.0-alpha.1** - Static analysis
- **Ktlint 1.5.0** - Code formatting
- **Kotlinx Coroutines 1.8.1** - Async operations (`kotlinx-coroutines-reactor`, `kotlinx-coroutines-core`)
## Key Dependencies
- `org.springframework.boot:spring-boot-starter-web` - HTTP layer (WebMVC, not WebFlux)
- `org.springframework.boot:spring-boot-starter-data-jpa` - ORM layer
- `com.microsoft.playwright:playwright:1.58.0` - Web scraping engine (Chromium)
- `org.jsoup:jsoup:1.22.1` - HTML content parsing
- `io.github.resilience4j:resilience4j-spring-boot3:2.3.0` - Circuit breaker for CardMarket calls
- `org.hibernate.orm:hibernate-envers` - Entity revision tracking
- `org.postgresql:postgresql` - PostgreSQL JDBC driver (runtime)
- `org.xerial:sqlite-jdbc:3.45.1.0` - SQLite driver (used for quicksearch import)
- `org.springframework.boot:spring-boot-starter-cache` + `com.github.ben-manes.caffeine:caffeine:3.1.8` - In-memory caching
- `org.springframework.boot:spring-boot-starter-actuator` - Health/metrics
- `de.codecentric:spring-boot-admin-starter-client:4.0.2` - Monitoring registration
- `io.github.oshai:kotlin-logging-jvm:7.0.14` - Kotlin logging facade
- **JUnit 5** - Test framework (`junit-jupiter-api`, `junit-jupiter-engine`)
- **MockK 1.13.12** - Kotlin mocking library (`io.mockk:mockk`)
- **Testcontainers** - PostgreSQL + ToxiProxy containers (`testcontainers-postgresql`, `testcontainers-toxiproxy`)
- **WireMock 3.13.2** - HTTP mock server (`wiremock-standalone`)
- **Kotlinx Coroutines Test 1.10.2** - Coroutine testing utilities
## Build System
- Single-module project (`tcgwatcher-backend`)
- JVM toolchain: 17
- OpenAPI code generation runs before `compileKotlin`
- Custom `integrationTest` task for tagged integration tests
- Detekt + Ktlint configured (ktlint disabled for main source, enabled for scripts)
- Test output: full exception details, standard streams visible
| Task | Purpose |
|------|---------|
| `clean` | Clean build artifacts |
| `build` | Full build |
| `build -x test` | Build without tests |
| `test` | Unit tests (excludes `integration`, `e2e` tags) |
| `integrationTest` | Integration tests (includes `integration` tag) |
| `detekt` | Static analysis |
| `ktlintFormat` | Auto-format code |
| `jacocoTestReport` | Coverage report |
## Configuration
- `WATCHER_READONLY_PWD` - Read-only DB user password
- `POSTGRES_PASSWORD` - Database root password
- `MIGRATION_PWD` - Liquibase migration password
- `WATCHER_MIG_PWD` - Migration user password
- `WATCHER_APP_PWD` - Application user password
- `APPLICATION_PWD` - Application password
- `src/main/resources/application.yml` - Primary config
- `src/main/resources/application-compose.yml` - Docker compose profile
- Spring Boot Admin client at `http://localhost:9090`
- Management server on port `8081`
- Circuit breaker: 50% failure threshold, 60-call sliding window, 30s open state wait
- Retry: 3 max attempts, 10s initial wait, 2x exponential backoff
- Caffeine: 1-hour expiry, 1000 max entries
## Platform Requirements
- JDK 17+
- Docker (for local PostgreSQL via compose)
- Chromium (for Playwright web scraping)
- Node.js (Playwright runtime dependency)
- Docker container (`eclipse-temurin:24-jre-alpine` base)
- PostgreSQL 18.1
- Chromium browser installed in container
- NGINX reverse proxy (deployment profile)
- Spring Boot Admin server for monitoring
- Ofelia for scheduled DB backups
## CI/CD
- Self-hosted runner
- JDK 17 (Temurin)
- Gradle cache via `maxnowack/local-cache`
- Runs `./gradlew check` on push/PR to `main`
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

## Naming
- Kotlin source files use `PascalCase.kt` matching the primary class name (enforced by Detekt `MatchingDeclarationName`)
- Test files: `ClassNameTest.kt` for unit tests, `ClassNameIT.kt` for integration tests
- Configuration files: `*Config.kt` (e.g., `CacheConfig.kt`, `Resilience4jConfig.kt`)
- Mapper files: `*Mapper.kt` (e.g., `CollectablesMapper.kt`, `ProductMapper.kt`)
- Entity files: `*Entity.kt` (e.g., `ProductEntity.kt`, `SeriesEntity.kt`)
- DTO files: Grouped in `Dtos.kt` or named `*Dto.kt`
- `camelCase` for all functions (Detekt pattern: `[a-z][a-zA-Z0-9]*`)
- Test methods use backtick descriptions: `` `search returns one product` ``
- Test methods also use `shouldDoSomethingWhenCondition` pattern
- Lifecycle callbacks: `onPrePersist`, `onPreUpdate`
- `camelCase` for all variables (Detekt pattern: `[a-z][A-Za-z0-9]*`)
- Private variables may start with underscore (Detekt: `(_)?[a-z][A-Za-z0-9]*`)
- `UPPER_SNAKE_CASE` in companion objects (e.g., `private const val serialVersionUID: Long = 1L`)
- Detekt pattern: `[A-Z][_A-Z0-9]*`
- `PascalCase` for classes, interfaces, data classes, sealed classes (Detekt: `[A-Z][a-zA-Z0-9]*`)
- Enum entries: `UPPER_SNAKE_CASE` (Detekt: `[A-Z][_a-zA-Z0-9]*`)
- `lowercase.dotted` (Detekt: `[a-z]+(\.[a-z][A-Za-z0-9]*)*`)
- Root: `io.github.havonte1.tcgwatcher.backend`
## Code Style
- 4-space indentation (no tabs)
- Max line length: 120 characters (Detekt `MaxLineLength`)
- No trailing whitespace
- Files end with single newline (Detekt `NewLineAtEndOfFile`)
- K&R brace style
- No wildcard imports (Detekt `WildcardImport` active)
- Order: third-party imports first, blank line, then project imports
- Alphabetical within each group
- Ktlint excludes generated code (`**/generated/**`)
- Prefer `val` over `var` (Detekt `VarCouldBeVal` active)
- Use scoped functions: `apply`, `run`, `also`, `let`
- Companion objects for constants (`const val`)
- `data class` for DTOs and domain models
- `sealed class` for algebraic types
- `typealias` for complex generic signatures
- `suspend` functions for async operations (coroutines)
- Prefer non-nullable types
- Use `?.let {}` or explicit null checks for nullable values
- Avoid `!!` (Detekt `UnsafeCallOnNullableType` active for non-test code)
- Max cyclomatic complexity per method: 14
- Max method length: 60 lines
- Max class length: 600 lines
- Max function parameters: 5 (6 for constructors)
- Max return statements: 2 (Detekt `ReturnCount`)
- Max throw statements: 2 (Detekt `ThrowsCount`)
- Max nested block depth: 4
- Max functions per file/class: 11
- Magic numbers: -1, 0, 1, 2 allowed (excludes test code and `.kts` files)
## File Organization
- HTML fixtures in `src/test/resources/` (e.g., `pikachu_gallery_30.html`)
- Test logging config: `src/test/resources/logback-test.000`
## Git Conventions
- Feature branches: `feature/<ticket-id>-description`
- Bugfix branches: `bugfix/<ticket-id>-description`
- Atomic commits with concise messages prefixed by intent
- Prefixes: `fix:`, `feat:`, `refactor:`
- No amend after push unless explicitly requested
## Documentation
- No JavaDoc
- No inline comments
- No KDoc
- Exception: `TODO` comments must reference an issue key
## Error Handling
- Use `Result<T>` for expected failures (e.g., web fetcher results)
- Use `throw` for unexpected errors
- Log via `io.github.oshai.kotlinlogging.KotlinLogging`
- Never swallow exceptions (Detekt `SwallowedException` active)
- Use `UseRequire`, `UseCheckOrError`, `UseCheckNotNull` (Detekt rules active)
- For JPA entities, use `@PrePersist` and `@PreUpdate` lifecycle callbacks for timestamp management
- Framework: `io.github.oshai:kotlin-logging-jvm:7.0.14`
- Pattern: `private val logger = KotlinLogging.logger {}`
- Levels observed: `debug`, `info`, `warn`
## Detekt Configuration
- `complexity`: CognitiveComplexMethod (off), CyclomaticComplexMethod (14), LargeClass (600), LongMethod (60), LongParameterList (5/6), NestedBlockDepth (4), TooManyFunctions (11)
- `naming`: ClassNaming, FunctionNaming, VariableNaming, PackageNaming, MatchingDeclarationName
- `style`: MaxLineLength (120), ReturnCount (2), ThrowsCount (2), WildcardImport, MagicNumber, VarCouldBeVal, UnusedPrivateMember
- `exceptions`: SwallowedException, TooGenericExceptionCaught, TooGenericExceptionThrown
- `empty-blocks`: All empty block checks active
- `performance`: ArrayPrimitive, ForEachOnRange, SpreadOperator
- `potential-bugs`: AvoidReferentialEquality, DoubleMutability, UnnecessaryNotNullOperator, UnsafeCast
- Version: 1.5.0
- Excludes: `**/generated/**`, `**/build/generated/**`
- `runKtlintCheckOverMainSourceSet` and `runKtlintFormatOverMainSourceSet` disabled (generated code)
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## Pattern Overview
- Domain layer defines port interfaces (`domain/port/out/`) with no external dependencies
- Adapters implement ports: inbound adapters (`adapter/inbound/`) handle REST requests, outbound adapters (`adapter/out/`) implement persistence and web scraping
- Application layer (`application/`) orchestrates use cases by coordinating ports
- OpenAPI-first API contract: `contract/openapi.yaml` generates Spring interfaces at compile time
- Coroutines-based reactive API (`suspend` functions) despite WebMVC runtime
- Caching at two levels: Caffeine in-memory cache (`@Cacheable`) and HTTP ETag/If-None-Match
- Resilience4j circuit breaker and retry for external CardMarket scraping calls
## Layers
- Purpose: Core business models and outbound port interfaces
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/`
- Contains: Domain models (`Product`, `SearchResult`, `SellOffer`, `ProductSet`, `ProductSeries`, `StringWithValidity`), port interfaces (`ProductRepository`, `SearchResultRepository`, `CardMarketScraperPort`)
- Depends on: Nothing external — pure Kotlin
- Used by: Application layer services
- Purpose: Use case orchestration — implements `SearchUseCase` interface
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/`
- Contains: `CollectablesService` (implements `SearchUseCase`), `SearchUseCase` interface
- Depends on: Domain ports (`CardMarketScraperPort`, `ProductRepository`, `SearchResultRepository`)
- Used by: Inbound REST adapters
- Purpose: Expose API to external clients via REST
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/`
- Contains: `CollectablesAdapter` (REST controller implementing generated `CollectablesApi`), `CollectablesMapper` (domain-to-DTO mapping)
- Depends on: Application layer (`SearchUseCase`), generated OpenAPI interfaces (`CollectablesApi`, DTOs)
- Used by: External HTTP clients
- Purpose: Implement persistence and external web scraping
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/`
- Contains:
- Depends on: Spring Data JPA, Playwright, Jsoup, SQLite JDBC
- Used by: Application layer via port interfaces
- Purpose: Spring configuration beans
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/`
- Contains: `CacheConfig` (Caffeine), `Resilience4jConfig` (circuit breaker), `CardMarketConfig` (base URL), `CardMarketConstants` (defaults)
## Data Flow
## Key Abstractions
- `CardMarketScraperPort` — web scraping contract (`search`, `fetchProductDetails`)
- `ProductRepository` — product persistence contract (CRUD + `findByCmId`)
- `SearchResultRepository` — search result caching contract
- `SearchUseCase` — application-level contract (`search`, `fetchProductDetails`, `getSearchCachedAt`, `getProductUpdatedAt`)
- `Product` — aggregate root with nested `ProductSet`, `ProductSeries`, `SellOffer`, `StringWithValidity`
- `SearchResult` — cached search with query string and product list
## Entry Points
- Location: `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/TcgWatcherApplication.kt`
- Standard Spring Boot `@SpringBootApplication` with `runApplication<TcgWatcherApplication>()`
- Generated from `contract/openapi.yaml` → `CollectablesApi` interface in `build/generated/`
- Implemented by `CollectablesAdapter` at `adapter/inbound/rest/CollectablesAdapter.kt`
- Endpoints: `GET /collectables/`, `GET /collectables/{cmId}`
- Actuator on port 8081: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- `QuicksearchImportRunner` — `ApplicationRunner` triggered at startup
## Error Handling
- Web fetcher returns `Result<String>` — scraper adapter uses `getOrElse` to return empty list/null on failure
- Spring `ResponseStatusException` for HTTP errors (e.g., 400 on missing setname)
- Resilience4j `@Retry` (3 attempts, exponential backoff 2x, 10s base) for transient scraping failures
- Resilience4j `@CircuitBreaker` (50% failure threshold, 60-call sliding window, 30s open state) for CardMarket outages
- `NotFoundException` ignored by circuit breaker (404 is expected, not a failure)
- Custom `CloudFlareException` for 403 responses from CardMarket
## Cross-Cutting Concerns
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
