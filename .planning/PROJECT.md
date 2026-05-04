# Project: TCGWatcher-Backend

**Last updated:** 2026-04-18 after initialization

## What This Is

Spring Boot Kotlin backend service that monitors and provides pricing data for trading cards (Pokemon, Magic: The Gathering, Yu-Gi-Oh!). Scrapes CardMarket.eu for pricing data, stores in PostgreSQL, exposes via REST API with caching and ETag support.

## Why This Matters

Traders need real-time pricing to make informed buying/selling decisions. Manual price checking is time-consuming. This service automates scraping and provides API access to cached pricing data.

## Core Value

**One thing that must work:** API returns accurate, cached pricing data for collectible cards when called with a product name.

## Context

**Current State:**
- Brownfield project with existing codebase mapped
- Hexagonal architecture implemented (domain → application → adapters)
- REST API functional for search and product details
- PostgreSQL for persistence, Caffeine for in-memory cache
- Playwright + Jsoup for web scraping
- Resilience4j circuit breaker for scraping resilience

**Environment:**
- Kotlin 1.9.x, Spring Boot 3.x
- PostgreSQL database
- Playwright (Chromium) for JS-rendered pages
- Jsoup for HTML parsing
- Liquibase for migrations
- Hibernate Envers for audit

**Constraints:**
- Must respect CardMarket's robots.txt
- Rate limiting on scraping to avoid bans
- Basic Auth for API access
- Separate management port (8081) for actuator

## Requirements

### Validated

- ✓ Card search via CardMarket — existing
- ✓ Product details retrieval — existing
- ✓ ETag-based HTTP caching — existing
- ✓ PostgreSQL persistence — existing
- ✓ Basic Auth security — existing
- ✓ Circuit breaker for scraping — existing
- ✓ Health and metrics endpoints — existing

### Active

- [ ] Add more TCG sources (different marketplaces)
- [ ] Price change alerts/notifications
- [ ] User authentication with roles
- [ ] GraphQL API support

### Out of Scope

- Frontend UI — separate project
- Mobile app — separate project
- Real-time WebSocket updates — defer
- Payment processing — not applicable

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| CardMarket as primary source | Major EU marketplace, good API coverage | Working |
| Hexagonal architecture | Clear separation, testable | Implemented |
| Playwright for scraping | Handles JS-rendered pages | Working |
| YOLO mode for planning | Fast iteration preferred | Selected |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---

*Last updated: 2026-04-18 after initialization*