# Expense Tracker API

> A production-ready REST API for personal expense management — secure by default, data-isolated per user, and built on the latest Spring Boot stack.

---

## Table of Contents

- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [API Endpoints](#api-endpoints)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running the Application](#running-the-application)
- [Running the Tests](#running-the-tests)
- [Security Notes](#security-notes)
- [Possible Future Improvements](#possible-future-improvements)

---

## Key Features

- **Expense CRUD** — create, read, update, and delete expenses; each expense carries a title, amount, date, and category
- **Category CRUD** — manage named spending categories, scoped per user with a DB-level unique constraint on `(user_id, name)`
- **Filtered, paginated listing** — filter expenses by category and/or date range; sort by any field; configurable page size
- **Spending statistics** — aggregate total, count, and average for any date range, with per-category breakdown
- **JWT authentication** — stateless access tokens (15 min) and refresh tokens (7 days), both typed and signed with HMAC-SHA
- **Refresh token rotation** — each `/auth/refresh` call issues a new token pair and immediately revokes the old refresh token
- **Token revocation on logout** — both access and refresh tokens are invalidated at logout and checked on every request
- **Login brute-force protection** — in-process rate limiter: 5 failures per IP+email within 10 minutes triggers a 15-minute lockout
- **Data isolation** — every query is scoped to the authenticated user; accessing another user's resources returns 404, not 403
- **Bean validation** — all request bodies validated via Jakarta Validation; errors return structured 400 responses
- **Startup secret guard** — application refuses to start in the `prod` profile if `JWT_SECRET` is unset or holds the dev default

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 25 |
| Framework | Spring Boot | 4.1.0 |
| Security | Spring Security | (managed by Boot) |
| Persistence | Spring Data JPA / Hibernate | (managed by Boot) |
| Database | MySQL | 8+ (runtime) |
| JWT | JJWT (jjwt-api / jjwt-impl / jjwt-jackson) | 0.12.6 |
| Object mapping | MapStruct | 1.6.3 |
| Boilerplate reduction | Lombok | 1.18.46 |
| Build tool | Maven (Maven Wrapper included) | — |
| Test DB | H2 (in-memory, test scope only) | — |

---

## Architecture Overview

Every request flows through a stateless filter chain before reaching any business logic.

```
┌─────────┐   HTTP    ┌──────────────────────────────────────────────┐
│ Client  │ ────────► │             Spring Security Filter Chain      │
└─────────┘           │  JwtAuthenticationFilter                      │
                      │    ├─ extract Bearer token from header        │
                      │    ├─ validate signature & expiry (JJWT)     │
                      │    ├─ check revocation table (revoked_tokens) │
                      │    └─ populate SecurityContext if valid       │
                      └────────────────────┬─────────────────────────┘
                                           │
                              ┌────────────▼────────────┐
                              │       Controller         │
                              │  (request + validation)  │
                              └────────────┬────────────┘
                                           │
                              ┌────────────▼────────────┐
                              │        Service           │
                              │  (business logic,        │
                              │   ownership checks)      │
                              └────────────┬────────────┘
                                           │
                              ┌────────────▼────────────┐
                              │       Repository         │
                              │   (Spring Data JPA)      │
                              └────────────┬────────────┘
                                           │
                              ┌────────────▼────────────┐
                              │         MySQL            │
                              └─────────────────────────┘
```

**Key auth design decisions:**

- **Stateless sessions** — no server-side session state; every request is authenticated from the JWT alone
- **Two-token model** — short-lived access tokens (15 min) minimise exposure; long-lived refresh tokens (7 days) enable silent renewal without re-login
- **Refresh token rotation** — a refresh token can only be used once; each use issues a new pair, so stolen tokens are self-invalidating after first legitimate use
- **Database-backed revocation** — revoked token IDs are persisted; expired rows are pruned hourly by a scheduled job
- **Concurrent-safe revocation** — the DB `UNIQUE` constraint on `token_id` is the arbiter; concurrent duplicate revocations are handled by catching `DataIntegrityViolationException`, not by a check-then-insert race

---

## API Endpoints

### Auth — `/api/auth` (no authentication required)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register a new user; returns access + refresh tokens |
| `POST` | `/api/auth/login` | No | Authenticate; returns access + refresh tokens |
| `POST` | `/api/auth/refresh` | No | Exchange a valid refresh token for a new token pair |
| `POST` | `/api/auth/logout` | Bearer token | Revoke the current access token (and optionally the refresh token) |

### Users — `/api/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/me` | Bearer token | Return the authenticated user's profile |

### Categories — `/api/categories`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/categories` | Bearer token | Create a new category for the current user |
| `GET` | `/api/categories` | Bearer token | List categories (paginated; sort by `id` or `name`) |
| `GET` | `/api/categories/{id}` | Bearer token | Get a single category by ID |
| `PUT` | `/api/categories/{id}` | Bearer token | Replace a category's name |
| `DELETE` | `/api/categories/{id}` | Bearer token | Delete a category |

### Expenses — `/api/expenses`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/expenses` | Bearer token | Create a new expense |
| `GET` | `/api/expenses` | Bearer token | List expenses; optional filters: `categoryId`, `startDate`, `endDate`; paginated; sortable by `id`, `title`, `amount`, `expenseDate`, `createdAt` |
| `GET` | `/api/expenses/statistics` | Bearer token | Aggregate stats (total, count, average) with optional `startDate`/`endDate`; includes per-category breakdown |
| `GET` | `/api/expenses/{id}` | Bearer token | Get a single expense by ID |
| `PUT` | `/api/expenses/{id}` | Bearer token | Replace an expense |
| `DELETE` | `/api/expenses/{id}` | Bearer token | Delete an expense |

---

## Getting Started

### Prerequisites

- **JDK 25** (or compatible)
- **MySQL 8+** running locally (or accessible via network)
- Maven is not required separately — the Maven Wrapper (`./mvnw`) is included

### 1 · Clone the repository

```bash
git clone https://github.com/<your-username>/expense-tracker.git
cd expense-tracker
```

### 2 · Create the database

```sql
CREATE DATABASE expense_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3 · Apply the schema

The application runs with `spring.jpa.hibernate.ddl-auto=validate` — it does **not** auto-create tables. Run your schema migration scripts against the database before starting the application.

### 4 · Set environment variables

See the [Environment Variables](#environment-variables) section below. At minimum, set `JWT_SECRET` and `DB_PASSWORD`.

---

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | **Yes (prod)** | HMAC signing key — must be ≥ 32 characters. A fallback dev value is used if unset, but the app will **refuse to start** in the `prod` Spring profile without this variable |
| `DB_PASSWORD` | **Yes** | MySQL user password |
| `DB_URL` | No | Full JDBC URL (default: `jdbc:mysql://localhost:3306/expense_tracker`) |
| `DB_USERNAME` | No | MySQL username (default: `root`) |
| `CORS_ALLOWED_ORIGINS` | No | Comma-separated list of allowed origins (default: `http://localhost:3000,http://localhost:5173`) |

---

## Running the Application

**Local development (default profile):**

```bash
./mvnw spring-boot:run
```

The server starts on port `7070`.

**Production profile (enables startup secret validation):**

```bash
JWT_SECRET=<your-real-secret> \
DB_PASSWORD=<your-db-password> \
SPRING_PROFILES_ACTIVE=prod \
./mvnw spring-boot:run
```

---

## Running the Tests

```bash
./mvnw test
```

Tests run against an H2 in-memory database with `create-drop` DDL. No external database or environment variables are needed.

The test suite includes:

- **Unit tests** — service layer, JWT service, rate limiter, token revocation (including the concurrent duplicate-insert path)
- **Repository tests** — `@DataJpaTest` slice against H2, covering filtered queries and statistics aggregation
- **Integration tests** — `@SpringBootTest` full-stack tests covering registration, login, token use, and logout revocation

---

## Security Notes

| Area | Decision |
|---|---|
| **Password hashing** | BCrypt (via Spring Security's `BCryptPasswordEncoder`) — adaptive cost factor, per-password salting |
| **JWT algorithm** | HMAC-SHA (key size determined by secret length) via JJWT 0.12.6 |
| **Access token expiry** | 15 minutes — short enough to limit exposure if intercepted |
| **Refresh token expiry** | 7 days — rotated on every use; revoked on logout |
| **Token revocation** | Stored in the `revoked_tokens` table; checked on every authenticated request; pruned hourly |
| **Brute-force protection** | In-process rate limiter keyed on IP + email; 5 failures → 15-minute lockout (configurable) |
| **CORS** | Restricted to `CORS_ALLOWED_ORIGINS`; credentials allowed; preflight cached for 1 hour |
| **Secrets** | All sensitive values read from environment variables; dev fallbacks are clearly labelled and blocked in production by `ProductionSecretValidator` |
| **Data isolation** | All queries include `user_id = :currentUserId`; cross-user access returns 404 |

---

## Possible Future Improvements

- **Distributed rate limiting** — the current login rate limiter is in-process (per instance). A Redis-backed implementation would be needed in a multi-instance deployment
- **OpenAPI / Swagger documentation** — adding `springdoc-openapi` would provide interactive API docs and a machine-readable contract for client generation
- **Email verification** — registration currently trusts the provided email address without confirmation
- **Role-based access control** — the current design grants all authenticated users equal permissions; an admin role could be added if management endpoints are needed
- **Frontend client** — the API is designed to back a single-page application; no client is included in this repository
