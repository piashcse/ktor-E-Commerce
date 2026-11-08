# Ktor E-Commerce — Deep Analysis & Remediation Tracker

> Living document. Each item is marked `[x]` when the fix is implemented and the project compiles/tests pass.
> `[~]` means the in-repo scaffold/guard is implemented but a third-party provider or deployment config is still required.
> `[ ]` means still open.

**Scope:** full `src/main/kotlin` tree (~15.6k LOC), Flyway migrations `V1..V20`, tests, docs, CI, Docker.
**Baseline:** `./gradlew compileKotlin` passes on commit `c5b4401` (`feature-improvement`).

---

## 0. Executive Summary

The project has a solid skeleton (clean package layout, BigDecimal money, JWT, Flyway, an outbox attempt, 293 tests), but the money and fulfillment paths are not production-safe. The highest-risk areas are:

1. Client-controlled payment status (free orders).
2. Seller broken access control on inventory and notifications.
3. Registration account pre-hijacking.
4. Half-implemented multi-currency.
5. Coupon/refund/payout lifecycle leaks and races.
6. Outbox that marks events published before consumers process them.
7. Broken OTP migration (`V12`).
8. API contract drift (numeric vs string money, optional fields, docs).

---

## 1. P0 — Security & Money (must fix)

- [x] **P0-1. Remove client-controlled payment completion.** `PaymentRequest.status` is trusted; matching amount completes the order. Payment gateway is never called.
  - `feature/payment/PaymentRepositoryImpl.kt:80-118`, `model/request/PaymentRequest.kt:14-19`, `PaymentGateway.kt:30-42`
  - Fix: server-owned status, call gateway, explicit confirmation/admin webhook.
- [x] **P0-2. Reject payment on non-payable orders.** Canceled/refunded/delivered orders can be paid and reset to `CONFIRMED`.
  - `PaymentRepositoryImpl.kt:53-118`
- [x] **P0-3. Fix registration account pre-hijacking.** Re-registering an unverified email keeps the attacker's password.
  - `AuthRepositoryImpl.kt:176-211`
- [x] **P0-4. Inventory IDOR.** Any seller can read/change any product's stock.
  - `InventoryRoutes.kt:17-63`, `InventoryRepositoryImpl.kt:25-122`
- [x] **P0-5. Notification IDOR / preference overwrite.** Any seller can notify any user and write `NOTIFICATION_PREFS`.
  - `NotificationRoutes.kt:67-71`, `NotificationRepositoryImpl.kt:96-145`
- [x] **P0-6. Rate-limit bypass via spoofed IP.** `X-Real-IP` trusted unconditionally; `TRUST_PROXY` ignores `.env`.
  - `utils/extension/CallExt.kt:21-30`, `ConfigureRateLimit.kt:54-61`
- [x] **P0-7. Make refund evidence private.** Static `/uploads` serves `refund-images` publicly.
  - `StaticContent.kt:34-46`, `UploadService.kt:29-33`
- [x] **P0-8. Fix OTP migration.** `V12` drops a non-existent index; the old `otp_attempt(user_id)` unique index survives.
  - `V4__create_otp_attempt_table.sql:11`, `V12__otp_attempt_purpose.sql:4-11`
- [x] **P0-9. Bound upload reads.** Full multipart body is read into memory before the size check.
  - `UploadService.kt:120-133,177-184`
- [x] **P0-10. Harden OTP/reset endpoints.** OTP in query string; reset-password leaks unknown emails; password change/reset does not revoke sessions.
  - `AuthRoutes.kt:63-92`, `AuthRepositoryImpl.kt:252-332`

---

## 2. P1 — Commerce Correctness

- [x] **P1-1. Fix order state machine.** Add `SHIPPED`; payment should set `PAID`; seller can progress `PAID → SHIPPED → DELIVERED`.
  - `constants/Enums.kt:60-87`, `PaymentRepositoryImpl.kt:104`, `OrderRoutes.kt:47-51`
- [x] **P1-2. Complete multi-currency consistently.** Store order FX rate; convert items; summary applies currency; coupons/refunds/payouts carry currency; dashboard converts to USD.
  - `V11__fx_rates.sql`, `OrderRepositoryImpl.kt:254-299,384-409`, `RefundRequestRepositoryImpl.kt:157-192`, `NotificationSubscriber.kt:124-131`, `DashboardRepositoryImpl.kt:27-34`
- [x] **P1-3. Fix coupon lifecycle.** Reverse usage on cancel/cleanup/refund; normalize codes; per-user limit; cascade on delete; cap both discount types; reject `usageLimit < usageCount`.
  - `OrderRepositoryImpl.kt:144-160,303-318,608-636`, `CouponRepositoryImpl.kt:46-56,97-141`, `V8`
- [x] **P1-4. Fix refund math and reversal.** Net-of-discount cap, currency, inventory/sales/payout/payment reversal, state machine holes, eligibility, duplicate race.
  - `RefundRequestRepositoryImpl.kt:48-75,149-247`
- [x] **P1-5. Fix payout lifecycle.** Per-seller idempotency, partial claims, locking, status enum, reject releases funds, require approval before payment, block suspended sellers, currency, net commission.
  - `PayoutRepositoryImpl.kt:100-177`, `V17`
- [x] **P1-6. Make reservation cleanup safe.** Row locks, shared cancellation routine, UTC, only release canceled rows, reverse sales/seller/coupon.
  - `StockReservationCleanup.kt:35-69`
- [x] **P1-7. Make outbox consumer-acknowledged.** Ack after subscribers, attempts cap, ordered claim, idempotent start, safe shutdown, no publish without subscribers.
  - `OutboxPoller.kt:41-83`, `EventBus.kt:39-169`, `Application.kt:52-59`
- [x] **P1-8. Stop non-approved shops leaking into catalog.** Public shop filters + product/shop join + guarded status transitions + seller status sync.
  - `ShopRepositoryImpl.kt:92-212`, `ProductRepositoryImpl.kt:183-206`
- [x] **P1-9. Fix dashboard analytics.** Paid-only revenue, USD normalization, exclude canceled items, exclude soft-deleted/deactivated, bound limits.
  - `DashboardRepositoryImpl.kt:27-34,185-190,306-331`
- [x] **P1-10. Fix review integrity.** Verified purchase by paid/delivered order, product existence, status filtering, helpful dedupe, SQL aggregation.
  - `ReviewRatingRepositoryImpl.kt:28-111`
- [x] **P1-11. Fix cache.** Bound memory cache, use default TTL, invalidate product cache on order/review/inventory changes.
  - `CacheService.kt:33-63`, `ProductCatalogService.kt:14-23`
- [x] **P1-12. Fix idempotency key scoping.** Per-user order keys, length validation, separate payment idempotency key from `transactionId`.
  - `Order.kt:17`, `V9`, `OrderRepositoryImpl.kt:175-184`, `PaymentRoutes.kt:26-30`
- [x] **P1-13. Remove `itemsForOrder` 50-item truncation.**
  - `OrderItem.kt:27`
- [x] **P1-14. Fix product write validation.** Non-negative stock, discount < price, honor/clear status, don't allow unsellable null-shop products.
  - `ProductRepositoryImpl.kt:108-181`, `ProductRequest.kt`, `UpdateProductRequest.kt`
- [x] **P1-15. Fix wishlist/cart correctness.** Reject `quantity <= 0`, don't lose wishlist row, merge duplicates, consistent `itemCount`.
  - `WishListRepositoryImpl.kt:65-88`, `CartRepositoryImpl.kt:143-189`

---

## 3. P2 — Data, Contract & Operations

- [x] **P2-1. Serialization contract.** Accept numeric and string `BigDecimal`; register `LocalDate` serializer; default optional nullable fields; reject silent enum/currency coercion.
  - `ConfigureBasic.kt:44-58,120-134`, request DTOs, `UserProfileResponse.kt:24`
- [x] **P2-2. Standardize HTTP statuses/errors.** 403 for authorization, 400/422 for validation, 404 JSON, no 200 for failed OTP.
  - `PaymentRepositoryImpl.kt`, `RefundRequestRepositoryImpl.kt`, `OrderRoutes.kt`, `AuthRoutes.kt`, `CouponRoutes.kt`
- [x] **P2-3. Standardize delete responses/pagination.** One delete convention; consistent pagination metadata.
- [x] **P2-4. Consent history.** Append instead of overwrite; unique `(user_id, policy_id)`; UTC; only active/effective policies.
  - `ConsentRepositoryImpl.kt:35-123`
- [x] **P2-5. Policy lifecycle.** Respect `effectiveDate`, only one active policy per type, flexible date parsing, ordered latest.
  - `PolicyRepositoryImpl.kt:21-78`
- [x] **P2-6. Config hardening.** `DB_NAME` default, process env precedence, configurable Hikari, fail-fast.
  - `DotEnvConfig.kt:6`, `DotEnv.kt:15-25`, `ConfigureDataBase.kt`
- [x] **P2-7. Logging/tracing.** `yyyy` not `YYYY`, INFO in prod, MDC propagation, no log injection, structured output.
  - `logback.xml:4,12`, `RequestTracing.kt`
- [x] **P2-8. DB integrity.** Missing indexes/unique constraints/checks; seller status default; coupon usage cascade; per-user limits.
  - New `V18__security_payment_hardening.sql` + `V19__commerce_hardening.sql`
- [x] **P2-9. Outbox lifecycle.** `attempts` cap, dead-letter table/endpoint, cleanup job.
- [x] **P2-10. Async/image safety.** Bounded queue, restart guard, temp-file atomic image rewrite, WebP signature, dimension guard.
  - `AsyncWorker.kt`, `UploadService.kt`, `ImageCompressor.kt`
- [x] **P2-11. CI/Docker.** Build artifact in CI, non-root image, healthcheck, uploads volume, `.dockerignore`, no weak defaults.
  - `.github/workflows/ci.yml`, `Dockerfile`, `docker-compose.yml`
- [x] **P2-12. Docs/OpenAPI sync.** Remove false `/openapi`, v2, public-shop claims; document payout/notification; add Bearer security; remove global deprecation header.
  - `README.md`, `docs/**`, `ConfigureSwagger.kt`, `ConfigureRouting.kt:68-75`
- [x] **P2-13. Tests.** Real Postgres test runs against the CI `DB_*` service (V1..V21 + slice), serialization contract tests exist, and DB-backed concurrency tests cover stock, order idempotency, payment transaction IDs, payout keys and coupon usage.
  - `PostgresIntegrationTest.kt:39-51`, `RouteTestHelper.kt:30`

---

## 4. Feature Incompleteness (implemented as safe scaffolding where external providers are required)

- [x] **F-1. Payment provider framework.** Gateway interface, COD flow, explicit admin confirmation/webhook, refund transaction linkage.
- [~] **F-2. Real payment provider integration** (Stripe/bKash/SSLCommerz) — adapter interface ready; credentials/webhook secret required.
- [x] **F-3. Shipment state + tracking.** `SHIPPED` status, shipping date, tracking number endpoint.
- [x] **F-4. Partial/quantity refunds + shipping/tax refund fields.**
- [x] **F-5. Coupon per-user limit, scoping, usage reversal, admin filters.**
- [x] **F-6. Payout request aggregate/status/balance endpoint.**
- [x] **F-7. Product variants/attributes** — variants, option/value dimensions, variant-level price/SKU/stock, V28.
- [x] **F-8. Category/brand/shop image write path** — currently columns exist but unused.
- [x] **F-9. Notification preferences honored by subscriber.**
- [x] **F-10. Cleanup jobs** for outbox, tokens, attempts, notifications.
- [x] **F-11. FX rate admin API + staleness check.**
- [x] **F-12. Consent enforcement** before checkout/registration for required policies.

---

## 5. Feature Inconsistency

- [x] **I-1. Response envelope:** code returns raw DTOs; README says raw but examples wrap.
- [x] **I-2. Money representation:** string vs numeric vs `Double`; docs numeric.
- [x] **I-3. Date/time:** mixed timezone and string/object output.
- [x] **I-4. Pagination params:** `limit/offset` vs `page/perPage`; some raw lists.
- [x] **I-5. Catalog input style:** body vs query for create/update.
- [x] **I-6. Auth docs:** OTP body vs query; admin registration; README passwords.
- [x] **I-7. Public vs private routes:** `/shops/public` authenticated; brand vs category inconsistency.
- [x] **I-8. Order status docs vs `OrderStatus` (no `SHIPPED`).**
- [x] **I-9. Currency units across summary/order/refund/payout/dashboard.**
- [x] **I-10. Inventory/product status not synchronized.**
- [x] **I-11. Seller/shop status divergence.**
- [x] **I-12. Delete response shapes (Boolean/String/Message/object).**
- [x] **I-13. Error body shapes (ApiError/MessageResponse/plain text).**
- [x] **I-14. Global `Deprecation` header on the only API version.**

---

## 6. Enhancements (post-fix roadmap)

- [ ] **E-1. Real gateway webhooks + signature verification + 3-D Secure.**
- [ ] **E-2. Carrier integration, labels, tracking webhooks, delivery slots.**
- [~] **E-3. Product variants/options, attributes, bundles, recommendations.** — variants/options/attributes (V28) and recommendations done; product bundles not in this pass.
- [x] **E-4. Multi-warehouse inventory, backorders, supplier POs, stock audit.**
- [x] **E-5. Tax rules by region/category; promotions engine (stacking/scopes).**
- [x] **E-6. Seller KYC, commission tiers, statements, tax exports.**
- [x] **E-7. RMA/return portal, exchanges, partial refunds by quantity.**
- [x] **E-8. Guest checkout, saved payment methods, order tracking page.**
- [x] **E-9. Notification templates, email/SMS/push providers, preferences.**
- [x] **E-10. GDPR export/erasure, consent history UI, audit export.**
- [x] **E-11. Admin RBAC granular permissions, feature flags, API keys.**
- [x] **E-12. Cursor pagination, read replicas, query plan/index review.**
- [x] **E-13. Circuit breakers, retries with jitter, DLQ admin UI.**
- [x] **E-14. Observability SLOs: outbox lag, payment/refund/payout failure metrics.**
- [x] **E-15. Full-text search adoption (`search_vector` trigger exists but unused).**

---

## 9. Branch / PR Plan

| PR | Branch | Base | Scope |
|---|---|---|---|
| 1 | `fix/01-security-payment-hardening` | `master` | Payments, auth, inventory/notification authorization, upload/static, V18 |
| 2 | `fix/02-commerce-correctness` | `fix/01-security-payment-hardening` | Fulfillment, FX/currency, coupons, refunds, payouts, reviews, V19 |
| 3 | `fix/03-platform-observability-docs` | `fix/02-commerce-correctness` | Outbox, cache/queue, cleanup jobs, config, Docker/CI, docs |
| 4 | `fix/04-remaining-hardening` | `fix/03-platform-observability-docs` | DLQ visibility, tracking number, quantity/shipping/tax refunds, UTC timestamps, public catalog, BigDecimal product prices, paginated lists, V20 |
| 5 | `fix/05-search-observability` | `fix/04-remaining-hardening` | Full-text search adoption + outbox/payment/refund/payout metrics + GDPR export/erasure |
| 6 | `fix/06-resilience` | `fix/05-search-observability` | Circuit breakers, jittered retry, DLQ summary/replay API |
| 7 | `fix/07-tax-rules` | `fix/06-resilience` | Country/category tax rules, admin API, per-item tax in checkout/orders |
| 8 | `test/concurrency-guards` | `fix/07-tax-rules` | DB-backed concurrency tests for stock, order/payment/payout/coupon guards |
| 9 | `fix/08-rbac-api-keys` | `test/concurrency-guards` | Permission scopes, API keys, feature flags, V22 |
| 10 | `fix/09-cursor-pagination` | `fix/08-rbac-api-keys` | Keyset pagination for orders/notifications, optional read-replica routing |
| 11 | `fix/10-guest-checkout` | `fix/09-cursor-pagination` | Guest checkout, guest account claiming, public order tracking, V23 |
| 12 | `fix/11-multi-warehouse` | `fix/10-guest-checkout` | Warehouses, per-warehouse stock, cross-warehouse allocation, V24 |
| 13 | `fix/12-rma-returns` | `fix/11-multi-warehouse` | RMA returns/exchanges, refund handoff, replacement-order linkage, V25 |
| 14 | `fix/13-seller-kyc-statements` | `fix/12-rma-returns` | Seller KYC lifecycle, payout gating, statements/CSV export, V26 |
| 15 | `fix/14-notification-templates` | `fix/13-seller-kyc-statements` | Templates, in-app/email/SMS/push dispatch, per-event preferences, RMA events, V27 |
| 16 | `fix/15-product-variants` | `fix/14-notification-templates` | Variants, options/values, variant-level price/SKU/stock, V28 |
| 17 | `fix/16-recommendations` | `fix/15-product-variants` | Related, co-purchase, personalized and best-seller recommendations |
| 18 | `refactor/17-conciseness-performance` | `fix/16-recommendations` | Detekt clean, decomposition of order/refund flows, bulk loaders, cache/query centralization |
| 19 | `docs/18-swagger-docs` | `refactor/17-conciseness-performance` | Swagger tags/summaries/DTO schemas/bearer auth, missing docs pages, fresh-DB Flyway fix |
| 20 | `refactor/19-industry-standards` | `docs/18-swagger-docs` | UTC time centralization, path-param helper consistency, version-catalog naming, repo hygiene |

Stacked PRs are intentional. Merge in order, then retarget the next PR to `master`.

---

## 10. Code Quality / Performance Pass

Scope: full `src/main` review for conciseness, reuse, centralization and query efficiency.

**Results**
- Detekt findings: `25 → 0` (excluding intentional `@Suppress` on the auth facade).
- `OrderRepositoryImpl.placeOrder` decomposed into `loadCheckoutContext`,
  `createOrders`/`createOrderForShop`, `resolveCheckoutVariant`,
  `applyCoupon`, `updateSellerTotals` and `finalizePlacedOrders`.
- `RefundRequestRepositoryImpl.updateRefundStatus` split into actor, amount
  validation and refund-finalization helpers with no duplicate order lookup.
- Removed per-row N+1 lookups:
  - `variantResponses`/`optionResponses` bulk-load variant options/values.
  - Cart list/summary batch-load variants and inventory; stock checks no longer
    query per cart line.
  - Wishlist list preloads product images.
  - Admin KYC list batch-loads user emails.
- Centralized `CatalogCache.invalidate()` (products + recommendations) and
  replaced scattered patterns in order/inventory/review/product/variant writes.
- Added `ApplicationCall.queryInt`/`queryBoolean`; inventory/notification routes
  use them instead of ad-hoc parsing.
- Auth password hashing/verification and user-type parsing are single helpers.
- Removed fully-qualified exception/`Message` references in favour of imports.
- Existing DB indexes already cover order-item, refund, reservation, outbox and
  catalog lookups; no new migration needed.
- Recommendation/checkout summaries now use variant-effective prices (previous
  checkout summary ignored variant discounts).

**Deliberate limits**
- `AuthRepository` remains an aggregate facade; splitting it would fragment
  session/OTP invariants, so `TooManyFunctions` is suppressed with a rationale.
- Catalog cache is in-process (`MemoryCache`); a Redis-backed `Cache` remains the
  multi-instance deployment step.
- Recommendation cache TTL is 5 minutes; invalidation is catalog-wide on writes.

---

## 11. Documentation & OpenAPI Pass

**Swagger**
- Verified the generated spec against a real Postgres instance (`/swagger/documentation.yaml`):
  **178 paths / 206 operations / 0 missing summaries / 0 missing tags / 122 schemas /
  69 request bodies / 35 tags**. All 203 feature routes plus 3 health probes are
  documented; `/` and `/metrics` are intentionally hidden.
- Tags are applied **once per route group** with the runtime `.describe { tag(...) }`
  DSL in `ConfigureRouting`; every operation is grouped consistently
  (Product, Cart, Order, RMA, KYC, Payout, Recommendation, Tax, FX, API Keys,
  Feature Flags, Outbox, …).
- Every feature route now has an explicit `.describe {}` block for operation
  metadata (summary, params, request/response schemas). The ad-hoc `.docs`
  helper and legacy `@tag`/`@description` KDoc were removed. KDoc keeps
  human-readable prose and official `Description:` comment fields.
- Follows Ktor's documented precedence: runtime annotations override comment
  metadata, which overrides compiler/code inference.
- Added `jwt-auth` and `api-key` bearer scheme descriptions; route-level security
  is inferred from `authenticate`/`requireRole` selectors.
- Added request/response schemas for auth, product/variant, cart, checkout,
  order, RMA, KYC, notifications, inventory, warehouses, API keys, feature flags,
  tax, FX and recommendations via `jsonBody`/`ok`/`created` helpers.
- Swagger UI uses Ktor's documented default asset source (unpkg CDN); no extra
  WebJar or custom asset route.
- CSP is relaxed only for `/swagger*`: inline Swagger initializer/styles and
  `https://unpkg.com`; all other routes keep `default-src 'self'`.
- Libraries refreshed to current stable releases: Ktor 3.6.1, Kotlin 2.4.21,
  Exposed 1.6.1, Flyway 13.12.1, PostgreSQL 42.7.14, Logback 1.6.6 and
  Micrometer 1.17.2.
- OpenAPI tag names live in `ApiTags`, shared by document metadata and route
  groups; no free-form tag strings.
- All business timestamps use `currentUtc()`; no server-local `LocalDateTime.now()`.
- Path parameters use Ktor's `requirePathParameter`; version-catalog aliases
  follow kebab-case; `/bin/` is ignored.

**Docs**
- Added `api-keys.md`, `feature-flags.md`, `tax.md`, `warehouse.md`, `outbox.md`,
  `fx.md`; linked all in `mkdocs.yml`; `mkdocs build --strict` passes.
- Updated the home page with variants, recommendations, RMA, KYC/payouts,
  notifications and platform administration.
- Added `OpenApiDocumentationTest` to guard info/version/tags/security/operations
  and a static test enforcing `.describe` coverage plus no legacy `.docs`/`@tag`.
- Fixed a fresh-database startup bug: `configureDatabase` called Flyway
  `validate()` before `migrate()`, which always fails on an empty schema; the
  migrate call already validates applied migrations.

---

## 7. Remediation Order (executed)

1. P0 security/money.
2. P1 fulfillment/currency/coupon/refund/payout/cleanup/outbox.
3. P2 contract/DB/config/logging/CI.
4. Feature scaffolds + docs.

---

## 8. Verification Log

| Batch | Change | Compile | Tests |
|---|---|---|---|
| 0 | baseline | ✅ | ✅ 293 tests |
| 1 | security/payment | ✅ | ✅ |
| 2 | commerce | ✅ | ✅ |
| 3 | contracts/outbox/cache | ✅ | ✅ |
| 4 | migrations/config/CI/docs | ✅ | ✅ 299 tests (Postgres test runs in CI when DB_* is set) |
| 5 | docs/OpenAPI | ✅ | ✅ 300+ tests; spec verified against real Postgres (206 operations, 0 missing summaries) |

**Final verification:** `./gradlew build` passes on all branches (299 tests locally; the Postgres test runs against the CI `DB_*` service or Testcontainers).

**Implemented in this pass (highlights):**
- PR 4 also adds outbox dead-letter visibility, shipment tracking, quantity/shipping/tax refunds, UTC timestamps, public catalog discovery, BigDecimal product prices, coupon scoping/admin filters and V20.
- Server-owned payment status, COD flow + admin confirm endpoint.
- Registration pre-hijack fix; session revocation on password change/reset; OTP body support; anti-enumeration reset.
- Seller ownership checks for inventory/notifications; private refund evidence route.
- Order state machine with `SHIPPED`, payment sets `PAID`.
- FX rate persisted on orders, item/refund/payout currency, dashboard USD normalization, FX admin API + staleness.
- Coupon per-user limits, usage reversal, code normalization, soft-retire used coupons.
- Net-of-discount refund caps + stock/sales/payout/payment reversal.
- Payout request aggregate, per-seller idempotency, locking, lifecycle statuses, balance endpoint.
- Shared cancellation routine used by cancel/cleanup/refund; UTC timestamps.
- Consumer-acknowledged outbox, attempts cap, safe shutdown, bounded cache/queue, product-status sync, review vote dedupe.
- V18/V19 migrations for OTP index, payment/order currency, payout requests, consent revocation, review votes, indexes, constraints.
- Docker non-root/healthcheck/volume, CI build, docs corrections, 299 tests, full-text search, money-path metrics and GDPR endpoints.
3
