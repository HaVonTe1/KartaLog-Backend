# AGENTS.md – Repository Guidelines
---
## Table of Contents
1. [Project Overview](#project-overview)
2. [Build & Test Commands](#build--test-commands)
3. [Running a Single Test](#running-a-single-test)
4. [Linting & Static Analysis](#linting--static-analysis)
5. [Code Style Guidelines](#code-style-guidelines)
   - 5.1 Imports
   - 5.2 Formatting
   - 5.3 Naming Conventions
   - 5.4 Types & Null‑Safety (Kotlin)
   - 5.5 Error Handling
   - 5.6 Documentation & Comments
   - 5.7 Kotlin‑Specific Idioms
   - 5.8 Java‑Specific Idioms
6. [CI / CD Tips](#ci--cd-tips)
7. [Git & Branch Conventions](#git--branch-conventions)
8. [Cursor / Copilot Rules](#cursor--copilot-rules)
9. [Agentic Workflow Tips](#agentic-workflow-tips)
---
## Project Overview
- **Languages:** Kotlin (JVM 24) + Java (legacy)
- **Build System:** Gradle Kotlin DSL, multi‑module (`domain`, `application`, `adapter`, `boot`).
- **Testing:** JUnit 5 (Surefire) with method‑level filtering.
- **Static Analysis:** Detekt, Checkstyle, SpotBugs, Ktlint.
---
## Build & Test Commands
All commands are run from the repository root.
| Action | Gradle Command | Description |
|--------|----------------|-------------|
| Clean | `./gradlew clean` | Delete `build/` directories |
| Compile Kotlin | `./gradlew compileKotlin` | Compile main Kotlin sources |
| Compile Java | `./gradlew compileJava` | Compile main Java sources |
| Build (jar/war) | `./gradlew build` | Compile, run tests, assemble artefacts |
| Publish locally | `./gradlew publishToMavenLocal` | Deploy to `~/.m2` |
| Run all tests | `./gradlew test` | Execute unit + integration tests |
| Skip tests (fast build) | `./gradlew build -x test` | Build without running tests |
| Detekt (Kotlin lint) | `./gradlew detekt` | Run Kotlin static analysis |
| Checkstyle (Java lint) | `./gradlew checkstyleMain` | Run Java style checks |
| SpotBugs | `./gradlew spotbugsMain` | Byte‑code analysis |
| Ktlint format | `./gradlew ktlintFormat` | Auto‑format Kotlin code |
| JaCoCo coverage | `./gradlew test jacocoTestReport` | Generate coverage report |
---
## Running a Single Test
Gradle's `--tests` filter selects specific classes or methods. Prefer the fully‑qualified class name.
```bash
# Run a whole test class
./gradlew test --tests "com.github.havonte1.adapter.out.webscraper.CardMarketScraperAdapterTest"

# Run a single test method (Surefire ≥ 3.0.0‑M5)
./gradlew test --tests "CardMarketScraperAdapterTest.shouldReturnOneProduct"
```
---
## Linting & Static Analysis
1. **Detekt** – configuration in `detekt.yml`; focuses on comments, complexity, naming, performance.
2. **Checkstyle** – XML config at `.idea/checkstyle.xml`.
3. **SpotBugs** – run via `./gradlew spotbugsMain`.
4. **Ktlint** – auto‑formatting via `ktlintFormat` and hooked into pre‑commit.
5. **Pre‑commit hook** (if present) runs `./gradlew build` before a commit.
---
## Code Style Guidelines
All agents must follow these conventions.
### 5.1 Imports
- **Kotlin:** third‑party imports, blank line, then project imports. No wildcard imports. Alphabetical within each group.
```kotlin
import com.example.service.UserService
import com.example.domain.User

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
```
- **Java:** same grouping and ordering.
### 5.2 Formatting
- Indent with **4 spaces**; never use tabs.
- Maximum line length **120 characters**.
- No trailing whitespace.
- Braces in **K&R** style.
- Blank lines separate top‑level declarations.
- Kotlin function signature: `fun foo(param: String)` (no space before `(`).
- Files end with a single newline.
### 5.3 Naming Conventions
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
### 5.4 Types & Null‑Safety (Kotlin)
- Prefer non‑nullable types; use `?` only when the domain permits null.
- Access nullable values via `?.let {}` or explicit `if (x != null)` checks.
- Use `data class` for DTOs, `sealed class` for algebraic types.
- When exposing Kotlin APIs to Java, annotate with `@Nullable` / `@NotNull` (JetBrains).
- Prefer `Result<T>` or a sealed `Either` for recoverable outcomes.
### 5.5 Error Handling
- **Kotlin:** `Result<T>` / `Either` for expected failures; `throw` for unexpected conditions.
- **Java:** Unchecked exceptions for most cases; checked exceptions only for truly recoverable scenarios.
- Logging via SLF4J (`private val logger = LoggerFactory.getLogger(this::class.java)`).
- Never swallow exceptions – always log or re‑throw.
- Custom exceptions live in `com.github.havonte1.exception` and end with `Exception`.
### 5.6 Documentation & Comments
- KDoc / JavaDoc for all public APIs (`@param`, `@return`, `@throws`).
- Inline comments only when the code isn’t self‑explanatory.
- `TODO` comments must reference an issue key, e.g., `// TODO JIRA‑123: refactor to async`.
- Keep comment lines ≤ 120 characters.
### 5.7 Kotlin‑Specific Idioms
- Prefer `val` over `var`.
- Use scoped functions (`apply`, `run`, `also`, `let`, `takeIf`).
- Companion object for constants (`const val`).
- `typealias` for complex generic signatures.
- Avoid platform types; add explicit nullability when calling Java.
### 5.8 Java‑Specific Idioms
- Prefer `java.util.Optional` for nullable returns.
- Constructor injection (`@Autowired` on the constructor) over field injection.
- Use streams sparingly; prioritize readability.
---
## CI / CD Tips
- **GitHub Actions:** `./gradlew build -x test` for fast lint checks on PRs.
- Full test suite on pushes to `main` with `./gradlew test`.
- Cache Gradle (`~/.gradle/caches`).
- Fail on any lint error: `./gradlew detekt && ./gradlew checkstyleMain`.
- Publish JaCoCo coverage as an artifact for PR comments.
---
## Git & Branch Conventions
- Feature branches: `feature/<ticket‑id>-short‑description`.
- Bug‑fix branches: `bugfix/<ticket‑id>-description`.
- Commits are atomic and prefixed with a concise *why* (`fix: handle null user ID`).
- No amend after push unless the user explicitly requests a force‑push.
- PR titles are short; body contains 1‑3 bullet‑point summary.
---
## Cursor / Copilot Rules
The repository currently **does not contain** a `.cursor` directory, `.cursorrules` file, nor a `.github/copilot-instructions.md`. If any of these files appear in the future, agents should:
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
