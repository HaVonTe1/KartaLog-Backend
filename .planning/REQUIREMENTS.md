# Requirements

**Project:** TCGWatcher-Backend
**Version:** 1.0

## v1 Requirements

### Authentication

- [ ] **AUTH-01**: User can register with email and password
- [ ] **AUTH-02**: User can log in and receive JWT access token (15-60 min expiry)
- [ ] **AUTH-03**: User can request refresh token to extend session
- [ ] **AUTH-04**: User can log out (invalidate refresh token)
- [ ] **AUTH-05**: User roles can be assigned (USER, ADMIN)
- [ ] **AUTH-06**: Secured endpoints enforce JWT authentication
- [ ] **AUTH-07**: Role-based authorization restricts admin-only actions

### Multi-Source

- [ ] **SRC-01**: Card search returns results from CardMarket (existing)
- [ ] **SRC-02**: Card search returns results from TCGPlayer (North America)
- [ ] **SRC-03**: Price data is normalized to common format in adapter layer
- [ ] **SRC-04**: Source-specific fields stored alongside normalized data
- [ ] **SRC-05**: Source registry tracks available scrapers and their status

### Product Details

- [ ] **DET-01**: Product details include current price per condition (NM, LP, MP, HP, DM)
- [ ] **DET-02**: Product details include availability status per seller
- [ ] **DET-03**: Product details include price history (24h, 7d, 30d changes)
- [ ] **DET-04**: Product details cached with ETag support (existing)

### Alerts

- [ ] **ALERT-01**: User can create price alert for specific card
- [ ] **ALERT-02**: User can set threshold (above/below target price)
- [ ] **ALERT-03**: User receives email notification when alert triggers
- [ ] **ALERT-04**: User can list their active alerts
- [ ] **ALERT-05**: User can delete/update alerts
- [ ] **ALERT-06**: Alert evaluation runs on scheduled basis (not during user request)

### Notifications

- [ ] **NOTIF-01**: Email notifications sent via SMTP or provider API
- [ ] **NOTIF-02**: Telegram webhook notifications supported
- [ ] **NOTIF-03**: Discord webhook notifications supported
- [ ] **NOTIF-04**: Notification delivery is async (non-blocking)

### API

- [ ] **API-01**: REST API provides search endpoint (existing)
- [ ] **API-02**: REST API provides product details endpoint (existing)
- [ ] **API-03**: REST API provides auth endpoints (/auth/register, /auth/login, /auth/refresh)
- [ ] **API-04**: REST API provides alert management endpoints
- [ ] **API-05**: OpenAPI spec documents all endpoints

## v2 Requirements (Deferred)

- [ ] **GRAPH-01**: GraphQL API for flexible queries
- [ ] **HIST-01**: Price history stored as daily snapshots
- [ ] **GRADED-01**: Graded card pricing (PSA/BGS/CGC)
- [ ] **COLLECT-01**: User collection tracking
- [ ] **WS-01**: Real-time WebSocket updates

## Out of Scope

- **Frontend UI** — separate project
- **Mobile app** — separate project
- **OAuth2 integration** — unnecessary complexity for read-only API
- **Payment processing** — not applicable
- **Real-time streaming** — polling sufficient for v1

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | 1 | Pending |
| AUTH-02 | 1 | Pending |
| AUTH-03 | 1 | Pending |
| AUTH-04 | 1 | Pending |
| AUTH-05 | 1 | Pending |
| AUTH-06 | 1 | Pending |
| AUTH-07 | 1 | Pending |
| SRC-01 | 2 | Pending |
| SRC-02 | 2 | Pending |
| SRC-03 | 2 | Pending |
| SRC-04 | 2 | Pending |
| SRC-05 | 2 | Pending |
| DET-01 | 2 | Pending |
| DET-02 | 2 | Pending |
| DET-03 | 2 | Pending |
| DET-04 | 2 | Pending |
| ALERT-01 | 3 | Pending |
| ALERT-02 | 3 | Pending |
| ALERT-03 | 3 | Pending |
| ALERT-04 | 3 | Pending |
| ALERT-05 | 3 | Pending |
| ALERT-06 | 3 | Pending |
| NOTIF-01 | 3 | Pending |
| NOTIF-02 | 3 | Pending |
| NOTIF-03 | 3 | Pending |
| NOTIF-04 | 3 | Pending |
| API-01 | 2 | Pending |
| API-02 | 2 | Pending |
| API-03 | 1 | Pending |
| API-04 | 3 | Pending |
| API-05 | 3 | Pending |

---

*Requirements defined: 2026-04-18*
*Generated from research and ecosystem analysis*