# AGENTS.md – Domain Module

**Generated:** 2026-02-06

## OVERVIEW
Domain layer defining core business entities, value objects, and port interfaces.

## STRUCTURE
```
domain/
├── model/   # Kotlin data classes representing DB entities & DTOs
├── port/
│   └── out/ # Port interfaces used by the application layer
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new entity | `domain/model` | Use `data class` with JPA annotations if persisted |
| Extend a port | `domain/port/out` | Define a Kotlin `interface` that the adapter will implement |
| Refactor a value object | `domain/model` | Keep classes immutable (`val` fields) and provide `copy()` when needed |

## CONVENTIONS (domain‑specific)
- **Naming**: Entities end with a noun (`User`, `Card`); value objects are `PascalCase` with no suffix.
- **Package**: `com.github.havonte1.domain.<layer>`.
- **Immutability**: Prefer `val` for all properties; expose only getters.
- **JPA Mapping**: Annotate entity classes with `@Entity`, `@Table`, and use `@Id` with generated values.
- **Documentation**: Public entities receive KDoc describing the domain concept.

## ANTI‑PATTERNS
- Do not place business logic inside entity classes; keep them as pure data holders.
- Avoid exposing JPA `EntityManager` directly to the application layer.

## UNIQUE STYLES
- `CollectablesMapper.kt` uses extension functions to map between persistence entities and domain models.
- `SearchUseCase.kt` (in application) works only with domain interfaces, never touches adapters.

## COMMANDS
```bash
# Run only domain‑related tests (if any are annotated with *Domain*)
./gradlew test --tests "*Domain*"
```

## NOTES
The domain should remain framework‑agnostic; any Spring or Playwright dependencies belong in adapters.
