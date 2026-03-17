# Migration Plan: Replace Resilience4j Retry & RateLimiter with Spring Framework 7 Native Implementations

## Overview

| Current library | Purpose | Spring‑7 native replacement |
|-----------------|---------|-----------------------------|
| **Resilience4j Retry** | Automatic retry of failing calls (e.g., HTTP client, DB access) | `org.springframework.core.retry` – `RetryTemplate`, `@Retryable`, `@EnableResilientMethods` |
| **Resilience4j RateLimiter** | Throttle outbound calls (fixed‑window, sliding‑window) | `@ConcurrencyLimit` (annotation) + `@EnableResilientMethods` – concurrency throttling built‑in to Spring Core/Context |

Both native features are part of **Spring Framework 7** and are enabled via a single configuration class annotated with `@EnableResilientMethods`.

### Key Documentation Consulted
- Spring Framework 7 Javadoc: `org.springframework.core.retry` (RetryTemplate, Retryable)
- Spring 7 “Core Spring Resilience Features” blog (Sep 2025) – `@Retryable`, `@ConcurrencyLimit`, `@EnableResilientMethods`
- Spring 7 release highlights: “Resilience: `@Retryable` & `@ConcurrencyLimitDeclarative` … built into spring‑core & spring‑context”.

## Goals

1. **Remove `resilience4j` dependency** from `build.gradle.kts`.
2. **Replace each Resilience4j Retry** usage with Spring’s `@Retryable` (or programmatic `RetryTemplate` when annotation‑driven approach isn’t possible).
3. **Replace each Resilience4j RateLimiter** usage with Spring’s `@ConcurrencyLimit` (declarative) or a custom `ConcurrencyLimiter` bean for fine‑grained control.
4. **Add a single configuration class** (`@Configuration @EnableResilientMethods`) that enables the native resilience features.
5. **Run the full test suite** (`./gradlew test`) to ensure behavior stays identical.
6. **Update documentation/comments** to reference the new Spring APIs.

## Implementation Strategy

| Phase | Description | Typical Files Affected |
|-------|-------------|------------------------|
| **1️⃣ Dependency Cleanup** | Remove `implementation("io.github.resilience4j:resilience4j-retry")` and `resilience4j-ratelimiter` from `build.gradle.kts`. | `build.gradle.kts` |
| **2️⃣ Add Spring Resilience Config** | Create `ResilienceConfig.kt` with `@Configuration @EnableResilientMethods`. Optionally expose beans for `RetryTemplate` customisation. | `src/main/kotlin/.../config/ResilienceConfig.kt` |
| **3️⃣ Replace Retry Annotations** | • Find all `Retry.decorate*`, `Retry.execute*`, etc.  • Convert to `@Retryable(maxAttempts = X, backoff = @Backoff(delay = Y, maxDelay = Z, multiplier = M))` on the target service method.  • If the method isn’t a bean, inject a `RetryTemplate` bean and call `retryTemplate.execute { … }`. | Service classes under `adapter/out`, `application`, utility helpers. |
| **4️⃣ Replace RateLimiter Calls** | • Locate all `RateLimiter.acquirePermission(...)` or `RateLimiter.getPermission(...)`.  • Add `@ConcurrencyLimit(limit = N, timeout = Duration.ofMillis(...))` on the calling method (or wrapper bean).  • For reactive code, use `limitRate` or Spring’s `ConcurrencyLimit` on the returning `Mono/Flux`. | HTTP client wrappers, scraper adapters, any outbound‑call component. |
| **5️⃣ Adjust Bean Registrations** | Remove any `RetryRegistry` or `RateLimiterRegistry` beans; rely on Spring’s auto‑configuration. | Configuration classes that create resilience beans. |
| **6️⃣ Update Tests** | Adjust imports, mock `RetryTemplate` where needed, or rely on real annotations. | `src/test/kotlin/...` |
| **7️⃣ Verify Build & Lint** | Run `./gradlew clean build -x test`, then `./gradlew test`. Finally `./gradlew detekt ktlintFormat`. | Entire project |
| **8️⃣ Documentation** | Add a short note in `README.md` (or module docs) about the migration to Spring native resilience. | `README.md` or module‑specific docs |

## Detailed To‑Do List (Easy‑to‑Follow)

- [ ] **Remove Resilience4j dependencies**
    - Edit `build.gradle.kts` → delete `implementation("io.github.resilience4j:resilience4j-retry")`
    - Delete `implementation("io.github.resilience4j:resilience4j-ratelimiter")`
    - Run `./gradlew clean` to ensure no stale jars remain.

- [ ] **Add Spring resilience configuration**
    - Create `src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/ResilienceConfig.kt`
    - Content:
    ```kotlin
    @Configuration
    @EnableResilientMethods
    class ResilienceConfig
    ```
    - (Optional) Define a custom `RetryTemplate` bean if non‑default backoff is needed.

- [ ] **Convert Retry usages**
    - Search for `Retry.` or `resilience4j.retry` via grep/search.
    - For each occurrence:
        1. Identify the method that is being retried.
        2. Add `@Retryable(maxAttempts = <N>, backoff = @Backoff(delay = <ms>, maxDelay = <ms>, multiplier = <factor>))` on that method.
        3. Remove the manual `Retry` construction and execution code.
        4. If the method cannot be annotated (e.g., lambda passed to another library), inject a `RetryTemplate` bean and wrap the call:
        ```kotlin
        @Autowired lateinit var retryTemplate: RetryTemplate
        fun callExternal(): Result = retryTemplate.execute { /* original logic */ }
        ```
    - Update imports (`org.springframework.retry.annotation.Retryable`, `org.springframework.retry.annotation.Backoff`).

- [ ] **Convert RateLimiter usages**
    - Search for `RateLimiter.` usages.
    - For each call:
        1. Determine the logical limit (requests per second, concurrent threads, etc.).
        2. Add `@ConcurrencyLimit(limit = <N>, timeout = Duration.ofMillis(<ms>))` on the method that performs the call.
        3. Remove the `RateLimiter.acquirePermission()` guard.
        4. If the call occurs inside a non‑bean utility, wrap it in a Spring‑managed bean and annotate the wrapper method.
    - Import `org.springframework.core.concurrency.annotation.ConcurrencyLimit`.

- [ ] **Delete legacy registry beans**
    - Locate any `@Bean fun retryRegistry(): RetryRegistry` or `rateLimiterRegistry()` definitions.
    - Delete those beans and any related config classes.

- [ ] **Update Tests**
    - Adjust any test stubs that imported `io.github.resilience4j.retry.Retry` to use Spring’s `RetryTemplate` or rely on the real annotation behavior.
    - Verify that mock beans (e.g., `@MockBean RetryTemplate`) are provided where needed.
    - Run `./gradlew test` – fix compilation errors stemming from removed imports.

- [ ] **Run full build & lint**
    - `./gradlew clean build -x test` → ensure compilation passes.
    - `./gradlew test` → all unit/integration tests must succeed.
    - `./gradlew detekt ktlintFormat` → enforce code style.

- [ ] **Commit the changes**
    - Stage all modified files.
    - Commit with message: `feat: replace Resilience4j retry & rate‑limiter with Spring 7 native resilience`.

- [ ] **Update documentation**
    - Add a short migration note in `README.md` under a “Resilience” section.
    - Reference Spring docs: https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/core/retry/package-summary.html and the blog post.

- [ ] **Optional – Verify native image compatibility**
    - Since Spring 7 includes GraalVM hints, run `/gradlew nativeBuild` (if project supports native images) to ensure no missing reflection configs from removed Resilience4j classes.

## Quick Reference – Spring Native Resilience Annotations

| Annotation | Package | Typical Use |
|------------|---------|-------------|
| `@Retryable` | `org.springframework.retry.annotation` | Declarative retries on method level. |
| `@Backoff` (nested) | `org.springframework.retry.annotation` | Controls delay, maxDelay, multiplier. |
| `@ConcurrencyLimit` | `org.springframework.core.concurrency.annotation` | Limits concurrent invocations (acts like a rate‑limiter). |
| `@EnableResilientMethods` | `org.springframework.context.annotation` | Enables processing of the above annotations. |
| `RetryTemplate` | `org.springframework.core.retry` | Programmatic retry when annotation isn’t feasible. |

---

### Next Steps

1. Create the file `migrate_resilience.md` in the repository root (or whichever directory you prefer).  
2. The content above is already written to that file.  
3. Proceed with the implementation phases when you’re ready.

If you have any questions about a particular part of the plan—or need clarification on how a specific component should be migrated—just let me know!