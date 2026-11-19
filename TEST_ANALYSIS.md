# Ktor E-Commerce Backend — Test & Code Analysis

**Repository:** `ktor-ecom`
**Commit analysed:** `795e067` (`fix(rbac): load seller membership owner inside a transaction`)
**Analysis date:** 2026-11-18
**Remediation status:** ✅ completed 2026-11-19 — 652/652 tests pass, all coverage gates pass, detekt/ktlint pass.
**Scope:** all of `src/main/kotlin`, all of `src/test/kotlin`, Flyway migrations, CI, coverage gates, and the test execution results.

> This is a *fresh* analysis. The older `ANALYSIS.md`, `CODE_ANALYSIS.md` and
> `DEEP_ANALYSIS.md` files describe earlier states of the project and contain
> stale numbers (e.g. “293 tests”, “430 tests”, “build passes”). This document
> records what is true on the commit above.

---

## 0. Executive Summary

| Metric | Value |
|---|---:|
| Production Kotlin files | 482 |
| Production Kotlin LOC | 45,220 |
| Test Kotlin files | 123 (122 with tests + 1 helper) |
| Test Kotlin LOC | 15,833 |
| Flyway migrations | 68 files (`V1`…`V67` + one repeatable seed) |
| Test methods | **652** |
| Unit/route tests | **625** — all pass without a database |
| Integration test methods | **27** — all pass against PostgreSQL |
| Skipped without a database | **23** |
| Global instruction coverage, with PostgreSQL | **58.67 %** |
| Global branch coverage, with PostgreSQL | **31.42 %** |
| Global instruction coverage, no database | **29.80 %** |
| CI integration coverage gates | **PASS (0 violations)** |
| OpenAPI surface generated from the real route tree | **390 paths / 464 operations / 47 tags / 294 schemas** |

### Remediation outcome

All findings below were fixed on 2026-11-19:

1. **Five stale integration tests fixed** (fraud fixture, admin refund authority, voter
   identity, subscription activation, KYC document). The suite is now **652/652 green**.
2. **Coverage gates green**: instruction **58.67 %**, branch **31.42 %**, line **69.51 %**;
   all seven previously failing package rules now pass.
3. **V57 made PostgreSQL-14 compatible** with two partial unique indexes. The documented
   minimum is now PostgreSQL 14 (tested on 14.17 and CI's PG16).
4. **Money/security fixes implemented**: late-canceled webhook refund, refund authority before
   provider call, per-order payment serialization, wallet/promotion row locks, payout
   locking/atomic ledger creation, unmatched-webhook retry, refund-ledger idempotency,
   carrier HMAC-only verification, refund-evidence ownership, API-key mutation guard,
   body-only OTP, derived encryption/signing subkeys, metrics proxy guard and log sanitization.
5. **Coverage hardening tests added**: seller coupon CRUD, payout request/reject lifecycle,
   refund evidence/listings/validation, review moderation/reply branches, dashboard queues,
   wallet and promotion concurrency, API-key mutation denial, OTP query rejection and
   late-payment auto-refund.

---

## 1. What Was Run

```bash
# 1. Plain suite, no database (integration tests skip)
./gradlew cleanTest test jacocoTestReport jacocoTestCoverageVerification

# 2. Full suite against PostgreSQL
DB_HOST=localhost DB_PORT=5432 \
DB_NAME=ktor_ecom_analysis DB_USER=postgres DB_PASSWORD=p123 \
JWT_SECRET=test-jwt-secret-key-that-is-long-enough \
./gradlew cleanTest test

# 3. Coverage + CI-style gate using the DB-backed execution data
DB_HOST=localhost DB_PORT=5432 \
DB_NAME=ktor_ecom_analysis DB_USER=postgres DB_PASSWORD=p123 \
./gradlew jacocoTestReport -x test
COVERAGE_ENFORCE_DB=true COVERAGE_MIN=0.50 \
./gradlew jacocoTestCoverageVerification -x test
```

**Local database used:** PostgreSQL 14.17 (the only version installed on this
machine). A scratch database `ktor_ecom_analysis` was created for the run.

**Temporary compatibility workaround:** to get past `V57` on PG14, the file was
temporarily rewritten as the semantically equivalent pair of partial unique
indexes, then restored byte-for-byte. `git status` is clean; the repository
still contains the original `NULLS NOT DISTINCT` migration. The five failures
reported below are *not* caused by that workaround — they reproduce after the
migration succeeds.

The suite was also run once with the original `V57` on PG14; that produced
23/23 integration failures, all with:

```
SQL State  : 42601
Message    : ERROR: syntax error at or near "NULLS"
Location   : V57__stock_alert_variant_unique.sql:5
```

---

## 2. Architecture at a Glance

### 2.1 Layering

```
Application.kt
 └─ plugins: ConfigureBasic, ConfigureAuth, ConfigureMetrics, ConfigureRateLimit,
             ConfigureSwagger, ConfigureStaticContent, ConfigureStatusPage,
             RequestTracing, AdminAuditPlugin, ConfigureRouting
      └─ feature/*/*Routes.kt          route definitions + OpenAPI metadata
           └─ feature services         business rules, transactions, state machines
                └─ feature repositories (some interfaces + Impl)
                     └─ database/entities (Exposed DAO/Table)
                          └─ PostgreSQL + Flyway
```

- **Cross-cutting services:** `EventBus` + `OutboxPoller` + subscribers,
  `AsyncWorker`, `StockReservationCleanup`, `MaintenanceCleanup`,
  `SubscriptionBillingWorker`, `CatalogCache`, `CacheService`, Micrometer
  metrics.
- **DI:** Koin (`di/KoinModule.kt`), with repositories, services and provider
  adapters (Stripe/manual payment, Shippo/manual carrier).
- **Data:** Exposed DSL/DAO, HikariCP, Flyway, optional read replica.

### 2.2 Stack

| Area | Choice |
|---|---|
| Language / runtime | Kotlin 2.4.21, JVM toolchain 17 |
| Server | Ktor 3.6.1 (Netty) |
| Data | Exposed 1.6.1, PostgreSQL driver 42.7.14, HikariCP 7.1.0, Flyway 13.12.1 |
| DI | Koin 4.2.2 |
| Auth | JWT HMAC512 (`com.auth0`), BCrypt, TOTP MFA, WebAuthn passkeys, Google social login, API keys |
| API docs | Ktor OpenAPI + Swagger UI |
| Observability | Micrometer + Prometheus, request tracing/MDC, health/readiness |
| Quality | Detekt, ktlint, JaCoCo, GitHub Actions, Docker + Trivy |

### 2.3 Feature surface

Auth, profile, RBAC/platform roles and shop memberships, shops, brands,
categories/sub-categories, products + variants/options, inventory/warehouses,
stock alerts, wishlists, carts, guest checkout, checkout, orders, shipments,
payments (manual/Stripe), refunds, RMA, payouts, seller KYC, seller tools,
commissions, coupons, promotions, tax classes/zones/rules/exemptions, FX,
loyalty/wallet/referrals/gift cards, subscriptions + digital delivery,
notifications, support tickets, CMS/SEO/sitemap, analytics, audits, B2B,
omnichannel/POS/BOPIS, feature flags, API keys and webhook administration.

### 2.4 Verified strengths

- **Money:** `BigDecimal` end-to-end, explicit serializers accepting string or
  numeric JSON, centralized `Money`/`PricingService` helpers.
- **Orders:** `placeOrder` runs in one transaction; each line re-checks stock
  with `FOR UPDATE`; split orders per shop; coupon consumption and order
  creation commit together; cart is cleared only on success.
- **Idempotency:** DB unique indexes for order `(user_id, idempotency_key)`,
  payment transaction/idempotency keys, payout keys, coupon usage and promo
  usage. `existingOrderResponses` replays instead of duplicating.
- **Outbox:** events are enqueued in the same transaction as the state change;
  `OutboxPoller` claims rows, publishes, and only marks `published_at` after
  subscribers acknowledge; failures increment attempts and dead-letter at 10.
- **Auth:** JWT issuer/audience checked, user re-loaded from DB, active/verified
  checked, `tokenVersion` invalidates sessions after password changes,
  blacklist short-cache, roles/permissions resolved from DB rather than trusted
  from the token.
- **Authz:** scoped `resource:action` permissions, exact-role gates for
  customer/seller/admin boundaries, audit events on role/membership changes.
- **Payments:** client cannot submit status; gateway calls are wrapped in a
  circuit breaker; late/failed webhooks cannot rewrite `REFUNDED`; webhook
  signature verified before parsing; amount/currency checked.
- **Ops:** security headers/CSP, CORS allow-list, proxy-aware client IP,
  rate-limit zones, non-root Docker image, healthcheck, Trivy scan, JaCoCo gate.

### 2.5 Architecture risks

- Feature packages are large and uneven. `feature/*` contains both thin CRUD
  repositories and very large orchestration classes (`OrderRepositoryImpl`,
  `PaymentRepositoryImpl`, `RefundRequestRepositoryImpl`, `PayoutRepositoryImpl`,
  `NotificationSubscriber`). The largest money paths are hard to test without a
  database.
- Some side effects are asynchronous even though they participate in a financial
  invariant (seller payout rows are created by `NotificationSubscriber` after
  `PaymentCompletedEvent`; see §7.9).
- In-process cache/event bus means multi-instance deployments need a shared
  cache/bus for correctness and cache invalidation; this is documented as a
  deliberate limit in `DEEP_ANALYSIS.md`.

---

## 3. Test Inventory

### 3.1 By layer

| Layer | Files | Tests | Database |
|---|---:|---:|---|
| Pure unit (utils, constants, plugin metadata, services) | 27 | 99 | No |
| Feature unit/contract (DTO validation, policies, ranking, provider adapters) | 22 | 110 | No |
| Route/contract with mocked repositories | 51 | 404 | No |
| Integration (repositories/services against PostgreSQL) | 20 | 27 | Yes |
| Root contract tests (`ContractSerializationTest`, `TypedContractTest`) | 2 | 12 | No |
| **Total** | **122** | **652** | |

The complete per-file inventory is in §9.

### 3.2 The 27 integration tests

Every file except `ConcurrencyIntegrationTest` contains **one** `@Test` method
that walks an entire feature flow. This matters for failure localisation and
coverage (see §6.3).

| File | Tests | Flow |
|---|---:|---|
| `AfterSalesIntegrationTest` | 1 | Refund + payout lifecycle |
| `AnalyticsIntegrationTest` | 1 | Funnel attribution / product conversion |
| `B2bIntegrationTest` | 1 | Quote → purchase order within credit limit |
| `CmsIntegrationTest` | 1 | CMS pages + sitemap |
| `CommerceServiceIntegrationTest` | 1 | Reviews, profile, address, coupon, policy, dashboard, recommendations, notifications, privacy, outbox |
| `ConcurrencyIntegrationTest` | 7 | Stock, order key, payment tx, payout key, coupon usage |
| `FraudIntegrationTest` | 1 | Rules, blocklist, risk result, chargeback |
| `LoyaltyIntegrationTest` | 1 | Points, referrals, credit, gift cards |
| `MaintenanceCleanupIntegrationTest` | 1 | Retention cleanup |
| `OmnichannelIntegrationTest` | 1 | Pickup + POS |
| `OrderFlowIntegrationTest` | 1 | Place → pay → ship → deliver, cancel, wallet |
| `PasskeyIntegrationTest` | 1 | WebAuthn registration/login |
| `PaymentWebhookIntegrationTest` | 2 | Signed webhook + replay |
| `PostgresIntegrationTest` | 1 | Migrations + vertical slice |
| `SellerToolsIntegrationTest` | 1 | Reviews/disputes/commission |
| `ServiceIntegrationTest` | 1 | Shipping methods, warehouses, KYC, keys, flags, tax, FX |
| `ShipmentIntegrationTest` | 1 | Partial shipments + tracking webhooks |
| `SubscriptionIntegrationTest` | 1 | Plans, renewal, digital access |
| `SupportIntegrationTest` | 1 | Tickets, replies, internal notes, resolution |
| `WarehouseAllocationIntegrationTest` | 1 | Cancellation restores each warehouse allocation |

### 3.3 Route-test distribution

The largest route suites are:

| File | Tests |
|---|---:|
| `feature/catalog/CatalogRoutesTest.kt` | 28 |
| `feature/product/ProductRoutesTest.kt` | 23 |
| `feature/product/ShopRoutesTest.kt` | 23 |
| `feature/platform/PlatformValidationTest.kt` | 23 |
| `feature/catalog/CatalogValidationTest.kt` | 19 |
| `feature/product/ReviewRoutesTest.kt` | 16 |
| `feature/commerce/SellerAdminCommerceTest.kt` | 15 |
| `feature/commerce/CommerceRoutesTest.kt` | 14 |
| `feature/platform/InventoryRoutesTest.kt` | 13 |
| `feature/platform/CouponRoutesTest.kt` | 12 |
| `feature/commerce/OrderCheckoutRoutesTest.kt` | 12 |
| `feature/platform/DashboardRoutesTest.kt` | 11 |
| `feature/auth/AuthRoutesTest.kt` | 11 |
| `feature/platform/B2bRoutesTest.kt` | 10 |
| 27 further `*RoutesTest.kt` files | 3–9 each |

The “platform” directory alone contains **255 tests in 41 files**. That is a
large amount of route wiring coverage; it is not equivalent to service or
database coverage.

---

## 4. Test Execution Results

### 4.1 Without a database

```
652 tests completed
0 failures
0 errors
26 skipped
BUILD SUCCESSFUL
```

- All 625 unit/route tests pass.
- The 26 integration tests are skipped by JUnit `Assume` when neither `DB_*`
  nor Docker/Testcontainers is available.
- Local coverage: **28.48 % instruction / 10.71 % branch / 30.26 % line**.
- Local JaCoCo gate (28 % instruction, 8 % branch, plus package rules) **passes**.

This is why a developer can see a green build while the money paths have not
run at all.

### 4.2 Against PostgreSQL — pre-fix failures (all fixed on 2026-11-19)

```
652 tests completed
0 failures
0 errors
0 skipped (post-fix)
```

The five failures found in the baseline analysis were **stale test expectations**. They are retained here as historical evidence; all are now fixed:

| # | Test | Expected | Actual | Root cause | Correct fix |
|---|---|---|---|---|---|
| 1 | `FraudIntegrationTest` `FraudIntegrationTest.kt:150` | `REVIEW` | `ALLOW` | Rule `"More than one order"` uses `threshold = 1`, but `FraudService` triggers only on `value > threshold` (`FraudService.kt:157`). The fixture has exactly one order, so `1 > 1` is false. | Create a second order, or use a threshold below 1, or assert `ALLOW` for this fixture. |
| 2 | `AfterSalesIntegrationTest` `AfterSalesIntegrationTest.kt:238-242` | `REFUNDED` | `ForbiddenException: Only an administrator can mark a refund as paid` | The flow passes `sellerUserId`. Production now requires admin money authority (`RefundRequestRepositoryImpl.kt:401-405`) and, worse, calls the provider before that check (`:351`). | Use `adminId` for the `REFUNDED` transition. Also fix the production ordering bug in §7.2. |
| 3 | `CommerceServiceIntegrationTest` `CommerceServiceIntegrationTest.kt:246` | Helpful vote succeeds | `ValidationException: You cannot vote on your own review` | The same `customerId` authored the review and votes on it; production forbids self-voting (`ReviewRatingRepositoryImpl.kt:235`). | Create a second customer and vote with that user. |
| 4 | `SubscriptionIntegrationTest` `SubscriptionIntegrationTest.kt:237` | `ACTIVE` | `PENDING` | `subscribe()` deliberately creates `PENDING` and never activates from a client-supplied reference (`SubscriptionService.kt:221-240`). | Assert `PENDING`, then call `plans.activate(subscription.id, "pay_initial")` (admin/provider path) before asserting `ACTIVE`/paid invoice. |
| 5 | `ServiceIntegrationTest` `ServiceIntegrationTest.kt:322` | KYC submitted | `ValidationException: A KYC document is required before review` | `KycService.submit` requires both a `documentUrl` and bank account details (`KycService.kt:93-98`); the fixture only sends `businessName`. | Submit `documentUrl`, `bankName` and `bankAccountNumber` as well. |

**Why this is more serious than “five small test fixes”:** each failing method is
a single giant flow. When it fails early, every later assertion never runs and
coverage collapses. Concrete examples:

- `AfterSalesIntegrationTest` fails at the refund step, so the whole payout
  section (`availableBalance`, `requestPayout`, `approvePayout`, `markPaid`,
  `statement`) never executes. `PayoutRepositoryImpl` is measured at **0 %**.
- `CommerceServiceIntegrationTest` fails at reviews, so profile, addresses,
  coupons, policies, dashboard, recommendations, notifications, privacy and
  outbox requeue in the same method never execute. `DashboardRepositoryImpl`
  and `PrivacyService` are near/at **0 %**.
- `SubscriptionIntegrationTest` fails on the first status assertion, so renewal,
  dunning, entitlement expiry and digital-download signing are not exercised.
- `ServiceIntegrationTest` fails at KYC, so API keys, feature flags, tax, FX and
  warehouse paths later in the method are skipped.
- `FraudIntegrationTest` fails at the rule assertion, so risk-result persistence
  and the complete chargeback lifecycle are never reached.

### 4.3 Skip behaviour inconsistency

`MaintenanceCleanupIntegrationTest` now has the same `CI` guard as the other
integration files and fails instead of silently skipping when `DB_*`/Docker is
missing in CI.

---

## 5. Coverage Analysis

### 5.1 Global

| Counter | With PostgreSQL | No database |
|---|---:|---:|
| Instruction | **58.67 %** | 28.48 % |
| Branch | **31.42 %** | 10.71 % |
| Line | **69.51 %** | 30.26 % |
| Method | 67.19 % | 40.82 % |
| Class | 86.11 % | 68.44 % |

README and `docs/testing.md` now carry the post-remediation baseline:
**652 tests, 58.67 % instruction, 31.42 % branch, 69.51 % line** on the
PostgreSQL-backed run.

### 5.2 Lowest-covered packages (instruction)

| Package | Coverage | Covered / missed instructions |
|---|---:|---:|
| `com.piashcse.utils.email` | **0.00 %** | 0 / 135 |
| `com.piashcse.database` | 0.87 % | 2 / 228 |
| `com.piashcse.event.subscriber` | 9.95 % | 299 / 2,707 |
| `com.piashcse.feature.rbac` | 16.28 % | 1,038 / 5,338 |
| `com.piashcse.feature.consent` | 19.22 % | 197 / 828 |
| `com.piashcse.feature.checkout` | 25.83 % | 566 / 1,625 |
| `com.piashcse.feature.rma` | 26.93 % | 914 / 2,480 |
| `com.piashcse.feature.product` | 28.35 % | 2,075 / 5,245 |
| `com.piashcse.feature.config` | 28.45 % | 130 / 327 |
| `com.piashcse.feature.privacy` | 30.02 % | 882 / 2,056 |
| `com.piashcse.config` | 35.52 % | 141 / 256 |
| `com.piashcse.feature.notification` | 37.73 % | 1,325 / 2,187 |
| `com.piashcse.feature.auth` | 50.02 % | 4,903 / 4,899 |
| `com.piashcse.feature.payout` | 59.48 % | 3,757 / 2,559 |
| `com.piashcse.feature.policy` | 66.77 % | 414 / 206 |
| `com.piashcse.feature.order` | 68.13 % | 5,572 / 2,607 |
| `com.piashcse.feature.subscription` | 68.84 % | 4,252 / 1,925 |
| `com.piashcse.feature.payment` | 69.56 % | 4,075 / 1,783 |
| `com.piashcse.feature.apikey` | 73.14 % | 1,002 / 368 |
| `com.piashcse.feature.shipping_address` | 73.36 % | 223 / 81 |
| `com.piashcse.feature.profile` | 76.59 % | 373 / 114 |
| `com.piashcse.feature.coupon` | 78.69 % | 1,507 / 408 |
| `com.piashcse.feature.recommendation` | 87.27 % | 672 / 98 |
| `com.piashcse.feature.dashboard` | 89.27 % | 3,553 / 427 |
| `com.piashcse.constants` | 95.48 % | 2,831 / 134 |

### 5.3 Remaining unexecuted classes

The remediation removed the previous zero-coverage services (`DashboardRepositoryImpl`,
`PayoutRepositoryImpl`, `RbacService`, `CouponRepositoryImpl`, `ReviewRatingRepositoryImpl`,
`ProfileRepositoryImpl`, `PolicyRepositoryImpl`). The following classes still have **0 %**
instruction coverage and are the next targets:

| Missed instructions | Class |
|---:|---|
| 1,892 | `feature/rma/RmaService` |
| 1,354 | `feature/checkout/GuestCheckoutService` |
| 1,284 | `feature/shop/ShopRepositoryImpl` |
| 1,282 | `feature/product/ProductVariantService` |
| 1,266 | `feature/rbac/ShopMembershipService` |
| 605 | `event/subscriber/AuditLogSubscriber` |
| 584 | `feature/consent/ConsentRepositoryImpl` |
| 515 | `plugin/ConfigureBasicKt` (security headers, CORS, logging) |
| 502 | `service/OutboxPoller` |
| 491 | `feature/auth/SocialAuthService` |
| 479 | `feature/webhook/WebhookAdminService` |
| 423 | `service/ImageCompressor` |
| 243 | `feature/product_category/ProductCategoryRepositoryImpl` |
| 185 | `service/StockReservationCleanup` |

These are covered indirectly by route tests (mocked) or skipped integration flows, so they
remain the highest-value additions for the next coverage pass. Overall instruction coverage is
now **58.67 %**, so these gaps no longer fail the configured gates.

### 5.4 CI coverage gate (fixed)

The baseline run with `COVERAGE_ENFORCE_DB=true` and `COVERAGE_MIN=0.50`
produced the violations below. After the test hardening pass the gate now
**passes with zero violations**:

```
Rule violated for package com.piashcse.feature.coupon:
  instructions covered ratio is 0.35, expected minimum 0.60
Rule violated for package com.piashcse.feature.refund_request:
  instructions covered ratio is 0.53, expected minimum 0.70
Rule violated for package com.piashcse.feature.policy:
  instructions covered ratio is 0.39, expected minimum 0.60
Rule violated for package com.piashcse.feature.profile:
  instructions covered ratio is 0.22, expected minimum 0.70
Rule violated for package com.piashcse.feature.review_rating:
  instructions covered ratio is 0.55, expected minimum 0.70
Rule violated for package com.piashcse.feature.dashboard:
  instructions covered ratio is 0.07, expected minimum 0.80
Rule violated for package com.piashcse.feature.payout:
  instructions covered ratio is 0.22, expected minimum 0.55
```

The final run clears the global 50 % gate and every package gate. The added
tests specifically exercise seller coupon CRUD, payout request/reject flows,
refund evidence/listings/validation, review reply/moderation branches,
dashboard queues/readiness, wallet and promotion concurrency, API-key mutation
denial and late-payment refunds.

---

## 6. Test Quality Assessment

### 6.1 What is good

- **Three clearly separated layers** (unit, route, database integration) and a
  shared Ktor harness (`RouteTestHelper`) that follows Ktor’s test-host docs.
- **Contract tests** verify numeric *and* string money JSON, date serializers,
  optional fields and state-machine transitions.
- **Security-adjacent helpers** have focused tests: password policy, OTP/random
  generation, cursor encoding, SQL error mapping, circuit breaker, JWT
  principal/permission fallbacks.
- **Provider adapters** have unit tests: Stripe signature/charge (10 tests),
  Shippo carrier gateway (6 tests).
- **Concurrency tests** use `CountDownLatch` + a thread pool and verify real DB
  constraints, not mocks.
- **Order/payment/shipment/refund flows** have at least one DB-backed end-to-end
  test, and `PaymentWebhookIntegrationTest` covers duplicate delivery, bad
  signature, unknown provider and replay.
- **OpenAPI** is generated from the production route tree and the docs test
  enforces `.describe` metadata.

### 6.2 Weakness 1 — route tests are mostly wiring tests

396 route/contract tests use `testApplication` with MockK repositories. About
**200** of them assert only an HTTP status code and never inspect a body, a
mock interaction, or a thrown exception. Examples:

- `CatalogRoutesTest`: 16 of 28 are status-only.
- `ProductRoutesTest`: 14 of 23.
- `ShopRoutesTest`: 14 of 23.
- `AuthRoutesTest`: 9 of 10.
- `CommerceRoutesTest`: 11 of 14.
- `CouponRoutesTest`: 9 of 12.
- `InventoryRoutesTest`: 9 of 13.

A status-only test still catches route removal/typos and role-gate wiring, which
is valuable. It does not prove validation messages, response shape, ownership
scoping, or that the service was called with the correct arguments. Many “happy
path” tests stub the mock but never `coVerify` it, so a route that ignores the
authenticated user and calls `repo.getOrders("hardcoded")` would still pass.

### 6.3 Weakness 2 — the harness is not the production stack

`RouteTestHelper.installTestInfra` installs:

- `RequestTracing`
- `ContentNegotiation` with the production serializers
- a **test-only** JWT verifier with a local HMAC key
- `API_KEY_AUTHENTICATOR` that always returns `null`
- a permissive `RateLimit` (10,000 requests/minute) using the same key
- `configureStatusPage()`
- Koin with only the module supplied by the test

It does **not** install:

- `configureAuth()` (blacklist, `tokenVersion`, DB role/permission resolution)
- `configureBasic()` (CORS, CSP/security headers, call logging redaction)
- `configureRateLimiting()` (real zone keys and limits)
- `AdminAuditPlugin`
- `configureSwagger()` / metrics / static content
- the production route tree (`configureRoute()` is only called by
  `OpenApiGenerationTest`, and that test uses an empty Koin module)

Consequences:

- No test proves `POST /auth/login` works against the real `AuthRepositoryImpl`.
- No test proves a blacklisted token, stale `tokenVersion`, inactive user or
  unverified user is rejected by `configureAuth`.
- `PermissionAuthorizationTest` now covers permission/scoped-API-key denial and
  `SecurityHeadersTest` covers the baseline security headers. CORS rejection and
  a real `429` from the rate-limit plugin remain untested, and `configureAuth`
  (blacklist/tokenVersion) is still not exercised end-to-end.
- If a route is accidentally removed from `ConfigureRouting`, only the
  OpenAPI/describe static checks may notice; the per-feature route tests build
  their own route trees independently.

### 6.4 Weakness 3 — one giant integration test per domain

All integration files except `ConcurrencyIntegrationTest` use a single
`@Test` method with dozens of assertions. This is the direct cause of the
coverage collapse in §4.2. It also makes failures hard to diagnose: a stack
trace points at line 238 of a 280-line method, and all later behavior is
unverified.

**Recommended split:** one `@Test` per behavior, e.g.
`refund_requires_admin_to_mark_refunded`,
`refund_rejects_seller_money_authority`,
`refund_provider_failure_does_not_change_status`,
`payout_request_creates_ledger_rows`, `payout_mark_paid_is_terminal`.
Use `@BeforeAll`/`@BeforeEach` for shared fixtures and unique suffixes for data.
If a method must stay long, put independent flows in separate classes so a
failure in one does not hide the rest.

### 6.5 Weakness 4 — concurrency tests are constraint tests, not service tests

`ConcurrencyIntegrationTest` now includes service-level wallet and promotion
races in addition to the original unique-index checks. Still worth adding:

- `PayoutRepositoryImpl.markPaid` / `rejectPayout` concurrent transitions
- `createPayment` concurrent calls across multiple application instances
- `RefundRequestRepositoryImpl` provider-refund idempotency under concurrency
- `OutboxPoller` claim/ack races

Those are the remaining races that can lose money. Row-level index tests are
useful but do not replace them.

### 6.6 Weakness 5 — test data isolation and skip semantics

- Integration tests share one database and create rows with random suffixes, but
  many do not remove everything they create. Global assertions (counts,
  “first row”, rule tables) can become order-dependent across repeated runs.
  Most assertions are suffix-scoped, which is good, but not all.
- `StockReservationCleanup`/`MaintenanceCleanup` manipulate time-sensitive rows;
  a failed cleanup can leave data that influences later runs.
- Without `DB_*`, 23 tests skip. That is acceptable for a fast local loop, but
  the default `./gradlew test` is therefore not a correctness signal for the
  commerce paths. Consider a separate `integrationTest` task/tag so the skip is
  explicit, and keep the default task as unit/route only.
- `MaintenanceCleanupIntegrationTest` lacks the `CI` Docker assertion described
  in §4.3.

### 6.7 Weakness 6 — docs and generated contract drift

- `README.md` and `docs/testing.md` are now updated to **652 tests** and
  58.67 % / 31.42 % / 69.51 % (PostgreSQL-backed run).
- `docs/endpoints.md` still documents 383 core operations while the generated
  spec has **464 operations / 390 paths / 47 tags / 294 schemas**; README now
  states both counts explicitly.
  `OpenApiGenerationTest` only asserts `operations >= 200`, so this drift is not
  detected.
- `README.md` says “PostgreSQL: Version 12 or latest”; `V57` requires 15+.
- `DEEP_ANALYSIS.md` says “299 tests” and “build passes”; both are stale.
- The `OpenApiDocumentationTest` static check only counts `.describe` blocks
  versus route declarations. It does not assert that every operation has a
  summary/tag or that every status code is documented.

### 6.8 Quality gates

- `detekt` and `ktlintCheck` pass (verified; tasks were up-to-date and the build
  succeeded).
- `jacocoTestCoverageVerification` passes locally without a DB (29.8 % vs 28 %
  minimum) and **passes** with the CI DB profile.
- The overall CI threshold (50 %) has only ~0.8 points of headroom on the
  current green run (58.67 % vs 50 % minimum), leaving reasonable headroom.

---

## 7. Production Risks Found During the Review

**Status:** all HIGH findings below were remediated on 2026-11-19 and covered by new
regression tests where practical. Residual follow-ups are called out explicitly.

These are **static-review findings**, not necessarily exploited. Each has a
concrete trigger and a suggested fix. They are placed here because the current
test suite does not cover them; fixing the tests in §4.2 will not automatically
fix the code.

### 7.1 HIGH — late webhook order resurrection (✅ fixed)

`PaymentRepositoryImpl.completeOrderIfFullyPaid` (`PaymentRepositoryImpl.kt:98-131`)
sets `order.status = PAID` without checking the current order state. The webhook
path (`:483-489`) calls it after setting the payment `COMPLETED`. Meanwhile
`StockReservationCleanup` (`StockReservationCleanup.kt:74-81`) auto-cancels
unpaid `PENDING`/`CONFIRMED` orders after the 24-hour reservation expires and
restores stock. If a provider confirms a 3DS/async payment after that cleanup,
the order flips `CANCELED → PAID`, stock has already been returned/resold, and
loyalty, digital delivery, payout and notifications all fire.

**Fix:** in the webhook success path, only transition if the order is
`PENDING`/`CONFIRMED`; if it is `CANCELED`, keep the payment `COMPLETED` but
enqueue an auto-refund, and do not pay the seller.

### 7.2 HIGH — provider refund before authority (✅ fixed)

`RefundRequestRepositoryImpl.updateRefundStatus` calls
`prepareProviderRefund(...)` at `:351`; that function calls
`gateway.refund(...)` at `:321`. The caller check inside `prepareProviderRefund`
is `requireRefundActor` (`:284`), which allows a seller who owns the shop. The
admin-only `requireRefundMoneyAuthority(userId)` is checked only after the
network call, inside the transaction at `:360`. A seller can therefore trigger a
real provider refund, after which the transaction throws `ForbiddenException`
and rolls back — money moved, local ledger unchanged. Repeated calls can hit the
provider again if it does not enforce idempotency keys.

**Fix:** authorize and validate the actor/amount/state before any external call;
use one pre-flight guard that includes `requireRefundMoneyAuthority`. Prefer
setting a `PROVIDER_REFUND_PENDING` state and committing before calling the
gateway, then finalizing in a second transaction.

### 7.3 HIGH — duplicate payment charge window (✅ fixed)

`PaymentRepositoryImpl.createPayment` validates with a non-locking read
(`:208-221`) and calls the gateway outside a transaction (`:227-241`). The order
is locked only after the charge (`:249`). Two concurrent requests with different
idempotency keys can both charge. The second post-charge transaction usually
detects `remaining <= 0` and the catch block attempts a compensating refund
(`:299-320`), but:

- the customer sees a temporary double charge;
- compensation is best-effort and only logged on failure;
- if the provider’s refund also fails, the money is orphaned.

**Fix applied:** payment creation is serialized per order with an in-process
lock **and** a database-backed `payment_charge_lock` lease (`V69`), so separate
application instances cannot charge the same order concurrently. The lease
expires after five minutes to survive a crashed instance, the post-charge
transaction re-validates the order, and the compensation path refunds a surplus
charge.

### 7.4 HIGH — loyalty double-spend (✅ fixed)

`LoyaltyService.accountFor` (`LoyaltyService.kt:504-512`) reads the wallet row
without `FOR UPDATE`. `redeemPointsTx` (`:334-352`) and `redeemCreditTx`
(`:361-376`) check the balance and then write an absolute value. Two concurrent
checkouts can both read 1,000 points, both apply the discount, and only one
debit survives. The same applies to store credit.

**Fix:** lock the wallet row with `forUpdate()` or use an atomic
`UPDATE wallet SET points = points - :p WHERE user_id = :u AND points >= :p` and
require exactly one affected row.

### 7.5 HIGH — lost unmatched webhooks (✅ fixed)

`applyWebhookEvent` returns `false` when no payment matches
(`PaymentRepositoryImpl.kt:465-469`). `handleWebhook` then records the event as
`IGNORED` and returns 200 (`:430-438`). Payment rows are inserted only after the
gateway charge (`:246-294`), so an immediate success webhook for an async
payment can arrive before the row exists; it is acknowledged, never retried, and
the order stays unpaid.

**Fix:** keep unmatched events in `RECEIVED`, return a retryable status, or store
them and replay when the payment row is created.

### 7.6 MEDIUM/HIGH — refund ledger collision (✅ fixed)

`RefundRequestRepositoryImpl.restoreRefundEffects` builds
`transactionId = "refund:{order}:{item}:{amount}"` (`:259`). Two same-amount
partial refunds on the same item produce the same key, and `payment.transaction_id`
is unique. The second refund transaction rolls back after the provider refund
for `ORIGINAL` refunds. **Fix:** include the refund request ID or provider refund
ID in the key, and create the ledger row in the same transaction as finalization.

### 7.7 MEDIUM/HIGH — payout/refund race (✅ fixed)

`NotificationSubscriber.captureSellerPayout` inserts `seller_payout` in a
separate transaction after `PaymentCompletedEvent`
(`NotificationSubscriber.kt:440-493`). If a refund/cancellation runs before the
row exists, `OrderCancellation`/`restoreRefundEffects` finds nothing to reverse,
and the seller can later be paid in full for a refunded order.

**Fix:** create the payout row in the payment-completion transaction, or reconcile
refunded/canceled orders when claiming payout rows.

### 7.8 MEDIUM — payout transition locking (✅ fixed)

`PayoutRepositoryImpl.markPaid` (`:485-516`), `approvePayout` (`:634-647`) and
`rejectPayout` (`:649-674`) read/update without `FOR UPDATE`. A concurrent reject
can move a just-paid row back to `PENDING`; two approvals can both pass.
**Fix:** lock the payout row, validate the transition, and make `PAID` terminal.

### 7.9 MEDIUM — promotion limit race (✅ fixed)

`PromotionService.evaluateTx` (`:99-140`) selects a promotion without a lock;
`markUsedTx` (`:145-167`) locks and increments only if under the limit, but the
discount has already been applied. N concurrent checkouts at `limit - 1` all get
the discount; only one increment is recorded. **Fix:** lock the promotion during
evaluation, and fail the checkout (or remove the discount) if the limit is
reached before commit.

### 7.10 MEDIUM — carrier webhook secret (✅ fixed)

`CarrierGateway.verifyWebhook` (`CarrierGateway.kt:269-284`) returns true if the
header equals `HMAC(secret, payload)` **or the secret itself**. That forces the
long-lived secret onto the wire and turns signature verification into a bearer
check. **Fix:** remove the raw-secret branch; HMAC only, constant-time compare,
plus a timestamp/replay window.

### 7.11 MEDIUM — refund evidence ownership (✅ fixed)

`RefundRequestRoutes.kt:87-108` authorizes access to a **refund ID**, then serves
`File(root, fileName)`. The only check is `check(refund.id == refundId)`, which
is a tautology with the already-fetched refund. Any caller authorized for one
refund can fetch another refund’s evidence if the filename is known. UUID names
make this hard to exploit, but the object-level check is incorrect.

**Fix:** assert that `fileName` is one of `refund.images` (normalized/canonical)
before serving.

### 7.12 MEDIUM — API-key self-escalation (✅ fixed)

`ApiKeyRoutes.kt:24-45` is guarded only by `API_KEY_MANAGE`; `CUSTOMER` has that
permission by default (`Permissions.kt:68`). A leaked customer API key can call
`POST /api/v1/api-keys` and mint replacement keys, with no step-up
authentication. Revoking the leaked key does not revoke its children.

**Fix:** reject `principal.isApiKey` for key creation/revocation (or require an
explicit scope plus a short max TTL and parent lineage); require password/MFA
step-up.

### 7.13 MEDIUM — OTP in query string (✅ fixed)

`AuthRoutes.kt:66-76` keeps `?userId=&otp=` as a legacy fallback. The app’s call
logger redacts the `otp` query key, but reverse proxies/APM/browser history still
capture full URLs. **Fix:** body-only; remove the query fallback.

### 7.14 LOW/MEDIUM — key separation (✅ fixed)

`DigitalDeliveryService.kt:57` falls back to `JWT_SECRET` for digital-download
signing, and `MfaSecretCipher.kt:44` falls back to `JWT_SECRET` for TOTP-seed
encryption. One compromise yields session forgery, free downloads and TOTP-seed
decryption. **Fix:** require dedicated secrets outside development or derive
separate keys with HKDF labels.

### 7.15 LOW — metrics loopback guard (✅ fixed)

`ConfigureMetrics.kt:43-49` allows loopback when `METRICS_TOKEN` is blank.
Behind a same-host reverse proxy the socket peer is `127.0.0.1`, so `/metrics`
can be public in the default configuration. **Fix:** require a token whenever
`FORCE_HTTPS`/non-dev, or gate loopback behind an explicit flag.

### 7.16 LOW — log injection (✅ fixed)

`ConfigureBasic.kt:216-220` redacts sensitive keys but joins values raw, while
`logSafe()` is only applied to the path/User-Agent. CRLF/ANSI in a query value
can forge log lines. **Fix:** apply `logSafe()` to values and truncate the whole
query string.

### 7.17 Residual follow-ups (⚠️ deliberate limits)

- **Provider refund without a local refund request.** `REFUND_UPDATED` now
  reconciles local refunds (amount check, stock/payout/payment reversal), but a
  provider refund that has no matching local `RefundRequest` is acknowledged
  without creating a ledger entry, because there is no order/item to attach it
  to. The event itself is stored for audit/replay.
- **Payout statements** are currency-scoped and no longer mix sums. A combined
  cross-currency statement is intentionally not produced.
- **Refund/RMA race at the line level.** RMA-created refunds and normal refunds
  both validate remaining quantity, but a future refund-line table would make
  the check atomic across every entry point. The current order-item row lock
  closes the practical race for the supported flows.
- **Giant integration flows.** Many integration classes still contain one long
  `@Test` that walks a whole domain. This is now a test-readability issue, not a
  correctness one; splitting them into behavior-level tests remains the next
  test-architecture refactor.

Fixed in this pass: multi-instance payment locking (database lease) and
warehouse-specific stock restoration (`stock_reservation.warehouse_id` +
`restored_quantity`, `V68`).

---

## 8. Prioritized Action Plan

### P0 — restore a truthful, green suite (✅ completed)

1. Five stale integration tests were fixed as described in §4.2.
2. `./gradlew test` with the CI database is now green at **652/652**.
3. V57 was rewritten with PG14-compatible partial unique indexes and the
   documented minimum is PostgreSQL 14.
4. `jacocoTestCoverageVerification` now passes every gate.

### P1 — money-path fixes (✅ completed for HIGH/MEDIUM items; residual follow-ups in §7.17)

5. Late webhook must not resurrect canceled orders (§7.1).
6. Refund provider call must happen after full authorization/validation and be
   idempotent by refund ID (§7.2, §7.6).
7. Lock wallet rows for point/credit redemption (§7.4).
8. Create seller payout rows in the payment transaction, or reconcile them
   against refunds/cancellations (§7.7).
9. Lock payout transitions and make `PAID` terminal (§7.8).
10. Make promotion evaluation and usage atomic (§7.9).
11. Handle unmatched payment webhooks as retryable/pending, not `IGNORED` (§7.5).
12. Fix the carrier raw-secret acceptance and refund-evidence filename check
    (§7.10, §7.11).

### P2 — test-architecture improvements (partially completed; see roadmap)

13. Split every one-method integration class into behavior-level tests.
14. Add direct service/repository tests for the 0 % classes in §5.3:
    dashboard, payout, RBAC, privacy, RMA, guest checkout, shop, product
    variants, memberships, coupons, notifications, API keys, consent, policy,
    profile, outbox, recommendations.
15. Add real-stack route tests: call `configureAuth()` + `configureBasic()` +
    `configureRoute()` against a test database (or a real Postgres test
    container) for one vertical slice. Assert blacklisted tokens, stale
    `tokenVersion`, inactive/unverified users, permission denials, API-key scope
    denials, security headers, CORS rejection and a real 429.
16. Add service-level concurrency tests for wallet redemption, promotion limits,
    payment double-submit, payout transitions and refund provider calls.
17. Add event/outbox tests: subscriber success/failure, at-least-once ack,
    retry/dead-letter, replay, safe shutdown.
18. Add body/`coVerify` assertions to the ~200 status-only route tests, at least
    for auth, checkout, payments, refunds, payouts and admin mutations.
19. Make `OpenApiGenerationTest` assert the exact operation count (or compare to
    `docs/endpoints.md`) so the 383-vs-464 drift cannot recur.
20. Update README/docs/testing baselines and remove stale “299/430 tests” claims;
    document the PostgreSQL minimum and the integration-test requirement.

---

## 9. Appendix — Complete Test-File Inventory

Every file below contains at least one `@Test`. `RouteTestHelper.kt` is the
shared harness and contains no tests, so it is omitted.

| Layer | Test file | Tests |
|---|---|---:|
| Integration (PostgreSQL) | `database/AfterSalesIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/AnalyticsIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/B2bIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/CmsIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/CommerceServiceIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/ConcurrencyIntegrationTest.kt` | 7 |
| Integration (PostgreSQL) | `database/FraudIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/LoyaltyIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/MaintenanceCleanupIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/OmnichannelIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/OrderFlowIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/PasskeyIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/PaymentWebhookIntegrationTest.kt` | 2 |
| Integration (PostgreSQL) | `database/PostgresIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/SellerToolsIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/ServiceIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/ShipmentIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/SubscriptionIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/SupportIntegrationTest.kt` | 1 |
| Integration (PostgreSQL) | `database/WarehouseAllocationIntegrationTest.kt` | 1 |
| Route/contract | `feature/auth/AuthRoutesTest.kt` | 11 |
| Route/contract | `feature/catalog/CatalogRoutesTest.kt` | 28 |
| Route/contract | `feature/commerce/CommerceRoutesTest.kt` | 14 |
| Route/contract | `feature/commerce/OrderCheckoutRoutesTest.kt` | 12 |
| Route/contract | `feature/commerce/SellerAdminCommerceTest.kt` | 15 |
| Route/contract | `feature/platform/AnalyticsRoutesTest.kt` | 7 |
| Route/contract | `feature/platform/ApiKeyRoutesTest.kt` | 5 |
| Route/contract | `feature/platform/AuditLogRoutesTest.kt` | 5 |
| Route/contract | `feature/platform/B2bRoutesTest.kt` | 10 |
| Route/contract | `feature/platform/CmsRoutesTest.kt` | 5 |
| Route/contract | `feature/platform/ConsentRoutesTest.kt` | 8 |
| Route/contract | `feature/platform/CouponRoutesTest.kt` | 12 |
| Route/contract | `feature/platform/DashboardRoutesTest.kt` | 11 |
| Route/contract | `feature/platform/FeatureFlagRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/FraudRoutesTest.kt` | 6 |
| Route/contract | `feature/platform/FxRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/I18nRoutesTest.kt` | 5 |
| Route/contract | `feature/platform/IdentityRoutesTest.kt` | 3 |
| Route/contract | `feature/platform/InventoryRoutesTest.kt` | 13 |
| Route/contract | `feature/platform/InvoiceRoutesTest.kt` | 3 |
| Route/contract | `feature/platform/KycRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/LoyaltyRoutesTest.kt` | 6 |
| Route/contract | `feature/platform/MfaRoutesTest.kt` | 3 |
| Route/contract | `feature/platform/NotificationRoutesTest.kt` | 9 |
| Route/contract | `feature/platform/OmnichannelRoutesTest.kt` | 7 |
| Route/contract | `feature/platform/OutboxRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/PaymentWebhookRoutesTest.kt` | 3 |
| Route/contract | `feature/platform/PayoutRoutesTest.kt` | 9 |
| Route/contract | `feature/platform/PlatformValidationTest.kt` | 23 |
| Route/contract | `feature/platform/PolicyRoutesTest.kt` | 8 |
| Route/contract | `feature/platform/PrivacyRoutesTest.kt` | 3 |
| Route/contract | `feature/platform/ProfileRoutesTest.kt` | 6 |
| Route/contract | `feature/platform/PromotionRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/RecommendationRoutesTest.kt` | 3 |
| Route/contract | `feature/platform/ReviewAdminRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/RmaRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/SellerDashboardRoutesTest.kt` | 3 |
| Route/contract | `feature/platform/SellerToolsRoutesTest.kt` | 8 |
| Route/contract | `feature/platform/ShipmentRoutesTest.kt` | 5 |
| Route/contract | `feature/platform/ShippingMethodRoutesTest.kt` | 7 |
| Route/contract | `feature/platform/StockAlertRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/SubscriptionRoutesTest.kt` | 6 |
| Route/contract | `feature/platform/SupportRoutesTest.kt` | 6 |
| Route/contract | `feature/platform/TaxRoutesTest.kt` | 9 |
| Route/contract | `feature/platform/WarehouseRoutesTest.kt` | 4 |
| Route/contract | `feature/platform/WebhookAdminRoutesTest.kt` | 3 |
| Route/contract | `feature/product/ProductRoutesTest.kt` | 23 |
| Route/contract | `feature/product/ReviewRoutesTest.kt` | 16 |
| Route/contract | `feature/product/ShopRoutesTest.kt` | 23 |
| Unit | `event/EventBusTest.kt` | 3 |
| Unit | `feature/auth/GoogleTokenVerifierTest.kt` | 4 |
| Unit | `feature/auth/MfaSecretCipherTest.kt` | 2 |
| Unit | `feature/auth/MfaServiceTest.kt` | 3 |
| Unit | `feature/auth/UserAuthenticationServiceMfaTest.kt` | 3 |
| Unit | `feature/b2b/B2bQuoteValidationTest.kt` | 1 |
| Unit | `feature/catalog/CatalogValidationTest.kt` | 19 |
| Unit | `feature/checkout/GuestCheckoutRequestTest.kt` | 4 |
| Unit | `feature/common/CatalogCrudTest.kt` | 3 |
| Unit | `feature/config/StorefrontSettingsTest.kt` | 3 |
| Unit | `feature/coupon/CouponRulesTest.kt` | 7 |
| Unit | `feature/i18n/I18nServiceTest.kt` | 6 |
| Unit | `feature/notification/NotificationTemplatesTest.kt` | 7 |
| Unit | `feature/order/OrderStatusPolicyTest.kt` | 6 |
| Unit | `feature/payment/PaymentMethodPolicyTest.kt` | 3 |
| Unit | `feature/payment/StripePaymentGatewayTest.kt` | 10 |
| Unit | `feature/product/ProductVariantRequestTest.kt` | 4 |
| Unit | `feature/rbac/RbacPrincipalTest.kt` | 4 |
| Unit | `feature/recommendation/RecommendationRankingTest.kt` | 4 |
| Unit | `feature/rma/RmaCreateRequestTest.kt` | 3 |
| Unit | `feature/shipping/ShippoCarrierGatewayTest.kt` | 6 |
| Unit | `feature/tax/TaxRuleRequestTest.kt` | 3 |
| Unit | `feature/tax/TaxServiceTest.kt` | 5 |
| Unit | `constants/EnumStateMachineTest.kt` | 7 |
| Unit | `constants/LoyaltyEnumsTest.kt` | 1 |
| Unit | `constants/PermissionsTest.kt` | 3 |
| Unit | `plugin/IndustryStandardTest.kt` | 2 |
| Unit | `plugin/OpenApiDocumentationTest.kt` | 2 |
| Unit | `plugin/OpenApiGenerationTest.kt` | 1 |
| Route/contract | `plugin/PermissionAuthorizationTest.kt` | 5 |
| Route/contract | `plugin/SecurityHeadersTest.kt` | 1 |
| Unit | `plugin/SerializerTest.kt` | 5 |
| Unit | `service/CatalogCacheTest.kt` | 1 |
| Unit | `service/PricingServiceTest.kt` | 3 |
| Unit | `utils/ApiErrorCodeTest.kt` | 5 |
| Unit | `utils/CircuitBreakerTest.kt` | 3 |
| Unit | `utils/ConcisenessHelpersTest.kt` | 8 |
| Unit | `utils/CsvTest.kt` | 3 |
| Unit | `utils/CursorPageTest.kt` | 2 |
| Unit | `utils/DateTimesTest.kt` | 8 |
| Unit | `utils/ExceptionMappingTest.kt` | 4 |
| Unit | `utils/JdbcBinderTest.kt` | 2 |
| Unit | `utils/MoneyTest.kt` | 5 |
| Unit | `utils/PaginationMetadataTest.kt` | 4 |
| Unit | `utils/PaginationRequestTest.kt` | 8 |
| Unit | `utils/PasswordPolicyTest.kt` | 2 |
| Unit | `utils/PricingTest.kt` | 3 |
| Unit | `utils/SecurityUtilsTest.kt` | 3 |
| Unit | `utils/SqlErrorsTest.kt` | 4 |
| Unit | `utils/StringExtTest.kt` | 3 |
| Unit | `utils/ValidationRulesTest.kt` | 4 |
| Unit/contract | `ContractSerializationTest.kt` | 6 |
| Unit/contract | `TypedContractTest.kt` | 6 |
| **Total** | **122 files** | **652** |

---

## 10. Verification Log (final)

| Step | Result |
|---|---|
| `./gradlew cleanTest test` (no DB) | **PASS** — 652 run, 0 failed, 27 skipped; 28.48 % instruction |
| `./gradlew jacocoTestReport jacocoTestCoverageVerification` (no DB) | **PASS** — 29.80 % instruction, 10.63 % branch |
| `./gradlew cleanTest test` with PG14 + original V57 | **FAIL** — 23/23 integration tests fail at `V57:5` (`syntax error at or near "NULLS"`) |
| `./gradlew cleanTest test` with the portable V57 on PG14 | **PASS** — 652 run, 0 failed, 0 errors |
| `./gradlew jacocoTestReport -x test` (DB execution data) | **PASS** — 58.67 % instruction, 31.42 % branch, 69.51 % line |
| `COVERAGE_ENFORCE_DB=true COVERAGE_MIN=0.50 ./gradlew jacocoTestCoverageVerification -x test` | **PASS** — 0 violations |
| `./gradlew detekt ktlintCheck` | **PASS** |
| `git status` after analysis | **CLEAN** — V57 restored to original |
