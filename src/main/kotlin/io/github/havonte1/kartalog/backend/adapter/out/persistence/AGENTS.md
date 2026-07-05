# AGENTS.md – Persistence Module

## OVERVIEW
JPA persistence layer implementing hexagonal outbound ports. Hibernate Envers for auditing, Liquibase for schema migrations.

## STRUCTURE
```
persistence/
├── entity/       # JPA entities with Envers auditing
├── mapper/       # Entity ↔ domain model mappers
└── repository/   # Port adapters + Spring Data JPA interfaces
```

## WHERE TO LOOK
| Task | Location |
|------|----------|
| Add new entity | `entity/` + Liquibase changelog in `src/main/resources/db/changelog/` |
| Map entity ↔ domain | `mapper/` — keep mapping logic isolated |
| Implement repository port | `repository/` — create adapter class + JPA interface |
| Add quicksearch data | `QuicksearchImportRunner.kt` — SQLite import logic |

## CONVENTIONS
- Adapter classes (`*Adapter`) implement domain port interfaces; never call directly
- `@Transactional(readOnly = true)` at class level, override per write method
- `@PrePersist` / `@PreUpdate` callbacks for timestamp management
- `RevisionInfoEntity` tracks Envers revision metadata

## ANTI-PATTERNS
- No JPA annotations in domain layer
- No `EntityManager` outside repository adapters
- No direct repository injection into application services
