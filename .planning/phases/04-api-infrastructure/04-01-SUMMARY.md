---
phase: 04-api-infrastructure
plan: 01
subsystem: api-infrastructure
tags: [rate-limiting, resilience4j, api-protection]
dependency_graph:
  requires: []
  provides: [API-04]
  affects: [CollectablesAdapter]
tech_stack:
  added: [resilience4j-ratelimiter]
  patterns: [@RateLimiter annotation, RateLimiterConfigCustomizer bean]
key_files:
  created: []
  modified:
    - src/main/resources/application.yml
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/Resilience4jConfig.kt
decisions:
  - "Rate limiter configured to allow 10 requests per second with immediate failure"
  - "Using Resilience4j RateLimiterConfigCustomizer for programmatic configuration"
metrics:
  duration: 2min
  completed_date: "2026-04-05"
---

# Phase 04 Plan 01: Rate Limiting Configuration

## Objective

Configure server-side rate limiting to protect CardMarket scraper from overuse.

## Summary

Implemented rate limiting using Resilience4j to throttle API requests and protect the CardMarket scraper from overuse that could lead to IP bans.

## Implementation

**Changes made:**

1. **application.yml** - Added rate limiter configuration under `resilience4j.ratelimiter.instances.apiRateLimiter`:
   - `limit-for-period: 10` - 10 requests per period
   - `limit-refresh-period: 1s` - Refresh every 1 second
   - `timeout-duration: 0s` - Fail immediately when limit reached
   - `register-health-indicator: true` - Enable health monitoring

2. **Resilience4jConfig.kt** - Added `RateLimiterConfigCustomizer` bean for programmatic configuration:
   - Configures same parameters as YAML (ensures consistency)
   - Uses constant `RATE_LIMIT_FOR_PERIOD = 10` for detekt compliance

The `@RateLimiter` annotations were already present on `CollectablesAdapter` methods from previous phases.

## Verification

- Build succeeds with `./gradlew build -x test`
- Rate limiter configuration present in application.yml
- Rate limiter customizer bean present in Resilience4jConfig

## Commits

- 179a52d feat(04-api-infrastructure): configure rate limiting to protect CardMarket scraper
