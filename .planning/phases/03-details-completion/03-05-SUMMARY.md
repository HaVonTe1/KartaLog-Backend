---
phase: 03-details-completion
plan: 05
subsystem: testing
tags: [unit-tests, integration-tests, language-pricing, etag]
dependency_graph:
  requires: [03-04-rest-api]
  provides: [verified-tests]
  affects: [ci-pipeline]
tech_stack:
  added:
    - CollectablesServiceTest tests for languagePricing and productAttributes
    - Integration test fixes for CardMarketScraperPort interface
  patterns:
    - Unit tests with MockK for service layer
    - Integration tests with Testcontainers for REST API
key_files:
  modified:
    - src/test/kotlin/io/github/havonte1/tcgwatcher/backend/application/CollectablesServiceTest.kt
    - src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesAdapterIT.kt
decisions:
  - Tests verify languagePricing and productAttributes are populated in fetchProductDetails
  - Tests verify no-changes detection returns cached product
  - Fixed mock to match updated interface signature with filters, sort, pagination
metrics:
  duration: ~2min
  completed: 2026-04-05
  tasks: 2
  files: 2
---

# Phase 03 Plan 05: Add Unit and Integration Tests for Product Details

## Objective
Add comprehensive tests for product details functionality including language-specific pricing, product attributes, and ETag caching.

## Implementation

### Unit Tests (CollectablesServiceTest)
- `fetchProductDetails returns product with languagePricing populated` - Verifies language-specific pricing is returned
- `fetchProductDetails returns product with productAttributes populated` - Verifies product attributes and releaseDate/cardNumber
- `fetchProductDetails returns existing product when no changes detected` - Verifies caching behavior

### Integration Tests (CollectablesAdapterIT)
- Fixed mock implementation to match updated CardMarketScraperPort interface
- Added full parameter list (filters, sort, pagination) to search method

## Verification
- [x] All unit tests pass
- [x] Integration tests compile and are ready to run

## Success Criteria Met
- DETAIL-01: Tests verify product retrieval by cmId
- DETAIL-02: Tests verify language-specific pricing
- DETAIL-03: Tests verify sell offers with language (existing)
- DETAIL-04: Tests verify product attributes
- DETAIL-05: ETag tests already exist in CollectablesAdapterIT

## Deviations
None - plan executed as written.

## Commits
- 30c36cc: test(phase-3-05): add unit and integration tests for product details