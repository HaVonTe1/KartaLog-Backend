# Roadmap

**Project:** TCGWatcher-Backend
**Version:** 1.0
**Granularity:** coarse (3-5 phases)

## Phases

- [x] **Phase 1: Authentication & Authorization** - User registration, JWT auth, role-based access ✓
- [ ] **Phase 2: Multi-Source Aggregation** - Additional TCG sources, normalized pricing, product details
- [ ] **Phase 3: Alerts & Notifications** - Price alerts, threshold monitoring, multi-channel notifications
- [ ] **Phase 4: Add series data to ProductDTO** - Add series data to ProductDTO

## Phase Details

### Phase 1: Authentication & Authorization

**Goal:** Users can securely register, authenticate, and access role-restricted endpoints

**Depends on:** Nothing (first phase)

**Requirements:** AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06, AUTH-07, API-03

**Success Criteria** (what must be TRUE):

1. User can register with email and password and receive confirmation
2. User can log in and receive JWT access token (15-60 min expiry)
3. User can request refresh token to extend session beyond access token expiry
4. User can log out and invalidate their refresh token
5. User roles (USER, ADMIN) are assignable and persisted with user records
6. Secured API endpoints reject requests without valid JWT
7. Admin-only endpoints (e.g., user management) reject non-admin users

**Plans:** TBD

### Phase 2: Multi-Source Aggregation

**Goal:** Users can search and view pricing data from multiple TCG marketplaces with normalized formats

**Depends on:** Phase 1

**Requirements:** SRC-01, SRC-02, SRC-03, SRC-04, SRC-05, DET-01, DET-02, DET-03, DET-04, API-01, API-02

**Success Criteria** (what must be TRUE):

1. Card search returns results from CardMarket (existing) and TCGPlayer simultaneously
2. Price data from different sources normalized to common format (currency, condition)
3. Source-specific fields (e.g., TCGPlayer specific IDs) stored alongside normalized data
4. Source registry tracks available scrapers and their operational status
5. Product details include current price per condition (NM, LP, MP, HP, DM)
6. Product details include availability status per seller
7. Product details include price history (24h, 7d, 30d changes)
8. Product details served with ETag support for cache validation

**Plans:** TBD

### Phase 3: Alerts & Notifications

**Goal:** Users can create price alerts and receive notifications when conditions are met

**Depends on:** Phase 2

**Requirements:** ALERT-01, ALERT-02, ALERT-03, ALERT-04, ALERT-05, ALERT-06, NOTIF-01, NOTIF-02, NOTIF-03, NOTIF-04, API-04, API-05

**Success Criteria** (what must be TRUE):

1. User can create price alert for specific card with target price threshold
2. User can set trigger condition (above or below target price)
3. User can list all their active alerts with current status
4. User can update or delete existing alerts
5. Alert evaluation runs on scheduled basis (not blocking user requests)
6. Email notifications sent when alert threshold crossed
7. Telegram webhook notifications delivered for triggered alerts
8. Discord webhook notifications delivered for triggered alerts
9. Notification delivery is async (non-blocking to API responses)
10. OpenAPI spec documents all alert and notification endpoints

**Plans:** TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Authentication & Authorization | 1/1 | ✓ Complete | 2026-04-18 |
| 2. Multi-Source Aggregation | 0/1 | Not started | - |
| 3. Alerts & Notifications | 0/1 | Not started | - |

### Phase 4: Add series data to ProductDTO

**Goal:** [To be planned]
**Requirements**: TBD
**Depends on:** Phase 3
**Plans:** 0 plans

Plans:
- [ ] TBD (run /gsd:plan-phase 4 to break down)

---

*Roadmap created: 2026-04-18*
*Derived from requirements, research, and granularity settings*