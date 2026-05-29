# AGENTS.md – Repository Guidelines

## Quick Start
- **Build:** `./gradlew build`
- **Full check (unit + integration):** `./gradlew check`
- **Build without tests (fast compile):** `./gradlew build -x test`
- **Run unit tests only:** `./gradlew test --exclude-tags integration`
- **Run integration tests only:** `./gradlew integrationTest`
- **Single test class:** `./gradlew test --tests "io.github.havonte1.tcgwatcher.backend.<package>.<TestClass>"`
- **Single test method:** `./gradlew test --tests "ClassName.\`method description\`"`
- **Format (ktlint):** `./gradlew ktlintFormat` (triggers OpenAPI codegen first)
- **Codegen (OpenAPI):** `./gradlew compileKotlin` (triggers `openApiGenerate` first)
- **Coverage report:** `./gradlew test jacocoTestReport`
- **Parallel build:** `./gradlew build -T 1C`
- **Run app (local):** `./gradlew bootRun` → `http://localhost:8083`, Swagger at `/swagger-ui.html`
- **Run app (Docker):** `SPRING_PROFILES_ACTIVE=compose ./gradlew bootRun` → `http://localhost:8080`

## Architecture
- **Pattern:** Hexagonal — `domain` (pure models + port interfaces) → `application` (use-case services) → `adapter/inbound` (REST controllers, security) → `adapter/out` (persistence + webscraper implementations)
- **Entry point:** `TcgWatcherApplication.kt`
- **API-first:** `contract/openapi.yaml` → OpenAPI Generator (kotlin-spring, coroutines mode) → `build/generated/src/main/kotlin/`. Controllers in `adapter/inbound/rest/` implement generated interfaces. **Do not edit generated code.**
- **Database:** PostgreSQL (runtime) + SQLite (embedded, for quicksearch import). Liquibase migrations in `src/main/resources/db/changelog/`. Hibernate Envers for entity auditing. Separate DB users: `watcher_mig` (migrations), `watcher_app` (app), `watcher_readonly` (read-only).
- **Scraping:** Playwright (1.58.0) + Jsoup (1.22.1) → `adapter/out/webscraper/`. Coroutines-based (`suspend` functions). Resilience4j (2.3.0) circuit breaker (50% failure threshold, 60-call sliding window, 30s open) + retry (3 attempts, 10s base, 2x backoff). `NotFoundException` is ignored by circuit breaker via Java config (YAML `ignore-exceptions` is bugged).
- **Caching:** Caffeine (1h expiry, 1000 max entries) + HTTP ETag/If-None-Match.
- **Actuator:** Management port 8084 (local) / 8081 (Docker). Endpoints: `health,info,metrics,prometheus,loggers,env,heapdump,threaddump`. Spring Boot Admin client auto-registers to `http://localhost:9090`.

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
- **CLAUDE.md** contains extensive GSD workflow instructions, tech stack details, and code style guidance.

## Config & Ops
- **Docker Compose:** `deployment/compose.yml` — profiles: `local` (postgres via `spring-boot-docker-compose`), `deployment` (full stack: app, nginx, admin-server, postgres, ofelia).
- **Docker build:** `deployment/Dockerfile` — multi-stage, JDK 17 build → JRE 24 runtime. Chromium + Node.js installed in runtime image.
- **Env vars:** `.env` file (gitignored). Required: `WATCHER_READONLY_PWD`, `POSTGRES_PASSWORD`, `MIGRATION_PWD`, `WATCHER_MIG_PWD`, `WATCHER_APP_PWD`, `APPLICATION_PWD`, `JWT_SECRET`. JWT has hardcoded fallback secret in `application.yml`.
- **CI:** Self-hosted runner, runs `./gradlew check` on push/PR to `main`.
- **Gradle:** Configuration cache enabled. JVM args: `-Xmx2048m`. `gradle.properties` enables parallel execution and build caching.
- **Boot quirk:** `compileKotlin` and `ktlintFormat` both depend on `openApiGenerate`, so codegen always runs before compilation or formatting.

## graphify

This project has a graphify knowledge graph at `graphify-out/`.

Rules:
- Before answering architecture or codebase questions, read `graphify-out/GRAPH_REPORT.md` for god nodes and community structure.
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep.
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost).
