# AGENTS.md – Guidelines for Automated Agents

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Build & Test Commands](#build--test-commands)
3. [Running a Single Test](#running-a-single-test)
4. [Linting & Code Quality](#linting--code-quality)
5. [Code Style Guidelines](#code-style-guidelines)
   - 5.1 Imports
   - 5.2 Formatting
   - 5.3 Naming Conventions
   - 5.4 Types & Null‑Safety
   - 5.5 Error Handling
   - 5.6 Documentation & Comments
6. [Cursor / Copilot Rules](#cursor--copilot-rules)
7. [Common Maven Modules Layout](#common-maven-modules-layout)
8. [Tips for Agentic Workflows](#tips-for-agentic-workflows)
---

## Project Overview
- **Language:** Kotlin (target JVM 24) & Java (if any legacy code).
- **Build System:** Maven multi‑module (`pom.xml` at repository root and per‑module).
- **Modules:** `domain`, `application`, `adapter` (with sub‑modules `rest`, `persistence`), `boot`.
- **Testing Framework:** JUnit 5 (default for Maven `maven-surefire-plugin`).
- **Lint / Static Analysis:** Detekt (Kotlin) and Checkstyle (Java) – IDE configuration files already present under `.idea`.

---

## Build & Test Commands
All commands should be executed from the repository root (`/Users/dirkkutzer/dev/src/TCGWatcher-Backend`).

| Action | Maven Command | Description |
|--------|----------------|-------------|
| **Clean** | `mvn clean` | Remove `target/` directories from all modules. |
| **Compile** | `mvn compile` | Compile all source sets. |
| **Package** | `mvn package` | Build JARs/war files for each module. |
| **Install** | `mvn install` | Install built artifacts to the local repository (`~/.m2`). |
| **Run All Tests** | `mvn test` | Execute unit and integration tests across modules. |
| **Run a Single Test Class** | `mvn -Dtest=MyTestClass test` | Replace `MyTestClass` with the class name (no package prefix). |
| **Run a Single Test Method** | `mvn -Dtest=MyTestClass#myMethod test` | Replace `myMethod` with the method name. |
| **Skip Tests** | `mvn package -DskipTests` | Useful for quick builds. |
| **Run Detekt (Kotlin Lint)** | `mvn detekt:check` | Requires `detekt-maven-plugin` configured in a module’s `pom.xml`. |
| **Run Checkstyle (Java Lint)** | `mvn checkstyle:check` | Uses the `checkstyle.xml` that may be in `.idea` or a dedicated config file. |
| **Run SpotBugs** | `mvn spotbugs:check` | Optional – enables static analysis for Java bytecode. |

> **Note for agents:** Prefer `mvn -q` (quiet) when output parsing is required and `-DskipTests` for non‑test builds.

---

## Running a Single Test
Running a single test speeds up feedback loops. Use the following pattern:
```bash
# From repository root
mvn -Dtest=com.example.myservice.MyServiceTest test            # Full class name (any package depth is allowed)
# Or target a method inside the class
mvn -Dtest=MyServiceTest#shouldCreateUser test
```
If the project uses the `junit-platform-surefire-provider` (default for JUnit 5), the above works out‑of‑the‑box. Ensure that the `surefire-plugin` version is >= 3.0.0‑M5 for method‑level selection.

---

## Linting & Code Quality
1. **Detekt (Kotlin)** – Enforces Kotlin‑specific rules.
   - Configuration file: `detekt.yml` (if missing, create one based on the official template).
   - Common rule groups: `comments`, `complexity`, `empty-blocks`, `naming`, `performance`.
2. **Checkstyle (Java)** – Enforces Java naming, whitespace, and import order.
   - Configuration file: `checkstyle.xml` (IDE already contains a basic version).
3. **SpotBugs (optional)** – Finds bugs in compiled bytecode.
4. **Ktlint** – Simple formatter, can be run via Maven plugin or as a pre‑commit hook.
5. **Pre‑commit Hook** – If a `.git/hooks/pre-commit` script exists, ensure it runs `mvn verify` to catch lint failures before commits.

---

## Code Style Guidelines
The following conventions are enforced for **all agents** editing the codebase. They aim to keep the repo consistent for both Kotlin and any Java files.

### 5.1 Imports
- **Kotlin:**
  ```kotlin
  import com.example.domain.*          // Group by top‑level package
  import kotlin.collections.*
  import kotlin.coroutines.*
  ```
  - Use a blank line between third‑party and project imports.
  - No wildcard imports (`import com.example.*`) – prefer explicit class imports.
  - Alphabetically order imports within each group.
- **Java:**
  ```java
  import com.example.domain.MyEntity;
  import java.util.List;
  import java.util.stream.Collectors;
  ```
  - Same grouping & ordering rules as Kotlin.

### 5.2 Formatting
- **Indentation:** 4 spaces, **no tabs**.
- **Line length:** Max 120 characters.
- **Trailing whitespace:** Disallow.
- **Braces:** K&R style – opening brace on same line for classes, methods, if/else, loops.
- **Blank lines:**
  - Separate top‑level declarations with a single blank line.
  - Separate logical sections inside a method with a blank line.
- **Kotlin specific:** Use `ktlint` defaults – `fun` definitions have a space before the opening parenthesis (`fun foo (param: String)` is **incorrect**; use `fun foo(param: String)`).

### 5.3 Naming Conventions
| Element | Kotlin | Java |
|---------|--------|------|
| **Package** | lower‑case, dot‑separated (`com.example.service`) | same |
| **Class / Interface** | PascalCase (`UserService`) | PascalCase |
| **Enum** | PascalCase (`OrderStatus`) | PascalCase |
| **Object** | PascalCase (`MySingleton`) | N/A |
| **Function / Method** | `camelCase` (`calculateTotal`) | `camelCase` |
| **Variable** | `camelCase` (`orderId`) | `camelCase` |
| **Constant** | `UPPER_SNAKE_CASE` (`MAX_RETRIES`) | `UPPER_SNAKE_CASE` |
| **Test Class** | `MyServiceTest` | `MyServiceTest` |
| **Test Method** | `shouldDoSomethingWhenCondition` (descriptive) | same |

### 5.4 Types & Null‑Safety (Kotlin)
- Prefer **non‑nullable** types unless the domain explicitly allows null.
- Use `?` only when required; wrap nullable values with `?.let {}` or explicit checks.
- Prefer **value classes** (`data class`) for DTOs, and **sealed classes** for algebraic data types.
- When interoperating with Java, annotate nullable Kotlin parameters with `@Nullable` and non‑null with `@NotNull` (JetBrains annotations).

### 5.5 Error Handling
- **Kotlin:** Use `Result<T>` or sealed `Either` for recoverable errors; throw exceptions only for unexpected failures.
- **Java:** Checked exceptions only for truly recoverable conditions; otherwise use unchecked (`RuntimeException`).
- Log at appropriate levels (`INFO`, `WARN`, `ERROR`). Prefer **SLF4J** API: `private val logger = LoggerFactory.getLogger(this::class.java)`.
- Do **not** swallow exceptions without logging or re‑throwing.
- Create custom exception hierarchy under `com.example.exception` package.

### 5.6 Documentation & Comments
- **KDoc** for public APIs:
  ```kotlin
  /**
   * Retrieves a user by its identifier.
   *
   * @param id the unique user id
   * @return the matching [User] or `null` if not found
   */
  fun findUser(id: UUID): User?
  ```
- **JavaDoc** follows the same pattern.
- Inline comments only when the code is not self‑explanatory.
- TODOs must include a Jira/issue reference, e.g., `// TODO JIRA-123: refactor this to async`.

---

## Cursor / Copilot Rules
The repository currently **does not contain** a `.cursor` directory or `.cursorrules` file, nor a `.github/copilot-instructions.md`. If such files are added in the future, agents should:
1. Mirror the exact rules verbatim into the **AGENTS.md** under a dedicated section.
2. Respect any `@cursor` or `@copilot` annotations present in source files.
3. Ensure generated code complies with the supplied linting configuration.

---

## Common Maven Modules Layout
```
root/
├─ pom.xml                 # Parent pom, aggregates modules
├─ app/
│  ├─ domain/
│  │   ├─ src/main/kotlin/com/example/...   # Domain entities & value objects
│  │   └─ src/test/kotlin/...               # Unit tests for domain logic
│  ├─ application/
│  │   └─ src/main/kotlin/com/example/...   # Use‑case / service layer
│  ├─ adapter/
│  │   ├─ rest/
│  │   │   └─ src/main/kotlin/com/example/...   # Controllers (Spring MVC/WebFlux)
│  │   └─ persistence/
│  │       └─ src/main/kotlin/com/example/...   # Repositories, JPA entities
│  └─ boot/
│      └─ src/main/kotlin/com/example/...   # Spring Boot entry point
└─ ...
```
Each module should define its own `pom.xml` with the appropriate `spring-boot-starter-*` dependencies and inherit the parent version properties.

---

## Tips for Agentic Workflows
- **Read before edit:** Always `read` a file before using `edit` or `write`.
- **Idempotent changes:** When adding a dependency, check if it already exists.
- **Commit granularity:** Although this agent does **not** auto‑commit, keep logical changes isolated – e.g., "Add Detekt plugin" vs. "Fix naming violations".
- **Testing:** After any code change, run `mvn -q test` (or a single test) to verify the change does not break the build.
- **Performance:** Use parallel Maven builds (`-T 1C`) for large modules, but avoid when running deterministic tests.
- **Documentation:** Update `README.md` or module‑specific docs only after functional changes.

---

*Generated by the OpenAI opencode agent to give other agents a clear, unified set of conventions and commands.*
