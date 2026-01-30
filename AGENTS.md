# AGENTS.md – Guidelines for Automated Agents

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Build / Test Commands](#build-test-commands)
3. [Running a Single Test](#running-a-single-test)
4. [Linting & Code Quality](#linting-code-quality)
5. [Code Style Guidelines](#code-style-guidelines)
   - 5.1 Imports
   - 5.2 Formatting
   - 5.3 Naming Conventions
   - 5.4 Types & Null‑Safety
   - 5.5 Error Handling
   - 5.6 Documentation & Comments
6. [Cursor / Copilot Rules](#cursor-copilot-rules)
7. [Tips for Agentic Workflows](#tips-for-agentic-workflows)
---

## Project Overview
- **Language:** Kotlin (target JVM 24) & Java (legacy)
- **Build System:** Maven multi‑module (`pom.xml` at repo root and per‑module)
- **Modules:** `domain`, `application`, `adapter` (`rest`, `persistence`), `boot`
- **Testing:** JUnit 5 via Maven Surefire plugin
- **Static Analysis:** Detekt (Kotlin), Checkstyle (Java), optional SpotBugs
---

## Build / Test Commands
All commands are run from the repository root (`/Users/dirkkutzer/dev/src/TCGWatcher-Backend`).

| Action | Maven Command | Description |
|--------|---------------|-------------|
| Clean | `mvn clean` | Remove all `target/` directories |
| Compile | `mvn compile` | Compile source sets |
| Package | `mvn package` | Assemble JAR/WAR artifacts |
| Install | `mvn install` | Deploy to local `~/.m2` repo |
| Run All Tests | `mvn test` | Execute unit & integration tests |
| Skip Tests | `mvn package -DskipTests` | Faster build when tests not needed |
| Detekt (Kotlin lint) | `mvn detekt:check` | Runs Detekt analysis |
| Checkstyle (Java lint) | `mvn checkstyle:check` | Runs Checkstyle analysis |
| SpotBugs (optional) | `mvn spotbugs:check` | Static byte‑code analysis |

> **Tip:** Use `mvn -q …` for quieter output when parsing results.
---

## Running a Single Test
Targeting a single test class or method dramatically speeds up feedback.

```bash
# Full class name (any package depth)
mvn -Dtest=com.example.service.MyServiceTest test

# Specific method inside the class
mvn -Dtest=MyServiceTest#shouldCreateUser test
```
The Surefire plugin version must be ≥ 3.0.0‑M5 for method‑level selection.
---

## Linting & Code Quality
1. **Detekt** – Kotlin lint. Config file: `detekt.yml`. Common rule groups: `comments`, `complexity`, `naming`, `performance`.
2. **Checkstyle** – Java lint. Config file: `checkstyle.xml` (present under `.idea`).
3. **SpotBugs** – Optional byte‑code analysis.
4. **Ktlint** – Formatter, available via Maven plugin or pre‑commit hook.
5. **Pre‑commit Hook** – If `.git/hooks/pre-commit` exists, ensure it runs `mvn verify`.
---

## Code Style Guidelines
All agents must adhere to the following conventions.

### 5.1 Imports
- **Kotlin:** Group third‑party imports, a blank line, then project imports. No wildcard imports. Alphabetical order within groups.
  ```kotlin
  import com.example.domain.User
  import com.example.service.UserService

  import kotlinx.coroutines.*
  import org.slf4j.LoggerFactory
  ```
- **Java:** Same grouping and ordering. Example:
  ```java
  import com.example.domain.User;
  import java.util.List;
  import org.slf4j.Logger;
  ```

### 5.2 Formatting
- Indentation: **4 spaces**, no tabs.
- Max line length: **120 characters**.
- No trailing whitespace.
- Braces: K&R style – opening brace on the same line.
- Blank lines: Separate top‑level declarations and logical block sections.
- Kotlin‑specific: `fun foo(param: String)` (no space before parentheses).

### 5.3 Naming Conventions
| Element | Kotlin | Java |
|---------|--------|------|
| Package | `lowercase.dotted` | same |
| Class / Interface | `PascalCase` | `PascalCase` |
| Enum | `PascalCase` | `PascalCase` |
| Object (Kotlin) | `PascalCase` | N/A |
| Function / Method | `camelCase` | `camelCase` |
| Variable | `camelCase` | `camelCase` |
| Constant | `UPPER_SNAKE_CASE` | `UPPER_SNAKE_CASE` |
| Test Class | `MyServiceTest` | same |
| Test Method | `shouldDoSomethingWhenCondition` | same |

### 5.4 Types & Null‑Safety (Kotlin)
- Prefer **non‑nullable** types; use `?` only when the domain permits null.
- Access nullable values via `?.let {}` or explicit checks.
- Use `data class` for DTOs, `sealed class` for algebraic types.
- When exposing Kotlin to Java, annotate with `@Nullable`/`@NotNull` (JetBrains).

### 5.5 Error Handling
- **Kotlin:** Use `Result<T>` or sealed `Either` for recoverable errors; throw exceptions for unexpected failures.
- **Java:** Prefer unchecked exceptions; checked only for truly recoverable scenarios.
- Logging: SLF4J API, e.g., `private val logger = LoggerFactory.getLogger(this::class.java)`.
- Never swallow exceptions silently; always log or re‑throw.
- Custom exceptions should reside under `com.example.exception`.

### 5.6 Documentation & Comments
- **KDoc / JavaDoc** for public APIs, including `@param`, `@return`, and `@throws` where appropriate.
- Inline comments only when the code isn’t self‑explanatory.
- `TODO` comments must reference an issue key, e.g., `// TODO JIRA-123: refactor to async`.
---

## Cursor / Copilot Rules
The repository currently **does not contain** a `.cursor` directory, `.cursorrules` file, nor a `.github/copilot-instructions.md`. If such files are added, agents should:
1. Copy the exact rules verbatim into this **AGENTS.md** under a dedicated subsection.
2. Respect any `@cursor` or `@copilot` annotations in source files.
3. Ensure generated code complies with the supplied linting configuration.
---

## Tips for Agentic Workflows
- **Read before edit:** Always `read` a file before `edit`/`write`.
- **Idempotent changes:** When adding dependencies, verify they aren’t already present.
- **Commit granularity:** Keep logical changes isolated; do not auto‑commit unless explicitly asked.
- **Testing:** After any change, run `mvn -q test` (or a targeted test) to verify build integrity.
- **Performance:** Use `-T 1C` for parallel Maven builds when appropriate.
- **Documentation:** Update `README.md` or module docs only after functional changes.
---

## Additional Build & Test Tips
- Build a specific module (and its dependencies): `mvn -pl :module-name -am compile`
- Run tests for a specific module: `mvn -pl :module-name test`
- Skip lint checks during fast builds: `-Ddetekt.skip=true -Dcheckstyle.skip=true`
- Run Ktlint formatter: `mvn ktlint:format`
- Run all linters together: `mvn verify -DskipTests`
- Generate code coverage report with JaCoCo: `mvn jacoco:prepare-agent test jacoco:report`
- Execute tests with Maven Surefire debug output: `mvn -X test`
- Run only tests matching a pattern: `mvn -Dtest=*RepositoryTest test`
- Use Maven parallel build for speed: `mvn -T 1C verify`
---

*Generated by the OpenAI opencode agent to provide a concise, agent‑friendly guideline set.*