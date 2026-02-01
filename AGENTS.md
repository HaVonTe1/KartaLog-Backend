# AGENTS.md – Guidelines for Automated Agents
---
## Table of Contents
1. [Project Overview](#project-overview)
2. [Build / Test Commands](#build-test-commands)
3. [Running a Single Test](#running-a-single-test)
4. [Advanced Build & Test Options](#advanced-build-test-options)
5. [Linting & Code Quality](#linting-code-quality)
6. [Code Style Guidelines](#code-style-guidelines)
   - 6.1 Imports
   - 6.2 Formatting
   - 6.3 Naming Conventions
   - 6.4 Types & Null‑Safety (Kotlin)
   - 6.5 Error Handling
   - 6.6 Documentation & Comments
   - 6.7 Kotlin‑Specific Idioms
   - 6.8 Java‑Specific Idioms
7. [CI / CD Tips](#ci-cd-tips)
8. [Git & Branch Conventions](#git-branch-conventions)
9. [Cursor / Copilot Rules](#cursor-copilot-rules)
10. [Tips for Agentic Workflows](#tips-for-agentic-workflows)
---
## Project Overview
- **Language:** Kotlin (target JVM 24) & Java (legacy)
- **Build System:** Maven multi‑module (`pom.xml` at repo root + per‑module)
- **Modules:** `domain`, `application`, `adapter` (`rest`, `persistence`), `boot`
- **Testing:** JUnit 5 via Surefire (supports method‑level selection)
- **Static Analysis:** Detekt, Checkstyle, optional SpotBugs, Ktlint
---
## Build / Test Commands
All commands run from the repository root (`/Users/dirkkutzer/dev/src/TCGWatcher-Backend`).
| Action | Maven Command | Description |
|--------|---------------|-------------|
| Clean | `mvn clean` | Remove all `target/` directories |
| Compile | `mvn compile` | Compile source sets |
| Package | `mvn package` | Assemble JAR/WAR artifacts |
| Install | `mvn install` | Deploy to local `~/.m2` repo |
| Run All Tests | `mvn test` | Execute unit & integration tests |
| Skip Tests | `mvn package -DskipTests` | Faster build when tests aren't needed |
| Detekt | `mvn detekt:check` | Kotlin lint |
| Checkstyle | `mvn checkstyle:check` | Java lint |
| SpotBugs | `mvn spotbugs:check` | Byte‑code analysis |
| Ktlint (format) | `mvn ktlint:format` | Auto‑format Kotlin sources |
> Tip: Use `mvn -q …` for quieter output and `-T 1C` for parallel builds.
---
## Running a Single Test
Target a specific test class or method to speed feedback.
```bash
# Full class name (any package depth)
mvn -Dtest=com.example.service.MyServiceTest test

# Specific method inside the class
mvn -Dtest=MyServiceTest#shouldCreateUser test
```
*Surefire must be ≥ 3.0.0‑M5 for method‑level selection.*
---
## Advanced Build & Test Options
- **Run with coverage:** `mvn test -Pcoverage` (coverage profile defined in `pom.xml`).
- **Fail‑fast:** Add `-DfailIfNoTests=false` to ignore missing tests during CI.
- **Integration tests:** Use `-Pintegration-tests` profile to activate `src/integration-test/java`.
- **Skipping lint:** `mvn verify -DskipChecks` disables Detekt/Checkstyle temporarily.
- **Verbose output:** `-X` flag for Maven debugging.
---
## Linting & Code Quality
1. **Detekt** – Config: `detekt.yml`. Common rule groups: `comments`, `complexity`, `naming`, `performance`.
2. **Checkstyle** – Config: `checkstyle.xml` (under `.idea`).
3. **SpotBugs** – Optional, run via `mvn spotbugs:check`.
4. **Ktlint** – Formatter, available via Maven plugin or pre‑commit hook.
5. **Pre‑commit** – If `.git/hooks/pre-commit` exists, ensure it runs `mvn verify`.
---
## Code Style Guidelines
All agents must follow these conventions.
### 6.1 Imports
- **Kotlin:** Third‑party imports, blank line, then project imports. No wildcards. Alphabetical within groups.
```kotlin
import com.example.domain.User
import com.example.service.UserService

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
```
- **Java:** Same grouping & ordering.
```java
import com.example.domain.User;
import java.util.List;
import org.slf4j.Logger;
```
### 6.2 Formatting
- Indentation: **4 spaces**, no tabs.
- Max line length: **120** characters.
- No trailing whitespace.
- Braces: K&R style.
- Blank lines separate top‑level declarations and logical sections.
- Kotlin: `fun foo(param: String)` (no space before parentheses).
- End files with a single newline.
### 6.3 Naming Conventions
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
| Extension Function | `fun ClassName.extName()` | N/A |
### 6.4 Types & Null‑Safety (Kotlin)
- Prefer non‑nullable types; use `?` only when the domain permits null.
- Access nullable values via `?.let {}` or explicit checks.
- Use `data class` for DTOs, `sealed class` for algebraic types.
- When exposing Kotlin to Java, annotate with `@Nullable`/`@NotNull` (JetBrains).
- Prefer `Result<T>` or a sealed `Either` for recoverable outcomes.
### 6.5 Error Handling
- **Kotlin:** Use `Result<T>`/`Either` for expected failures; throw for unexpected ones.
- **Java:** Prefer unchecked exceptions; use checked only for truly recoverable cases.
- Logging: SLF4J (`private val logger = LoggerFactory.getLogger(this::class.java)`).
- Never swallow exceptions; always log or re‑throw.
- Custom exceptions belong under `com.example.exception` and must end with `Exception`.
### 6.6 Documentation & Comments
- **KDoc / JavaDoc** for public APIs (include `@param`, `@return`, `@throws`).
- Inline comments only when the code isn’t self‑explanatory.
- `TODO` comments must reference an issue key, e.g., `// TODO JIRA-123: refactor to async`.
- Keep comment line length ≤ 120 characters.
### 6.7 Kotlin‑Specific Idioms
- Use `val` over `var` whenever possible.
- Prefer scoped functions (`apply`, `run`, `also`, `let`, `takeIf`) for builder‑style code.
- Use `companion object` for constants; mark them `const val`.
- Leverage `typealias` for complex generic signatures.
- Avoid platform types; always add explicit nullability when interoperating with Java.
### 6.8 Java‑Specific Idioms
- Prefer `java.util.Optional` for nullable returns.
- Use `var` only for fields that truly change after construction.
- Keep `@Autowired` constructors preferred over field injection.
- Use streams sparingly; favor readability over clever one‑liners.
---
## CI / CD Tips
- **GitHub Actions:** `mvn -B verify -DskipTests` for fast lint checks on PRs.
- Run full test suite on `push` to `main` with `mvn test`.
- Cache Maven dependencies (`~/.m2/repository`) to speed builds.
- Fail the workflow on any lint error (`detekt:check`/`checkstyle:check`).
- Publish JaCoCo coverage report as an artifact for PR comment.
---
## Git & Branch Conventions
- Feature branches: `feature/<ticket‑id>-short‑description`.
- Bugfix branches: `bugfix/<ticket‑id>-description`.
- Keep commits atomic and prefixed with a concise **why** (e.g., `fix: handle null user ID`).
- Do not amend commits that have been pushed unless a force‑push is explicitly requested.
- Write clear PR titles and a summary body with 1‑3 bullet points.
---
## Cursor / Copilot Rules
The repository currently **does not contain** a `.cursor` directory, `.cursorrules` file, nor a `.github/copilot-instructions.md`. If such files are added, agents should:
1. Copy the exact rules verbatim into this **AGENTS.md** under a dedicated subsection.
2. Respect any `@cursor` or `@copilot` annotations in source files.
3. Ensure generated code complies with the supplied linting configuration.
---
## Tips for Agentic Workflows
- **Read before edit:** Always `read` a file before `edit`/`write`.
- **Idempotent changes:** Verify dependencies aren’t already present before adding.
- **Commit granularity:** Keep logical changes isolated; do not auto‑commit unless explicitly asked.
- **Testing:** After any change, run `mvn -q test` (or a targeted test) to verify integrity.
- **Performance:** Use `-T 1C` for parallel Maven builds.
- **Documentation:** Update `README.md` or module docs only after functional changes.
- **Memory:** Store reusable knowledge in `.github/instructions/memory.instruction.md` with proper front‑matter.
---
*Generated by the OpenAI opencode agent.*