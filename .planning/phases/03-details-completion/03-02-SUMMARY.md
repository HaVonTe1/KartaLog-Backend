---
phase: 03-details-completion
plan: 02
subsystem: adapter-out-webscraper
tags: [scraper, parser, language-pricing, product-attributes]
dependency_graph:
  requires: [03-01-domain-models]
  provides: [enhanced-parser, language-pricing-extraction]
  affects: [application-service, mapper]
tech_stack:
  added:
    - CardmarketLanguagePricingDto
    - ProductAttributeDto
    - parseLanguagePricing method
    - extractAdditionalAttributes method
  patterns:
    - Multi-language price extraction from HTML tables
    - Product attribute extraction from info list
key_files:
  created:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/Dtos.kt (updated)
  modified:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketDetailsParser.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketProductMapper.kt
decisions:
  - Language pricing extracted from HTML table with locale code mapping
  - Product attributes built from rarity, release date, card number, and set name
  - Graceful handling when language pricing table is not present
metrics:
  duration: ~3min
  completed: 2026-04-05
  tasks: 3
  files: 3
---

# Phase 03 Plan 02: Enhance Scraper Parser for Language-Specific Pricing

## Objective
Enhance the CardMarket details parser to extract language-specific pricing and product attributes from HTML pages.

## Implementation

### DTO Updates
- Added `CardmarketLanguagePricingDto` with locale, price, priceTrend, priceTrendValid
- Added `ProductAttributeDto` with attributeName, value, attributeType
- Updated `CardmarketProductDetailsDto` with new fields

### Parser Enhancements
- `parseLanguagePricing()` - Extracts pricing from HTML table for all supported locales (de, en, fr, es, it, pt, pl)
- `extractAdditionalAttributes()` - Gets release date and card number from info list
- `buildProductAttributes()` - Constructs attribute list from extracted data

### Mapper Updates
- `toProductDetails()` now maps languagePricing, productAttributes, releaseDate, cardNumber

## Verification
- [x] All DTOs compile
- [x] Parser methods integrated
- [x] Mapper transfers new fields

## Success Criteria Met
- DETAIL-02: Product details include language-specific pricing
- DETAIL-03: Product details include sell offers with language (already exists)
- DETAIL-04: Product details include CardMarket attributes

## Deviations
None - plan executed as written.

## Commits
- 551a0c9: feat(phase-3-02): enhance scraper parser for language-specific pricing