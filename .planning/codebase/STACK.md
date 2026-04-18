# Technology Stack

**Analysis Date:** 2026-04-05

## Languages

**Primary:**
- **Kotlin 2.2.20** (JVM) - All application code in `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/`
- **Java 17** (JVM target) - Runtime compatibility via `jvmToolchain(17)`

**Secondary:**
- **YAML** - Configuration (`application.yml`, OpenAPI spec, Liquibase changelogs)
- **SQL** - Database migrations (`src/main/resources/db/changelog/`)

## Runtime

**Environment:**
- **JVM 17** (build), **JRE 24** (runtime container via `eclipse-temurin:24-jre-alpine`)
- **Spring Boot 4.0.2** - Application framework
- **Package Manager:** Gradle 9.x (Kotlin DSL)
- **Lockfile:** Not detected (Gradle uses version catalogs or direct version pins)

## Frameworks

**Core:**
- **Spring Boot 4.0.2** - Application framework with WebMVC (`spring-boot-starter-web`)
- **Spring Data JPA** - Database access layer (`spring-boot-starter-data-jpa`)
- **Hibernate Envers** - Entity auditing (`hibernate-envers`)
- **Liquibase** - Database schema migration (`spring-boot-starter-liquibase`)
- **Spring Boot Actuator** - Health/metrics endpoints (`spring-boot-starter-actuator`)
- **Spring Boot Admin Client 4.0.2** - Application monitoring (`de.codecentric:spring-boot-admin-starter-client`)

**Web Scraping:**
- **Playwright 1.58.0** - Headless browser automation for CardMarket scraping (`com.microsoft.playwright:playwright`)
- **Jsoup 1.22.1** - HTML parsing (`org.jsoup:jsoup`)

**Resilience:**
- **Resilience4j 2.3.0** - Circuit breaker + retry patterns (`resilience4j-spring-boot3`, `resilience4j-all`, `resilience4j-kotlin`)

**Caching:**
- **Spring Cache** with **Caffeine 3.1.8** - In-memory caching (`com.github.ben-manes.caffeine:caffeine`)

**API Documentation:**
- **SpringDoc OpenAPI 2.7.0** - Swagger UI (`springdoc-openapi-starter-webmvc-ui`)

**Build/Dev:**
- **OpenAPI Generator 7.19.0** - Generates Kotlin Spring interfaces from `contract/openapi.yaml`
- **Detekt 2.0.0-alpha.1** - Static analysis
- **Ktlint 1.5.0** - Code formatting
- **Kotlinx Coroutines 1.8.1** - Async operations (`kotlinx-coroutines-reactor`, `kotlinx-coroutines-core`)

## Key Dependencies

**Critical:**
- `org.springframework.boot:spring-boot-starter-web` - HTTP layer (WebMVC, not WebFlux)
- `org.springframework.boot:spring-boot-starter-data-jpa` - ORM layer
- `com.microsoft.playwright:playwright:1.58.0` - Web scraping engine (Chromium)
- `org.jsoup:jsoup:1.22.1` - HTML content parsing
- `io.github.resilience4j:resilience4j-spring-boot3:2.3.0` - Circuit breaker for CardMarket calls
- `org.hibernate.orm:hibernate-envers` - Entity revision tracking

**Infrastructure:**
- `org.postgresql:postgresql` - PostgreSQL JDBC driver (runtime)
- `org.xerial:sqlite-jdbc:3.45.1.0` - SQLite driver (used for quicksearch import)
- `org.springframework.boot:spring-boot-starter-cache` + `com.github.ben-manes.caffeine:caffeine:3.1.8` - In-memory caching
- `org.springframework.boot:spring-boot-starter-actuator` - Health/metrics
- `de.codecentric:spring-boot-admin-starter-client:4.0.2` - Monitoring registration

**Logging:**
- `io.github.oshai:kotlin-logging-jvm:7.0.14` - Kotlin logging facade

**Testing:**
- **JUnit 5** - Test framework (`junit-jupiter-api`, `junit-jupiter-engine`)
- **MockK 1.13.12** - Kotlin mocking library (`io.mockk:mockk`)
- **Testcontainers** - PostgreSQL + ToxiProxy containers (`testcontainers-postgresql`, `testcontainers-toxiproxy`)
- **WireMock 3.13.2** - HTTP mock server (`wiremock-standalone`)
- **Kotlinx Coroutines Test 1.10.2** - Coroutine testing utilities

## Build System

**Gradle Kotlin DSL** (`build.gradle.kts`):
- Single-module project (`tcgwatcher-backend`)
- JVM toolchain: 17
- OpenAPI code generation runs before `compileKotlin`
- Custom `integrationTest` task for tagged integration tests
- Detekt + Ktlint configured (ktlint disabled for main source, enabled for scripts)
- Test output: full exception details, standard streams visible

**Key Tasks:**
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

**Environment Variables:**
- `WATCHER_READONLY_PWD` - Read-only DB user password
- `POSTGRES_PASSWORD` - Database root password
- `MIGRATION_PWD` - Liquibase migration password
- `WATCHER_MIG_PWD` - Migration user password
- `WATCHER_APP_PWD` - Application user password
- `APPLICATION_PWD` - Application password

**Application Config:**
- `src/main/resources/application.yml` - Primary config
- `src/main/resources/application-compose.yml` - Docker compose profile
- Spring Boot Admin client at `http://localhost:9090`
- Management server on port `8081`

**Resilience4j Config:**
- Circuit breaker: 50% failure threshold, 60-call sliding window, 30s open state wait
- Retry: 3 max attempts, 10s initial wait, 2x exponential backoff

**Caching Config:**
- Caffeine: 1-hour expiry, 1000 max entries

## Platform Requirements

**Development:**
- JDK 17+
- Docker (for local PostgreSQL via compose)
- Chromium (for Playwright web scraping)
- Node.js (Playwright runtime dependency)

**Production:**
- Docker container (`eclipse-temurin:24-jre-alpine` base)
- PostgreSQL 18.1
- Chromium browser installed in container
- NGINX reverse proxy (deployment profile)
- Spring Boot Admin server for monitoring
- Ofelia for scheduled DB backups

## CI/CD

**GitHub Actions** (`.github/workflows/ci.yml`):
- Self-hosted runner
- JDK 17 (Temurin)
- Gradle cache via `maxnowack/local-cache`
- Runs `./gradlew check` on push/PR to `main`

---

*Stack analysis: 2026-04-05*
