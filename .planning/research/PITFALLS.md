# Project Research — Pitfalls

**Project:** TCGWatcher-Backend
**Domain:** TCG price monitoring backend service

## Common Mistakes to Avoid

### 1. Embedding Source Logic in Domain

**Problem:** Adding CardMarket/TCGPlayer-specific logic directly in domain models or services.

**Warning Signs:**
- Domain models contain source-specific fields like `cardmarketId`, `tcgplayerSku`
- Service layer has switch statements on `source` parameter

**Prevention:** Normalize all data in the adapter layer. Domain models should be source-agnostic. Use port interfaces that return normalized domain objects.

**Phase to Address:** Phase 2 (Multi-Source)

### 2. Synchronous Alert Evaluation

**Problem:** Evaluating alert rules during the user's request (blocking response).

**Warning Signs:**
- `/api/search` response time increases when alerts are active
- User requests timeout under load

**Prevention:** Use scheduled batch evaluation. Store alerts in database, run evaluation on schedule, dispatch notifications asynchronously.

**Phase to Address:** Phase 3 (Alert System)

### 3. Storing Raw Prices Without Normalization

**Problem:** Storing prices as-is from different sources (EUR, USD, GBP) without conversion.

**Warning Signs:**
- Price comparisons return incorrect results
- Historical price charts show jumps when currency rates change

**Prevention:** Normalize currency in adapter layer. Store normalized price + original price + currency code. Use adapter-level conversion before domain.

**Phase to Address:** Phase 2 (Multi-Source)

### 4. JWT Without Refresh Tokens

**Problem:** Long-lived JWT tokens or no refresh mechanism.

**Warning Signs:**
- Users logged out unexpectedly
- Security team complains about long token lifetime

**Prevention:** Short access tokens (15-60 min), implement refresh token flow. JJWT 0.13.x supports refresh tokens.

**Phase to Address:** Phase 1 (Authentication)

### 5. Scraping Without Rate Limiting

**Problem:** Hitting marketplace APIs too frequently, getting IP banned.

**Warning Signs:**
- 403 errors from CardMarket
- Circuit breaker always open

**Prevention:** Implement per-source rate limiting. Track last request timestamp per source. Use Resilience4j for circuit breaker + retry with backoff.

**Phase to Address:** Existing (extend current implementation)

### 6. Eager Loading in GraphQL

**Problem:** Loading all related entities immediately in GraphQL resolvers (N+1 problem).

**Warning Signs:**
- GraphQL queries slow despite fast REST API
- Database connection pool exhausted

**Prevention:** Use DataLoader with batch loading. Implement caching in DataLoader to avoid repeated fetches.

**Phase to Address:** Phase 4 (GraphQL API)

### 7. Ignoringrobots.txt

**Problem:** Scraping pages that disallow crawling.

**Warning Signs:**
- Getting banned from CardMarket
- Legal issues

**Prevention:** Respect robots.txt. Use Playwright's `respectRobotsTxt` option. Add explicit allowlist for permitted paths.

**Phase to Address:** Existing (ensure compliance)

---

*Pitfalls documented: 2026-04-18*
*Based on research synthesis and common hexagonal architecture anti-patterns*