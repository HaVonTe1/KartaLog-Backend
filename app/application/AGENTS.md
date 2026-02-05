---
### OVERVIEW
* Implements the **application** layer – orchestrates domain logic for use‑cases.
* Pure Kotlin, Spring `@Service` beans, no framework‑specific code beyond DI.
* Each service is thin: validates input, delegates to domain/repository, returns DTOs or `Result`.

### SERVICE CLASSES
* **Naming** → `<Feature>Service` (e.g. `UserService`).
* Annotated with `@Service` (Spring).
* `private val logger = LoggerFactory.getLogger(this::class.java)` at top.
* Constructor‑injected dependencies only; `var` avoided, use `val`.
* Functions are `suspend` only when async work required; otherwise plain `fun`.

### USE‑CASE PATTERNS
* Service implements a **use‑case interface** located in `domain.usecase`.
  ```kotlin
  interface CreateUserUseCase { suspend fun execute(cmd: CreateUserCmd): Result<UserDto> }
  ```
* Service class declares `: CreateUserUseCase`.
* Interface groups related operations; implementation stays in one file.
* Tests target the interface → mock dependencies, verify behavior.

### DEPENDENCY INJECTION
* **Constructor injection** exclusively – no field or setter injection.
* Dependencies are other services, repositories (`@Repository`), or external clients.
* All injected types are interfaces when possible to aid testability.
* Spring creates beans automatically; no explicit `@Bean` definitions required.

### LOGGING
* Use **SLF4J** via `LoggerFactory`.
* Log at appropriate levels:
  * `debug` – entry/exit, payload snapshots (when not sensitive).
  * `info` – business‑important events (e.g., “User created”).
  * `warn` – recoverable anomalies.
  * `error` – unexpected exceptions; include stacktrace.
* Message templates: `logger.info("Created user {}", userId)`.

### NOTES
* Services must remain **stateless** – no mutable fields that survive beyond a method call.
* Validate external input early; throw `IllegalArgumentException` only for programming errors.
* Return `Result<T>` or domain‑specific sealed `Either` for expected failures.
* Keep public API surface minimal – expose only use‑case methods.
* Follow project code‑style (4‑space indent, K&R braces, max 120 char lines).
---
*File created by Sisyphus‑Junior.*