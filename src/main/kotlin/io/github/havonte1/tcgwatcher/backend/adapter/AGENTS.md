# AGENTS.md – Adapter Module

**Generated:** 2026-02-06

## OVERVIEW
Adapter layer exposing the domain to external systems (REST, persistence, web‑scraping).

## STRUCTURE
```
adapter/
├── inbound/   # inbound adapters (e.g., REST controllers)
│   └── rest/  # Spring MVC controllers & request mapping
├── out/       # outbound adapters (e.g., DB persistence, web‑scraper)
│   ├── persistence/   # JPA repositories & entities
│   └── webscraper/    # Playwright‑based scraper
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add new REST endpoint | `adapter/inbound/rest` | Follow existing `CollectablesAdapter` pattern |
| Persist new entity | `adapter/out/persistence` | Use Spring Data JPA repository conventions |
| Scrape external site | `adapter/out/webscraper` | Re‑use `Playwright` helper utilities |

## CONVENTIONS (adapter‑specific)
- **Naming**: Classes end with `Adapter`, `Repository`, or `Mapper` as appropriate.
- **Package**: `com.github.havonte1.adapter.<direction>.<technology>`.
- **Logging**: `private val logger = LoggerFactory.getLogger(this::class.java)` at top of each class.
- **Error handling**: Wrap external calls in `Result<T>` or `Either` and log failures.

## ANTI‑PATTERNS
- Avoid putting business logic in adapters; delegate to `application` services.
- Do not expose JPA entities directly via REST controllers.

## UNIQUE STYLES
- `CollectablesMapper.kt` uses extension functions to transform entities ↔ DTOs.
- Web‑scraper adapters employ `runBlocking` only at entry points; internal code stays suspendable.

## COMMANDS
```bash
# Run only adapter tests
./gradlew test --tests "adapter.*"
```

## NOTES
Keep adapters thin; any new feature should start in `application` and be wired here.
