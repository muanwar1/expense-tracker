# Expense Tracker — Project Review Snapshot

> Generated: 2026-09-08
> Last updated: 2026-09-08 (reflected fix round — see "Change Summary" below)
> Base package: `com.speed_anwer.expensetracker`

---

## Change Summary (fix round, 2026-09-08)

The following issues were addressed in the code (verified: project compiles, **50/50 tests pass**):

1. **CORS tightened** (`SecurityConfig`) — `setAllowedHeaders("*")` replaced with explicit whitelist: `Authorization`, `Content-Type`, `Accept`.
2. **`getStatistics()` guarded** (`ExpenseServiceImpl`) — empty result from `aggregateTotals()` now returns a zeroed `ExpenseStatistics` instead of risking `IndexOutOfBoundsException`.
3. **`@Transactional` added** to all write methods (`create`/`update`/`delete`) in `CategoryServiceImpl` and `ExpenseServiceImpl`.
4. **`ddl-auto` hardened** (`application.properties`) — `update` → `validate` for the production config.
5. **`UserMapper.toentity()` renamed** → `toEntity()` (mapper, impl, and test).
6. **Removed dead code** — `CategoryRepository.existsByNameAndUser()`.
7. **`/api/expenses/statistics` route ordering** — verified **already correct** (declared before `/{expenseId}`); no change was needed.
8. **`aggregateTotals()` projection** — a record-based JPQL constructor surfaced a Hibernate limitation (`SemanticException: Missing constructor for type` with `COALESCE`/`COUNT`); reverted to `List<Object[]>` and kept the empty-list guard instead.

---

## 1. Project Overview

| Aspect | Detail |
|---|---|
| **Description** | REST API for personal expense tracking with per-user expenses, categories, and statistics |
| **Framework** | Spring Boot 4.1.0 |
| **Java Version** | 25 |
| **Build System** | Maven (Spring Boot parent POM) |
| **Database** | MySQL (production), H2 in-memory (tests) |
| **Security** | Stateless JWT with access + refresh tokens |
| **Key Libraries** | Lombok 1.18.46, MapStruct 1.6.3, JJWT 0.12.6, Spring Security, Spring Data JPA, Bean Validation |

---

## 2. Project Structure

```
src/main/java/com/speed_anwer/expensetracker/
├── ExpenseTrackerApplication.java          # @SpringBootApplication + @EnableScheduling
├── config/
│   └── SecurityConfig.java                 # Security filter chain, CORS, password encoder
├── controller/
│   ├── AuthController.java                 # /api/auth/** — register, login, refresh, logout
│   ├── CategoryController.java             # /api/categories/** — CRUD
│   ├── ExpenseController.java              # /api/expenses/** — CRUD + statistics
│   └── UserController.java                 # /api/users/me
├── dto/
│   ├── request/
│   │   ├── CategoryRequest.java
│   │   ├── ExpenseRequest.java
│   │   ├── LoginRequest.java
│   │   ├── LogoutRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   └── RegisterRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── CategoryResponse.java
│       ├── CategorySummary.java            # record
│       ├── ErrorResponse.java
│       ├── ExpenseResponse.java
│       ├── ExpenseStatistics.java
│       ├── PagedResponse.java              # generic
│       └── UserResponse.java
├── entity/
│   ├── Category.java
│   ├── Expense.java
│   ├── RevokedToken.java
│   └── User.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── InvalidTokenException.java
│   ├── RateLimitExceededException.java
│   ├── ResourceConflictException.java
│   └── ResourceNotFoundException.java
├── mapper/
│   ├── CategoryMapper.java
│   ├── ExpenseMapper.java
│   └── UserMapper.java
├── repository/
│   ├── CategoryRepository.java
│   ├── ExpenseRepository.java
│   ├── RevokedTokenRepository.java
│   └── UserRepository.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   ├── LoginRateLimiter.java
│   ├── RestAccessDeniedHandler.java
│   ├── RestAuthenticationEntryPoint.java
│   ├── TokenRevocationService.java
│   └── UserPrincipal.java
├── service/
│   ├── impl/
│   │   ├── AuthServiceImpl.java
│   │   ├── CategoryServiceImpl.java
│   │   ├── ExpenseServiceImpl.java
│   │   └── UserServiceImpl.java
│   └── interfaces/
│       ├── AuthService.java
│       ├── CategoryService.java
│       ├── ExpenseService.java
│       └── UserService.java
└── util/
    └── PageableUtils.java

src/main/resources/
└── application.properties

src/test/java/com/speed_anwer/expensetracker/
├── ExpenseTrackerApplicationTests.java
├── integration/
│   └── AuthFlowIntegrationTest.java
├── repository/
│   └── ExpenseRepositoryTest.java
├── security/
│   ├── JwtServiceTest.java
│   └── LoginRateLimiterTest.java
└── service/impl/
    ├── AuthServiceImplTest.java
    ├── CategoryServiceImplTest.java
    ├── ExpenseServiceImplTest.java
    └── UserServiceImplTest.java

src/test/resources/
└── application.properties
```

**File counts:** 53 production Java files, 9 test files, 1 production config, 1 test config.

---

## 3. Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST controller layer, embedded Tomcat |
| `spring-boot-starter-data-jpa` | Hibernate ORM, Spring Data repositories |
| `spring-boot-starter-security` | Authentication/authorization framework |
| `spring-boot-starter-validation` | Bean Validation (`@NotBlank`, `@Email`, `@Size`, etc.) |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.12.6) | JWT token creation, parsing, and signing |
| `mapstruct` (1.6.3) | Compile-time DTO ↔ entity mapping |
| `lombok` (1.18.46) | Boilerplate reduction (`@Data`, `@AllArgsConstructor`) |
| `mysql-connector-j` | MySQL JDBC driver (runtime) |
| `h2` | In-memory database for tests |
| `spring-boot-starter-test` | JUnit, Mockito, AssertJ |
| `spring-boot-starter-data-jpa-test` | `@DataJpaTest` support |
| `spring-boot-resttestclient` | `RestClient` for integration tests |

---

## 4. Configuration

### `src/main/resources/application.properties`

| Property | Value (default) | Purpose |
|---|---|---|
| `spring.application.name` | `expense-tracker` | Application name |
| `server.port` | `7070` | HTTP listen port |
| `spring.jpa.open-in-view` | `false` | Disable Open EntityManager in View |
| `spring.datasource.url` | `${DB_URL:jdbc:mysql://localhost:3306/expense_tracker}` | Database URL |
| `spring.datasource.username` | `${DB_USERNAME:root}` | Database user |
| `spring.datasource.password` | `${DB_PASSWORD:1234}` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Schema validation (production config) |
| `spring.jpa.show-sql` | `true` | Log SQL statements |
| `spring.jpa.properties.hibernate.format_sql` | `true` | Pretty-print SQL |
| `jwt.secret` | `${JWT_SECRET:dev-only-secret-key-change-me-must-be-at-least-32-chars}` | HMAC signing key |
| `jwt.access-expiration-ms` | `900000` (15 min) | Access token lifetime |
| `jwt.refresh-expiration-ms` | `604800000` (7 days) | Refresh token lifetime |
| `app.cors.allowed-origins` | `${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}` | CORS origins |
| `app.login.max-attempts` | `5` | Max failed logins before lockout |
| `app.login.attempt-window-ms` | `600000` (10 min) | Window for counting failures |
| `app.login.lock-duration-ms` | `900000` (15 min) | Lockout duration |

> **Note:** All secrets support environment variable overrides. Default values are development-only placeholders. The JWT default key literally contains `dev-only-secret-key-change-me` — it MUST be overridden in production.

> **Recent change (2026-09-08):** `spring.jpa.hibernate.ddl-auto` changed from `update` to `validate` for the main production config. The test config keeps `create-drop`.

### `src/test/resources/application.properties`

Same structure, uses H2 in-memory DB, `create-drop` DDL, and a test-only JWT secret.

---

## 5. Entities

### User

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | `@Id`, `IDENTITY` generation |
| `fullName` | `String` | `@NotBlank` |
| `email` | `String` | `@Email`, `@Column(nullable=false, unique=true)` |
| `password` | `String` | `@NotBlank` |
| `createdAt` | `LocalDateTime` | Set manually in service layer |

- Table: `users`
- No relationships defined on this side (categories and expenses reference User via `@ManyToOne`)

### Expense

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | `@Id`, `IDENTITY` generation |
| `title` | `String` | `@NotBlank` |
| `amount` | `BigDecimal` | `@NotNull`, `@DecimalMin("0.01")` |
| `expenseDate` | `LocalDate` | `@NotNull`, `@PastOrPresent` |
| `createdAt` | `LocalDateTime` | Set manually in service layer |
| `user` | `User` | `@ManyToOne(LAZY)`, `@JoinColumn(name="user_id", nullable=false)` |
| `category` | `Category` | `@ManyToOne(LAZY)`, `@JoinColumn(name="category_id", nullable=false)` |

- Table: `expenses`

### Category

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | `@Id`, `IDENTITY` generation |
| `name` | `String` | `@NotBlank`, `@Column(nullable=false)` |
| `user` | `User` | `@ManyToOne(LAZY)`, `@JoinColumn(name="user_id", nullable=false)` |

- Table: `categories`
- Uniqueness enforced at service level via `CategoryRepository.findByNameAndUser`

### RevokedToken

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | `@Id`, `IDENTITY` generation |
| `tokenId` | `String` | `@Column(name="token_id", nullable=false, unique=true)` |
| `expiresAt` | `LocalDateTime` | `@Column(name="expires_at", nullable=false)` |

- Table: `revoked_tokens`
- Cleaned up hourly via `@Scheduled(cron = "0 0 * * * *")`

---

## 6. DTOs

### Request DTOs

| Class | Fields | Validation | Purpose |
|---|---|---|---|
| `RegisterRequest` | `fullName`, `email`, `password` | `@NotBlank`, `@Email`, `@Size(min=8, max=20)` | User registration |
| `LoginRequest` | `email`, `password` | `@Email`, `@NotBlank` | User login |
| `LogoutRequest` | `refreshToken` | *(none)* | Logout (body optional) |
| `RefreshTokenRequest` | `refreshToken` | `@NotBlank` | Token refresh |
| `ExpenseRequest` | `title`, `amount`, `expenseDate`, `categoryId` | `@NotBlank`, `@NotNull`, `@DecimalMin("0.01")`, `@PastOrPresent` | Create/update expense |
| `CategoryRequest` | `name` | `@NotBlank` | Create/update category |

### Response DTOs

| Class | Fields | Purpose |
|---|---|---|
| `AuthResponse` | `accessToken`, `refreshToken`, `tokenType`, `user` (UserResponse) | Auth endpoint responses |
| `UserResponse` | `id`, `fullName`, `email`, `createdAt` | User data (never exposes password) |
| `ExpenseResponse` | `id`, `title`, `amount`, `expenseDate`, `createdAt`, `categoryId` | Expense data |
| `CategoryResponse` | `id`, `name`, `userId` | Category data |
| `CategorySummary` | `categoryId`, `categoryName`, `totalAmount`, `expenseCount` | record — used in statistics |
| `ExpenseStatistics` | `totalAmount`, `averageAmount`, `expenseCount`, `byCategory` (List\<CategorySummary\>) | Aggregated stats |
| `PagedResponse<T>` | `content`, `page`, `size`, `totalElements`, `totalPages`, `last` | Generic pagination wrapper |
| `ErrorResponse` | `status`, `message`, `errors` (Map), `timeStamp` | Error response body |

---

## 7. Controllers

### AuthController — `/api/auth`

| Endpoint | Method | Auth Required | Request Body | Response | Status |
|---|---|---|---|---|---|
| `/api/auth/register` | POST | No | `RegisterRequest` | `AuthResponse` | 201 |
| `/api/auth/login` | POST | No | `LoginRequest` | `AuthResponse` | 200 |
| `/api/auth/refresh` | POST | No | `RefreshTokenRequest` | `AuthResponse` | 200 |
| `/api/auth/logout` | POST | Yes* | `LogoutRequest` (optional) | *(empty)* | 204 |

*Logout extracts Bearer token from `Authorization` header manually via `resolveBearerToken()`.

### CategoryController — `/api/categories`

All endpoints require authentication (`@AuthenticationPrincipal UserPrincipal`).

| Endpoint | Method | Request | Response | Status |
|---|---|---|---|---|
| `/api/categories` | POST | `CategoryRequest` | `CategoryResponse` | 201 |
| `/api/categories` | GET | query: `page`, `size`, `sortBy`, `sortDir` | `PagedResponse<CategoryResponse>` | 200 |
| `/api/categories/{categoryId}` | GET | — | `CategoryResponse` | 200 |
| `/api/categories/{categoryId}` | PUT | `CategoryRequest` | `CategoryResponse` | 200 |
| `/api/categories/{categoryId}` | DELETE | — | *(empty)* | 204 |

Allowed sort fields: `id`, `name`.

### ExpenseController — `/api/expenses`

All endpoints require authentication.

| Endpoint | Method | Request | Response | Status |
|---|---|---|---|---|
| `/api/expenses` | POST | `ExpenseRequest` | `ExpenseResponse` | 201 |
| `/api/expenses` | GET | query: `categoryId`, `startDate`, `endDate`, `page`, `size`, `sortBy`, `sortDir` | `PagedResponse<ExpenseResponse>` | 200 |
| `/api/expenses/statistics` | GET | query: `startDate`, `endDate` | `ExpenseStatistics` | 200 |
| `/api/expenses/{expenseId}` | GET | — | `ExpenseResponse` | 200 |
| `/api/expenses/{expenseId}` | PUT | `ExpenseRequest` | `ExpenseResponse` | 200 |
| `/api/expenses/{expenseId}` | DELETE | — | *(empty)* | 204 |

Allowed sort fields: `id`, `title`, `amount`, `expenseDate`, `createdAt`.

> **Note:** `GET /api/expenses/statistics` is declared before `GET /api/expenses/{expenseId}` (verified 2026-09-08), so Spring correctly resolves `/statistics` as the dedicated endpoint rather than a path variable. Routing order is correct.

### UserController — `/api/users`

| Endpoint | Method | Auth Required | Response |
|---|---|---|---|
| `/api/users/me` | GET | Yes | `UserResponse` |

---

## 8. Services

### AuthService (interface) / AuthServiceImpl

**Dependencies:** UserService, UserRepository, UserMapper, JwtService, AuthenticationManager, TokenRevocationService, LoginRateLimiter

| Method | Responsibility |
|---|---|
| `register(RegisterRequest)` | Delegates to `userService.register()`, then generates token pair |
| `login(LoginRequest, clientIp)` | Rate-limit check → `AuthenticationManager.authenticate()` → on failure records failure + rethrows → on success records success → generates token pair |
| `refresh(String)` | Parse + validate token → verify it is a refresh type → check not revoked → look up user → revoke old refresh (rotation) → generate new pair |
| `logout(String accessToken, String refreshToken)` | Quietly revokes both tokens (silently catches parse failures) |

**Key logic:**
- Access and refresh tokens are always generated together as a pair.
- Refresh tokens are rotated — the used refresh token is revoked upon use.
- Login rate limiting uses composite key: `clientIp:email`.

### UserService (interface) / UserServiceImpl

**Dependencies:** UserRepository, UserMapper, PasswordEncoder

| Method | Responsibility |
|---|---|
| `register(RegisterRequest)` | Check email uniqueness → map to entity → encode password → save → return UserResponse |
| `getUserById(Long)` | Fetch by ID, throw 404 if missing |
| `getUserByEmail(String)` | Fetch by email, throw 404 if missing |

### CategoryService (interface) / CategoryServiceImpl

**Dependencies:** CategoryRepository, UserRepository, CategoryMapper

`createCategory`, `updateCategory`, and `deleteCategory` are annotated `@Transactional` (added 2026-09-08) so multi-step operations can't leave partial writes on failure.

| Method | Responsibility |
|---|---|
| `createCategory(CategoryRequest, userId)` | Verify user exists → check name uniqueness per user → save |
| `getAllCategories(userId, Pageable)` | Fetch paginated list scoped to user |
| `getCategoryById(categoryId, userId)` | Fetch by ID scoped to user → 404 if missing |
| `updateCategory(categoryId, userId, CategoryRequest)` | Verify user + category ownership → check name uniqueness (excluding self) → save |
| `deleteCategory(categoryId, userId)` | Verify user + category ownership → delete |

### ExpenseService (interface) / ExpenseServiceImpl

**Dependencies:** UserRepository, ExpenseRepository, CategoryRepository, ExpenseMapper

`createExpense`, `updateExpense`, and `deleteExpense` are annotated `@Transactional` (added 2026-09-08) so multi-step operations can't leave partial writes on failure. Note: a `Projection`/DTO for `aggregateTotals()` was attempted but reverted — Hibernate's `Missing constructor for type` semantic exception prevents a record-constructor JPQL projection with `COALESCE`/`COUNT`; the `List<Object[]>` return was kept. An empty-list guard was added in `getStatistics()` instead.

| Method | Responsibility |
|---|---|
| `createExpense(ExpenseRequest, userId)` | Verify user + category ownership → map → set `createdAt` → save |
| `getAllExpenses(userId, categoryId, startDate, endDate, Pageable)` | Paginated query with dynamic filters, scoped to user |
| `getStatistics(userId, startDate, endDate)` | Aggregate totals (sum, count, avg) + per-category breakdown (guarded against empty result, returns zeroed stats) |
| `getExpenseById(expenseId, userId)` | Fetch by ID scoped to user → 404 if missing |
| `updateExpense(expenseId, userId, ExpenseRequest)` | Verify user + expense + category ownership → update fields → save |
| `deleteExpense(expenseId, userId)` | Verify user + expense ownership → delete |

---

## 9. Repositories

### UserRepository

| Method | Used For |
|---|---|
| `findByEmail(String)` | Login, registration uniqueness check, token refresh user lookup |

### ExpenseRepository

| Method | Used For |
|---|---|
| `findByUserWithFilters(user, categoryId, startDate, endDate, pageable)` | Paginated expense listing with optional filters |
| `summarizeByCategory(user, startDate, endDate)` | Category breakdown statistics (returns `CategorySummary` via JPQL constructor) |
| `aggregateTotals(user, startDate, endDate)` | Sum, count, avg aggregates (returns `Object[]`) |
| `findByIdAndUser(id, user)` | Single expense fetch with ownership verification |

### CategoryRepository

| Method | Used For |
|---|---|
| `findByUser(user, pageable)` | Paginated category listing |
| `findByNameAndUser(name, user)` | Duplicate name check on create |
| `findByIdAndUser(id, user)` | Single category fetch with ownership verification |
| `findByNameAndUserAndIdNot(name, user, id)` | Duplicate name check on update (excluding self) |

### RevokedTokenRepository

| Method | Used For |
|---|---|
| `existsByTokenId(tokenId)` | Check if a token has been revoked |
| `deleteAllExpiredBefore(LocalDateTime)` | Hourly cleanup of expired revoked tokens |

---

## 10. Mappers

All mappers use `@Mapper(componentModel = "spring")` and `ReportingPolicy.IGNORE`.

### UserMapper

| Method | Direction | Notes |
|---|---|---|
| `toEntity(RegisterRequest)` → `User` | Request → Entity | |
| `toResponse(User)` → `UserResponse` | Entity → Response | Password field ignored (not present in UserResponse) |

### CategoryMapper

| Method | Direction | Notes |
|---|---|---|
| `toEntity(CategoryRequest)` → `Category` | Request → Entity | |
| `toResponse(Category)` → `CategoryResponse` | Entity → Response | `@Mapping(source="user.id", target="userId")` |
| `toResponseList(List<Category>)` → `List<CategoryResponse>` | List mapping | |

### ExpenseMapper

| Method | Direction | Notes |
|---|---|---|
| `toEntity(ExpenseRequest)` → `Expense` | Request → Entity | `@Mapping(target="category", ignore=true)` — category set manually in service |
| `toResponse(Expense)` → `ExpenseResponse` | Entity → Response | `@Mapping(source="category.id", target="categoryId")` |
| `toResponseList(List<Expense>)` → `List<ExpenseResponse>` | List mapping | |

---

## 11. Exception Handling

### Custom Exceptions

| Exception | Extends | Usage |
|---|---|---|
| `ResourceNotFoundException` | `RuntimeException` | Entity not found (404) |
| `ResourceConflictException` | `RuntimeException` | Duplicate data (409) |
| `InvalidTokenException` | `RuntimeException` | Invalid/revoked/expired JWT (401) |
| `RateLimitExceededException` | `RuntimeException` | Too many login attempts (429) |

### GlobalExceptionHandler (`@RestControllerAdvice`)

| Exception Handled | HTTP Status | Response |
|---|---|---|
| `ResourceNotFoundException` | 404 NOT_FOUND | `ErrorResponse` with message |
| `ResourceConflictException` | 409 CONFLICT | `ErrorResponse` with message |
| `BadCredentialsException` | 401 UNAUTHORIZED | `ErrorResponse` — always returns `"Invalid email or password"` (no leak) |
| `InvalidTokenException` | 401 UNAUTHORIZED | `ErrorResponse` with message |
| `RateLimitExceededException` | 429 TOO_MANY_REQUESTS | `ErrorResponse` with remaining lockout time |
| `MethodArgumentTypeMismatchException` | 400 BAD_REQUEST | `ErrorResponse` with invalid param detail |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | `ErrorResponse` with field-level error map |

### Error Response Structure

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": { "email": "Invalid email format" },
  "timeStamp": "2026-09-08T12:00:00"
}
```

### Custom Entry Point / Access Denied Handler

- `RestAuthenticationEntryPoint` → returns 401 JSON for unauthenticated access to protected resources
- `RestAccessDeniedHandler` → returns 403 JSON when authenticated user lacks permission

---

## 12. Security

### Architecture Overview

```
Client → Request → Security Filters → JWT Validation → Authentication → SecurityContext → Authorization → Controller → Service → Repository
```

### SecurityConfig

- **Session management:** `STATELESS` (no HTTP sessions)
- **CSRF:** Disabled (REST API)
- **CORS:** Configured via `CorsConfigurationSource` bean — allowed origins from config, methods: GET/POST/PUT/PATCH/DELETE/OPTIONS, allowed headers: `Authorization`/`Content-Type`/`Accept`, credentials allowed, max age 3600s
- **Public endpoints:** `POST /api/auth/**` (register, login, refresh, logout)
- **Protected endpoints:** All other requests require authentication
- **Filter chain:** `JwtAuthenticationFilter` added before `UsernamePasswordAuthenticationFilter`
- **401 handling:** `RestAuthenticationEntryPoint`
- **403 handling:** `RestAccessDeniedHandler`
- **Password encoder:** `BCryptPasswordEncoder`

### JwtService

**Signing:** HMAC-SHA with secret from `jwt.secret` property via `Keys.hmacShaKeyFor()`

**Token Generation:**
- Both access and refresh tokens are generated as a pair via `buildAuthResponse()`
- Each token contains:
  - `jti`: UUID (used as `tokenId` for revocation tracking)
  - `sub`: User email (used as subject)
  - `userId`: Custom claim (Long)
  - `type`: `"access"` or `"refresh"`
  - `iat`: Issued at
  - `exp`: Expiration

**Expiration:**
- Access token: 900,000 ms = 15 minutes
- Refresh token: 6,048,00,000 ms = 7 days

**Parsing:** `Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token)`

### JwtAuthenticationFilter

Extends `OncePerRequestFilter`. Runs on every request:

1. Extract `Authorization: Bearer <token>` header
2. If missing/invalid → pass through (unauthenticated)
3. Parse JWT → verify it is `type=access` → verify not revoked → verify no existing auth
4. Load `UserPrincipal` via `CustomUserDetailsService` (email-based lookup)
5. Create `UsernamePasswordAuthenticationToken` and set in `SecurityContextHolder`
6. On any exception → clear context

### CustomUserDetailsService

Loads `User` from database by email → wraps in `UserPrincipal` via `UserPrincipal.from(user)`

### UserPrincipal

Implements `UserDetails`. Always assigns `ROLE_USER`. Exposes `getId()` for service layer ownership checks.

### TokenRevocationService

- `revoke(tokenId, expiresAtMs)` — stores revoked token in DB (idempotent)
- `isRevoked(tokenId)` — checks DB for revocation
- `cleanupExpiredTokens()` — `@Scheduled(cron = "0 0 * * * *")` — deletes expired revoked tokens hourly

### LoginRateLimiter

- In-memory `ConcurrentHashMap<String, AttemptState>`
- Key: `clientIp:email`
- Max 5 failures within 10 min window → lock for 15 min
- Success clears the record
- Stale entry eviction when map exceeds 10,000 entries

### Logout Flow

1. Client sends `POST /api/auth/logout` with `Authorization: Bearer <accessToken>` header
2. Controller extracts access token from header via `resolveBearerToken()`
3. Optional `LogoutRequest` body may contain `refreshToken`
4. `authService.logout(accessToken, refreshToken)` quietly revokes both tokens
5. Revoked tokens stored in `revoked_tokens` table
6. Returns 204 No Content

### Refresh Token Flow

1. Client sends `POST /api/auth/refresh` with `RefreshTokenRequest` body
2. Parse + validate refresh token signature and expiration
3. Verify `type=refresh` claim
4. Verify token not in revoked_tokens table
5. Look up user by email from token claims
6. **Revoke the old refresh token** (rotation — prevents reuse)
7. Generate new access + refresh token pair
8. Return `AuthResponse`

### Public Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### Protected Endpoints

- `GET /api/users/me`
- All `/api/categories/**`
- All `/api/expenses/**`

### CORS Configuration

- Origins: configurable via `app.cors.allowed-origins` (default: `http://localhost:3000`, `http://localhost:5173`)
- Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
- Headers: `Authorization`, `Content-Type`, `Accept` (changed from `*` on 2026-09-08 — no longer wildcard-permissive alongside `allowCredentials(true)`)
- Credentials: allowed
- Max age: 3600s

---

## 13. Authorization / Ownership

### How Ownership Is Enforced

Ownership is enforced at the **service layer** through repository queries that scope results to the authenticated user:

| Resource | Ownership Check Location | Mechanism |
|---|---|---|
| **Expenses** | `ExpenseServiceImpl` | All CRUD operations call `expenseRepository.findByIdAndUser(expenseId, user)` or `expenseRepository.findByUserWithFilters(user, ...)` — queries always filter by `WHERE e.user = :user` |
| **Categories** | `CategoryServiceImpl` | All CRUD operations call `categoryRepository.findByIdAndUser(categoryId, user)` or `categoryRepository.findByUser(user, pageable)` — queries always filter by `WHERE c.user = :user` |
| **User data** | `UserServiceImpl` | `getUserById()` is called with `currentUser.getId()` from the controller — the ID comes from the JWT |

### Where Ownership Is Checked — Exact Locations

- `ExpenseServiceImpl.createExpense()` — `categoryRepository.findByIdAndUser(request.getCategoryId(), user)` ensures the user owns the category
- `ExpenseServiceImpl.getExpenseById()` — `expenseRepository.findByIdAndUser(expenseId, user)` ensures the user owns the expense
- `ExpenseServiceImpl.updateExpense()` — both `expenseRepository.findByIdAndUser()` and `categoryRepository.findByIdAndUser()` are checked
- `ExpenseServiceImpl.deleteExpense()` — `expenseRepository.findByIdAndUser(expenseId, user)`
- `CategoryServiceImpl.createCategory()` — category is assigned to the authenticated user
- `CategoryServiceImpl.getCategoryById()` — `categoryRepository.findByIdAndUser(categoryId, user)`
- `CategoryServiceImpl.updateCategory()` — `categoryRepository.findByIdAndUser(categoryId, user)`
- `CategoryServiceImpl.deleteCategory()` — `categoryRepository.findByIdAndUser(categoryId, user)`
- `UserController.getCurrentUser()` — `userService.getUserById(currentUser.getId())` — the `userId` comes from the JWT principal

### Verdict

**Yes, ownership is properly enforced.** A user cannot access another user's expenses, categories, or profile data because:
1. The user ID always comes from `@AuthenticationPrincipal UserPrincipal` (derived from JWT), never from client-supplied path/query parameters
2. All repository queries scope results to the authenticated user
3. If a resource belongs to a different user, the query returns empty → `ResourceNotFoundException` (404)

This also means a user cannot even see that another user's resource exists — they get a generic "not found" response.

---

## 14. Current API Flow

### Register

```
Client → POST /api/auth/register {fullName, email, password}
  → AuthController.register()
    → AuthServiceImpl.register()
      → UserServiceImpl.register()
        → Check email uniqueness (409 if duplicate)
        → Map to User entity
        → Encode password (BCrypt)
        → Set createdAt
        → Save to DB
      → Generate access + refresh tokens (both with email, userId)
    → Return AuthResponse {accessToken, refreshToken, tokenType: "Bearer", user}
```

### Login

```
Client → POST /api/auth/login {email, password} + X-Forwarded-For / remoteAddr
  → AuthController.login()
    → AuthServiceImpl.login(clientIp)
      → Rate limit check (key = clientIp:email)
      → AuthenticationManager.authenticate() (Spring Security)
      → On BadCredentials: record failure → throw 401
      → On success: record success → fetch user from DB → generate tokens
    → Return AuthResponse
```

### Access Protected Endpoint

```
Client → GET /api/expenses + Authorization: Bearer <accessToken>
  → JwtAuthenticationFilter.doFilterInternal()
    → Parse JWT
    → Verify type=access, not revoked, no existing auth
    → Load UserPrincipal from DB by email
    → Set SecurityContext
  → ExpenseController.getAllExpenses()
    → ExpenseServiceImpl.getAllExpenses(userId, ...)
      → Query scoped to user
    → Return PagedResponse<ExpenseResponse>
```

### Refresh Token

```
Client → POST /api/auth/refresh {refreshToken}
  → AuthController.refresh()
    → AuthServiceImpl.refresh()
      → Parse + validate refresh token
      → Verify type=refresh
      → Verify not revoked
      → Lookup user by email from claims
      → REVOKE old refresh token (rotation)
      → Generate new access + refresh pair
    → Return AuthResponse (new tokens)
```

### Logout

```
Client → POST /api/auth/logout + Authorization: Bearer <accessToken> + optional {refreshToken}
  → AuthController.logout()
    → Extract access token from Authorization header
    → AuthServiceImpl.logout(accessToken, refreshToken)
      → Quietly revoke access token (catch parse errors)
      → Quietly revoke refresh token if provided (catch parse errors)
    → Return 204 No Content
```

### Access Another User's Resource

```
Client → GET /api/expenses/123 + Authorization: Bearer <token_of_user_A>
  → JwtAuthenticationFilter → authenticates as user_A
  → ExpenseController.getExpense(expenseId=123)
    → ExpenseServiceImpl.getExpenseById(123, user_A.id)
      → expenseRepository.findByIdAndUser(123, user_A)
      → If expense belongs to user_B → returns empty → throws ResourceNotFoundException (404)
    → Return 404 "Expense not found"
```

---

## 15. Current Problems / Risks

> **Versions note:** Earlier versions of this review flagged Spring Boot 4.1.0, Java 25, and the `tools.jackson.databind` imports as problems. These are **correct** for Spring Boot 4.1 / Jackson 3 and are NOT issues. Confirmed 2026-09-08. The project compiles and all 50 tests pass.

### ✅ Resolved (2026-09-08)

| # | File | Problem | Resolution |
|---|---|---|---|
| — | `ExpenseController.java` | Route ordering: `/api/expenses/statistics` vs `/{expenseId}` was feared to shadow the statistics endpoint. | **Verified as already correct** — `/statistics` is declared before `/{expenseId}`. No change needed. |
| — | `ExpenseServiceImpl.java` | `getStatistics()` called `.get(0)` on `aggregateTotals()` result with no empty-list guard → risk of `IndexOutOfBoundsException`. | **Fixed** — added `result.isEmpty()` guard that returns a zeroed `ExpenseStatistics` (`BigDecimal.ZERO` totals, `0L` count). |
| — | `SecurityConfig.java` | CORS `setAllowedHeaders(List.of("*"))` combined with `allowCredentials(true)` was too permissive. | **Fixed** — allowed headers restricted to `Authorization`, `Content-Type`, `Accept`. |
| — | `application.properties` | `spring.jpa.hibernate.ddl-auto=update` in production config could silently mutate schema. | **Fixed** — changed to `validate`. |
| — | `CategoryServiceImpl` / `ExpenseServiceImpl` | No `@Transactional` on multi-step write operations (`create`/`update`/`delete`). | **Fixed** — added `@Transactional` to all write methods in both services. |
| — | `UserMapper.java` | Method named `toentity` (lowercase) — convention inconsistency. | **Fixed** — renamed to `toEntity()` (mapper, impl, and test). |
| — | `CategoryRepository.java` | `existsByNameAndUser()` declared but never called. | **Fixed** — removed the dead method. |

### ❌ HIGH

| # | File | Problem | Why It Is a Problem | Severity |
|---|---|---|---|---|
| 1 | `LoginRateLimiter.java` | **Rate limiter is in-memory (`ConcurrentHashMap`).** Resets on application restart. Does not work across multiple instances. | In a production multi-instance deployment, rate limiting is ineffective. An attacker can bypass it by restarting or targeting different instances. | HIGH |
| 2 | `TokenRevocationService.java` | **`revoke()` is not synchronized.** Two concurrent requests for the same refresh token could both pass the `existsByTokenId()` check; the DB `unique` constraint prevents duplicates but one save will fail. | Could cause 500 errors under concurrent refresh/logout attempts with the same token. | MEDIUM |

### ⚠️ MEDIUM

| # | File | Problem | Why It Is a Problem | Severity |
|---|---|---|---|---|
| 3 | `ExpenseServiceImpl.java` | `((Number) totals[1]).longValue()` — unchecked cast of `Object[]` element from `aggregateTotals()`. JPQL `COUNT` returns `Long`, but casting via `Number` is fragile. | Works in practice, but a type mismatch would throw `ClassCastException`. | LOW |
| 4 | `SecurityConfig.java` | No explicit authorization rules beyond `/api/auth/**` permit-all; no role-based restrictions. | Every authenticated user has identical access. No admin role. Not a bug for this use case, but limits future extensibility. | LOW |
| 5 | `application.properties` | Default `jwt.secret` is `dev-only-secret-key-change-me-must-be-at-least-32-chars`. If `JWT_SECRET` env var is not set, the app runs with a known secret. | Any attacker who reads this code can forge valid JWTs. | MEDIUM (mitigated by env var override) |
| 6 | `application.properties` | Default `DB_PASSWORD` is `1234`. | Weak default credential. | MEDIUM (mitigated by env var override) |

### ⚠️ LOW

| # | File | Problem | Why It Is a Problem | Severity |
|---|---|---|---|---|
| 7 | `LogoutRequest.java` | `refreshToken` field has no validation annotations; controller passes it through unvalidated. | A blank/empty refresh token is processed (silently caught by `revokeQuietly`). Not a bug, slightly wasteful. | LOW |
| 8 | `Expense.java` / `Category.java` | No `@UniqueConstraint` on `(user_id, name)` for categories or equivalent for expenses. Uniqueness enforced only at service layer. | Direct DB access or multiple service instances could bypass the check and create duplicates. | LOW |
| 9 | `ExpenseRepository.java` | `aggregateTotals()` returns `List<Object[]>` — untyped, fragile, no compile-time safety. A record-constructor JPQL projection was attempted but Hibernate throws `Missing constructor for type` (`SemanticException`) on `COALESCE`/`COUNT`; reverted to `Object[]`. | Difficult to maintain. A query return change would silently break. An interface projection or native query could solve this. | LOW |
| 10 | `ExpenseServiceImpl.java` / `CategoryServiceImpl.java` | Every method starts with `userRepository.findById(userId)` to resolve the User. Redundant; could be fetched once and passed through, or a custom `findByIdOrThrow` helper used. | Minor redundancy and code repetition. Not a performance issue (small data), but a design concern. | LOW |
| 11 | `JwtAuthenticationFilter.java` | The filter relies on `Jwts.parser().verifyWith(signingKey).build().parseSignedClaims()` to reject expired tokens; expired tokens silently fall through (filter chain continues unauthenticated). | Client gets a generic 401 instead of an explicit "token expired" message. Functional, but not user-friendly. | LOW |
| 12 | `ExpenseTrackerApplication.java` | `@EnableScheduling` enables the default Spring scheduling infrastructure for the single scheduled cleanup task. | Minor. Could be scoped more narrowly. | LOW |

---

## 16. Current Status

### Correct / Working

- ✅ Entity model design — clean relationships, proper `@ManyToOne(LAZY)`, correct `@JoinColumn` constraints
- ✅ DTO layer — complete request/response separation, proper validation annotations
- ✅ MapStruct mappers — correctly configured with `componentModel="spring"`, field-level mappings accurate
- ✅ Ownership enforcement — all service methods scope queries to authenticated user via repository
- ✅ JWT authentication flow — access/refresh token generation, validation, and revocation
- ✅ Refresh token rotation — old refresh token is revoked on use
- ✅ Logout — both access and refresh tokens revoked
- ✅ Global exception handler — handles all expected exceptions with appropriate HTTP status codes
- ✅ Custom 401/403 handlers — return JSON error responses, not default HTML
- ✅ Password encoding — BCrypt via Spring Security
- ✅ Input validation — Bean Validation on all request DTOs
- ✅ Pagination — `PagedResponse` + `PageableUtils` with max size cap (100) and sort field whitelist
- ✅ CORS configuration — configurable origins, explicit allow-header whitelist (Authorization, Content-Type, Accept)
- ✅ Stateless session policy — no server-side sessions
- ✅ Revoked token cleanup — hourly `@Scheduled` task
- ✅ Custom exceptions — clean hierarchy
- ✅ Repository queries — JPQL with dynamic filters, aggregates, constructor expressions
- ✅ Transactional writes — `@Transactional` on all `create`/`update`/`delete` methods in `CategoryServiceImpl` and `ExpenseServiceImpl`
- ✅ `getStatistics()` is guarded against empty results (returns zeroed stats)
- ✅ **Test suite: 50 tests, 0 failures, 0 errors** (verified `.\mvnw.cmd test`, 2026-09-08)

### Working — Confirmed, No Longer Needs Review

- ✅ `/api/expenses/statistics` route ordering — verified correct (declared before `/{expenseId}`)
- ✅ `ddl-auto=validate` in production config — schema is validated, not auto-mutated
- ✅ Jackson 3.x imports (`tools.jackson.databind`) — correct for Spring Boot 4.1 / Jackson 3

### Needs Review

- ⚠️ In-memory `LoginRateLimiter` — single-instance only, resets on restart
- ⚠️ `TokenRevocationService.revoke()` — not synchronized under concurrent same-token requests
- ⚠️ `aggregateTotals()` still returns `List<Object[]>` — a DTO projection was attempted but Hibernate rejects the record constructor with `COALESCE`/`COUNT`; an interface projection or native query would be the way forward
- ⚠️ Role-based authorization — all authenticated users currently have identical access

### Broken / Incorrect

*(none identified — project compiles and full test suite passes)*

### Cannot Determine from Code Alone

- ❓ Whether the database schema matches the entities (requires `Hibernate validate` against the real MySQL DB)
- ❓ Whether CORS env var override is configured in deployment
- ❓ Whether the JWT secret env var override is configured in deployment
- ❓ Runtime behavior under concurrent load
- ❓ Deployment topology (single vs multi-instance) — affects rate limiter and revocation strategy

---

## 17. Recommended Next Steps

### Priority 1 — Production Security

1. **Replace in-memory `LoginRateLimiter`** with a Redis-backed or database-backed solution for production multi-instance support.
2. **Synchronize `TokenRevocationService.revoke()`** (or use optimistic locking / DB upsert) to avoid 500s under concurrent refresh with the same token.
3. **Enforce secrets at deployment** — ensure `JWT_SECRET`, `DB_USERNAME`, and `DB_PASSWORD` env vars are always set; do not rely on the dev defaults.
4. **Add database-level uniqueness** — `@UniqueConstraint` on `Category(user_id, name)` and, if appropriate, on `Expense` fields.

### Priority 2 — Code Quality

5. **Type the `aggregateTotals()` return** — Use a Spring Data interface projection or a JPA `@SqlResultSetMapping`/native query instead of `List<Object[]>` (record-constructor JPQL fails under Hibernate for `COALESCE`/`COUNT`).
6. **Add `@Transactional` to remaining paths** — verify `UserServiceImpl.register()` and `AuthServiceImpl.refresh()`/`logout()` transaction boundaries.
7. **Add database constraints / validation** — see item 4.

### Priority 3 — Production Readiness

8. **Add health check endpoint** — Consider Spring Boot Actuator.
9. **Add API documentation** — Consider SpringDoc OpenAPI / Swagger.
10. **Add structured logging** — For auth events (login success/failure, token refresh, logout).
11. **Keep integration tests current** — Re-run the full suite before any dependency or config changes.
12. **Introduce schema management** — Either rely on `validate` + manual SQL migrations, or add Flyway/Liquibase as the schema evolves.
