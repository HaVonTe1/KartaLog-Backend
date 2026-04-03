# Coding Conventions

**Analysis Date:** 2026-04-03

## Naming

**Files:**
- Kotlin source files use `PascalCase.kt` matching the primary class name (enforced by Detekt `MatchingDeclarationName`)
- Test files: `ClassNameTest.kt` for unit tests, `ClassNameIT.kt` for integration tests
- Configuration files: `*Config.kt` (e.g., `CacheConfig.kt`, `Resilience4jConfig.kt`)
- Mapper files: `*Mapper.kt` (e.g., `CollectablesMapper.kt`, `ProductMapper.kt`)
- Entity files: `*Entity.kt` (e.g., `ProductEntity.kt`, `SeriesEntity.kt`)
- DTO files: Grouped in `Dtos.kt` or named `*Dto.kt`

**Functions/Methods:**
- `camelCase` for all functions (Detekt pattern: `[a-z][a-zA-Z0-9]*`)
- Test methods use backtick descriptions: `` `search returns one product` ``
- Test methods also use `shouldDoSomethingWhenCondition` pattern
- Lifecycle callbacks: `onPrePersist`, `onPreUpdate`

**Variables:**
- `camelCase` for all variables (Detekt pattern: `[a-z][A-Za-z0-9]*`)
- Private variables may start with underscore (Detekt: `(_)?[a-z][A-Za-z0-9]*`)

**Constants:**
- `UPPER_SNAKE_CASE` in companion objects (e.g., `private const val serialVersionUID: Long = 1L`)
- Detekt pattern: `[A-Z][_A-Z0-9]*`

**Types:**
- `PascalCase` for classes, interfaces, data classes, sealed classes (Detekt: `[A-Z][a-zA-Z0-9]*`)
- Enum entries: `UPPER_SNAKE_CASE` (Detekt: `[A-Z][_a-zA-Z0-9]*`)

**Packages:**
- `lowercase.dotted` (Detekt: `[a-z]+(\.[a-z][A-Za-z0-9]*)*`)
- Root: `io.github.havonte1.tcgwatcher.backend`

## Code Style

**Formatting (Ktlint 1.5.0 + Detekt):**
- 4-space indentation (no tabs)
- Max line length: 120 characters (Detekt `MaxLineLength`)
- No trailing whitespace
- Files end with single newline (Detekt `NewLineAtEndOfFile`)
- K&R brace style

**Imports:**
- No wildcard imports (Detekt `WildcardImport` active)
- Order: third-party imports first, blank line, then project imports
- Alphabetical within each group
- Ktlint excludes generated code (`**/generated/**`)

**Kotlin Idioms:**
- Prefer `val` over `var` (Detekt `VarCouldBeVal` active)
- Use scoped functions: `apply`, `run`, `also`, `let`
- Companion objects for constants (`const val`)
- `data class` for DTOs and domain models
- `sealed class` for algebraic types
- `typealias` for complex generic signatures
- `suspend` functions for async operations (coroutines)

**Null-Safety:**
- Prefer non-nullable types
- Use `?.let {}` or explicit null checks for nullable values
- Avoid `!!` (Detekt `UnsafeCallOnNullableType` active for non-test code)

**Complexity Limits (Detekt):**
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

**Package Structure (Hexagonal/Ports-and-Adapters):**
```
src/main/kotlin/io/github/havonte1/tcgwatcher/backend/
├── config/           # Spring configuration classes
├── domain/           # Pure business-logic models & port interfaces
│   ├── model/        # Core domain entities (Product, SearchResult, etc.)
│   └── port/         # Hexagonal ports (interfaces)
│       ├── repository/   # CRUD contracts
│       └── scraper/      # Scraper contracts
├── application/      # Use-case / service layer
└── adapter/
    ├── inbound/      # REST controllers, mappers
    │   └── rest/
    │       └── api/      # Generated OpenAPI interfaces
    │       └── model/    # Generated DTOs
    └── out/          # Port implementations
        ├── persistence/   # JPA entities, repositories, mappers
        │   ├── entity/
        │   ├── mapper/
        │   └── repository/
        └── webscraper/    # Playwright/Jsoup scrapers
```

**Test Structure:**
```
src/test/kotlin/io/github/havonte1/tcgwatcher/backend/
├── application/      # Unit tests for services
├── adapter/
│   ├── inbound/rest/    # IT tests for REST adapters
│   ├── out/
│   │   ├── persistence/ # IT tests for persistence
│   │   └── webscraper/  # Unit + IT tests for scrapers
└── *IT.kt            # Integration tests at root level
```

**Test Resources:**
- HTML fixtures in `src/test/resources/` (e.g., `pikachu_gallery_30.html`)
- Test logging config: `src/test/resources/logback-test.000`

## Git Conventions

**Branch Naming:**
- Feature branches: `feature/<ticket-id>-description`
- Bugfix branches: `bugfix/<ticket-id>-description`

**Commit Messages:**
- Atomic commits with concise messages prefixed by intent
- Prefixes: `fix:`, `feat:`, `refactor:`
- No amend after push unless explicitly requested

## Documentation

**CRITICAL RULE: DO NOT WRITE COMMENTS!**
- No JavaDoc
- No inline comments
- No KDoc
- Exception: `TODO` comments must reference an issue key

**Note:** Do not delete existing comments when editing files.

## Error Handling

**Patterns:**
- Use `Result<T>` for expected failures (e.g., web fetcher results)
- Use `throw` for unexpected errors
- Log via `io.github.oshai.kotlinlogging.KotlinLogging`
- Never swallow exceptions (Detekt `SwallowedException` active)
- Use `UseRequire`, `UseCheckOrError`, `UseCheckNotNull` (Detekt rules active)
- For JPA entities, use `@PrePersist` and `@PreUpdate` lifecycle callbacks for timestamp management

**Logging:**
- Framework: `io.github.oshai:kotlin-logging-jvm:7.0.14`
- Pattern: `private val logger = KotlinLogging.logger {}`
- Levels observed: `debug`, `info`, `warn`

## Detekt Configuration

**Location:** `config/detekt/detekt.yml`

**Key Active Rules:**
- `complexity`: CognitiveComplexMethod (off), CyclomaticComplexMethod (14), LargeClass (600), LongMethod (60), LongParameterList (5/6), NestedBlockDepth (4), TooManyFunctions (11)
- `naming`: ClassNaming, FunctionNaming, VariableNaming, PackageNaming, MatchingDeclarationName
- `style`: MaxLineLength (120), ReturnCount (2), ThrowsCount (2), WildcardImport, MagicNumber, VarCouldBeVal, UnusedPrivateMember
- `exceptions`: SwallowedException, TooGenericExceptionCaught, TooGenericExceptionThrown
- `empty-blocks`: All empty block checks active
- `performance`: ArrayPrimitive, ForEachOnRange, SpreadOperator
- `potential-bugs`: AvoidReferentialEquality, DoubleMutability, UnnecessaryNotNullOperator, UnsafeCast

**Ktlint Configuration:**
- Version: 1.5.0
- Excludes: `**/generated/**`, `**/build/generated/**`
- `runKtlintCheckOverMainSourceSet` and `runKtlintFormatOverMainSourceSet` disabled (generated code)

---

*Convention analysis: 2026-04-03*