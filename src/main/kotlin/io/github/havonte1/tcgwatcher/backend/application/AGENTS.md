# AGENTS.md – Application Module

**Generated:** 2026-02-06

## OVERVIEW
Application layer orchestrating use‑cases, coordinating domain services and adapters.

## STRUCTURE
```
application/
├── CollectablesService.kt   # Service implementation (use‑case entry point)
├── SearchUseCase.kt          # Domain‑oriented search logic
└── (optional) config/       # Application‑specific configuration beans
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new use‑case | `application/` | Create a service class that injects domain ports & adapters |
| Wire a new bean | `application/config/` (if needed) | Use `@Configuration` + `@Bean` methods |
| Refactor existing logic | `CollectablesService.kt` | Keep business rules in pure Kotlin functions, avoid Spring‑specific code |

## CONVENTIONS (application‑specific)
- **Naming**: Service classes end with `Service`; use‑case classes end with `UseCase`.
- **Package**: `com.github.havonte1.application`.
- **DI**: Prefer constructor injection (`@Autowired` on the constructor) for all dependencies.
- **Error handling**: Return `Result<T>` or a sealed `Either` from public methods; log unexpected exceptions with `logger.error`.
- **Kotlin style**: `val` for immutable dependencies, scoped functions (`apply`, `also`) for builder‑style configuration.

## ANTI‑PATTERNS
- Do not place repository/JPA code inside the application layer – keep it in adapters.
- Avoid using Spring `@Component` directly on domain entities; keep them framework‑agnostic.

## UNIQUE STYLES
- `CollectablesService.kt` uses a **pipeline** of functional transformations (`filter`, `map`, `toList`) to build the result set.
- `SearchUseCase.kt` aggregates results from multiple adapters (web‑scraper + persistence) and merges them into a single DTO.

## COMMANDS
```bash
# Run only application‑related tests (if you name them *ServiceTest* or *UseCaseTest*)
./gradlew test --tests "*Service*" --tests "*UseCase*"
```

## NOTES
The application layer should remain thin – it coordinates, does not contain persistence or HTTP details.
