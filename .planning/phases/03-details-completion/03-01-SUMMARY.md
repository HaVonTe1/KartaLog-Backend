---
phase: 03-details-completion
plan: 01
subsystem: domain-model
tags: [domain, language-pricing, product-attributes]
dependency_graph:
  requires: []
  provides: [LanguagePricing, ProductAttribute, ProductAttributeType]
  affects: [adapter-out-webscraper, application-service, adapter-inbound-rest]
tech_stack:
  added:
    - LanguagePricing data class
    - ProductAttribute data class
    - ProductAttributeType enum
  patterns:
    - Domain model extension for multi-language support
key_files:
  created:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/LanguagePricing.kt
  modified:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/Product.kt
decisions:
  - Used separate data classes for LanguagePricing and ProductAttribute for type-safe access
  - ProductAttributeType enum follows CardMarket attribute naming (RARITY, RELEASE_DATE, etc.)
metrics:
  duration: ~2min
  completed: 2026-04-05
  tasks: 2
  files: 2
---

# Phase 03 Plan 01: Domain Models for Language Pricing and Product Attributes

## Objective
Define domain models for language-specific pricing and enhanced product attributes to support multi-language product details.

## Implementation

### LanguagePricing Data Class
- `locale: Locale` - Language this pricing applies to
- `price: String` - Lowest price for this language
- `priceTrend: String` - 7-day average price trend
- `priceTrendValid: Boolean` - Whether price trend is valid

### ProductAttribute Data Class
- `attributeName: String` - Name of attribute (e.g., "rarity", "releaseDate")
- `value: String` - The attribute value
- `attributeType: ProductAttributeType` - Type enum for type-safe access

### ProductAttributeType Enum
- RARITY, RELEASE_DATE, CARD_NUMBER, EXTENSION, SET_CODE, SERIES

### Product Model Updates
Added fields:
- `languagePricing: List<LanguagePricing>` - Prices per language
- `productAttributes: List<ProductAttribute>` - Additional attributes
- `releaseDate: String?` - Release date from CardMarket
- `cardNumber: String?` - Card number in set

## Verification
- [x] Domain models compile successfully
- [x] All required exports present

## Success Criteria Met
- DETAIL-01: User can retrieve detailed information by cmId (domain model ready)
- DETAIL-02: Product details include language-specific pricing (LanguagePricing model)
- DETAIL-04: Product details include CardMarket attributes (ProductAttribute model)

## Deviations
None - plan executed exactly as written.

## Commits
- 53e92e1: feat(phase-3-01): define domain models for language pricing and product attributes