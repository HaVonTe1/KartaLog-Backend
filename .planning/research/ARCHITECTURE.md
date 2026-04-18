# Architecture Research: TCG Price Monitoring System

**Domain:** Trading Card Game Price Monitoring Backend
**Researched:** 2026-04-18
**Confidence:** HIGH

## Executive Summary

TCG price monitoring systems require four architectural extensions beyond basic scraping: multi-source aggregation, alert engines, user authentication with RBAC, and optional GraphQL API. The research confirms the existing hexagonal architecture provides an ideal foundation — each new capability plugs into the domain core via defined ports, keeping business logic isolated from infrastructure concerns.

Key findings:
- **Multi-source:** Use unified port interfaces with source-specific adapters, normalize data at the adapter boundary
- **Alerts:** Event-driven with async notification dispatch, threshold triggers stored as domain objects
- **Auth:** JWT with roles, tenant context via ThreadLocal, Spring Security with custom filters
- **GraphQL:** graphql-kotlin schema generator integrates cleanly with hexagonal ports

---

## Recommended Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Inbound Adapters                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ REST API   │  │ GraphQL   │  │ WebSocket  │   (future)   │   │  │ (existing) │  │ (optional) │  │            │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘              │
│        │               │                │                       │
├────────┴───────────────┴────────────────┴───────────────────┤
│                      Application Layer                        │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐  │
│  │ Collectables  │  │ AlertService  │  │ AuthService  │  │  │  Service     │  │            │  │            │  │
│  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘  │
│         │                  │                  │                  │
├─────────┴──────────────────┴──────────────────┴────────────────┤
│                       Domain Core                            │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  Models: Product, Price, Alert, User, Role, Permission │    │
│  │  Ports: ScrapingPort, AlertPort, AuthPort            │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                               │
├───────────────────────────────────────────────────────────────┤
│                     Outbound Adapters                       │
│  ┌──────────────┐  ┌──────────┐  ┌──────────────┐              │
│  │ Scraper   │  │ Notifier │  │ Repository │              │
│  │ Adapters │  │ Adapters │  │ Adapters  │              │
│  └──────────┘  └──────────┘  └──────────────┘              │
│  ┌──────────────┐  ┌──────────┐                              │
│  │ CardMarket  │  │ TCGPlayer │  (future sources)              │
│  │ Adapter    │  │ Adapter  │                              │
│  └──────────────┘  └──────────┘                              │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Implementation |
|-----------|---------------|----------------|
| `CollectablesService` | Search and product details orchestration | Existing — extend with multi-source |
| `AlertService` | Alert rule evaluation, notification triggering | New — domain service |
| `AuthService` | User registration, JWT issuance, role management | New — domain service |
| `ScraperPort` | Unified scraping interface | Existing interface — implement per source |
| `AlertPort` | Alert storage and evaluation | New interface |
| `NotificationSender` | Channel-specific notification delivery | Pluggable adapters (email, webhook) |
| `CardMarketAdapter` | CardMarket.eu scraping | Existing |
| `TCGPlayerAdapter` | TCGPlayer.com scraping | New adapter |

---

## Multi-Source Architecture

### Pattern: Unified Port, Multiple Adapters

Each TCG marketplace implements a common port interface. The application service normalizes data to the unified domain model before it reaches the domain core.

```
domain/port/out/ScraperPort.kt
    │
    ├── fun search(query: String): List<SearchResult>
    └── fun getProductDetails(productId: String): Product

adapter/out/webscraper/cardmarket/CardMarketAdapter.kt  → implements ScraperPort
adapter/out/webscraper/tcgplayer/TCGPlayerAdapter.kt    → implements ScraperPort
adapter/out/webscraper/ebay/EbayAdapter.kt          → implements ScraperPort (future)
```

**Why this works:**
- Domain remains unaware of source-specific details
- New sources added without touching application logic
- Circuit breakers operate per-adapter (existing Resilience4j config)
- Price normalization happens in adapter layer

### Data Normalization

Each scraper adapter transforms source-specific data to the canonical domain model:

```kotlin
// In TCGPlayerAdapter
override fun search(query: String): List<SearchResult> {
    val rawResults = fetchTCGPlayerSearch(query)
    return rawResults.map { raw ->
        SearchResult(
            productId = normalizeProductId(raw.id),  // TCGPlayer ID → canonical
            name = normalizeCardName(raw.name),
            price = normalizePrice(raw.price),      // USD → canonical currency
            condition = normalizeCondition(raw.grade),
            source = Source.TCGPLAYER
        )
    }
}
```

### Source Registry

A source registry tracks available sources and their health:

```kotlin
// SourceRegistry tracks which sources are available
@Service
class SourceRegistry(
    private val scraperClients: List<ScraperPort>
) {
    fun getActiveSources(): List<Source> = scraperClients.map { it.source }
    fun getSource(source: Source): ScraperPort? = clients[source]
}
```

---

## Alert System Architecture

### Pattern: Event-Driven Threshold Alerts

The alert system follows an event-driven pattern:
1. User creates alert rule (threshold, product watchlist)
2. Scheduled job scrapes prices
3. Price changes emit events
4. Alert service evaluates rules
5. Notification dispatchers fan out to channels

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Scraper Job │ → │ PriceEvent │ → │ AlertEval  │ → │ Dispatcher │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                                      │
                              ┌───────────────────────┼───────────────────────┐
                              ▼                       ▼                       ▼
                       ┌──────────┐           ┌──────────┐           ┌──────────┐
                       │  Email   │           │ Webhook  │           │ Push    │
                       │ Sender  │           │ Sender  │           │ Sender  │
                       └──────────┘           └──────────┘           └──────────┘
```

### Domain Model

```kotlin
// Alert rule entity
@Entity
class Alert(
    @Id val id: UUID,
    val userId: UUID,
    val productId: String?,
    val productName: String?,  // For name-based alerts
    val condition: AlertCondition,
    val thresholdPrice: BigDecimal?,
    val thresholdPercent: BigDecimal?,  // e.g., "price drops 10%"
    val notificationChannel: Channel,
    val webhookUrl: String?,
    val enabled: Boolean = true,
    val createdAt: Instant
)

enum class AlertCondition {
    PRICE_DROP_BELOW,      // Absolute threshold
    PRICE_RISE_ABOVE,       // Absolute threshold
    PERCENT_CHANGE_ABOVE,    // Percentage change
    PERCENT_CHANGE_BELOW
}

enum class Channel { EMAIL, WEBHOOK, PUSH }

data class PriceChangedEvent(
    val productId: String,
    val oldPrice: BigDecimal,
    val newPrice: BigDecimal,
    val percentChange: BigDecimal,
    val timestamp: Instant
)
```

### Alert Evaluation Service

```kotlin
@Service
class AlertEvaluationService(
    private val alertRepository: AlertRepository,
    private val notificationService: NotificationService
) {
    @EventListener
    suspend fun onPriceChanged(event: PriceChangedEvent) {
        val matchingAlerts = alertRepository.findMatching(event.productId)
        
        matchingAlerts.forEach { alert ->
            if (evaluateCondition(alert, event)) {
                notificationService.send(alert, event)
            }
        }
    }
    
    private fun evaluateCondition(alert: Alert, event: PriceChangedEvent): Boolean {
        return when (alert.condition) {
            PRICE_DROP_BELOW -> event.newPrice < alert.thresholdPrice
            PRICE_RISE_ABOVE -> event.newPrice > alert.thresholdPrice
            PERCENT_CHANGE_ABOVE -> event.percentChange > alert.thresholdPercent
            PERCENT_CHANGE_BELOW -> event.percentChange < alert.thresholdPercent
        }
    }
}
```

### Notification Dispatcher

```kotlin
interface NotificationSender {
    val type: Channel
    suspend fun send(request: NotificationRequest)
}

@Service
class NotificationService(
    private val senders: Map<Channel, NotificationSender>
) {
    suspend fun send(alert: Alert, event: PriceChangedEvent) {
        val sender = senders[alert.notificationChannel]
        sender?.send(buildRequest(alert, event))
    }
}
```

---

## User Authentication Architecture

### Pattern: JWT with RBAC

Authentication uses JWT tokens with embedded roles. A custom filter validates tokens and populates Spring Security context.

```
┌─────────────────────────────────────────────────────────────┐
│  User Flow                                                │
│  ─────────────────────────────────────────────────────── │
│  1. POST /auth/register → AuthService.register()         │
│  2. POST /auth/login    → AuthService.login()  → JWT    │
│  3. Request + JWT       → JwtFilter.validate()           │
│  4. Spring Security     → hasRole("USER")                 │
└─────────────────────────────────────────────────────────────┘
```

### Domain Model

```kotlin
@Entity
class User(
    @Id val id: UUID,
    val email: String,
    val passwordHash: String,
    val tenantId: String,  // For multi-tenancy
    @ManyToMany val roles: Set<Role>,
    val enabled: Boolean,
    val createdAt: Instant
)

@Entity
class Role(
    @Id val id: UUID,
    val name: String,  // "ADMIN", "USER"
    @ManyToMany val permissions: Set<Permission>
)

@Entity
class Permission(
    @Id val id: UUID,
    val name: String  // "READ_PRODUCTS", "MANAGE_ALERTS", "READ_PRICES"
)
```

### JWT Service

```kotlin
@Service
class JwtService(
    @Value("\$app.jwt.secret}") private val secret: String
) {
    fun generateToken(user: User): String {
        val roles = user.roles.map { it.name }
        val permissions = user.roles.flatMap { it.permissions }.map { it.name }
        
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("roles", roles)
            .claim("permissions", permissions)
            .claim("tenant_id", user.tenantId)
            .issuedAt(Instant.now())
            .expiration(Instant.now().plus(1, ChronoUnit.HOURS))
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .compact()
    }
}
```

### Security Filter

```kotlin
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {
    
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val token = extractToken(request)
        
        if (token != null && jwtService.validate(token)) {
            val userId = jwtService.extractUserId(token)
            val roles = jwtService.extractRoles(token)
            val permissions = jwtService.extractPermissions(token)
            val tenantId = jwtService.extractTenantId(token)
            
            val auth = UsernamePasswordAuthenticationToken(
                userId, null,
                roles.map { SimpleGrantedAuthority("ROLE_$it") } +
                permissions.map { SimpleGrantedAuthority(it) }
            )
            
            SecurityContextHolder.getContext().authentication = auth
            TenantContext.setTenantId(tenantId)  // ThreadLocal for request scope
        }
        
        chain.doFilter(request, response)
    }
}
```

### Spring Security Configuration

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtService: JwtService
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
```

### Tenant Context

```kotlin
object TenantContext {
    private val tenantId = ThreadLocal<String>()
    
    fun getTenantId(): String? = tenantId.get()
    fun setTenantId(id: String) = tenantId.set(id)
    fun clear() = tenantId.remove()
}
```

---

## GraphQL API Architecture

### Pattern: Schema-First with graphql-kotlin

GraphQL integrates via schema generation from Kotlin code. The graphql-kotlin-schema-generator introspects annotated classes to build the schema.

```
Dependencies:
- com.graphql-java-kotlin:graphql-kotlin-spring-server
- com.graphql-java-kotlin:graphql-kotlin-federation (future, if needed)
```

### Schema Definition

```graphql
type Query {
    searchProducts(query: String!, source: Source): [Product!]!
    product(id: String!, source: Source): Product
    myAlerts: [Alert!]!
}

type Mutation {
    createAlert(input: CreateAlertInput!): Alert!
    deleteAlert(id: ID!): Boolean!
    register(input: RegisterInput!): AuthPayload!
    login(input: LoginInput!): AuthPayload!
}

type Product {
    id: ID!
    name: String!
    prices: [PriceInfo!]!
    lowestPrice: Float
    marketPrice: Float
    updatedAt: String!
}

type PriceInfo {
    source: Source!
    condition: String!
    price: Float!
}

type Alert {
    id: ID!
    productName: String
    condition: AlertCondition!
    threshold: Float!
    channel: Channel!
    enabled: Boolean!
}

enum Source { CARDMARKET, TCGPLAYER, EBAY }
enum AlertCondition { PRICE_DROP_BELOW, PRICE_RISE_ABOVE, PERCENT_CHANGE }
enum Channel { EMAIL, WEBHOOK, PUSH }

input CreateAlertInput {
    productName: String
    productId: String
    condition: AlertCondition!
    threshold: Float!
    channel: Channel!
    webhookUrl: String
}
```

### Controller Implementation

```kotlin
@Controller
class GraphQLController(
    private val collectablesService: CollectablesService,
    private val alertService: AlertService
) {
    @Query
    fun searchProducts(@Argument query: String, @Argument source: Source): List<Product> {
        return collectablesService.search(query, source.toDomain())
    }
    
    @Query
    fun product(@Argument id: String, @Argument source: Source): Product? {
        return collectablesService.getProductDetails(id, source.toDomain())
    }
    
    @Query
    fun myAlerts(auth: SpringGraphQLContext): List<Alert> {
        val userId = auth.userId()
        return alertService.getUserAlerts(userId)
    }
    
    @Mutation
    fun createAlert(@Argument input: CreateAlertInput, auth: SpringGraphQLContext): Alert {
        val userId = auth.userId()
        return alertService.createAlert(userId, input)
    }
    
    @Mutation
    fun deleteAlert(@Argument id: String, auth: SpringGraphQLContext): Boolean {
        val userId = auth.userId()
        return alertService.deleteAlert(userId, id)
    }
}
```

### GraphQL Context

```kotlin
class AppGraphQLContext(
    private val securityContext: SecurityContext?
) : GraphQLContext {
    fun userId(): String = securityContext?.userId ?: throw UnauthorizedException()
}

class SecurityContext {
    val userId: String
    val roles: List<String>
    val tenantId: String
}
```

### Dataloader for N+1 Prevention

```kotlin
@DataLoader
class ProductDataLoader(
    private val collectablesService: CollectablesService
) : DataLoader<String, Product>() {
    
    override fun load(keys: List<String>): CompletableFuture<List<Product>> = runAsync {
        keys.map { collectablesService.getProductDetails(it) }
    }
}
```

---

## Data Flow Diagrams

### Search Flow (Existing, Extended)

```
Client Request
    ↓
REST Adapter / GraphQL Controller
    ↓
CollectablesService.search(query, source)
    ↓
┌────────────────────────────────────────┐
│ Check Cache (Caffeine + ETag)              │
│   Hit → Return cached                   │
└────────────────────────────────────────┘
    ↓ (Miss)
SourceRegistry.getSource(source)
    ↓
ScraperAdapter.search()  [CardMarket/TCGPlayer]
    ↓
Normalize to domain model
    ↓
Cache result (PostgreSQL)
    ↓
Response → Client
```

### Alert Flow

```
Scheduled Job (every N minutes)
    ↓
For each source:
    CollectablesService.refreshPrices()
    ↓
For each price change:
    PriceChangedEvent.emit(event)
    ↓
AlertEvaluationService.onPriceChanged()
    ↓
alertRepository.findMatching(productId)
    ↓
For matching alerts:
    NotificationService.send(alert, event)
    ↓
✓ NotificationSender (Email/Webhook/Push)
```

### Authentication Flow

```
POST /auth/register
    ↓
AuthService.register(email, password)
    ↓
PasswordEncoder.hash(password)
    ↓
Create User with default ROLE_USER
    ↓
UserRepository.save()
    ↓
Return user created

POST /auth/login
    ↓
AuthService.login(email, password)
    ↓
Validate credentials
    ↓
JwtService.generateToken(user)
    ↓
Return JWT token

Subsequent Requests:
    ↓
JwtAuthenticationFilter
    ↓
JwtService.validate(token)
    ↓
Set SecurityContext
    ↓
Set TenantContext
    ↓
Route to controller
```

---

## Build Order & Dependencies

### Phase 1: User Authentication (Foundation)

**Before anything else — other features depend on knowing WHO is using the system**

| Order | Component | Depends On | Rationale |
|-------|-----------|-----------|----------|
| 1 | User/Role/Permission entities | — | Domain models first |
| 2 | AuthService.register/login | 1 | Enable user creation |
| 3 | JwtService | 2 | Token generation |
| 4 | JwtAuthenticationFilter | 3 | Request validation |
| 5 | SecurityConfig | 4 | Protect endpoints |
| 6 | /auth/* endpoints | 5 | Public API for auth |

### Phase 2: Multi-Source Support

**Once users exist, enable price data from multiple marketplaces**

| Order | Component | Depends On | Rationale |
|-------|-----------|-----------|----------|
| 1 | ScraperPort interface | — | Already exists |
| 2 | TCGPlayerAdapter | 1 | New source adapter |
| 3 | SourceRegistry | 2 | Orchestrate sources |
| 4 | Multi-source search | 2, Auth | Aggregate results |
| 5 | Price normalization | 2 | Unified model |

### Phase 3: Alert System

**Once multi-source works, enable notifications**

| Order | Component | Depends On | Rationale |
|-------|-----------|-----------|----------|
| 1 | Alert entities | User | Alert ownership |
| 2 | AlertRepository | 1 | Persistence |
| 3 | AlertEvaluationService | 2, Multi-source | Trigger logic |
| 4 | NotificationSender interface | 2 | Channel abstraction |
| 5 | Email/Webhook adapters | 4 | Delivery |
| 6 | Scheduled price job | Multi-source, 3 | Pull prices |
| 7 | /alerts/* endpoints | Auth(Phase1), 2 | User API |

### Phase 4: GraphQL API (Optional)

**Parallel to REST — choose based on frontend needs**

| Order | Component | Depends On | Rationale |
|-------|-----------|-----------|----------|
| 1 | graphql-kotlin dependency | — | Library addition |
| 2 | GraphQL schema | Domain | Define types |
| 3 | GraphQLController | All services | Bind queries/mutations |
| 4 | GraphQL context | Auth(Phase1) | User context |
| 5 | /graphql endpoint | 4 | Alternative API |

---

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-1k users | Single instance, simple scheduling is fine |
| 1k-100k users | Add Redis for distributed alert evaluation, consider DataLoader |
| 100k+ users | GraphQL federation for schema splitting, Kafka for event streaming |

### Scaling Priorities

1. **First bottleneck:** Scraping rate limits — solve with source rotation and caching
2. **Second bottleneck:** Alert evaluation — solve with scheduled batches, not real-time events
3. **Third bottleneck:** Notification delivery — solve with async queue (RabbitMQ or Kafka)

---

## Anti-Patterns

### Anti-Pattern 1: Embedding Source Logic in Domain

**What people do:** Put CardMarket-specific logic in CollectablesService
**Why it's wrong:** Violates hexagonal isolation, makes adding sources painful
**Do this instead:** Normalize in adapter layer, domain only sees unified model

### Anti-Pattern 2: Synchronous Alert Evaluation

**What people do:** Evaluate alerts immediately on every price fetch
**Why it's wrong:** Blocks scraping, duplicates evaluations, no batching
**Do this instead:** Batch evaluations, emit events, handle asynchronously

### Anti-Pattern 3: Storing Raw Prices Without Normalization

**What people do:** Store prices in source-native format with separate columns per source
**Why it's wrong:** Query logic becomes source-dependent, hard to aggregate
**Do this instead:** Normalize to canonical currency/condition in adapter

### Anti-Pattern 4: JWT Without Expiration Refresh

**What people do:** Long-lived JWTs (7+ days)
**Why it's wrong:** No revocation capability, security risk
**Do this instead:** Short access tokens (15-60 min), refresh tokens for re-auth

---

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|-------------------|-------|
| CardMarket.eu | Playwright + Jsoup | Existing |
| TCGPlayer.com | REST API + scraper | New adapter needed |
| SendGrid/SMTP | Spring Mail | Email notifications |
| Discord/Slack | Webhook | Alert notifications |
| Keycloak | OAuth2 resource server | Future: external identity |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Adapter ↔ Application | Port interface | Adapters implement ports |
| Application ↔ Domain | Direct call | Services use domain models |
| Domain ↔ Outbound | Port interface | Domain defines contracts |
| REST ↔ GraphQL | Shared services | Both use same application layer |

---

## Sources

- Multi-source patterns: TCG Price Lookup SDK architecture, Databay data pipeline research
- Alert patterns: Price alert systems (website search articles), event-driven notification architecture
- Authentication: Spring Security documentation, JWT with RBAC patterns, multi-tenant patterns
- GraphQL: graphql-kotlin official documentation, Spring Boot GraphQL integration

---

*Architecture research for TCGWatcher-Backend*
*Researched: 2026-04-18*