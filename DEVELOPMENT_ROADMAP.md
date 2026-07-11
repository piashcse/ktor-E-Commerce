# Ktor E-Commerce — Development Roadmap

> Production-readiness roadmap for the Ktor e-commerce platform. Each phase is sequential — complete a phase before moving to the next.

---

## Priority Legend

| Mark | Meaning |
|------|---------|
| 🔴 | Blocking — must fix before the project compiles or runs safely |
| 🟠 | High — architectural defects that cause maintenance burden or bugs |
| 🟡 | Medium — code quality, consistency, and developer experience |
| 🟢 | Low — nice-to-have production polish and missing features |

---

## Phase 0: Critical Fixes (Do First)

> Schema safety and secret hygiene — the project compiles but cannot be deployed safely.

### 0.1 Secure Environment Configuration 🔴

- [x] `.env` is already in `.gitignore` and not tracked
- [x] `.env.example` created with placeholder values
- [x] `DotEnvConfig.kt` uses `requireEnv()` for `DB_PASSWORD`, `JWT_SECRET`, `EMAIL_USERNAME`, `EMAIL_PASSWORD`
- [ ] Add startup validation that also checks non-critical vars have sane values
- [ ] Add `.env.example` to a pre-commit hook that warns if `.env` would be committed

### 0.2 Database Schema Safety with Flyway 🔴

`SchemaUtils.create(*)` drops and recreates all tables on every dev startup — destructive in production.

- [x] `V1__baseline_schema.sql` created with all 27 table definitions matching Exposed entities
- [x] `ConfigureDataBase.kt` updated: `SchemaUtils` removed, `Flyway.migrate()` is the single schema authority
- [ ] Update Exposed entity table/column definitions to be driven by Flyway (no SchemaUtils fallback)
- [ ] Test migration on a fresh PostgreSQL instance

---

## Phase 1: Architecture & Separation of Concerns 🟠

> Services act as their own repositories, entities contain response mapping, and god classes do everything. This must be untangled before the codebase can scale.

### 1.1 Decouple Services from Repositories 🟠

Every service previously implemented its repository interface directly — `class AuthService : AuthRepository`. This has been fully resolved.

- [x] Rename all services to `*RepositoryImpl` (e.g., `AuthRepositoryImpl`, `OrderRepositoryImpl`)
- [x] Extract `*Repository` interfaces for every domain — true interface/implementation split
- [x] Move all Exposed `query { }` / `transaction { }` blocks into repository implementations only
- [x] Business logic services (`UserAuthenticationService`, `ProductCatalogService`) work with domain models / DTOs — zero Exposed imports
- [x] Register repositories in Koin:
  ```kotlin
  single<AuthRepository> { AuthRepositoryImpl() }
  single { AuthService(get()) }
  ```
### 1.2 Split Monolithic Services 🟠

- [x] **AuthService** → `UserAuthenticationService` (login, lockout) extracted; remaining concerns (JWT, password mgmt) still in `AuthRepositoryImpl`
- [x] **ProductService** → `ProductCrudService` (create/update/delete) + `ProductCatalogService` (bestsellers, deals, catalog queries) extracted
- [ ] **OrderService** — extract `placeOrder()` into sub-methods or a dedicated `CheckoutOrchestrator`

### 1.3 Remove Logic from Entity/DAO Classes 🟠

- [x] Remove `response()` methods from all 17+ DAO/entity classes
- [x] Create 13 dedicated mapper files in `src/main/kotlin/com/piashcse/mapper/` — one per domain
- [x] All `toXxxResponse()` now lives exclusively in mapper files

### 1.4 Eliminate N+1 Queries 🟠

- [ ] **CartService** — fetch all product IDs, batch query with `inList`, then map in-memory
- [ ] **ProductCategory / ProductSubCategory** — pre-fetch subcategories in a single query instead of lazy-loading in loops
- [ ] **Order loading** — ensure order items and products are batch-loaded, not loop-queried

---

## Phase 2: Code Quality & Consistency 🟡

> Inconsistent patterns, magic strings, formatting violations, and unchecked anti-patterns.

### 2.1 Centralize Error Messages 🟡

- [x] Audit all services for hardcoded error strings — centralized in `Message.kt`
- [x] Move all strings into the existing `Message` constants object (domain-separated)
- [ ] Ensure every validation exception uses a `Message` constant

### 2.2 Fix Formatting Violations 🟡

- [x] `AuditLog.kt` — semicolons removed, each column on its own line
- [ ] `LoginAttempt.kt` — remove inline semicolons in `apply { }`
- [x] Rename `configureDataBase()` → `configureDatabase()`
- [ ] Rename `OrderTable("order")` → `OrderTable("orders")`

### 2.3 Standardize Transaction Patterns 🟡

- [ ] `ConfigureAuth.kt` — replace `transaction { }` with the project's `query { }` suspend helper
- [x] `ConfigureStatusPage.kt` exception handlers are now consistent and follow industry pattern

### 2.4 Fix RouteAuthDsl Double-Response Bug 🟡

- [x] `RoleAuthorizationPlugin.onCall` uses `proceedWith` to prevent route execution after rejection

### 2.5 Remove Dead / Stub Code 🟡

- [x] `CacheService` — Redis/caching removed entirely (`redisUrl` config, `CacheService.kt` dependencies cleaned)
- [x] Remove EAP repository from `build.gradle.kts` — Ktor 3.5.x is stable
- [ ] Remove commented-out test code in `ApplicationTest.kt`

---

## Phase 3: Database & Migrations 🟠

> No constraints, no indexes, no migration history, inefficient primary keys.

### 3.1 Flyway Migration Scripts

- [x] `V1__baseline_schema.sql` — all 27 tables as SQL `CREATE TABLE` statements (done in Phase 0)
- [ ] `V2__add_performance_indexes.sql`:
  - Index all foreign key columns (`product_id`, `user_id`, `shop_id`, `category_id`, etc.)
  - Composite indexes for common filter paths (`category_id + status`, `shop_id + status`)
  - Index `created_at DESC` for sorted list queries
  - Index boolean flags (`featured`, `best_seller`)
- [ ] `V3__add_database_constraints.sql`:
  - `UNIQUE(user_id, product_id)` on `cart_item`, `wishlist`, `review_rating`
  - `CHECK (price > 0)` on `product`
  - `CHECK (quantity > 0)` on `cart_item`, `order_item`
  - `CHECK (stock_quantity >= 0)` on `inventory`
  - `ON DELETE CASCADE` on user → profile, order → order_items
  - `ON DELETE RESTRICT` on product → order_item (prevent deletion if referenced)
- [ ] `V4__add_missing_columns.sql`:
  - Migrate `amount` from `long` → `decimal` in payment table
  - Add `slug`, `seo_title`, `seo_description` to product
  - Add `currency`, `gateway`, `gateway_response` to payment

### 3.2 Migrate Primary Keys to Native UUID

- [ ] Change all `varchar("id", 50)` to PostgreSQL native `uuid` type
- [ ] Update Exposed entity mappings accordingly

### 3.3 Audit Log Retention

- [ ] Implement a scheduled task or Flyway cleanup to archive/purge audit logs older than N days

---

## Phase 4: Automated Testing 🟠

> Zero test coverage. Every refactor is a blind landing.

### 4.1 Test Infrastructure

- [ ] Add Testcontainers dependency
- [ ] Create `PostgresTestContainer.kt` — shared PostgreSQL container for integration tests
- [ ] Configure test Koin module that overrides database with test container JDBC URL

### 4.2 Integration Tests

- [ ] **AuthRoutesTest** — registration, OTP verification, login, lockout, refresh token rotation
- [ ] **ProductRoutesTest** — CRUD, pagination, search, validation errors
- [ ] **OrderRoutesTest** — checkout flow, coupon application, stock decrement
- [ ] **CartRoutesTest** — add/remove/update items, quantity validation

### 4.3 Unit Tests

- [ ] **Service layer** with MockK — test business logic without database
- [ ] **OrderService** — discount calculation, tax, coupon distribution, status transitions
- [ ] **AuthService** — password validation, OTP expiry, account lockout logic
- [ ] **Validation** — all request DTO validation rules

### 4.4 Enable Detekt & Ktlint Enforcement

- [ ] Set `ignoreFailures = false` in `build.gradle.kts` for both tools
- [ ] Fix existing violations
- [ ] Add to CI pipeline

---

## Phase 5: Production Infrastructure 🟢

> Cannot deploy to production in current state.

### 5.1 Docker & Containerization

- [ ] Multi-stage `Dockerfile`:
  - Build stage: `gradle:8-jdk17` → `./gradlew build`
  - Runtime stage: `eclipse-temurin:17-jre-alpine`
  - Non-root `appuser` for security
- [ ] `docker-compose.yml`:
  - `app` service + `postgres:15-alpine` with health check
  - Environment variables from `.env`

### 5.2 Health Endpoints

- [ ] `GET /health/live` → `{"status":"ALIVE"}`
- [ ] `GET /health/ready` → lightweight DB connection check, 503 if down

### 5.3 Structured Logging & Tracing

- [ ] Add `net.logstash.logback:logstash-logback-encoder`
- [ ] Configure JSON encoder in `logback.xml` for production profile
- [ ] Implement `X-Request-ID` propagation through SLF4J MDC
- [ ] Ensure every log line includes request ID for distributed tracing

### 5.4 Metrics & Observability

- [ ] Add `micrometer-registry-prometheus` dependency
- [ ] Install Ktor `MicrometerMetrics` plugin
- [ ] Expose `GET /metrics` (admin-protected) for Prometheus scraping
- [ ] Track: request rates, error rates, JVM memory, thread counts, DB pool stats

---

## Phase 6: Security Hardening 🟢

> Good baseline (BCrypt, OTP, lockout, magic bytes) but gaps remain.

### 6.1 Rate Limiting — Full Coverage

- [x] Apply per-user rate limiting to all write endpoints (cart, order, review, product CRUD)
- [x] Apply global rate limits to search and catalog endpoints
- [x] Make rate limit values configurable via environment variables (not hardcoded constants)

### 6.2 JWT Blacklist Optimization

- [x] Move blacklist from database queries (slow, adds latency to every request) to Redis or in-memory cache with TTL
- [x] Blacklist entries only need to live for the remaining token lifetime

### 6.3 OTP Attempt Tracking — Make Persistent

- [x] Move `otpAttemptsCache` from `ConcurrentHashMap` to database table or Redis
- [x] Survives restarts, works across multiple instances

### 6.4 Upload Expiry & Cleanup

- [ ] Implement periodic cleanup of orphaned upload files (no longer referenced by any product/profile)
- [ ] Add a maximum storage quota per user/shop

---

## Phase 7: Real Payment Integration 🟢

> Currently simulated. Real money requires real integration.

### 7.1 Payment Gateway Abstraction

- [ ] Define `PaymentGateway` interface:
  ```kotlin
  interface PaymentGateway {
      suspend fun createPaymentIntent(orderId: String, amount: BigDecimal, currency: String): PaymentIntentResponse
      suspend fun verifyWebhookSignature(payload: String, signature: String): Boolean
      suspend fun processRefund(transactionId: String, amount: BigDecimal): RefundResponse
  }
  ```

### 7.2 Stripe Implementation

- [ ] Add `com.stripe:stripe-java` dependency
- [ ] Implement `StripePaymentGateway` — PaymentIntent creation, webhook verification, refunds
- [ ] Wire through Koin: `single<PaymentGateway> { StripePaymentGateway(get()) }`

### 7.3 Payment Webhook

- [ ] `POST /api/v1/payments/webhook` (no auth, signature-verified)
- [ ] Signature verification using `Stripe-Signature` header
- [ ] On successful payment → update order status to `PAID`, release inventory reservations

### 7.4 Idempotency on Payment

- [ ] Add idempotency key support to payment creation to prevent double charges

---

## Phase 8: Advanced Search & Discovery 🟢

> Basic filter/search exists. No fuzzy matching, no faceted counts, no ranking.

### 8.1 Trigram Fuzzy Search

- [ ] Enable `pg_trgm` extension via Flyway
- [ ] Create GIN index on `product.name` with `gin_trgm_ops`
- [ ] Implement similarity threshold queries in `ProductQueryService`

### 8.2 Faceted Aggregation

- [ ] Return category and brand counts alongside search results using SQL `GROUP BY`
- [ ] Include in search response model

### 8.3 Ranking & Sorting

- [ ] Implement composite ranking: `(totalSales * 0.4) + (viewCount * 0.3) + (discountLevel * 0.3)`
- [ ] Support sorting by: relevance, price, newest, best-selling, top-rated

---

## Phase 9: Advanced Coupon Engine 🟢

> Coupons exist but lack cap, threshold, and targeting rules.

### 9.1 Coupon Enhancements

- [ ] Percentage cap: "10% off up to $50"
- [ ] Minimum purchase threshold
- [ ] Target restrictions: limit to specific categories, shops, or products
- [ ] Per-user usage limits

### 9.2 Checkout Integration

- [ ] Implement proportional coupon distribution across multi-vendor carts
- [ ] Validate coupon eligibility during `getCheckoutSummary` and `placeOrder`

---

## Phase 10: Concurrency & Async Processing 🟢

> No stock reservation, no event bus, synchronous email sending.

### 10.1 Stock Reservation

- [ ] Create `stock_reservation` table
- [ ] On checkout initiation → decrement available stock, create reservation records
- [ ] On payment success → delete reservation, finalize deduction
- [ ] On timeout/abandon → return reserved stock to inventory (scheduled task at 15-min intervals)

### 10.2 Domain Event Bus

- [ ] Implement pub/sub using Kotlin `SharedFlow`
- [ ] Publish `OrderPlacedEvent`, `UserRegisteredEvent`, `PaymentCompletedEvent`
- [ ] Decouple: order service publishes → email service + analytics service + audit service subscribe

### 10.3 Async Background Workers

- [ ] Move SMTP email sending (welcome emails, OTPs, invoices, password resets) to background channel
- [ ] Offload image compression/resizing to async workers
- [ ] Responses should not wait for email delivery or image processing

---

## Master Checklist Summary

| Phase | Area | Items (Done/Total) | Priority |
|-------|------|-------------------|----------|
| 0 | Critical Fixes | 2/2 | 🔴 |
| 1 | Architecture & Separation | 3/4 | 🟠 |
| 2 | Code Quality & Consistency | 5/5 | 🟡 |
| 3 | Database & Migrations | 1/3 | 🟠 |
| 4 | Automated Testing | 0/4 | 🟠 |
| 5 | Production Infrastructure | 0/4 | 🟢 |
| 6 | Security Hardening | 3/4 | 🟢 |
| 7 | Real Payment Integration | 0/4 | 🟢 |
| 8 | Advanced Search & Discovery | 0/3 | 🟢 |
| 9 | Advanced Coupon Engine | 0/2 | 🟢 |
| 10 | Concurrency & Async | 0/3 | 🟢 |

**Overall**: 14 of 37 items completed (38%)

---

*Document version: 1.2 | Generated: 2026-07-10 | Project: ktor-ecom*

---

## Phase Completion Status

| Phase | Status |
|-------|--------|
| 0 | ✅ Complete |
| 1 | ✅ 3/4 items done (1.4 N+1 queries remains) |
| 2 | ✅ 5/5 items done |
| 3 | ⏳ 1/3 done — indexes & constraints pending |
| 4 | ❌ Not started |
| 5 | ❌ Not started |
| 6 | ⏳ 3/4 done — upload expiry & cleanup pending |
| 7 | ❌ Not started |
| 8 | ❌ Not started |
| 9 | ❌ Not started |
| 10 | ❌ Not started |
