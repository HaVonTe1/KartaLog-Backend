# Project Research Summary

**Project:** TCGWatcher-Backend
**Domain:** TCG price monitoring backend service
**Researched:** 2026-04-18
**Confidence:** MEDIUM-HIGH (PITFALLS.md missing)

## Executive Summary

TCGWatcher is a trading card game price monitoring backend that scrapes marketplace data (CardMarket, TCGPlayer) and provides APIs for search, pricing, and alert features. The research confirms the existing hexagonal architecture is well-suited for extensions — multi-source aggregation plugs in via defined ports, alerts follow event-driven patterns, and authentication uses standard JWT with RBAC.

Key recommendations:
- **Stack upgrade:** Spring Boot 4.0.x (Kotlin 2.2.x, Java 21+) — latest stable with native null-safety
- **GraphQL:** Spring for GraphQL (not graphql-kotlin — WebFlux-only conflicts with WebMVC)
- **Auth:** JWT with roles + permissions (JJWT 0.13.x), self-serve API keys (NOT OAuth)
- **Alerts:** Event-driven with async notification dispatch, database queue for rate limiting

Primary risk: The PITFALLS.md file was not created, so critical pitfalls are not documented. This requires attention before roadmap finalization.

## Key Findings

### Recommended Stack

Summary from STACK.md — HIGH confidence

**Core technologies:**
- **Spring Boot 4.0.x** — requires Java 21+, native Kotlin null-safety via JSpecify
- **Kotlin 2.2.x** — baseline for SB4, coroutines for async
- **PostgreSQL 16+** — existing, unchanged
- **Spring Security 7.x** — ships with SB4, stateless JWT auth
- **JJWT 0.13.x** — pure Java, no legacy deps, supports JWE/JWK
- **Spring for GraphQL** — transport-agnostic, works with WebMVC (NOT graphql-kotlin)
- **Playwright 1.48.x** — existing scraping, unchanged
- **Caffeine 3.x** — existing caching, unchanged

**New additions for expansion:**
- **NotifyHub** (1.x) — unified notification API (email, Slack, Telegram, Discord)
- **Firebase Admin SDK** (9.x) — push notifications

### Expected Features

Summary from FEATURES.md — MEDIUM confidence

**Must have (table stakes):**
- Card search by name — fuzzy/partial matching critical
- Product details with current pricing
- Per-condition pricing (NM, LP, MP, HP, DM)
- Multi-game support (Pokemon, MTG, Yu-Gi-Oh!)
- Email price alerts
- API key authentication (self-serve)
- Price change tracking (24h, 7d, 30d)

**Should have (differentiators):**
- Multiple marketplace sources (TCGPlayer + eBay + Card Kingdom)
- Price history (daily snapshots)
- Graded card pricing (PSA/BGS/CGC)
- Telegram + Discord notifications

**Defer (v2+):**
- Real-time WebSocket (polling sufficient)
- Graded card pricing (niche market)
- Collection tracking (complex state)

### Architecture Approach

Summary from ARCHITECTURE.md — HIGH confidence

**Major components:**
1. **CollectablesService** — search and product details orchestration (existing, extend multi-source)
2. **AlertService** — alert rule evaluation, notification triggering (new domain service)
3. **AuthService** — user registration, JWT issuance, role management (new domain service)
4. **ScraperPort** — unified scraping interface (existing, implement per source)
5. **AlertPort** — alert storage and evaluation (new interface)
6. **NotificationSender** — channel-specific delivery (pluggable adapters)

**Key patterns:**
- Multi-source: Unified port interfaces, source-specific adapters, normalize at adapter boundary
- Alerts: Event-driven with scheduled price scraping, batch evaluation, async notification dispatch
- Auth: JWT with embedded roles, custom Spring Security filter, ThreadLocal tenant context
- GraphQL: Schema-first with graphql-kotlin integration, DataLoader for N+1 prevention

### Critical Pitfalls

**NOT DOCUMENTED** — PITFALLS.md was not created by researchers.

ARCHITECTURE.md contains anti-patterns to avoid:
1. Embedding source logic in domain — normalize in adapter layer
2. Synchronous alert evaluation — batch evaluations, emit events
3. Storing raw prices without normalization — normalize currency/condition in adapter
4. JWT without expiration refresh — short access tokens (15-60 min), refresh tokens

This gap MUST be addressed before roadmap finalization.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: User Authentication (Foundation)
**Rationale:** Other features depend on knowing WHO is using the system
**Delivers:** User/Role/Permission entities, AuthService, JWT issuance, secured endpoints
**Implements:** JWT filter, SecurityConfig, /auth/* public API
**Avoids:** Features deferred until auth exists (alerts, user-specific data)

### Phase 2: Multi-Source Aggregation
**Rationale:** Once users exist, enable price data from multiple marketplaces
**Delivers:** TCGPlayerAdapter, SourceRegistry, normalized price model
**Implements:** ScraperPort implementations per source, price normalization
**Avoids:** Anti-pattern — embedding source logic in domain

### Phase 3: Alert System
**Rationale:** Once multi-source works, enable notifications
**Delivers:** Alert entities, AlertEvaluationService, NotificationSenders (email, webhook)
**Implements:** Scheduled price job, event-driven evaluation, async dispatch
**Avoids:** Anti-pattern — synchronous alert evaluation, storing raw prices

### Phase 4: GraphQL API (Optional)
**Rationale:** Parallel to REST — choose based on frontend needs
**Delivers:** GraphQL schema, GraphQLController, /graphql endpoint
**Uses:** Existing services (Collectables, Alert, Auth)
**Implements:** graphql-kotlin integration, DataLoader for N+1

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2 (Multi-Source):** TCGPlayer API scraping specifics, rate limit handling — needs validation
- **Phase 3 (Alerts):** Notification delivery reliability, webhook retry logic — needs implementation research

Phases with standard patterns (skip research-phase):
- **Phase 1 (Auth):** JWT + RBAC is well-documented, standard Spring Security patterns
- **Phase 4 (GraphQL):** Schema-first pattern is standard with graphql-kotlin

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Official Spring docs, verified versions |
| Features | MEDIUM | WebSearch findings verified against competitors, no single authoritative spec |
| Architecture | HIGH | Hexagonal patterns well-documented, existing foundation |
| Pitfalls | **LOW** | PITFALLS.md not created |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **PITFALLS.md missing:** Critical gap — must be created before roadmap finalization. Use ARCHITALL.md anti-patterns as starting point but document specific pitfalls for this domain.
- **TCGPlayer scraping specifics:** Actual API details not deep-dived — needs validation during Phase 2 implementation.
- **Notification delivery:** Webhook reliability, retry logic — needs specific implementation research.

## Sources

### Primary (HIGH confidence)
- Spring Boot 4 announcement (2025-12) — framework baseline
- Spring for GraphQL docs — graphql integration
- JJWT releases (0.13.x) — token handling

### Secondary (MEDIUM confidence)
- tcgpricelookup.com (2026-02) — market analysis, feature expectations
- TCG API (tcgapi.dev) — API model for TCG sources
- tcgSniper.com, TCGPriceAlert.com — alert system patterns
- TCGFast.com blog (2026-04) — API comparison

### Tertiary (LOW confidence)
- Competitor feature lists — inferred rather than documented
- PITFALLS — not researched

---
*Research completed: 2026-04-18*
*Ready for roadmap: conditional — PITFALLS.md must be created*