---
phase: 03-details-completion
plan: 04
subsystem: adapter-inbound-rest
tags: [rest-api, openapi, etag, language-pricing]
dependency_graph:
  requires: [03-03-application-persistence]
  provides: [enhanced-api, etag-support]
  affects: [api-clients]
tech_stack:
  added:
    - LanguagePricingDTO in OpenAPI
    - ProductAttributeDTO in OpenAPI
    - Updated ProductDetailsDTO with new fields
  patterns:
    - REST API with ETag caching (existing implementation)
key_files:
  modified:
    - contract/openapi.yaml
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesMapper.kt
decisions:
  - ETag generation uses product.updatedAt epoch seconds (already implemented)
  - 304 responses include ETag header per HTTP spec
metrics:
  duration: ~2min
  completed: 2026-04-05
  tasks: 3
  files: 2
---

# Phase 03 Plan 04: Update REST API with Language Pricing and Verify ETag Support

## Objective
Update REST API to return language-specific pricing and product attributes, and verify ETag caching works correctly.

## Implementation

### OpenAPI Updates
- Added `LanguagePricingDTO` schema with locale, price, priceTrend, priceTrendValid
- Added `ProductAttributeDTO` schema with attributeName, value, attributeType
- Updated `ProductDetailsDTO` with languagePricing, productAttributes, releaseDate, cardNumber

### Mapper Updates
- `toDetailDto()` now maps languagePricing from domain to DTO
- `toDetailDto()` now maps productAttributes from domain to DTO
- `toDetailDto()` maps releaseDate and cardNumber fields

### ETag Verification
- ETag is generated from product.updatedAt epoch seconds
- If-None-Match comparison works correctly
- 304 responses include ETag header
- Cache control: max-age=1-hour, public

## Verification
- [x] OpenAPI spec generates DTOs correctly
- [x] Mapper transfers all new fields
- [x] ETag implementation verified (already present)

## Success Criteria Met
- DETAIL-01: API returns enhanced product details
- DETAIL-02: API includes language-specific pricing
- DETAIL-04: API includes product attributes
- DETAIL-05: ETag header and 304 Not Modified work correctly

## Deviations
None - plan executed as written.

## Commits
- 6ed658d: feat(phase-3-04): update REST API with language pricing and verify ETag