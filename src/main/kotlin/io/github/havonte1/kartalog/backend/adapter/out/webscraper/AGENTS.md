# AGENTS.md – Webscraper Module

## OVERVIEW
Playwright + Jsoup scraping pipeline for CardMarket.eu pricing data. Coroutines-based with Resilience4j fault tolerance.

Runtime-switchable scraping strategies via management API (`/actuator/scraper/strategy`).

## STRUCTURE
```
webscraper/
├── PlaywrightManager.kt        # Browser lifecycle singleton (legacy, not used by strategies)
├── BrowserContextPool.kt       # Reusable browser context pool
├── strategy/                   # Runtime-switchable scraping strategies
│   ├── ScrapingStrategy.kt               # Interface: id, displayName, isAvailable, fetch(), close()
│   ├── ScrapingStrategyRegistry.kt       # Strategy lookup by id
│   ├── ScrapingStrategySelector.kt       # @Component, AtomicReference for runtime switch
│   ├── ChromiumPlaywrightStrategy.kt     # In-process: Java Playwright + Chromium
│   ├── CamoufoxPlaywrightStrategy.kt     # In-process: Java Playwright + Camoufox (Firefox fork)
│   ├── ChromeCdpStrategy.kt             # Out-of-process: real Chrome via CDP (the only Cloudflare-bypassing strategy)
│   ├── WorkerStrategy.kt                 # Sealed base: HTTP POST to external worker
│   ├── PuppeteerWorkerStrategy.kt        # Out-of-process: → scraper-worker (puppeteer-extra)
│   ├── PlaywrightExtraWorkerStrategy.kt  # Out-of-process: → scraper-worker-playwright (playwright-extra)
│   └── CamoufoxPythonWorkerStrategy.kt   # Out-of-process: → scraper-worker-python (camoufox + ClickSolver)
└── cardmarket/                 # CardMarket-specific implementation
    ├── CardMarketScraperAdapter.kt    # Port implementation
    ├── CardMarketWebFetcher.kt        # URL building + delegates to active strategy via selector
    ├── CardMarketWebFetcherPort.kt    # Internal port for strategy delegation
    ├── CardMarketGalleryParser.kt     # Search result HTML parser
    ├── CardMarketDetailsParser.kt     # Product detail HTML parser
    ├── CardMarketProductMapper.kt     # DTO → domain model mapper
    ├── CardMarketSearchParams.kt      # Search parameter types
    ├── Dtos.kt                        # Raw scraping DTOs
    ├── ParseErrors.kt                 # Typed parsing failures (MissingElement, UnexpectedFormat)
    ├── CloudFlareException.kt         # Anti-bot detection exception
    └── TranslationMap.kt              # CardMarket locale → domain locale mapping
```

## STRATEGIES
| Id | Display Name | Type | Available When |
|----|-------------|------|----------------|
| `chromium` | Java Playwright - Chromium | In-process | Always |
| `camoufox` | Java Playwright - Camoufox | In-process | Always (binary required) |
| `puppeteer-worker` | Puppeteer scraper-worker | HTTP → Node.js | `scraper.workers.puppeteer.url` set |
| `playwright-extra-worker` | playwright-extra scraper-worker | HTTP → Node.js | `scraper.workers.playwright-extra.url` set |
| `camoufox-python-worker` | Camoufox Python worker (playwright-captcha) | HTTP → Python | `scraper.workers.camoufox-python.url` set |
| `chrome-cdp` | Real Chrome via CDP | CDP → host Chrome | `scraper.chrome-cdp.url` set (default: enabled) |
| `camoufox-cdp` | Camoufox (Firefox fork) via Playwright Server | WS → host Node.js bridge → Camoufox | `scraper.camoufox-cdp.url` set (default: enabled) |

Switch at runtime: `PUT /actuator/scraper/strategy` `{"strategy": "chrome-cdp"}` or `"camoufox-cdp"`

## CONVENTIONS
- Pipeline: Strategy → Fetcher → Parser → Mapper (separate concerns)
- `suspend` functions for all async scraping operations
- Raw scraping DTOs in `Dtos.kt`; never exposed outside adapter
- `CardMarketProductMapper` handles DTO → domain mapping
- `ParseErrors.kt` for typed parsing failures (`MissingElement`, `UnexpectedFormat`)
- `CloudFlareException` for anti-bot detection handling

## ANTI-PATTERNS
- No Playwright/Jsoup imports outside this module
- No domain model references in parser classes
- No raw DTOs returned from the adapter port implementation
