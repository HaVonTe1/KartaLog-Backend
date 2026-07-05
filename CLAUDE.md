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
| Run a **single test class** (fully‑qualified name) | `./gradlew test --tests "io.github.havonte1.kartalog.backend.<package>.<TestClass>"` |
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
src/main/kotlin/io/github/havonte1/kartalog/backend/
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
└── KartaLogApplication.kt   # Spring Boot entry point (`@SpringBootApplication`)
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
src/main/kotlin/io/github/havonte1/kartalog/backend/
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

**Project: KartaLog-Backend**

Spring Boot Kotlin backend service that monitors and provides pricing data for trading cards (Pokemon, Magic: The Gathering, Yu-Gi-Oh!). Scrapes CardMarket.eu for pricing data, stores in PostgreSQL, exposes via REST API with caching and ETag support.

**Core Value:** **One thing that must work:** API returns accurate, cached pricing data for collectible cards when called with a product name.
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Recommended Stack
### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Spring Boot | 4.0.x | Application framework | Latest stable (4.0 released Dec 2025). Kotlin 2.2 is baseline. JSpecify null-safety native. |
| Kotlin | 2.2.x | Language | Official baseline for Spring Boot 4. Null-safety with JSpecify. Coroutines for async. |
| Java | 21+ | Runtime | Spring Boot 4 requires Java 21+. Better GC performance. |
### Database (unchanged from existing)
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| PostgreSQL | 16+ | Primary database | Mature, robust, JSON support. Existing project uses this. |
| Liquibase | 4.x | Migration management | Declarative, rollback support. Already integrated. |
| Hibernate Envers | 6.x | Audit logging | Entity versioning. Already integrated. |
### Authentication & Security
| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Spring Security | 7.x | Authentication framework | Default for Spring Boot 4. Stateless JWT auth. |
| JJWT | 0.13.x | JWT token handling | Latest stable (Aug 2025). Java 8+ required for 0.14+. Pure Java, no dependencies on legacy libs. Supports JWE/JWK. |
| BCrypt | - | Password hashing | Included with Spring Security. Default strength 10 (use 12 for sensitive). |
### GraphQL API
| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Spring for GraphQL | 1.x | Official GraphQL support | **RECOMMENDED.** Transport-agnostic (HTTP, WebSocket, RSocket). Native coroutines support. First-party Spring project. |
| graphql-kotlin-spring-server | 9.x | Expedia's GraphQL Kotlin | Alternative. WebFlux-only (cannot mix with WebMVC). |
### Notification System
| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Spring Mail | 3.x | Email sending | Built-in. Use with Thymeleaf for templates. |
| NotifyHub | 1.x | Multi-channel notifications | Email, Slack, Telegram, Discord, SMS unified API. Async support. Fallback chains. |
| Firebase Admin SDK | 9.x | Push notifications | Firebase Cloud Messaging for mobile/web push. |
### Web Scraping (unchanged from existing)
| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Playwright | 1.48.x | Browser automation | JS-rendered pages. Existing project uses this. |
| Jsoup | 1.18.x | HTML parsing | Static HTML parsing. Existing project uses this. |
| Resilience4j | 2.x | Circuit breaker | Existing integration. 50% failure threshold working. |
| ScrapingAnt | API | Managed scraping | Optional. For protected sites with CAPTCHAs/rate limiting. |
### Caching (unchanged from existing)
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Caffeine | 3.x | In-memory cache | 1h expiry, 1000 max — existing config works. |
| ETag/If-None-Match | - | HTTP-level caching | Existing implementation. |
### Monitoring
| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Spring Boot Actuator | 4.x | Health/metrics | Already integrated on port 8081. |
| Micrometer | 2.x | Metrics facade | Included with Actuator. Prometheus export. |
| Spring Boot Admin | 3.x | Admin UI | Existing project uses this via Docker. |
### TCG Data Sources
| Source | Coverage | Pricing | API Type |
|--------|----------|---------|---------|
| TCG Price Lookup API | 8 games | Real-time | REST |
| TCG API (tcgapi.dev) | 89+ games | Real-time | REST |
| JustTCG | Multiple | Real-time | REST |
| Cardmarket API | EU-focused | Real-time | REST |
- Single API preferred: TCG Price Lookup (cleanest API, good free tier) or TCG API (most games)
- Multiple sources: Cardmarket (EU) + TCGPlayer (US) = dual-market arbitrage
- Avoid: DIY scraper for protected sites
### Alternatives Considered
| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| GraphQL | Spring for GraphQL | graphql-kotlin-spring-server | WebFlux-only conflicts with WebMVC |
| JWT | JJWT | java-jwt | Less maintained, Auth0-specific |
| Notifications | Database queue | RabbitMQ/Kafka | Adds infrastructure complexity for MVP |
| Scraping | Playwright | HtmlUnit | HtmlUnit slower, less compatible with modern JS |
## Installation
# Core
# GraphQL
# Database
# JWT
# Scraping
# Caching
# Resilience
# YAML config
# Dev
# Test
## Version Compatibility Matrix
| Component | Current (2026-04) | Recommended | Notes |
|-----------|-------------------|-------------|-------|
| Spring Boot | 3.x | 4.0.x | Requires Java 21+ |
| Kotlin | 1.9.x | 2.2.x | Baseline for SB4 |
| Java | 17 | 21+ | SB4 requires 21+ |
| Spring Security | 6.x | 7.x | Ships with SB4 |
| JJWT | 0.12.x | 0.13.x | Latest stable |
| Playwright | 1.42+ | 1.48.x | Browser automation |
| Hibernate | 6.x | 6.x | No major change |
## Sources
- [Spring Boot 4 announcement](https://spring.io/blog/2025/12/18/next-level-kotlin-support-in-spring-boot-4)
- [Spring for GraphQL documentation](https://docs.spring.io/spring-boot/reference/web/spring-graphql.html)
- [JJWT releases](https://github.com/jwtk/jjwt/releases)
- [TCG API comparison 2026](https://tcgfast.com/blog/best-tcg-apis-2026/)
- [Playwright best practices](https://playwright.dev/docs/best-practices)
- [Spring Security JWT guide](https://katyella.com/blog/spring-boot-security-best-practices/)
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## Pattern Overview
- Domain core is framework-agnostic with pure models and port interfaces
- Application layer contains use-case services that orchestrate domain logic
- Adapters are pluggable implementations (inbound = driving, outbound = driven)
- Dependencies point inward — domain has no external dependencies
## Layers
- Purpose: Pure domain models and port (interface) definitions
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/domain/`
- Contains: Domain models (`domain/model/`), Port interfaces (`domain/port/out/`)
- Depends on: None (pure Kotlin)
- Used by: Application layer
- Purpose: Use-case services orchestrating domain logic
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/application/`
- Contains: `CollectablesService.kt` (implements `SearchUseCase`), `SearchUseCase.kt` (interface)
- Depends on: Domain ports
- Used by: Inbound adapters (REST controller)
- Purpose: Expose domain functionality to external clients
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/adapter/inbound/rest/`
- Contains: `CollectablesAdapter.kt` (REST controller implementing OpenAPI interface)
- Depends on: Application use-case interfaces
- Examples: `CollectablesAdapter`
- Purpose: Implement domain ports for external systems
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/adapter/out/`
- Contains: Webscraper adapters (`webscraper/cardmarket/`), Persistence adapters (`persistence/`)
- Depends on: Domain ports
## Data Flow
- Search results cached in PostgreSQL via `SearchResultRepository`
- Product details cached in PostgreSQL via `ProductRepository`
- HTTP-level caching via ETag/If-None-Match (1 hour max-age)
- Caffeine cache for in-memory caching (`listCache`, `detailsCache`)
## Key Abstractions
- Purpose: Persist and retrieve Product aggregates
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/domain/port/out/ProductRepository.kt`
- Pattern: Repository pattern with JPA implementation
- Purpose: Scrape CardMarket for search results and product details
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/domain/port/out/CardMarketScraperPort.kt`
- Pattern: Port interface (driven adapter)
- Purpose: Define collectables search/use-case contract
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/application/SearchUseCase.kt`
- Pattern: Use-case interface
## Entry Points
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/KartaLogApplication.kt`
- Triggers: Spring Boot startup
- Responsibilities: Auto-configuration, component scanning
- Location: `src/main/kotlin/io/github/havonte1/kartalog/backend/adapter/inbound/rest/CollectablesAdapter.kt`
- Triggers: HTTP requests to `/api/collectables` (from OpenAPI spec)
- Responsibilities: Request validation, ETag handling, rate limiting, response mapping
## Error Handling
- Web scraping errors: Return `null` or wrap in `Result.failure()` → caught and handled gracefully
- NotFoundException: Ignored by circuit breaker (expected "not found" responses)
- Database errors: Spring's exception translation → HTTP 500
- Validation errors: `ResponseStatusException(HttpStatus.BAD_REQUEST)`
## Cross-Cutting Concerns
- 50% failure threshold
- 60-call sliding window
- 30s open duration
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
