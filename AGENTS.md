# AGENTS.md – Repository Guidelines
---
## Table of Contents
1. [Project Overview](#project-overview)
2. [Build / Test Commands](#build--test-commands)
3. [Running a Single Test](#running-a-single-test)
4. [Advanced Build & Test Options](#advanced-build--test-options)
5. [Linting & Code Quality](#linting--code-quality)
6. [Code Style Guidelines](#code-style-guidelines)
   - 6.1 Imports
   - 6.2 Formatting
   - 6.3 Naming Conventions
   - 6.4 Types & Null‑Safety (Kotlin)
   - 6.5 Error Handling
   - 6.6 Documentation & Comments
   - 6.7 Kotlin‑Specific Idioms
   - 6.8 Java‑Specific Idioms
7. [CI / CD Tips](#ci--cd-tips)
8. [Git & Branch Conventions](#git--branch-conventions)
9. [Cursor / Copilot Rules](#cursor--copilot-rules)
10. [Agentic Workflow Tips](#agentic-workflow-tips)
---
## Project Overview
- **Languages:** Kotlin (target JVM 24) + Java (legacy)
- **Build:** Gradle Kotlin DSL, multi‑module (`domain`, `application`, `adapter`, `boot`).
- **Testing:** JUnit 5 via Surefire (method‑level selection supported).
- **Static Analysis:** Detekt, Checkstyle, SpotBugs (optional), Ktlint.
---
## Build / Test Commands
All commands run from the repository root.
| Action | Gradle Command | Description |
|--------|----------------|-------------|
| Clean | `./gradlew clean` | Deletes `build/` directories |
| Compile Kotlin | `./gradlew compileKotlin` | Compiles main Kotlin sources |
| Compile Java | `./gradlew compileJava` | Compiles main Java sources |
| Build (jar/war) | `./gradlew build` | Compiles, runs tests, assembles artefacts |
| Publish locally | `./gradlew publishToMavenLocal` | Deploys to `~/.m2` |
| Run all tests | `./gradlew test` | Executes unit + integration tests |
| Skip tests (fast build) | `./gradlew build -x test` |
| Detekt (Kotlin lint) | `./gradlew detekt` |
| Checkstyle (Java lint) | `./gradlew checkstyleMain` |
| SpotBugs (byte‑code analysis) | `./gradlew spotbugsMain` |
| Ktlint format | `./gradlew ktlintFormat` |
| JaCoCo coverage | `./gradlew test jacocoTestReport` |
---
## Running a Single Test
Use Gradle's `--tests` filter (full class name preferred):
```bash
# Run a specific test class
./gradlew test --tests "com.github.havonte1.adapter.out.webscraper.CardMarketScraperAdapterTest"

# Run a single method (Surefire ≥ 3.0.0‑M5)
./gradlew test --tests "CardMarketScraperAdapterTest.shouldReturnOneProduct"
```
---
## Advanced Build & Test Options
- **Coverage:** `./gradlew test jacocoTestReport` (HTML report under `build/reports/jacoco`).
- **Fail‑fast:** Default; add `--continue` to keep going after errors.
- **Parallel builds:** `./gradlew --parallel` for multi‑module concurrency.
- **Skip lint:** `./gradlew build -x detekt -x checkstyleMain`.
- **Verbose output:** Append `--info` or `--debug`.
---
## Linting & Code Quality
1. **Detekt** – config in `detekt.yml`; key rule groups: `comments`, `complexity`, `naming`, `performance`.
2. **Checkstyle** – XML config under `.idea/checkstyle.xml`.
3. **SpotBugs** – run with `./gradlew spotbugsMain`.
4. **Ktlint** – auto‑formatting via `ktlintFormat` and pre‑commit hook.
5. **Pre‑commit hook** (if present) runs `./gradlew build` before a commit.
---
## Code Style Guidelines
All agents must adhere to these conventions.
### 6.1 Imports
- **Kotlin:** third‑party imports, blank line, then project imports. No wildcards. Alphabetical within groups.
  ```kotlin
  import com.example.domain.User
  import com.example.service.UserService

  import kotlinx.coroutines.*
  import org.slf4j.LoggerFactory
  ```
- **Java:** same grouping and ordering.
### 6.2 Formatting
- Indent with **4 spaces**, no tabs.
- Max line length **120 characters**.
- No trailing whitespace.
- Braces in **K&R** style.
- Blank lines separate top‑level declarations.
- Kotlin functions: `fun foo(param: String)` (no space before `(`).
- Files end with a single newline.
### 6.3 Naming Conventions
| Element | Kotlin | Java |
|--------|--------|------|
| Package | `lowercase.dotted` | same |
| Class / Interface | `PascalCase` | `PascalCase` |
| Enum | `PascalCase` | `PascalCase` |
| Object (Kotlin) | `PascalCase` | N/A |
| Function / Method | `camelCase` | `camelCase` |
| Variable | `camelCase` | `camelCase` |
| Constant | `UPPER_SNAKE_CASE` | `UPPER_SNAKE_CASE` |
| Test class | `MyServiceTest` | same |
| Test method | `shouldDoSomethingWhenCondition` | same |
### 6.4 Types & Null‑Safety (Kotlin)
- Prefer non‑nullable types; use `?` only when the domain permits null.
- Access nullable values via `?.let {}` or explicit checks.
- Use `data class` for DTOs, `sealed class` for algebraic types.
- When exposing Kotlin to Java, annotate with `@Nullable` / `@NotNull` (JetBrains).
- Prefer `Result<T>` or a sealed `Either` for recoverable outcomes.
### 6.5 Error Handling
- **Kotlin:** `Result<T>` / `Either` for expected failures; `throw` for unexpected.
- **Java:** Unchecked exceptions preferred; checked only for truly recoverable cases.
- Logging via SLF4J (`private val logger = LoggerFactory.getLogger(this::class.java)`).
- Never swallow exceptions – always log or re‑throw.
- Custom exceptions live in `com.github.havonte1.exception` and end with `Exception`.
### 6.6 Documentation & Comments
- KDoc / JavaDoc for all public APIs (`@param`, `@return`, `@throws`).
- Inline comments only when code isn’t self‑explanatory.
- `TODO` comments must reference an issue key, e.g., `// TODO JIRA‑123: refactor to async`.
- Keep comment lines ≤ 120 characters.
### 6.7 Kotlin‑Specific Idioms
- Use `val` over `var` whenever possible.
- Scoped functions (`apply`, `run`, `also`, `let`, `takeIf`).
- Companion object for constants (`const val`).
- `typealias` for complex generic signatures.
- Avoid platform types; add explicit nullability when calling Java.
### 6.8 Java‑Specific Idioms
- Prefer `java.util.Optional` for nullable returns.
- Constructor injection (`@Autowired` on the constructor) over field injection.
- Streams sparingly – prioritize readability.
---
## CI / CD Tips
- **GitHub Actions:** `./gradlew build -x test` for fast lint checks on PRs.
- Full test suite on pushes to `main` with `./gradlew test`.
- Cache Gradle (`~/.gradle/caches`).
- Fail on any lint error (`./gradlew detekt && ./gradlew checkstyleMain`).
- Publish JaCoCo coverage as an artifact for PR comments.
---
## Git & Branch Conventions
- Feature branches: `feature/<ticket‑id>-short‑description`.
- Bugfix branches: `bugfix/<ticket‑id>-description`.
- Commits are atomic and prefixed with a concise *why* (`fix: handle null user ID`).
- No amend after push unless user explicitly requests a force‑push.
- PR titles short; body contains 1‑3 bullet‑point summary.
---
## Cursor / Copilot Rules
The repository currently **does not contain** a `.cursor` directory, `.cursorrules` file, nor a `.github/copilot-instructions.md`. If such files appear, agents should:
1. Copy the exact rules verbatim into this **AGENTS.md** under a dedicated subsection.
2. Respect any `@cursor` or `@copilot` annotations in source files.
3. Ensure generated code complies with the supplied lint configuration.
---
## Agentic Workflow Tips
- **Read before edit:** always `read` a file before `edit`/`write`.
- **Idempotent changes:** verify a change isn’t already present.
- **Commit granularity:** keep logical changes isolated; do **not** auto‑commit without explicit request.
- **Testing:** after any change, run the relevant test(s) (`./gradlew test --tests "<pattern>"`).
- **Performance:** use `./gradlew -T 1C` for parallel execution.
- **Documentation:** update `README.md` only after functional changes.
- **Memory:** store reusable knowledge in `.github/instructions/memory.instruction.md` with proper front‑matter.
---
*Generated by the OpenAI opencode agent.*
