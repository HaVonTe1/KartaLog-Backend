# AGENTS.md – Repository Guidelines

## Project Overview
- **Languages:** Kotlin (JVM 17) + Java (legacy)
- **Build System:** Gradle Kotlin DSL, single-module
- **Package Structure:** `domain`, `application`, `adapter/inbound`, `adapter/out`
- **Testing:** JUnit 5 with Testcontainers (PostgreSQL) for integration tests
- **Static Analysis:** Detekt, Ktlint
- **API Generation:** OpenAPI Generator (kotlin-spring) from `contract/openapi.yaml`
- **Key Tech Stack:**
  - Spring Boot 4.0.2 with WebMVC
  - Spring Data JPA + Hibernate Envers (auditing)
  - Liquibase (database migrations)
  - Playwright + Jsoup (web scraping)
  - Kotlin Coroutines + Reactor integration
  - Resilience4j (circuit breakers)
- **Important:** DO NOT ADD ANY COMMENTS! No JavaDoc. No inline Comments. Nothing.

## Build & Test Commands

| Action | Command |
|--------|---------|
| Clean | `./gradlew clean` |
| Build | `./gradlew build` |
| Build (skip tests) | `./gradlew build -x test` |
| Run all tests | `./gradlew test` |
| Detekt lint | `./gradlew detekt` |
| Ktlint format | `./gradlew ktlintFormat` |
| JaCoCo coverage | `./gradlew test jacocoTestReport` |
| Parallel build | `./gradlew build -T 1C` |

**Test Configuration:** JUnit 5 with Testcontainers for integration tests. Show standard streams and full exceptions in test output (configured in `build.gradle.kts`). Use `./gradlew test --tests "ClassName" --info` for verbose test debugging.

## Running a Single Test

```bash
# Run entire test class (use fully-qualified name)
./gradlew test --tests "io.github.havonte1.tcgwatcher.backend.adapter.out.webscraper.CardMarketScraperAdapterTest"

# Run single test method
./gradlew test --tests "CardMarketScraperAdapterTest.search returns one product built from HTML"

# Run integration tests only (classes ending with IT)
./gradlew test --tests "*IT"
```

Use `-i` for verbose output when debugging flaky tests.

## Code Style Guidelines

### Imports
- No wildcard imports
- Order: third-party imports first, blank line, then project imports
- Alphabetical within each group

### Formatting
- 4-space indentation (no tabs)
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

### Types & Null-Safety
- Prefer non-nullable types
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
- Spring Boot 4.0.2 with WebMVC
- Playwright 1.58.0 for web scraping
- kotlinx-coroutines 1.8.1 for async
- Kotlin-logging (`io.github.oshai:kotlin-logging-jvm:7.0.14`)
- MockK 1.13.12 for mocking in tests
- Testcontainers for integration tests
- Hibernate Envers for auditing
- Liquibase for database migrations
- Resilience4j 2.2.0 for circuit breakers

## Git Conventions
- Feature branches: `feature/<ticket-id>-description`
- Bugfix branches: `bugfix/<ticket-id>-description`
- Atomic commits with concise messages prefixed by intent (`fix:`, `feat:`, `refactor:`)
- No amend after push unless explicitly requested

## CI/CD Tips
- Fast lint check on PRs: `./gradlew build -x test`
- Full test suite on pushes to `main`: `./gradlew test`
- Fail on any lint error: `./gradlew detekt ktlintFormat`

## Cursor / Copilot Rules
No `.cursorrules` or `.github/copilot-instructions.md` present. If added, copy rules here.

## OpenAPI Code Generation
- Generated from `contract/openapi.yaml` using `openapi-generator` (kotlin-spring)
- Output: `build/generated/src/main/kotlin/`
- Configured with reactive coroutines mode and Spring Boot 3 compatibility
- Run `./gradlew compileKotlin` to regenerate after API changes

## Agent Workflow Tips
- Read files before editing
- Run relevant tests after changes
- Do NOT auto-commit without explicit request
- Run lint before finalizing: `./gradlew detekt ktlintFormat`
- Use `./gradlew -T 1C` for parallel execution

## Spring Boot Specifics
- Web layer uses WebMVC (not WebFlux) despite reactive config in OpenAPI generator
- JPA entities use lifecycle callbacks (`@PrePersist`, `@PreUpdate`) for timestamp management
- Database schema managed by Liquibase, history tracked by Hibernate Envers
- Configuration via `application.yml` and `application-{profile}.yml` files
