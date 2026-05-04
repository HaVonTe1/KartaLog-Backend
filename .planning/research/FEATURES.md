# Feature Landscape

**Domain:** TCG price monitoring backend service
**Researched:** 2026-04-18
**Project:** TCGWatcher-Backend (existing Spring Boot Kotlin backend)

---

## Executive Summary

The TCG price monitoring ecosystem in 2026 is dominated by alert-focused services (tcgSniper, TCGPriceAlert, PokeNotify) and API providers (TCG API, TCGPriceLookup, TCGAPIs). Table stakes include card search, per-condition pricing, multi-game support, and email alerts. Differentiators are multi-source data (TCGPlayer + eBay), price history, graded card values, and simple API authentication. Anti-features include OAuth complexity for read-only APIs and per-variant pricing models.

---

## Table Stakes

Features users expect. Missing = product feels incomplete. No competitive viability without these.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Card search by name | Primary access method; users type "Charizard" not SKU | Low | Fuzzy matching, partial match critical |
| Product details retrieval | Core value; card image, set, rarity, condition | Low | Must include current pricing |
| Per-condition pricing (NM, LP, MP, HP, DM) | Standard in market; users buy/sell by condition | Low | TCGPlayer standard: NM, LP, MP, HP, DM |
| Multi-game support | Pokemon, MTG, Yu-Gi-Oh! at minimum | Medium | CardMarket = EU only; adding sources harder |
| Email price alerts | table stakes since CamelCamelCamel (Amazon) | Low | TCGPlayer has no native alerts |
| API key authentication | Self-serve; no approval process | Low | TCGPlayer OAuth blocks indie devs |
| Price change tracking (24h, 7d, 30d) | Market movement visibility | Low | Percentage and absolute |
| Cached pricing (Etag/If-None-Match) | Reduce scraping load, faster responses | Low | Already implemented |

### Source Attribution

- tcgpricelookup.com (2026-02): Per-condition pricing from TCGPlayer + eBay dual source
- tcgSniper.com (2026): Email alerts baseline, 15 free alerts
- TCG API (tcgapi.dev): 89+ games, simple X-API-Key auth

---

## Differentiators

Features that set product apart. Not expected, but create competitive advantage.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Multiple marketplace sources | Price comparison; TCGPlayer + eBay + Card Kingdom | High | Cross-market alerts; winner feature |
| Price history (daily snapshots) | Trend analysis; portfolio tracking | Medium | 30-day min; premium tier often |
| Graded card pricing (PSA/BGS/CGC) | Investor market; higher-value cards | High | TCGPlayer + TCGPriceLookup offer |
| Live TCGPlayer listings | True real-time; lowest listed vs sold | High | TCGSync exclusive feature |
| Bulk endpoints (500+ cards/request) | Dealer inventory; deck builders | Medium | TCG API Pro+ tier |
| Telegram + Discord notifications | Power user channel preference | Low | AddsSMS costs more |
| Real-time price alerts | Priority monitoring; faster detection | Low | Premium vs standard check frequency |
| Collection(value tracking | Portfolio over time | Medium | Not offered by alert services |

### Source Attribution

- TCGPriceLookup (2026-02): Dual-source TCGPlayer + eBay comparison; graded values
- TCGSync.com: Live TCGPlayer listings integration (industry-first)
- TCGPriceAlert.com: 6x real-time updates on Premium tier

---

## Anti-Features

Features to explicitly NOT build. These are ecosystem mistakes or misalignments.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| OAuth 2.0 for read-only API | Over-engineering; blocks indie devs; token refresh burden | Simple X-API-Key header (standard) |
| TCGPlayer-style application approval | Excludes hobbyist developers; 6+ month wait common | Self-serve sign-up |
| Per-variant alert counting | Anti-pattern; variant = condition + foil + language combo | unlimited variants per alert (tcgSniper model) |
| Seller-focused data model | Marketplace SKUs vs clean card lookups | Card-centric schema |
| Rate limits without clear response | Ambiguous 429s with no Retry-After | Transparent limit + header documentation |
| Single-game only API | Forces multi-source stitching; developer friction | Universal coverage (89+ games) |

### Source Attribution

- developer.tcgplayer.com: OAuth 2.0 + application approval process (not issuing new keys)
- Best TCG APIs 2026 comparison (tcgfast.com): OAuth complexity cited as primary reason to avoid marketplace APIs

---

## Feature Dependencies

```
Card Search
    ├── Game Selection → Multi-game support
    ├── Fuzzy/Partial Match → Search quality
    └── Set/Set Code Filter → Price lookup prep

Price Lookup
    ├── Condition Filter → Per-condition pricing
    ├── Foil/Normal Filter → Multiple printings
    └── Language Filter → Int'l cards

Price Alert
    ├── User Account → Alert storage
    ├── Target Price Threshold → Trigger logic
    ├── Notification Channel → Email/Telegram/Discord
    └── Price Check Frequency → Real-time vs standard

Multi-Source Pricing
    ├── TCGPlayer Source → Primary NA market
    ├── eBay Sold Source → Secondary market (sold vs listing)
    └── Card Kingdom Source → EU alternative
    └── Cross-Source Comparison → Differentiator value
```

---

## MVP Recommendation

Prioritize features in this order for viable product:

1. **Card search + details** (table stakes) — Already implemented via CardMarket
2. **Per-condition pricing** (table stakes) — Already exists on CardMarket
3. **Email price alerts** (table stakes) — Active requirement in PROJECT.md
4. **Multiple TCG sources** (differentiator) — Active requirement; add Card Kingdom + TCGPlayer
5. **User authentication with roles** (table stakes for alerts) — Active requirement
6. **Price history** (differentiator) — Store daily snapshots
7. **GraphQL API** (differentiation opportunity in PROJECT.md) — No major competitor offers this

Defer: Real-time WebSocket (not standard; polling sufficient), Graded card pricing (niche), Collection tracking (complex state)

---

## Gap Analysis

### What TCGWatcher Has (from PROJECT.md)
- Card search via CardMarket ✓
- Product details ✓
- ETag-based caching ✓
- PostgreSQL persistence ✓
- Basic Auth security ✓
- Circuit breaker ✓
- Health/metrics endpoints ✓

### What's Missing (from PROJECT.md)
- Additional TCG sources (CardMarket only)
- Price change alerts/notifications
- User authentication with roles
- GraphQL API support

### Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Table Stakes | HIGH | Market analysis confirms standard features from multiple sources |
| Differentiators | MEDIUM | Multi-source + history not common but documented in competitor offerings |
| Anti-features | HIGH | OAuth complexity widely criticized; clear pattern |
| Dependencies | HIGH | Simple linear dependency chain; search → lookup → alerts |

---

## Sources

- tcgpricelookup.com/blog/introducing-tcg-api (2026-02): Dual-market pricing, developer DX
- tcgSniper.com/how-it-works (2026): Alert system, variants, notification channels
- TCGPriceAlert.com (2026): Free tier, pricing tiers, notification delivery
- TCG API (tcgapi.dev): 89+ games coverage, X-API-Key auth model
- TCGSync.com/autopricing: Live TCGPlayer listings (first-to-market)
- Best TCG APIs 2026 comparison (tcgfast.com/blog, 2026-04): OAuth criticism, developer barriers
- TCGPlayer developer docs (2017): OAuth 2.0 + application approval (historical)
- PokeNotify.com: Restock alerts + Pokemon investment tools

---

*Research confidence: MEDIUM — WebSearch findings verified against multiple competitor sources; no single authoritative spec exists for TCG API features*