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

- [x] Enable `pg_trgm` extension via Flyway
- [x] Create GIN index on `product.name` with `gin_trgm_ops`
- [x] Implement similarity threshold queries in `ProductQueryService`

### 8.2 Faceted Aggregation

- [x] Return category and brand counts alongside search results using SQL `GROUP BY`
- [x] Include in search response model

### 8.3 Ranking & Sorting

- [x] Implement composite ranking: `(totalSales * 0.4) + (viewCount * 0.3) + (discountLevel * 0.3)`
- [x] Support sorting by: relevance, price, newest, best-selling, top-rated

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

- [x] Create `stock_reservation` table
- [x] On checkout initiation → decrement available stock, create reservation records
- [x] On payment success → delete reservation, finalize deduction
- [x] On timeout/abandon → return reserved stock to inventory (scheduled task at 15-min intervals)

### 10.2 Domain Event Bus

- [x] Implement pub/sub using Kotlin `SharedFlow`
- [x] Publish `OrderPlacedEvent`, `UserRegisteredEvent`, `PaymentCompletedEvent`
- [x] Decouple: order service publishes → email service + analytics service + audit service subscribe

### 10.3 Async Background Workers

- [x] Move SMTP email sending (welcome emails, OTPs, invoices, password resets) to background channel
- [x] Offload image compression/resizing to async workers
- [x] Responses should not wait for email delivery or image processing

---

## Phase 11: Schema Alignment & Data Integrity 🟠

> Exposed entities use `enumerationByName` (VARCHAR) but V1 SQL creates columns as INTEGER. Duplicate migrations. Wrong table references. These will cause runtime failures.

### 11.1 Fix Enum Column Type Mismatches 🟠

The following columns are `INTEGER` in V1 SQL but `enumerationByName` (stores string names) in Exposed:

- [x] `user.user_type` — change V1 SQL from `INTEGER` to `VARCHAR(100)` to match `UserType` enum
- [x] `"order".payment_method` — change from `INTEGER` to `VARCHAR(50)`
- [x] `"order".payment_status` — change from `INTEGER` to `VARCHAR(30)`
- [x] `"order".status` — change from `INTEGER` to `VARCHAR(30)`
- [x] `payment.status` — change from `INTEGER` to `VARCHAR(30)`
- [x] `payment.payment_method` — change from `INTEGER` to `VARCHAR(50)`
- [x] `coupon.discount_type` — change from `INTEGER` to `VARCHAR(20)`
- [x] `shop.status` — change from `INTEGER` to `VARCHAR(50)`
- [x] `product.status` — change from `INTEGER` to `VARCHAR(50)`
- [x] `review_rating.status` — change from `INTEGER` to `VARCHAR(20)`
- [x] `policy_documents.type` — change from `INTEGER` to `VARCHAR(30)`
- [x] `refund_request.status` — change from `INTEGER` to `VARCHAR(20)`
- [x] `refund_request.refund_method` — change from `INTEGER` to `VARCHAR(50)`
- [x] `login_attempt.user_type` — change from `INTEGER` to `VARCHAR(20)`

Or alternatively, change all Exposed entities to use `integerByEnum` to match existing SQL.

### 11.2 Fix V3 Migration — Coupon Table Duplication 🟠

- [x] Remove or rewrite `V3__create_coupon_table.sql` — V1 baseline already creates `coupon` with `discount_type INTEGER`. V3 re-creates it with `discount_type VARCHAR(20)`. On a fresh Flyway run, V1 wins (INTEGER), but Exposed expects VARCHAR (`enumerationByName`).
- [x] Create replacement V3 migration that only alters column type if needed, or merge into V2.

### 11.3 Fix V5 Migration — Wrong Table Reference 🟠

- [x] `V5__create_stock_reservation_table.sql` line 6: Change `REFERENCES orders(id)` to `REFERENCES "order"(id)`. V1 baseline names the table `"order"` (quoted, singular), not `orders`.

### 11.4 Remove SchemaUtils Dev Mode Fallback 🟠

- [x] `ConfigureDataBase.kt` still calls `SchemaUtils.createMissingTablesAndColumns()` in dev mode (line 65). This creates tables using Exposed-inferred schema (VARCHAR for enums) which diverges from Flyway SQL (INTEGER).
- [x] Remove `createTables()`, the `allTables` array, and the `isDev` branching entirely. Flyway should be the single schema authority in all environments.

### 11.5 Consolidate Dual Stock Management 🟠

- [x] `product.stock_quantity` (V1 SQL line 204) and `inventory` table both track stock. Only one source of truth.
- [x] Remove `stock_quantity` from `product` table, or remove `inventory` table and add inventory columns to product.

### 11.6 Fix ConfigureAuth.kt Blocking Query 🟡

- [x] `ConfigureAuth.kt` line 34 runs `query { }` (a suspend helper) inside JWT `validate` lambda which is **not a suspend context**. This blocks the event loop thread under load.
- [x] Move blacklist check to a pre-interceptor or use cache-only check in the validate lambda, deferring DB checks.

---

## Phase 12: Code Consistency & Boilerplate Reduction 🟡

> Every domain follows the same 3-file pattern. Simple CRUD domains get dedicated Repository interface, Impl, and Routes files. Service layer is inconsistent. Centralization gaps.

### 12.1 Generic CRUD Repository Base 🟡

- [ ] Create generic `CrudRepository<T, ID>` base interface with standard operations: `getById`, `getAll`, `create`, `update`, `delete`
- [ ] Create generic `CrudRepositoryImpl<T, ID, E>` base implementation using Exposed DAO reflection
- [ ] Apply to simple CRUD domains: `Brand`, `ShopCategory`, `ShippingMethod`, `Policy`, `Consent` — no custom business logic needed
- [ ] Eliminates ~15 files (3 per domain → 1 shared + 1 per domain)

### 12.2 Consistent Service Layer Across All Features 🟡

- [ ] Currently only Auth (`UserAuthenticationService`), Product (`ProductCatalogService` + `ProductCrudService`), and Profile (`ProfileService`) have service layers. All other routes call repositories directly.
- [ ] Add `*Service` layer for every feature, injecting the corresponding `*Repository`. Routes should only interact with services.
- [ ] `ProfileService` currently wraps `ProfileRepositoryImpl` but delegates all calls directly — merge or justify separation.

### 12.3 Extract CheckoutOrchestrator Service 🟡

- [ ] Phase 1.2 carry-over: Move checkout logic from `CheckoutRoutes.kt` into a dedicated `CheckoutOrchestrator` or `CheckoutService`
- [ ] Move `ShippingAddressRepository` and `ShippingMethodRepository` usage out of `CheckoutRoutes.kt` into their own dedicated route files matching the project pattern

### 12.4 Koin Module Boilerplate Reduction 🟡

- [ ] 22+ repetitive `single<XRepository> { XRepositoryImpl() }` lines. Introduce a Koin `module` helper or use Koin annotation-based injection.
- [ ] Alternatively, create an `installRepositories()` extension that bulk-registers via a list or reflection.

### 12.5 Centralize All Configurable Constants 🟡

- [ ] `MAX_LOGIN_ATTEMPTS=5`, `ACCOUNT_LOCKOUT_MINUTES=30`, `BCRYPT_COST=12`, `DEFAULT_TAX_PERCENTAGE=0.05` are hardcoded in `AppConstants.kt` and repository implementations
- [ ] Move all business constants to `.env` / `DotEnvConfig.kt` — make them overridable without recompilation

### 12.6 Fix Remaining Code Quality Issues 🟡

- [ ] Phase 2.2 carry-over: Remove inline semicolons in `LoginAttempt.kt` `apply { }` block
- [ ] Remove commented-out test code in `ApplicationTest.kt`

---

## Phase 13: Security Hardening (Additions) 🟢

> Good baseline (BCrypt, OTP, lockout, magic bytes) but gaps remain beyond Phase 6.

### 13.1 Email Sending Rate Limit 🟢

- [x] No protection against OTP/password-reset email abuse. A bad actor can flood the SMTP server and exhaust email quota.
- [x] Implement per-email rate limiting (e.g., max 3 OTP emails per email per 15 min) checked before enqueuing to `AsyncWorker`
- [x] Track in DB or cache: `email_send_attempt` table or reuse existing `otp_attempt` tracking

### 13.2 File Upload Hardening 🟢

- [x] Phase 6.4 carry-over: Implement periodic cleanup of orphaned upload files (no longer referenced by any product/profile)
- [x] Add maximum storage quota per user/shop
- [x] Sanitize original filename before UUID rename (strip path separators, null bytes)

### 13.3 Replace AWT ImageIO with Container-Safe Processing 🟢

- [x] `ImageCompressor` uses `BufferedImage`/`ImageIO` (AWT) which is problematic in headless containers and can cause native memory leaks
- [x] Or add JVM flag `-Djava.awt.headless=true` and document requirement
- [ ] Replace with `ImageJ` / `Twelvemonkeys` / or delegate to an external service (Cloudinary, imgproxy) (future work)

---

## Phase 14: Production Resilience & Observability 🟢

> Complements Phase 5 (infrastructure). Adds resilience patterns not yet covered.

### 14.1 Transaction Retry with Exponential Backoff 🟢

- [x] Concurrent stock operaftions during checkout can race (optimistic locking failure)
- [x] Wrap `placeOrder` transaction in retry logic: 3 retries with 100ms/200ms/400ms backoff
- [x] Apply to any write transaction with concurrent access (stock, inventory, coupon usage)

### 14.2 Event Bus Dead Letter Queue 🟢

- [x] `EventBus` (SharedFlow) currently drops events if a subscriber throws. No retry mechanism.
- [x] Add subscriber error handling with configurable retry (3 attempts, exponential backoff)
- [x] Log permanently failed events to a dead letter channel for manual inspection
- [x] Add EventBus health metric: published vs consumed vs failed counts

### 14.3 API Versioning Strategy 🟢

- [x] Currently only `/api/v1/` — no deprecation/compatibility strategy for breaking changes
- [x] Add `@Deprecated` annotation and `Sunset` header mechanism for old versions
- [ ] Adopt `Accept` header versioning (`Accept: application/vnd.ecom.v2+json`) or URL prefix (`/api/v2/`) (architectural decision — deferred)

### 14.4 Distributed Cache for JWT Blacklist 🟢

- [x] Current `CacheService` uses in-memory `ConcurrentHashMap` — doesn't scale across instances
- [x] Document that Redis was intentionally omitted — add it back by implementing `Cache` with a Redis client
- [x] `CacheService` has a pluggable `Cache` interface (in-memory for dev, Redis for prod)

### 14.5 Structured Request Tracing 🟢

- [x] Phase 5.3 carry-over: Ensure X-Request-ID is implemented as a Ktor plugin that:
  - [x] Generates or forwards `X-Request-ID` header
  - [x] Injects into SLF4J MDC
  - [x] Included in all API error responses
  - [x] Passed to downstream subscribers in EventBus events

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
| 8 | Advanced Search & Discovery | 3/3 | 🟢 |
| 9 | Advanced Coupon Engine | 0/2 | 🟢 |
| 10 | Concurrency & Async | 3/3 | 🟢 |
| 11 | Schema Alignment & Data Integrity | 6/6 | 🟠 |
| 12 | Code Consistency & Boilerplate Reduction | 0/6 | 🟡 |
| 13 | Security Hardening (Additions) | 3/3 | 🟢 |
| 14 | Production Resilience & Observability | 5/5 | 🟢 |

**Overall**: 34 of 58 items completed (58%)

---

*Document version: 1.4 | Generated: 2026-07-11 | Project: ktor-ecom*

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
| 8 | ✅ Complete |
| 9 | ❌ Not started |
| 10 | ✅ Complete |
| 11 | ❌ Not started — schema mismatches will break production |
| 12 | ❌ Not started |
| 13 | ❌ Not started |
| 14 | ❌ Not started |
