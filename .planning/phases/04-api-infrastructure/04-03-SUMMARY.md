---
phase: 04-api-infrastructure
plan: 03
subsystem: api-infrastructure
tags: [spring-boot-admin, health-monitoring, metrics]
dependency_graph:
  requires: []
  provides: [API-06]
  affects: [CollectablesAdapter, CardMarketHealthIndicator]
tech_stack:
  added: [spring-boot-admin-client, micrometer, CardMarketHealthIndicator]
  patterns: [HealthIndicator, Timer metrics, Counter metrics]
key_files:
  created:
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/config/CardMarketHealthIndicator.kt
  modified:
    - src/main/resources/application.yml
    - src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/CollectablesAdapter.kt
decisions:
  - "Using spring-boot-health 4.0 API (org.springframework.boot.health.contributor package)"
  - "Metrics use Timer.record() with explicit TimeUnit for compatibility"
  - "Admin server assumed at localhost:9090"
metrics:
  duration: 4min
  completed_date: "2026-04-05"
---

# Phase 04 Plan 03: Spring Boot Admin Integration

## Objective

Integrate Spring Boot Admin for health monitoring and API metrics display.

## Summary

Enabled Spring Boot Admin client registration and created custom health indicator to monitor CardMarket scraper health via circuit breaker state. Added Micrometer metrics to track API request duration and counts.

## Implementation

**Changes made:**

1. **application.yml** - Enabled Spring Boot Admin client:
   - `spring.boot.admin.client.enabled: true`
   - `uri: http://localhost:9090` - Admin server location
   - `management-port: 8081` - Actuator port for metrics exposure

2. **CardMarketHealthIndicator.kt** - Created new custom health indicator:
   - Monitors circuit breaker state (CLOSED/OPEN/HALF_OPEN)
   - Returns UP when CLOSED/HALF_OPEN, DOWN when OPEN
   - Uses `spring-boot-health` 4.0 API (different from actuator 2.x)
   - Handles exceptions gracefully returning UNKNOWN status

3. **CollectablesAdapter.kt** - Added Micrometer metrics:
   - Injected `MeterRegistry` into constructor
   - Added `Timer` for `api.search.duration` and `api.details.duration`
   - Added `Counter` for `api.search.requests` and `api.details.requests`
   - Uses `Timer.record(millis, TimeUnit.MILLISECONDS)` for compatibility

## Verification

- Build succeeds with `./gradlew build -x test -x integrationTest`
- Spring Boot Admin client enabled with proper URI
- Custom CardMarket health indicator created
- API metrics configured with Timer and Counter

## Commits

- 04117ca feat(04-api-infrastructure): integrate Spring Boot Admin for health and metrics
