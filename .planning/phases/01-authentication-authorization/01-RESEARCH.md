# Phase 1: Authentication & Authorization - Research

**Researched:** 2026-04-18
**Domain:** JWT authentication for Spring Boot Kotlin with role-based access control
**Confidence:** HIGH

## Summary

Phase 1 implements user registration, JWT-based authentication, and role-based authorization. The project has no existing authentication system - this is a greenfield implementation. Based on research, the recommended stack uses JJWT 0.12.7 for token handling, Spring Security 6+ (included with Spring Boot 4.0.2), and BCrypt for password hashing.

**Primary recommendation:** Use JJWT 0.12.7 with Spring Security 6 filter chain, storing users and refresh tokens in PostgreSQL with Liquibase migrations.

## Standard Stack

### Core Dependencies (add to build.gradle.kts)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| spring-boot-starter-security | 4.0.2 | Authentication framework | Comes with Spring Boot, integrates with WebMVC |
| io.jsonwebtoken:jjwt-api | 0.12.7 | JWT token operations | Most used JWT library (11K stars), Java 8+ support |
| io.jsonwebtoken:jjwt-impl | 0.12.7 | JWT signing/validation | Runtime dependency |
| io.jsonwebtoken:jjwt-jackson | 0.12.7 | JSON serialization | Runtime dependency |

**Installation:**
```kotlin
// Add to build.gradle.kts dependencies block
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("io.jsonwebtoken:jjwt-api:0.12.7")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.7")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.7")
```

### Database Schema (Liquibase migrations required)

| Table | Columns | Purpose |
|-------|---------|---------|
| watcher.users | id (BIGINT PK), email (VARCHAR 255 UNIQUE), password_hash (VARCHAR 255), role (VARCHAR 20), created_at, updated_at | User accounts |
| watcher.refresh_tokens | id (BIGINT PK), user_id (BIGINT FK), token_hash (VARCHAR 255), expires_at (TIMESTAMP), revoked (BOOLEAN) | Refresh token storage |

### Domain Models (src/main/kotlin/io/github/havonte1/tcgwatcher/backend/domain/model/)

| File | Purpose |
|------|---------|
| User.kt | User entity with id, email, passwordHash, role, timestamps |
| Role.kt | Enum: USER, ADMIN |
| RefreshToken.kt | Refresh token entity with user reference |

### Application Services (src/main/kotlin/io/github/havonte1/tcgwatcher/backend/application/)

| Service | Purpose |
|---------|---------|
| UserService.kt | User CRUD, password management |
| AuthenticationService.kt | Registration, login, token generation |
| JwtTokenService.kt | JWT access token and refresh token operations |

### Security Components (src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/security/)

| Component | Purpose |
|-----------|---------|
| JwtAuthenticationFilter.kt | Extract JWT from request, authenticate user |
| JwtUtil.kt | Token generation and validation |
| SecurityConfig.kt | Security filter chain configuration |

### REST Controllers (src/main/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/)

| Controller | Endpoints |
|------------|----------|
| AuthController.kt | POST /auth/register, POST /auth/login, POST /auth/refresh, POST /auth/logout |
| UserController.kt | GET /users/me (secured), admin endpoints |

## Architecture Patterns

### Hexagonal Architecture for Auth

```
src/main/kotlin/io/github/havonte1/tcgwatcher/backend/
├── domain/
│   ├── model/           # User, Role, RefreshToken entities
│   └── port/
│       └── in/          # UserService interface (ports stay in domain)
├── application/         # Use cases - AuthenticationService, UserService
├── adapter/
│   ├── inbound/
│   │   └── rest/      # AuthController, UserController
│   └── out/
│       └── persistence/ # UserJpaRepository, RefreshTokenJpaRepository
└── config/            # SecurityConfig, JwtUtil
```

### JWT Token Flow (per requirements)

1. **Registration (AUTH-01):** User submits email/password → BCrypt hash password → Store in users table → Return user record
2. **Login (AUTH-02):** User submits credentials → Verify against password_hash → Generate JWT access token (15-60 min expiry) → Return tokens
3. **Refresh (AUTH-03):** User submits refresh token → Validate → Generate new access token + new refresh token (rotation) → Return tokens
4. **Logout (AUTH-04):** User submits refresh token → Mark revoked in refresh_tokens table → Return success
5. **Authorization (AUTH-06/07):** JWT filter validates token → Extract roles → Apply security rules

### Pattern: JWT Access Token

```kotlin
// Source: https://github.com/jwtk/jjwt README
@Service
class JwtTokenService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-expiration-ms}") private val accessExpiration: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(user: User): String {
        val now = Date()
        val expiry = Date(now.time + accessExpiration)
        return Jwts.builder()
            .subject(user.email)
            .claim("role", user.role.name)
            .claim("userId", user.id)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        }
    }
}
```

### Pattern: Spring Security Configuration

```kotlin
// Source: Spring Security 6 documentation
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val UserDetailsService: CustomUserDetailsService
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }  // Stateless API
            .session { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }
}
```

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|------------|------------|-----|
| Password hashing | Custom hash comparison | BCrypt (Spring Security) | Handles salting, work factor, timing attack protection |
| JWT signing | Custom HMAC implementation | JJWT library | Battle-tested, prevents signature vulnerabilities |
| Token storage | In-memory refresh tokens | PostgreSQL table | Required for logout/revocation, session invalidation |
| Role checking | Custom string comparison | Spring Security hasRole() | Integrates with filter chain |

## Common Pitfalls

### Pitfall 1: JWT Secret Management
**What goes wrong:** Hardcoding secret in application.yml, secret in git
**Why it happens:** Developer convenience, not understanding production security
**How to avoid:** Load JWT_SECRET from environment variable with default for dev only
**Warning signs:** Secret visible in application.yml or git history

### Pitfall 2: Refresh Token Not Revoked on Logout
**What goes wrong:** Logout doesn't invalidate refresh token, user remains logged in
**Why it happens:** Not storing refresh tokens in database
**How to avoid:** Store token hash in refresh_tokens table, check revoked flag on refresh
**Warning signs:** No refresh_tokens table in schema

### Pitfall 3: Password Stored in Plaintext
**What goes wrong:** Storing password as-is or with simple hash
**Why it happens:** Not understanding BCrypt required for security
**How to avoid:** Use BCryptPasswordEncoder, never store plain password
**Warning signs:** password_hash column shows plaintext or MD5

### Pitfall 4: Long-lived Access Tokens
**What goes wrong:** Access token valid for days/weeks
**Why it happens:** Not implementing refresh token flow correctly
**How to avoid:** Access tokens 15-60 minutes, refresh tokens 7-30 days
**Warning signs:** jwt.expiration longer than 3600000 (1 hour)

### Pitfall 5: Missing Role in JWT Claims
**What goes wrong:** Role not included in JWT, requiring DB lookup per request
**Why it happens:** Not understanding stateless authentication
**How to avoid:** Include role claim in JWT access token
**Warning signs:** Database call in authentication filter

## Code Examples

### Registration Endpoint Pattern

```kotlin
// Source: Baeldung Spring Security JWT with Kotlin
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authenticationService: AuthenticationService
) {
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val user = authenticationService.register(request.email, request.password)
        val tokens = authenticationService.generateTokens(user)
        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val user = authenticationService.authenticate(request.email, request.password)
        val tokens = authenticationService.generateTokens(user)
        return ResponseEntity.ok(tokens)
    }
}
```

### User Entity with BCrypt

```kotlin
// Source: Standard Spring Security practice
@Entity
@Table(name = "users", schema = "watcher")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, unique = true)
    val email: String,
    
    @Column(nullable = false)
    var passwordHash: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class Role {
    USER, ADMIN
}
```

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 (via spring-boot-starter-test) |
| Config file | None required (Spring Boot auto-config) |
| Quick run (unit tests) | `./gradlew test --exclude-tags integration` |
| Full suite | `./gradlew check` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | User can register with email/password | unit/integration | `test --tests "*AuthController*register*"` | ❌ Wave 0 |
| AUTH-02 | User can login and receive JWT | unit/integration | `test --tests "*AuthController*login*"` | ❌ Wave 0 |
| AUTH-03 | User can refresh token | unit/integration | `test --tests "*AuthController*refresh*"` | ❌ Wave 0 |
| AUTH-04 | User can logout | unit/integration | `test --tests "*AuthController*logout*"` | ❌ Wave 0 |
| AUTH-05 | Roles assigned and persisted | unit | `test --tests "*UserService*role*"` | ❌ Wave 0 |
| AUTH-06 | Secured endpoints reject invalid JWT | unit | `test --tests "*SecurityConfig*"` | ❌ Wave 0 |
| AUTH-07 | Admin-only endpoints reject non-admin | unit | `test --tests "*AdminEndpoint*"` | ❌ Wave 0 |

### Wave 0 Gaps

- [ ] `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/AuthControllerTest.kt` — AUTH-01, AUTH-02, AUTH-03, AUTH-04
- [ ] `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/rest/UserControllerTest.kt` — AUTH-06, AUTH-07
- [ ] `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/application/AuthenticationServiceTest.kt` — service layer tests
- [ ] `src/test/kotlin/io/github/havonte1/tcgwatcher/backend/adapter/inbound/security/JwtAuthenticationFilterTest.kt` — filter tests
- [ ] `src/test/resources/schema.sql` or testcontainers setup for integration tests

### Integration Tests Required

- Auth controller tests need PostgreSQL via Testcontainers (already in project)
- Use `@SpringBootTest` with `@AutoConfigureMockMvc`

## Open Questions

1. **Email Confirmation Flow**
   - What we know: Requirement states "receive confirmation" but spec is incomplete
   - What's unclear: Email confirmation link? Auto-login after registration?
   - Recommendation: Implement registration → immediate login for v1 (keep confirmation as future enhancement)

2. **Refresh Token Rotation**
   - What we know: Best practice is rotation (new refresh on each use)
   - What's unclear: Is rotation required or optional?
   - Recommendation: Implement rotation as per 2025 best practices

3. **API Endpoints Path**
   - What we know: API-03 specifies /auth/register, /auth/login, /auth/refresh
   - What's unclear: User management endpoints path (/users?)
   - Recommendation: Use /users for user-specific endpoints, /admin for admin-only

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| PostgreSQL | Data layer | ✓ | Via Testcontainers | — |
| Spring Security | Auth framework | ✓ (4.0.2) | — |
| JJWT 0.12.7 | JWT handling | ✗ (new dep) | Add to build.gradle.kts |

**Missing dependencies with no fallback:**
- JJWT libraries — must add to build.gradle.kts

## Sources

### Primary (HIGH confidence)
- [JJWT GitHub](https://github.com/jwtk/jjwt) - Token library documentation, v0.12.7 release Aug 2025
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/) - Spring Security 6 configuration
- [Baeldung: Spring Security JWT with Kotlin](https://www.baeldung.com/kotlin/spring-security-jwt) - Kotlin-specific patterns

### Secondary (MEDIUM confidence)
- [OneUptime: JWT Spring Security 2025](https://oneuptime.com/blog/post/2026-01-25-jwt-authentication-spring-security/view) - 2025 best practices
- [Java Code Geeks: JWT Authentication 2025](https://www.javacodegeeks.com/2025/05/how-to-secure-rest-apis-with-spring-security-and-jwt-2025-edition.html) - Refresh token flow

### Tertiary (LOW confidence)
- [Medium: JWT Authentication 2025](https://medium.com/@pendemmukulsai/implementing-jwt-authentication-in-spring-boot-2025-03a565333814) - Tutorial reference

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — JJWT 0.12.7 confirmed via Maven Central (Aug 2025)
- Architecture: HIGH — Standard Spring Security 6 patterns well-documented
- Pitfalls: HIGH — Common issues well-understood

**Research date:** 2026-04-18
**Valid until:** 2026-05-18 (30 days for stable library versions)

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | User can register with email and password | BCrypt for password hashing, Liquibase schema for users table |
| AUTH-02 | User can log in and receive JWT access token | JWT service with 15-60 min expiry configuration |
| AUTH-03 | User can request refresh token to extend session | Refresh token table + rotation pattern |
| AUTH-04 | User can log out (invalidate refresh token) | Refresh token revocation in DB |
| AUTH-05 | User roles can be assigned (USER, ADMIN) | Role enum + JWT claims + Spring Security hasRole |
| AUTH-06 | Secured endpoints enforce JWT authentication | JwtAuthenticationFilter + security chain config |
| AUTH-07 | Role-based authorization restricts admin-only actions | requestMatchers("/admin/**").hasRole("ADMIN") |
| API-03 | REST API provides auth endpoints | /auth/register, /auth/login, /auth/refresh, /auth/logout in AuthController |