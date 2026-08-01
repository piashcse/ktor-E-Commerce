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

### ✅ DONE — S1. Anyone can self-register as `ADMIN`
**Before:** `RegisterRequest.kt:17-18` validated `userType` including `admin`; the public register route stored exactly that role (AuthRepositoryImpl.kt:113-149). No server-side guard.
**Fixed by:**
- `RegisterRequest.kt` now restricts self-registration to `CUSTOMER`/`SELLER` only.
- `UserAuthenticationService.register` rejects `ADMIN`/`SUPER_ADMIN` server-side (defense in depth) with `Message.Auth.REGISTRATION_ROLE_FORBIDDEN`.

### ✅ DONE — S2. Privilege escalation: ADMIN → SUPER_ADMIN
**Before:** `changeUserType` (AuthRepositoryImpl.kt:334-341) only checked the **target's current** type via `canManage` (Enums.kt:167-173), never the **new** type. An ADMIN could promote any CUSTOMER/SELLER to ADMIN or SUPER_ADMIN.
**Fixed by:**
- `changeUserType` now also requires `currentUser.userType.canManage(newUserType)` and forbids changing one's own type (prevents e.g. a SUPER_ADMIN locking everyone out).

### ✅ DONE — S3. OTP never delivered → registration/reset unusable (also a logic bug)
**Before:** `UserAuthenticationService.kt:40-46,108-114` published `SendEmailEvent` whose body never contained the OTP. `EmailSender.sendOtp` — the only method including the OTP — had zero call sites. Worse: `EmailSubscriber.kt:10-11` emailed `OrderPlacedEvent.userId`/`PaymentCompletedEvent.userId` — a **UUID**, not an email address.
**Fixed by:**
- Registration (`Created` and `OtpResent`) and forgot-password now email the actual OTP via `SendEmailEvent`; `AuthRepository.getRegistrationOtp`/`forgotPassword` (returns OTP) added; `RegistrationResult.OtpResent` now carries `id`/`email`.
- Dead `EmailSender.sendOtp` removed.
- `OrderPlacedEvent`/`PaymentCompletedEvent` now carry `email` (looked up at publish time); `EmailSubscriber` sends to the real address.

### ✅ DONE — S4. Deactivation / blacklist are ineffective
**Before:** `refreshAccessToken` (AuthRepositoryImpl.kt:284) never checked `isActive`/`isVerified`; deactivation (343-353) didn't revoke refresh tokens → deactivated users kept working. Access-token blacklist was validated **only** from in-memory `CacheService` (ConfigureAuth.kt:20-30); the DB `blacklisted_token` table was never read → after restart or on a second instance, logged-out tokens worked again.
**Fixed by:**
- `refreshAccessToken` now rejects deactivated/unverified users.
- `deactivateUser` revokes all of the target user's refresh tokens in the same transaction.
- JWT `validate` (ConfigureAuth.kt) now checks the DB `blacklisted_token` table (durable path) and the user's active+verified status on every authenticated request.

### ✅ DONE — S5. OTP brute-force + permanent lockout
**Before:** `reset-password` had **no** per-user attempt limit (AuthRepositoryImpl.kt:194-212). OTP verification lockout was permanent: failed count never decayed (`lockOtpAttempts` wrote `lockedUntil` but nothing read it).
**Fixed by:**
- `resetPassword` now enforces a per-user OTP attempt limit (5 → 30-min lock) backed by `otp_attempt`, and returns `ResetResult.Locked` (429).
- `otpVerification` now respects `isOtpLocked` and resets the counter once the lock window expires → lockout is time-based, not permanent.
- Login lockout counters also reset once an expired lock passes (no instant re-lock).

### ✅ DONE — S6. Email enumeration
**Before:** Login returned 404 "User not found" vs 401 wrong-password (UserAuthenticationService.kt:63-74); registration and reset errors embedded the email.
**Fixed by:**
- Login now returns the identical 401 `InvalidCredentialsException` for unknown email and wrong password (lockout still applied).
- `Message.Auth.userNotFoundForRole` no longer echoes the email; all reset/forgot-password errors are generic.

### ✅ DONE — S7. Per-IP rate limit is proxy-naive and enables account DoS
**Before:** `requestKey { call.request.local.remoteHost }` (ConfigureRateLimit.kt:57) ignored `X-Forwarded-For`; `ipAddress` was always stored `null` (UserAuthenticationService.kt:64,69).
**Fixed by:**
- Added `ApplicationCall.clientIp` (prefers `X-Forwarded-For`, then `X-Real-IP`, falls back to socket host); all global + per-user rate-limit keys now use it.
- Login now records the real client IP in `login_attempt.ip_address`.

### ✅ DONE — S8. Weak password policy
**Before:** `RegisterRequest.kt:16` only enforced 8-64 chars; `ChangePasswordRequest`/`ResetRequest.newPassword` had **no** validation at all — despite a documented complexity policy. Plain (non-constant-time) string comparison of OTPs (:205, :219). SMTP SSL on STARTTLS port 587 (EmailSender.kt:49-50, DotEnvConfig.kt:17).
**Fixed by:**
- New shared `PasswordPolicy` (uppercase + lowercase + digit + special char, 8-64) enforced on `RegisterRequest`, `ChangePasswordRequest`, and `ResetRequest`.
- OTP comparisons use constant-time `MessageDigest.isEqual` (`constantTimeEquals`).
- SMTP now uses STARTTLS on port 587 by default, with optional SSL via `EMAIL_SSL` env flag.

---

## 3. Missing Functionality

- ~~**Idempotency keys** on payments~~ ✅ (C4): `transactionId` idempotency + partial unique index implemented.
- ~~**Atomic stock decrement**~~ ✅ (C3): `effectiveStock(forUpdate = true)` row-locked read + `decrementStock` throws instead of clamping.

### ✅ DONE — M1. Refund authorization/transition validation
**Before:** any seller could approve any refund (RefundRequestRepositoryImpl.kt:143); no transition validation; no amount cap; no `paymentStatus = REFUNDED`; only PENDING duplicates rejected.
**Fixed by:**
- `updateRefundStatus` now requires a seller to own the refund's order shop before acting.
- Added transition matrix `canTransitionTo` (PENDING→APPROVED/REJECTED; APPROVED/SHIPPED→REFUNDED/REJECTED; terminal states immutable) with `Message.Refunds.invalidTransition`.
- Refund amount is capped at the order item total (`AMOUNT_EXCEEDS_ITEM_TOTAL`); `REFUNDED` requires an amount (defaulting to the item total).
- Marking a refund `REFUNDED` sets `order.paymentStatus = REFUNDED`.
- `createRefundRequest` dedup now rejects existing PENDING/APPROVED/SHIPPED refunds for the same item.

### ✅ DONE — M2. Cancel path consistency
**Before:** `PATCH /orders/status/{id}` to `CANCELED` bypassed stock restore, reservation release, and refund; only `cancelOrder` restored stock.
**Fixed by:** `updateOrderStatus` delegates CANCELED to a shared `applyOrderCancellation` (sets `canceledDate`, logs history, restores stock, releases reservations, marks paid orders `paymentStatus = REFUNDED`); `cancelOrder` uses the same routine.

### ✅ DONE — M3. `updateOrderStatus` lifecycle timestamps
**Before:** `updateOrderStatus` never set `shippingDate`/`deliveredDate`/`completedDate`/`paymentStatus`.
**Fixed by:** DELIVERED sets `deliveredDate` (and `shippingDate` if unset); RECEIVED sets `completedDate`; PAID sets `paymentStatus = COMPLETED`; CANCELED handled by M2.

### ✅ DONE — M4. Seller products get inventory row
**Before:** seller create-product route passed `shopId = null` → no inventory row → `effectiveStock` = 0 → unsellable.
**Fixed by:** `createProduct` falls back to the seller's `shopId` when none is given and creates the inventory row (stock from request) against that shop.

### ✅ DONE — M5. Ranking/analytics fields maintained
**Before:** `totalSales`, `rating`, `totalReviews`, `viewCount` (dead), `discountPercentage` (stale), `bestSeller` (never set) were not maintained.
**Fixed by:**
- `ProductDAO.addSales/removeSales` update `totalSales` and auto-set/clear `bestSeller` at the threshold (order place, createOrder, and cancel).
- Reviews add/update/delete recompute the product `rating` and `totalReviews`.
- Product detail view calls `incrementViewCount`.
- `updateProduct` recomputes `discountPercentage` after price/discount edits.

### ✅ DONE — M6. Coupon abuse
**Before:** `getCheckoutSummary` preview incremented `usageCount`; no per-user usage tracking; `LocalDateTime.now()` vs UTC.
**Fixed by:**
- Preview now uses a side-effect-free `calculateCouponDiscount`; only `placeOrder` consumes usage via `consumeCoupon`.
- New `coupon_usage` table (V8) records coupon per user per order.
- Coupon date validation uses UTC (`LocalDateTime.now(ZoneOffset.UTC)`).

### ✅ DONE — M7. Hard deletes violate FKs
**Before:** deleting a product/category/brand with child rows returned 500 (no `ON DELETE CASCADE`).
**Fixed by:** `V8` migration rewrites FK actions — CASCADE for owned children (product_image, inventory, review_rating, cart_item, wishlist, stock_reservation, sub_category, category→product) and SET NULL for nullable product refs (brand/sub-category/shop).

### ✅ DONE — M8. `StockReservationCleanup` inventory leak
**Before:** expired reservations on non-PENDING orders were released without restoring stock.
**Fixed by:** cleanup now auto-cancels + restores stock for expired reservations on PENDING *and* CONFIRMED (unpaid) orders; paid/terminal orders are left untouched (stock legitimately consumed).

### ✅ DONE — M9. Audit logging non-functional
**Before:** `AuditLogRepositoryImpl.log` was never called; `AuditLogSubscriber` only printed to console.
**Fixed by:** `AuditLogSubscriber` now receives `AuditLogRepository` via DI (wired in `Application.kt`) and persists DB audit rows for order-placed, user-registered, and payment-completed events (failures logged, non-fatal).

---

## 4. Architectural Issues

### ✅ DONE — A1. Routes call repositories directly
**Before:** Service layer was deleted (git `ea46016`) despite README claiming Clean Architecture. Routes injected `XRepository` directly; `OrderRepositoryImpl` was a 500-line god object mixing persistence, pricing, coupon validation, stock locking, commissions, and event publishing.
**Fixed by:** Restored a thin service layer for every feature — `BrandService`, `CartService`, `CheckoutService`, `ConsentService`, `CouponService`, `DashboardService`, `InventoryService`, `OrderService`, `PaymentService`, `PolicyService`, `ProductCategoryService`, `ProductSubCategoryService`, `RefundRequestService`, `ReviewRatingService`, `ShippingMethodService`, `ShopService`, `ShopCategoryService`, `WishListService`, `AuditLogService` — plus the existing `UserAuthenticationService`, `ProfileService`, `ProductCatalogService`, `ProductCrudService`. `CheckoutRoutes` now goes through a single `CheckoutService` (no cross-package repo access). All 22 route files now inject services, never repositories. Wired in `KoinModule.kt` (`serviceModule`).

### ✅ DONE — A2. DAO objects leak through repo interfaces
**Before:** `AuthRepository.kt:17-29` returned `UserDAO`/`RefreshTokenDAO`; response DTOs lived inside entity files (`User.kt:60-81` `UserResponse`/`LoginResponse`, `Seller.kt:78-98`, `Cart.kt:30`, `WishList.kt:28`).
**Fixed by:** DAOs no longer cross repo boundaries — `AuthRepository` returns domain models (`AuthUser`, `StoredRefreshToken`, `LoginAttemptInfo` in `model/domain/`). DTOs relocated to `model/response/` (`UserResponse`, `LoginResponse`, `SellerResponse`, `CartResponse`, `WishListResponse`) and `model/request/ChangePassword.kt`; entity files are now pure tables/DAOs. Mappers updated (`UserMappers`, `CartWishListMappers`).

### ✅ DONE — A3. Events published inside transactions
**Before:** `tryEmit` before commit → phantom events if tx rolls back; no outbox pattern. `EventBus` counters/subscribers were unsynchronized; `deadLetterCount` read `replayCache` of a `replay=0` flow → always 0; `tryEmit` return value ignored (silent drop on full buffer). `EmailSender` swallowed exceptions, defeating EventBus retry/dead-letter.
**Fixed by:** `EventBus.publish` now defers events while a DB transaction is active; `query`/`retryQuery` flush them only after commit and discard on rollback (after-commit publishing in `TransactionExt.kt`). Counters are `AtomicLong`, subscribers use `CopyOnWriteArrayList`, `deadLetterCount` is a real counter, `tryEmit` falls back to async `emit` when the buffer is full. `EmailSender.send` rethrows `EmailException` (so EventBus retries then dead-letters) and restores the rate-limit counter on failure.

### ✅ DONE — A4. Money math duplicated 4×
**Before:** Money/pricing duplicated in cart summary, checkout summary, placeOrder, createOrder with different rounding/shipping semantics — no shared `Money`/`PriceCalculator`.
**Fixed by:** Shared `com.piashcse.utils.money.Money` module (2dp HALF_UP rounding, `lineTotal`, `subtotal`, `taxOn`, `shippingTotal`, `total`, `percent`, `discountPercent`, `average`). Applied in `CartRepositoryImpl`, `OrderRepositoryImpl` (`getCheckoutSummary`, `placeOrder`, `createOrder`), `ProductRepositoryImpl`, `Inventory`, `DashboardRepositoryImpl`, `ReviewRatingRepositoryImpl`.

### ✅ DONE — A5. No generic base repository
**Before:** 22 near-identical CRUD repo pairs (Brand vs Category vs ShopCategory vs SubCategory...). No modular monolith boundaries; `CheckoutRoutes` reached across 3 feature packages.
**Fixed by:** Generic `BaseCrudRepository<DAO, RESP>` in `repository/base/` (create, paged getAll/listAll, findByIdOrThrow, update, delete, exists; abstract `DAO.toResponse()`). Refactored to extend it: `BrandRepositoryImpl`, `ShopCategoryRepositoryImpl`, `ProductCategoryRepositoryImpl`, `ProductSubCategoryRepositoryImpl`, `ShippingMethodRepositoryImpl`, `CouponRepositoryImpl`, `PolicyRepositoryImpl` (policy keeps custom deactivate-siblings logic). Custom-ownership features (shipping_address, consent, profile, order, etc.) intentionally left as-is. Checkout now delegates through `CheckoutService`.

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

**P0 (blocks everything):** ~~align entity↔migration schema (C1)~~ ✅; ~~money types → `BigDecimal` everywhere (C2)~~ ✅; ~~make stock mutations atomic (C3)~~ ✅; ~~payment idempotent + no double-charge (C4)~~ ✅; ~~coupon negative totals (C5)~~ ✅; ~~checkout summary vs actual shipping (C6)~~ ✅; ~~fix OTP delivery (S3)~~ ✅; ~~block admin self-registration (S1)~~ ✅; ~~fix `changeUserType` authorization (S2)~~ ✅.

**P1:** ~~cancel-path stock/refund consistency~~ ✅; ~~seller products get inventory~~ ✅; ~~refund authorization/transitions~~ ✅; ~~coupon preview side-effect~~ ✅; ~~deactivation + blacklist enforcement (S4)~~ ✅; ~~reset OTP brute-force + time-based lockout (S5)~~ ✅; ~~login enumeration + email echo (S6)~~ ✅; ~~rate-limit proxy awareness + client IP capture (S7)~~ ✅; ~~password policy + constant-time OTP + SMTP STARTTLS (S8)~~ ✅.

**P2 (architecture):** ~~restore service layer~~ ✅; ~~move DTOs out of `database/entities`~~ ✅; ~~outbox/durable events + fix EmailSubscriber addressing~~ ✅ (after-commit publish); ~~generic `BaseRepository`~~ ✅; ~~shared pricing module~~ ✅; single error-envelope (`ApiError`) everywhere.

**P3 (industry standard):** one integration test (`testApplication` + Testcontainers); real health checks; JSON logging + MDC cleanup; OpenAPI auth schemes; enable lint failures in CI; `retryQuery` → catch only transient DB errors.
