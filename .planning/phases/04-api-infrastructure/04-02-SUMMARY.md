---
phase: 04-api-infrastructure
plan: 02
subsystem: api-infrastructure
tags: [circuit-breaker, resilience4j, fallback]
dependency_graph:
  requires: []
  provides: [API-05]
  affects: [CardMarketWebFetcher, CollectablesService]
tech_stack:
  added: [CircuitBreakerRegistry, fallback methods]
  patterns: [@CircuitBreaker annotation with fallbackMethod, onFailureRateExceeded event logging]
key_files:
  created: []
  modified:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/Resilience4jConfig.kt
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/out/webscraper/cardmarket/CardMarketWebFetcher.kt
decisions:
  - "Circuit breaker ignores NotFoundException to avoid counting 404s as failures"
  - "Fallback methods return failed Result for graceful degradation"
  - "Circuit breaker failure rate threshold: 50%, sliding window: 60 calls"
metrics:
  duration: 3min
  completed_date: "2026-04-05"
---

# Phase 04 Plan 02: Circuit Breaker Configuration

## Objective

Configure circuit breaker with fallback behavior for CardMarket scraping calls.

## Summary

Implemented circuit breaker pattern with fallback handlers to provide graceful degradation when CardMarket is unavailable. Instead of returning 500 errors, the system returns failed results that can be handled upstream.

## Implementation

**Changes made:**

1. **Resilience4jConfig.kt** - Enhanced circuit breaker configuration:
   - Added `CircuitBreakerRegistry` bean with event logging for failure rate exceeded
   - Updated `CircuitBreakerConfigCustomizer` with explicit thresholds (50% failure rate, 60 call window, 30s wait in open state)
   - Circuit breaker ignores `NotFoundException` to avoid counting 404s as failures

2. **CardMarketWebFetcher.kt** - Added fallback methods:
   - `fetchFallback` - Handles circuit breaker open for search operations
   - `fetchDetailsFallback` - Handles circuit breaker open for details operations
   - Both fallback methods log warnings and return `Result.failure(exception)`
   - Updated `@CircuitBreaker` annotations to reference fallback methods

## Verification

- Build succeeds with `./gradlew build -x test`
- Circuit breaker config includes fallback event logging
- CardMarketWebFetcher has fallback methods for both fetch operations
- Circuit breaker ignores NotFoundException

## Commits

- e47d257 feat(04-api-infrastructure): configure circuit breaker with fallback behavior
