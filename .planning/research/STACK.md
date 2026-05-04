# Technology Stack

**Project:** TCGWatcher-Backend
**Domain:** TCG price monitoring service
**Researched:** 2026-04-18
**Confidence:** HIGH

## Recommended Stack

### Core Framework

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Spring Boot | 4.0.x | Application framework | Latest stable (4.0 released Dec 2025). Kotlin 2.2 is baseline. JSpecify null-safety native. |
| Kotlin | 2.2.x | Language | Official baseline for Spring Boot 4. Null-safety with JSpecify. Coroutines for async. |
| Java | 21+ | Runtime | Spring Boot 4 requires Java 21+. Better GC performance. |

**Source:** [Spring Boot 4 announcement](https://spring.io/blog/2025/12/18/next-level-kotlin-support-in-spring-boot-4) — HIGH confidence

### Database (unchanged from existing)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| PostgreSQL | 16+ | Primary database | Mature, robust, JSON support. Existing project uses this. |
| Liquibase | 4.x | Migration management | Declarative, rollback support. Already integrated. |
| Hibernate Envers | 6.x | Audit logging | Entity versioning. Already integrated. |

**Source:** Project existing — HIGH confidence

### Authentication & Security

| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Spring Security | 7.x | Authentication framework | Default for Spring Boot 4. Stateless JWT auth. |
| JJWT | 0.13.x | JWT token handling | Latest stable (Aug 2025). Java 8+ required for 0.14+. Pure Java, no dependencies on legacy libs. Supports JWE/JWK. |
| BCrypt | - | Password hashing | Included with Spring Security. Default strength 10 (use 12 for sensitive). |

**Source:** [JJWT releases](https://github.com/jwtk/jjwt/releases) — HIGH confidence
**Source:** [Spring Security JWT guide](https://katyella.com/blog/spring-boot-security-best-practices/) — HIGH confidence

**NOT use:** `java-jwt` (Auth0) — JJWT is more actively maintained and native Java.

### GraphQL API

| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Spring for GraphQL | 1.x | Official GraphQL support | **RECOMMENDED.** Transport-agnostic (HTTP, WebSocket, RSocket). Native coroutines support. First-party Spring project. |
| graphql-kotlin-spring-server | 9.x | Expedia's GraphQL Kotlin | Alternative. WebFlux-only (cannot mix with WebMVC). |

**Source:** [Spring for GraphQL docs](https://docs.spring.io/spring-boot/reference/web/spring-graphql.html) — HIGH confidence
**Source:** [GraphQL Kotlin docs](https://expediagroup.github.io/graphql-kotlin/docs/10.x.x/server/spring-server/spring-overview) — MEDIUM confidence

**Why NOT graphql-kotlin-spring-server:** Forces WebFlux stack which conflicts with existing WebMVC architecture. Spring for GraphQL works with both.

### Notification System

| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Spring Mail | 3.x | Email sending | Built-in. Use with Thymeleaf for templates. |
| NotifyHub | 1.x | Multi-channel notifications | Email, Slack, Telegram, Discord, SMS unified API. Async support. Fallback chains. |
| Firebase Admin SDK | 9.x | Push notifications | Firebase Cloud Messaging for mobile/web push. |

**Source:** [NotifyHub](https://dev.to/gabrielbbaldez/notifyhub-unified-notifications-for-java-and-spring-boot-4jj2) — MEDIUM confidence
**Source:** [Firebase push notifications](https://medium.com/@AlexanderObregon/sending-push-notifications-using-spring-boot-and-firebase-e1227a7eea99) — HIGH confidence

**Recommendation:** Use database queue table with scheduled processor for rate limiting (not external queue). Simple, no additional infrastructure.

### Web Scraping (unchanged from existing)

| Technology | Version | Purpose | When to Use |
|------------|---------|---------|-------------|
| Playwright | 1.48.x | Browser automation | JS-rendered pages. Existing project uses this. |
| Jsoup | 1.18.x | HTML parsing | Static HTML parsing. Existing project uses this. |
| Resilience4j | 2.x | Circuit breaker | Existing integration. 50% failure threshold working. |
| ScrapingAnt | API | Managed scraping | Optional. For protected sites with CAPTCHAs/rate limiting. |

**Source:** [Playwright best practices](https://playwright.dev/docs/best-practices) — HIGH confidence

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

**Source:** [TCG API comparison](https://tcgfast.com/blog/best-tcg-apis-2026/) — HIGH confidence
**Source:** [Cardmarket API](https://www.cardmarket-api.com/) — HIGH confidence

**Recommendation for expansion:**
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

```bash
# Core
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-mail")
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation("org.springframework.boot:spring-boot-starter-validation")

# GraphQL
implementation("org.springframework.boot:spring-boot-starter-graphql")
implementation("com.graphql-java:graphql-java-tools:5.1.0")  # if using schema-first

# Database
implementation("org.postgresql:postgresql")
implementation("org.liquibase:liquibase-core")
implementation("org.hibernate.orm:hibernate-envers")

# JWT
implementation("io.jsonwebtoken:jjwt-api:0.13.0")
implementation("io.jsonwebtoken:jjwt-impl:0.13.0") {
    exclude group: "org.bouncycastle"
}
implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")

# Scraping
implementation("com.microsoft.playwright:playwright:1.48.0")
implementation("org.jsoup:jsoup:1.18.4")

# Caching
implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

# Resilience
implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")

# YAML config
implementation("org.yaml:snakeyaml")

# Dev
detekt(":config/detekt/detekt.yml", "/config/detekt/detekt-baseline.xml")
ktlint()

# Test
testImplementation("org.springframework.boot:spring-boot-starter-test")
testImplementation("io.mockk:mockk:1.13.12")
testImplementation("org.testcontainers:testcontainers:1.20.2")
testImplementation("org.testcontainers:postgresql:1.20.2")
testRuntimeOnly("com.h2database:h2")
```

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