---
phase: 03-details-completion
plan: 03
subsystem: application-persistence
tags: [persistence, service, liquibase]
dependency_graph:
  requires: [03-02-scraper-parser]
  provides: [enhanced-persistence, service-change-detection]
  affects: [adapter-inbound-rest]
tech_stack:
  added:
    - ProductEntity columns for releaseDate, cardNumber, languagePricing, productAttributes
    - ProductMapper serialization/deserialization
    - Liquibase migration for new columns
  patterns:
    - String-based serialization for JSON-like data in TEXT columns
key_files:
  modified:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/entity/ProductEntity.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/persistence/mapper/ProductMapper.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/CollectablesService.kt
    - src/main/resources/application.yml
  created:
    - src/main/resources/db/changelog/20260405-add-product-details-columns.xml
    - src/main/resources/db/changelog/db.changelog-master.xml
decisions:
  - Used semicolon-separated serialization for languagePricing and productAttributes
  - Added Liquibase master changelog to include all migrations
metrics:
  duration: ~3min
  completed: 2026-04-05
  tasks: 3
  files: 6
---

# Phase 03 Plan 03: Update Application Layer and Persistence

## Objective
Wire enhanced product details through service and persistence layers, ensuring new fields are persisted and compared in change detection.

## Implementation

### Entity Updates
- Added columns: `release_date`, `card_number`, `language_pricing`, `product_attributes`
- All new columns use TEXT type for flexibility

### Mapper Updates
- `serializeLanguagePricing()` - Converts list to semicolon-separated string
- `deserializeLanguagePricing()` - Parses string back to list
- `serializeProductAttributes()` - Converts list to semicolon-separated string
- `deserializeProductAttributes()` - Parses string back to list

### Service Updates
- `hasChanges()` now compares: releaseDate, cardNumber, languagePricing (per locale), productAttributes

### Database Migration
- Liquibase changeSet for products table columns
- Liquibase changeSet for products_aud audit table columns

## Verification
- [x] Build compiles successfully
- [x] Entity columns match domain model fields
- [x] Change detection includes new fields

## Success Criteria Met
- DETAIL-01: Product details persist with all fields
- DETAIL-02: Language-specific pricing persists
- DETAIL-04: Product attributes persist

## Deviations
None - plan executed as written.

## Commits
- f36781d: feat(phase-3-03): update application layer and persistence for enhanced details