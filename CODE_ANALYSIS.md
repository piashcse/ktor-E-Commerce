# Ktor E-Commerce — Codebase Analysis Report

Analyzed 212 Kotlin files. Findings verified by direct inspection of source. Ordered by severity.

> **Status legend:** ✅ DONE = implemented and verified (compiles + `./gradlew build` passes). Items not marked remain open.

---

## 1. Critical Logical Bugs (crash / money-loss / security)

### ✅ DONE — C1. Schema/entity table-name mismatch — `orders` vs `order`
**Before:** `OrderTable : BaseIdTable("orders")` (Order.kt:13), but migration creates `CREATE TABLE "order"` (V1__baseline_schema.sql:264) and **every FK** references `"order"` (V1:303,319,329,412; V5:6). Every order/cart/checkout/stock-reservation query generated SQL against `orders` → `relation "orders" does not exist`. Same family: `inventory.status` was `INTEGER` in SQL (V1:370) vs `enumerationByName<InventoryStatus>` (VARCHAR) in Inventory.kt:55, and `order_status_history.status` was `INTEGER` (V1:320) vs `enumerationByName<OrderStatus>` (VARCHAR) (OrderStatusHistory.kt:11).

**Fixed by:**
- `Order.kt:13` — table renamed `"orders"` → `"order"` (matches migration + all FKs).
- `V1__baseline_schema.sql` — `order_status_history.status` and `inventory.status` now `VARCHAR(30)`/`VARCHAR(50)` (fresh installs correct).
- `V7__fix_schema_entity_drift.sql` — new migration converts existing DBs (`INTEGER` → `VARCHAR` with `USING CASE` mapping for both enum columns).

### ✅ DONE — C2. Money stored without decimal precision (`Long`/`Double`/`Float`)
**Before:** `Payment.kt:13` stored amount as `Long`; order total is `BigDecimal(10,2)`. `BigDecimal("19")` vs `19.99` always `!= 0` (PaymentRepositoryImpl.kt:29-33) — cents orders could never be paid. Money also stored as `Double`/`Float` in Coupon.kt:13-15, ShippingMethod.kt:11, OrderRequest.kt:11-13, refunds, and domain events.

**Fixed by:** all money now `BigDecimal` end-to-end:
- Entities: `Payment.amount`, `Coupon.discountValue/minOrderAmount/maxDiscountAmount`, `ShippingMethod.price` → `decimal(10,2)`.
- Requests: `PaymentRequest.amount`, `CouponRequest.*`, `ShippingMethodRequest.price`, `OrderRequest.subTotal/total/shippingCharge`, `UpdateRefundStatusRequest.refundAmount` → `BigDecimal` (fields annotated `@Contextual` for the existing `BigDecimalSerializer`).
- Response: `PaymentResponse.amount` → `String` (`toPlainString()`, consistent with other money responses).
- Events: `OrderPlacedEvent.total` / `PaymentCompletedEvent.amount` → `BigDecimal`.
- `CartRepositoryImpl.getCartSummary` subtotal now rounded to 2dp.
- `V1` + `V7` migration: `payment.amount BIGINT → DECIMAL(10,2)`, coupon/shipping `DOUBLE PRECISION → DECIMAL(10,2)`.

### ✅ DONE — C3. Stock overselling (TOCTOU) in `createOrder`
**Before:** OrderRepositoryImpl.kt:325 read `effectiveStock()` unlocked, decremented at :379 with silent `coerceAtLeast(0)` clamp (Inventory.kt:31) — two concurrent orders could oversell.

**Fixed by:**
- `createOrder` stock check now uses `effectiveStock(forUpdate = true)` (row-locked read) and the redundant second read was removed.
- `ProductDAO.decrementStock` (Inventory.kt) now throws `insufficientStock` instead of silently clamping to 0.
- `InventoryRepositoryImpl.updateStock` reads the inventory row with `forUpdate()` → read-modify-write is serialized, no lost updates.

### ✅ DONE — C4. Double-payment / no idempotency
**Before:** PaymentRepositoryImpl.kt:35-57 checked existing payments with no `forUpdate` and no unique constraint — two concurrent `createPayment` calls double-completed an order; `transactionId` was stored but never checked.

**Fixed by:**
- `createPayment` is now `retryQuery` + locks the order row (`forUpdate()`) so concurrent payments serialize.
- Idempotency: a `transactionId` that already exists returns the existing payment instead of creating a duplicate.
- `V7` migration adds `payment_order_completed_idx` — a partial unique index on `payment(order_id) WHERE status = 'COMPLETED'` as a DB-level guard.

### ✅ DONE — C5. FIXED coupon → negative order totals
**Before:** OrderRepositoryImpl.kt:101 applied `BigDecimal(coupon.discountValue)` uncapped — a $10 FIXED coupon on a $5 order produced a negative `total`.

**Fixed by:** `applyCoupon` now caps discount at `min(discount, orderAmount)` for both FIXED and PERCENTAGE types; percentage math uses `BigDecimal` division (no binary-double residue).

### ✅ DONE — C6. Multi-shop shipping mismatch
**Before:** `placeOrder` charged full shipping per split order (:148-160), but `getCheckoutSummary` added it once (:283) — customers charged more than the quoted summary.

**Fixed by:** `getCheckoutSummary` now computes shipping per distinct shop (`shippingMethod.price × shopCount`), matching what `placeOrder` actually charges per order.

---

## 2. Security Vulnerabilities

### S1. Anyone can self-register as `ADMIN`
`RegisterRequest.kt:17-18` validates `userType` includes `admin`; the public register route stores exactly that role (AuthRepositoryImpl.kt:113-149). No server-side guard.

### S2. Privilege escalation: ADMIN → SUPER_ADMIN
`changeUserType` (AuthRepositoryImpl.kt:334-341) only checks the **target's current** type via `canManage` (Enums.kt:167-173), never the **new** type. An ADMIN can promote any CUSTOMER/SELLER to ADMIN or SUPER_ADMIN. The query param accepts any enum value (AuthRoutes.kt:135).

### S3. OTP never delivered → registration/reset unusable (also a logic bug)
`UserAuthenticationService.kt:40-46,108-114` publish `SendEmailEvent` whose body never contains the OTP. `EmailSender.sendOtp` — the only method including the OTP — has zero call sites. Verification/reset flows are dead. Worse: `EmailSubscriber.kt:10-11` emails `OrderPlacedEvent.userId`/`PaymentCompletedEvent.userId` — a **UUID**, not an email address.

### S4. Deactivation / blacklist are ineffective
- `refreshAccessToken` (AuthRepositoryImpl.kt:284) never checks `isActive`/`isVerified`; deactivation (343-353) doesn't revoke refresh tokens → deactivated users keep working.
- Access-token blacklist is validated **only** from in-memory `CacheService` (ConfigureAuth.kt:20-30); the DB `blacklisted_token` table is never read → after restart or on a second instance, logged-out tokens work again.

### S5. OTP brute-force + permanent lockout
`reset-password` has **no** per-user attempt limit (AuthRepositoryImpl.kt:194-212) — 6-digit OTP, only per-IP rate limit protects it. Conversely, OTP verification lockout is permanent: failed count never decays (`lockOtpAttempts` writes `lockedUntil` but nothing reads it).

### S6. Email enumeration
Login returns 404 "User not found" vs 401 wrong-password (UserAuthenticationService.kt:63-74); registration and reset errors embed the email.

### S7. Per-IP rate limit is proxy-naive and enables account DoS
`requestKey { call.request.local.remoteHost }` (ConfigureRateLimit.kt:57) ignores `X-Forwarded-For`; Ktor's in-memory limiter resets per instance. Account lockout (5 wrong guesses → 30 min) is a deliberate DoS primitive on any known email; `ipAddress` is always stored `null` (UserAuthenticationService.kt:64,69).

### S8. Weak password policy
`RegisterRequest.kt:16` only enforces 8-64 chars; `ChangePasswordRequest`/`ResetRequest.newPassword` have **no** validation at all — despite a documented complexity policy. Plain (non-constant-time) string comparison of OTPs (:205, :219). SMTP SSL on STARTTLS port 587 (EmailSender.kt:49-50, DotEnvConfig.kt:17).

---

## 3. Missing Functionality

- **Idempotency keys** on payments (C4); `transactionId` stored but never checked.
- **Atomic stock decrement** (`UPDATE ... WHERE stock_quantity >= ?` instead of read→clamp→write).
- **Refund authorization/transition validation**: any seller can approve any refund (RefundRequestRepositoryImpl.kt:143), no transition validation, no amount cap, no `paymentStatus = REFUNDED`, repeatable after approval (only PENDING duplicates rejected :44-52).
- **Cancel path inconsistency**: `PATCH /orders/status/{id}` to `CANCELED` (Enums.kt:77, OrderRoutes.kt:44) bypasses stock restore + refund entirely; `cancelOrder` alone restores stock and restricts statuses.
- **`updateOrderStatus` never sets `shippingDate`/`deliveredDate`/`completedDate`/`paymentStatus`** (OrderRepositoryImpl.kt:425-429).
- **Seller products get no inventory row** → permanently unsellable (`ProductRoutes.kt:86` passes `shopId=null`; `effectiveStock` returns 0).
- **Ranking/analytics fields never maintained**: `totalSales`, `rating`, `totalReviews`, `viewCount` (dead code), `discountPercentage` (stale after price edit), `bestSeller` (never set) → best-selling/top-rated/hot-deals sorts are arbitrary.
- **Coupon abuse**: `getCheckoutSummary` increments `usageCount` on preview (OrderRepositoryImpl.kt:293-298 + :94); no per-user usage tracking; `LocalDateTime.now()` vs UTC timestamps.
- **Hard deletes violate FKs** (no `ON DELETE CASCADE`): deleting a product/category/brand with children → 500.
- **`StockReservationCleanup`** (service/StockReservationCleanup.kt:45-59) releases reservations for non-PENDING orders without restoring stock → inventory leak.
- **Audit logging is non-functional**: `AuditLogRepositoryImpl.log` never called; `AuditLogSubscriber` just prints to console.

---

## 4. Architectural Issues

1. **Routes call repositories directly** — service layer was deleted (git `ea46016`) despite README claiming Clean Architecture. `OrderRepositoryImpl` is a 500-line god object mixing persistence, pricing, coupon validation, stock locking, commissions, and event publishing.
2. **DAO objects leak through repo interfaces** (`AuthRepository.kt:17-29` returns `UserDAO`/`RefreshTokenDAO`); response DTOs live inside entity files (`User.kt:60-81` `UserResponse`/`LoginResponse`, `Seller.kt:78-98`, `Cart.kt:30`, `WishList.kt:28`).
3. **Events published inside transactions** — `tryEmit` before commit → phantom events if tx rolls back; no outbox pattern. `EventBus` counters/subscribers are unsynchronized; `deadLetterCount` reads `replayCache` of a `replay=0` flow → always 0; `tryEmit` return value ignored (silent drop on full buffer). `EmailSender` swallows exceptions, defeating EventBus retry/dead-letter.
4. **Money math duplicated 4×** (cart summary, checkout summary, placeOrder, createOrder) with different rounding/shipping semantics — no shared `Money`/`PriceCalculator`.
5. **No generic base repository** — 22 near-identical CRUD repo pairs (Brand vs Category vs ShopCategory vs SubCategory...). No modular monolith boundaries; `CheckoutRoutes` reaches across 3 feature packages.

---

## 5. Missing Industry Standards

| Standard | Status |
|---|---|
| Tests | `ApplicationTest.kt` is 1 empty line; 24 test dirs empty; zero unit/integration tests |
| Health checks | Static stub returns "UP" (ConfigureRouting.kt:50); docs claim `/health/live`/`/health/ready` — don't exist; no DB/pool probe |
| Structured logging / metrics | Plain-text logback; no Micrometer/Prometheus; `MDC.put("requestId")` never `MDC.remove()` (RequestTracing.kt:17) |
| OpenAPI | No Bearer security scheme, no error schemas; README references non-existent `openapi.json` |
| Request validation | `ktor-server-request-validation` declared but never installed; valiktor on some DTOs only |
| Env/config validation | `ignoreIfMissing=true`/`ignoreIfMalformed=true`; no fail-fast startup validation; dual HOCON+.env systems |
| Error consistency | Ad-hoc `mapOf`/raw-string bodies alongside `ApiError`; Ktor rate-limit 429 is plain text; malformed JSON → 500 (no `ContentTransformationException` handler); literal bug `"Message.Auth.ACCOUNT_ACTIVATED"` (AuthRoutes.kt:162) |
| Lint/CI | Both `detekt` and `ktlint` have `ignoreFailures=true` (build.gradle.kts:94,98) → hooks never block; `retryQuery` retries *all* exceptions (3× on validation errors, TransactionExt.kt:30-40); graceful shutdown uses deprecated API |
| Concurrency | Missing indexes on FK/date/filter columns; no unique constraints on `wishlist(user_id,product_id)`, `cart_item(user_id,product_id)`, `review_rating`; dashboard does per-day queries + N+1 |

---

## 6. Priority-Ranked Fix List

> ✅ = DONE. Remaining items below.

**P0 (blocks everything):** ~~align entity↔migration schema (C1)~~ ✅; ~~money types → `BigDecimal` everywhere (C2)~~ ✅; ~~make stock mutations atomic (C3)~~ ✅; ~~payment idempotent + no double-charge (C4)~~ ✅; ~~coupon negative totals (C5)~~ ✅; ~~checkout summary vs actual shipping (C6)~~ ✅; **fix OTP delivery (S3)**; **block admin self-registration (S1)**; **fix `changeUserType` authorization (S2)**.

**P1:** cancel-path stock/refund consistency; seller products get inventory; refund authorization/transitions; coupon preview side-effect; deactivation + blacklist enforcement.

**P2 (architecture):** restore service layer; move DTOs out of `database/entities`; outbox/durable events + fix EmailSubscriber addressing; generic `BaseRepository`; shared pricing module; single error-envelope (`ApiError`) everywhere.

**P3 (industry standard):** one integration test (`testApplication` + Testcontainers); real health checks; JSON logging + MDC cleanup; OpenAPI auth schemes; enable lint failures in CI; `retryQuery` → catch only transient DB errors.
