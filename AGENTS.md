# AGENTS.md – Repository Guidelines

## Quick Start
- **Build:** `./gradlew build`
- **Full check (unit + integration):** `./gradlew check`
- **Build without tests (fast lint):** `./gradlew build -x test`
- **Run unit tests only:** `./gradlew test --exclude-tags integration`
- **Run integration tests only:** `./gradlew integrationTest`
- **Single test class:** `./gradlew test --tests "io.github.havonte1.tcgwatcher.backend.<package>.<TestClass>"`
- **Single test method:** `./gradlew test --tests "ClassName.\`method description\`"`
- **Lint + format:** `./gradlew detekt ktlintFormat`
- **Codegen (OpenAPI):** `./gradlew compileKotlin` (triggers `openApiGenerate` first)
- **Coverage report:** `./gradlew test jacocoTestReport`
- **Parallel build:** `./gradlew build -T 1C`
- **Run app:** `./gradlew bootRun` → `http://localhost:8080`, Swagger at `/swagger-ui.html`

## Architecture
- **Pattern:** Hexagonal — `domain` (pure models + port interfaces) → `application` (use-case services) → `adapter/inbound` (REST controllers, security) → `adapter/out` (persistence + webscraper implementations)
- **Entry point:** `TcgWatcherApplication.kt`
- **API-first:** `contract/openapi.yaml` → OpenAPI Generator (kotlin-spring, coroutines mode) → `build/generated/src/main/kotlin/`. Controllers in `adapter/inbound/rest/` implement generated interfaces. **Do not edit generated code.**
- **Database:** PostgreSQL (runtime) + SQLite (embedded, for quicksearch import). Liquibase migrations in `src/main/resources/db/changelog/`. Hibernate Envers for entity auditing.
- **Scraping:** Playwright (1.58.0) + Jsoup (1.22.1) → `adapter/out/webscraper/`. Coroutines-based (`suspend` functions). Resilience4j (2.3.0) circuit breaker (50% failure threshold, 60-call sliding window, 30s open) + retry (3 attempts, 10s base, 2x backoff). `NotFoundException` is ignored by circuit breaker.
- **Caching:** Caffeine (1h expiry, 1000 max entries) + HTTP ETag/If-None-Match.
- **Actuator:** Separate management port 8081 (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`). Spring Boot Admin at `http://localhost:9090`.

## Critical Constraints
- **NO COMMENTS:** No JavaDoc, no KDoc, no inline comments. Strict rule (enforced by Detekt `ForbiddenComment`). Exception: `TODO` must reference an issue key.
- **Null-safety:** Prefer non-nullable types. Use `Result<T>` for expected failures; `throw` for unexpected.
- **WebMVC, not WebFlux:** Despite reactive OpenAPI config, the runtime is Spring WebMVC.
- **Import ordering:** Third-party imports first, blank line, then project imports. Alphabetical within each group. No wildcards (Detekt `WildcardImport` active).
- **Code style:** 4-space indentation, 120 char line length, PascalCase classes, camelCase functions/variables, UPPER_SNAKE_CASE constants.

## Testing
- **Mocking:** `mockk` only.
- **Unit tests:** `src/test/kotlin/`, exclude `integration` and `e2e` JUnit tags.
- **Integration tests:** `*IT` classes, include `integration` tag. Use `@SpringBootTest` + Testcontainers (PostgreSQL via Testcontainers). `@AutoConfigureCache` for cache testing.
- **Naming:** Backtick descriptions preferred: `` `search returns one product` ``. Test classes: `ClassNameTest` (unit), `ClassNameIT` (integration).
- **Async tests:** Wrap `suspend` function calls in `runBlocking`.
- **Fixtures:** HTML snapshots in `src/test/resources/` (e.g., `pikachu_gallery_30.html`).
- **Verbose debugging:** Append `--info` or `-i` to any Gradle command.

## Workflow
- **Branching:** `feature/<ticket-id>-description` or `bugfix/<ticket-id>-description`.
- **Commits:** Atomic, prefixed `fix:`, `feat:`, `refactor:`.
- **Final check:** Always run `./gradlew detekt ktlintFormat` before finishing.
- **Edit tool:** Use `filePath` parameter (not `path`).
- **GSD workflow:** For planned work, use `/gsd:quick`, `/gsd:debug`, or `/gsd:execute-phase` before making repo edits.

## Config & Ops
- **Detekt config:** `config/detekt/detekt.yml`. Baselines: `detekt-baseline*.xml`.
- **Docker Compose:** `deployment/compose.yml` — profiles: `local` (postgres), `deployment` (full stack: app, nginx, admin-server, postgres, ofelia).
- **Docker build:** `deployment/Dockerfile` — multi-stage, JDK 17 build → JRE 24 runtime. Chromium + Node.js installed in runtime image.
- **Env vars:** `.env` file (gitignored). Required vars: `WATCHER_READONLY_PWD`, `POSTGRES_PASSWORD`, `MIGRATION_PWD`, `WATCHER_MIG_PWD`, `WATCHER_APP_PWD`, `APPLICATION_PWD`.
- **CI:** Self-hosted runner, runs `./gradlew check` on push/PR to `main`.

## graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)
