# AGENTS.md – Webscraper Module

## OVERVIEW
Playwright + Jsoup scraping pipeline for CardMarket.eu pricing data. Coroutines-based with Resilience4j fault tolerance.

## STRUCTURE
```
webscraper/
├── PlaywrightManager.kt     # Browser lifecycle singleton
├── BrowserContextPool.kt    # Reusable browser context pool
└── cardmarket/              # CardMarket-specific implementation
    ├── CardMarketScraperAdapter.kt   # Port implementation
    ├── CardMarketWebFetcher.kt       # HTTP fetch (Playwright)
    ├── CardMarketGalleryParser.kt    # Search result HTML parser
    ├── CardMarketDetailsParser.kt    # Product detail HTML parser
    ├── CardMarketProductMapper.kt    # DTO → domain model mapper
    └── Dtos.kt                       # Raw scraping DTOs
```

## CONVENTIONS
- Pipeline: Fetcher → Parser → Mapper (separate concerns)
- `suspend` functions for all async scraping operations
- Raw scraping DTOs in `Dtos.kt`; never exposed outside adapter
- `CardMarketProductMapper` handles DTO → domain mapping
- `ParseErrors.kt` for typed parsing failures (`MissingElement`, `UnexpectedFormat`)
- `CloudFlareException` for anti-bot detection handling

## ANTI-PATTERNS
- No Playwright/Jsoup imports outside this module
- No domain model references in parser classes
- No raw DTOs returned from the adapter port implementation
