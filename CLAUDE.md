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
