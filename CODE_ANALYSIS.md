# Ktor E-Commerce — Codebase Analysis Report

Analyzed 246 Kotlin files. Findings verified by direct inspection of source. Ordered by severity.

> **Status legend:** ✅ DONE = implemented and verified (compiles + `./gradlew build` passes). Items not marked remain open.
>
> **Round 2 (2026-08-01):** full re-audit for logical bugs, role-management/permission gaps and missing functionality → see **§7**. Findings there are **open** unless marked ✅.

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
- `createPayment` is now `suspendRetryQuery` + locks the order row (`forUpdate()`) so concurrent payments serialize.
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
**Fixed by:** `EventBus.publish` now defers events while a DB transaction is active; `query`/`suspendRetryQuery` flush them only after commit and discard on rollback (after-commit publishing in `TransactionExt.kt`). Counters are `AtomicLong`, subscribers use `CopyOnWriteArrayList`, `deadLetterCount` is a real counter, `tryEmit` falls back to async `emit` when the buffer is full. `EmailSender.send` rethrows `EmailException` (so EventBus retries then dead-letters) and restores the rate-limit counter on failure.

### ✅ DONE — A4. Money math duplicated 4×
**Before:** Money/pricing duplicated in cart summary, checkout summary, placeOrder, createOrder with different rounding/shipping semantics — no shared `Money`/`PriceCalculator`.
**Fixed by:** Shared `com.piashcse.utils.money.Money` module (2dp HALF_UP rounding, `lineTotal`, `subtotal`, `taxOn`, `shippingTotal`, `total`, `percent`, `discountPercent`, `average`). Applied in `CartRepositoryImpl`, `OrderRepositoryImpl` (`getCheckoutSummary`, `placeOrder`, `createOrder`), `ProductRepositoryImpl`, `Inventory`, `DashboardRepositoryImpl`, `ReviewRatingRepositoryImpl`.

### ✅ DONE — A5. No generic base repository
**Before:** 22 near-identical CRUD repo pairs (Brand vs Category vs ShopCategory vs SubCategory...). No modular monolith boundaries; `CheckoutRoutes` reached across 3 feature packages.
**Fixed by:** Generic `BaseCrudRepository<DAO, RESP>` in `repository/base/` (create, paged getAll/listAll, findByIdOrThrow, update, delete, exists; abstract `DAO.toResponse()`). Refactored to extend it: `BrandRepositoryImpl`, `ShopCategoryRepositoryImpl`, `ProductCategoryRepositoryImpl`, `ProductSubCategoryRepositoryImpl`, `ShippingMethodRepositoryImpl`, `CouponRepositoryImpl`, `PolicyRepositoryImpl` (policy keeps custom deactivate-siblings logic). Custom-ownership features (shipping_address, consent, profile, order, etc.) intentionally left as-is. Checkout now delegates through `CheckoutService`.

### ✅ DONE — A6. Services own business logic + code-quality cleanup (2026-08)
Full-project pass moving rules out of repositories and removing duplication/dead code. All items verified (`./gradlew compileKotlin`, `./gradlew ktlintCheck`).
- **Authz moved to services via typed access facts** — product create (`ProductCreateAccess`, `ProductCrudService` throws `SELLER_REQUIRED`/`NOT_SHOP_OWNER`) and order ownership (`PlaceOrderAccess`, `OrderService` throws `SHIPPING_ADDRESS_UNAUTHORIZED`). Repos are pure persistence + fact reads (`getXxxAccess`); writes are wrapped in `suspendRetryQuery` with authz inside the transaction.
- **`OrderService.updateOrderStatus`/`cancelOrder` now fully atomic** — authz + transition commit in one transaction; access check runs inside the tx.
- **Enum parsing centralized** — `String.parseEnum<T>(field)` (→ `InvalidEnumValueException`) used across order/shop/dashboard/inventory/auth; `UserType.fromString` replaces the 3 duplicated `valueOf` try/catch blocks in `JwtTokenRequest`; new `StockOperation` enum (ADD/SUBTRACT/SET).
- **Not-found normalized** — repos use `String.throwNotFound(resourceName)` instead of `ValidationException(Message.*.NOT_FOUND)`; raw-string 404/400 responses in coupon/inventory/audit-log/refund-request routes replaced with typed exceptions; `UserAuthenticationService` not-founds now identify the missing resource.
- **Pagination factory** — `PaginatedResponse.of(data, totalCount, limit, offset)` replaces hand-built sites in 5 repos and fixes a `limit=data.size` bug in `ProductRepositoryImpl`.
- **Coupon date parsing moved to `CouponService`**; `mapper/CouponMappers.kt` extracted (kills inline `toResponse`).
- **Route authz DSL aligned** — cart/wishlist/profile/auth customer routes use `customerAuth {}` instead of no-role `requireRole {}`; dead `ApplicationCall.requireRole` suspend helper deleted.
- **Money math consolidated** — `Money.average`/`Money.round`/`Money.plain` in dashboard, payment, and email subscriber; `RoundingMode` duplicated code removed.
- **Single-source constants** — JWT expiry (`AppConstants.Authentication.JWT_EXPIRY_SECONDS`; was hardcoded 3 ways) and stock min/max (`AppConstants.Inventory.*`); cache keys unified in `constants/CacheKeys.kt`; `ProductCrudService` cache invalidation deduped into one `withCacheInvalidation` helper.
- **Dead code removed** — `verifyOwnership`, `requireSellerByUserId`, no-arg `throwNotFound()`, non-suspend `retryQuery` (superseded by `suspendRetryQuery`), `SecurityUtils.generateToken`, `Message.Errors.NOT_OWNER`, and 6 unused exceptions (`UnverifiedAccountException`, `DeactivatedAccountException`, `RateLimitExceededException`, `InternalServerException`, `DatabaseException`, `EmailException`).
- **Repo KDoc corrected** for `InventoryRepository.createOrUpdateInventory` (status computed repo-side because min/max defaults resolve inside the composite-key write); refund `REFUNDED` amount defaulting removed from the repo (unreachable — `RefundRequestService` already requires an amount).

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
| Error consistency | Partially fixed: coupon/inventory/audit-log/refund-request routes now throw typed exceptions (`NotFoundException`/`ValidationException`/`MissingParameterException` → `ApiError`) and the `"Message.Auth.ACCOUNT_ACTIVATED"` literal bug is fixed (now the constant, AuthRoutes.kt:167). Remaining: ad-hoc `mapOf` bodies in auth/product/cart/consent routes; Ktor rate-limit 429 is plain text; malformed JSON → 500 (no `ContentTransformationException` handler) |
| Lint/CI | Both `detekt` and `ktlint` have `ignoreFailures=true` (build.gradle.kts:94,98) → hooks never block; `suspendRetryQuery` retries *all* exceptions (3× on validation errors, TransactionExt.kt:39); graceful shutdown uses deprecated API |
| Concurrency | Missing indexes on FK/date/filter columns; no unique constraints on `wishlist(user_id,product_id)`, `cart_item(user_id,product_id)`, `review_rating`; dashboard does per-day queries + N+1 |

---

## 6. Priority-Ranked Fix List

> ✅ = DONE. Remaining items below.

**P0 (blocks everything):** ~~align entity↔migration schema (C1)~~ ✅; ~~money types → `BigDecimal` everywhere (C2)~~ ✅; ~~make stock mutations atomic (C3)~~ ✅; ~~payment idempotent + no double-charge (C4)~~ ✅; ~~coupon negative totals (C5)~~ ✅; ~~checkout summary vs actual shipping (C6)~~ ✅; ~~fix OTP delivery (S3)~~ ✅; ~~block admin self-registration (S1)~~ ✅; ~~fix `changeUserType` authorization (S2)~~ ✅.

**P1:** ~~cancel-path stock/refund consistency~~ ✅; ~~seller products get inventory~~ ✅; ~~refund authorization/transitions~~ ✅; ~~coupon preview side-effect~~ ✅; ~~deactivation + blacklist enforcement (S4)~~ ✅; ~~reset OTP brute-force + time-based lockout (S5)~~ ✅; ~~login enumeration + email echo (S6)~~ ✅; ~~rate-limit proxy awareness + client IP capture (S7)~~ ✅; ~~password policy + constant-time OTP + SMTP STARTTLS (S8)~~ ✅.

**P2 (architecture):** ~~restore service layer~~ ✅; ~~move DTOs out of `database/entities`~~ ✅; ~~outbox/durable events + fix EmailSubscriber addressing~~ ✅ (after-commit publish); ~~generic `BaseRepository`~~ ✅; ~~shared pricing module~~ ✅; services own business logic (access facts, atomic writes, centralized enum/not-found/pagination/money, single-source constants, dead code removed) ✅ (A6); single error-envelope (`ApiError`) everywhere (partially done — see §5).

**P3 (industry standard):** one integration test (`testApplication` + Testcontainers); real health checks; JSON logging + MDC cleanup; OpenAPI auth schemes; enable lint failures in CI; `suspendRetryQuery` → catch only transient DB errors.

---

## 7. Round-2 Deep Audit — logical bugs, permissions & missing functionality (2026-08-01)

Re-audit of every route/service/repo plus auth, money movement, inventory, catalog and caching. All findings below were verified by direct source inspection (file:line). Resolution status for each item is tracked in [7.7 Round-2 fixes applied](#77-round-2-fixes-applied).

### 7.1 Critical

| # | Finding | Location | Impact |
|---|---|---|---|
| R2-C1 | **Payment bypass: client-supplied `status` + no order-ownership check + finalize ignores the new payment's status.** `createPayment` forces `amount == order.total`, stores `request.status` verbatim, and finalizes the order as paid when `paid + amount >= total` regardless of whether the *new* payment is `COMPLETED`. No payment gateway call exists anywhere. | `PaymentService.kt:21-52`, `PaymentRepositoryImpl.kt:39-61`, `PaymentRequest.kt:17`, `PaymentRoutes.kt:25-27` | Any authenticated user can POST `amount=order.total, status=FAILED` and get a paid order for free; can also pay for someone else's order. |
| R2-C2 | **Cross-tenant inventory: any seller can read/write any shop's stock.** Routes never pass `currentUserId`; service/repo have no `sellerOwnsShop`/product-owner check. `getLowStockProducts` leaks every shop's low-stock data. | `InventoryRoutes.kt:27-73`, `InventoryService.kt:21-76`, `InventoryRepositoryImpl.kt:21-88` | Seller A can zero a competitor's stock, inflate own stock, or scrape all shops' inventory. |
| R2-C3 | **Payment/order IDOR on reads.** `getPaymentById` / `getPaymentsByOrderId` have no ownership/role filter; `customerAuth` admits any authenticated user. | `PaymentService.kt:57-63`, `PaymentRepositoryImpl.kt:67-84`, `PaymentRoutes.kt:34-47` | Any user reads any order's payment records (amounts, transaction ids). |
| R2-C4 | **Same order item can be refunded repeatedly (over-refund).** Dedup only blocks `PENDING/APPROVED/SHIPPED`; after `REFUNDED` (or `REJECTED`) a new request is allowed. `maxRefundAmount` is per-request (`orderItem.total`), not the remaining balance. | `RefundRequestRepositoryImpl.kt:94-102`, `:47` | 2× (or n×) refund of a single item beyond what was paid. |
| R2-C5 | **`finalizeOrderPayment` never sets `order.status = PAID`** (only `paymentStatus=COMPLETED` + reservations FINALIZED), so the order lifecycle and money movement are decoupled; conversely the admin `PAID` transition sets `paymentStatus=COMPLETED` with no payment record and no reservation finalize. | `PaymentRepositoryImpl.kt:54-61`, `OrderRepositoryImpl.kt:448-454` | Paid orders stay `PENDING` (customer can still cancel; seller can't progress `CONFIRMED→DELIVERED`), and unpaid orders can be flagged paid. |
| R2-C6 | **Coupon usage and seller `totalSales`/`totalCommission` are never reversed on cancellation.** `applyOrderCancellation` restores stock/sales but not coupon `usageCount`/`CouponUsage` rows or seller payout metrics credited at placement. | `OrderRepositoryImpl.kt:238-265` vs `476-503` | Canceled orders permanently burn coupon capacity and inflate seller/dashboard payouts. |
| R2-C7 | **Review/rating allowed without purchase; sellers can rate their own products.** Only rating range + duplicate check are enforced. | `ReviewRatingService.kt:21-28`, `ReviewRatingRepositoryImpl.kt:58-77` | Review-bombing and 5-star self-rating manipulate `product.rating`/`totalReviews`. |
| R2-C8 | **Products can be published with no shop / non-approved shop, as `ACTIVE`, and appear in the public catalog.** Route hardcodes `shopId=null`; `getCreateProductAccess` returns nulls when the seller has no shop, skipping the ownership guard; no `ShopStatus.APPROVED` check; no `SellerTable.status` check anywhere. | `ProductRoutes.kt:86`, `ProductCrudService.kt:26-37`, `ProductRepositoryImpl.kt:81-135,171-173`, `AuthRepositoryImpl.kt:160-162` | Unapproved/no-shop sellers list phantom products that pass `placeOrder` validation yet can never be bought (`SHOP_INACTIVE`). |

### 7.2 High

| # | Finding | Location | Impact |
|---|---|---|---|
| R2-H1 | **Password change / reset / role change do NOT revoke tokens.** `changePassword`, `resetPassword`, `changeUserType` update the hash/type but never call `revokeAllUserTokens` or blacklist access tokens. | `UserAuthenticationService.kt:200-206,247-260,262-286` | Stolen refresh tokens survive a password change; demoted users keep old-role access tokens until expiry. |
| R2-H2 | **JWT `userType` claim is trusted; DB role is never re-checked per request.** `ConfigureAuth.validate` checks only blacklist + active/verified. | `ConfigureAuth.kt:37-62`, `JwtConfig.kt:26-28` | A demoted ADMIN/SELLER keeps full old-role authority for up to 900 s; role changes are not enforced immediately. |
| R2-H3 | **Locked account can still mint tokens via `/auth/refresh`.** `refreshAccessToken` checks active/verified but never the login lockout. | `UserAuthenticationService.kt:209-227` | Lockout is trivially bypassed with any pre-lock refresh token. |
| R2-H4 | **Customer can cancel a PAID order through `PATCH /orders/status`.** The status matrix allows `PAID→CANCELED` and customers may set CANCELED, whereas `cancelOrder` restricts to `PENDING/CONFIRMED` (`canBeCanceled`). Cancel sets `paymentStatus=REFUNDED` with no refund process. | `OrderService.kt:78-85` vs `106-107`, `Enums.kt:91,97`, `OrderRepositoryImpl.kt:484-486` | Inconsistent cancellation rules; a paid order can be canceled/flagged "refunded" with no money returned. |
| R2-H5 | **Refund requests accepted for unpaid / non-delivered orders.** `createRefundRequest` only checks the caller owns the order. | `RefundRequestService.kt:24-28`, `RefundRequestRepositoryImpl.kt:83-115` | Refunds "paid out" for money never collected. |
| R2-H6 | **One refunded item marks the whole order `paymentStatus=REFUNDED`.** | `RefundRequestRepositoryImpl.kt:147-152` | Multi-item orders become "refunded" while other items remain paid. |
| R2-H7 | **Self-deactivation allowed (SUPER_ADMIN can lock themselves out).** `deactivateUser` has no `currentUser.id == targetUser.id` guard (unlike `changeUserType`). | `UserAuthenticationService.kt:288-305` | A SUPER_ADMIN deactivating themselves leaves no admin able to re-activate. |
| R2-H8 | **Forgot/reset-password leaks account existence and role.** 404 vs 200 and two distinct 404 messages depending on email/role. | `AuthRepositoryImpl.kt:126-133`, `AuthRoutes.kt:49-52` | Unauthenticated email enumeration. |
| R2-H9 | **Shared OTP attempt counter between register and reset; public `userId` lets anyone DoS a victim's OTP flow.** Product/shop responses expose other users' `userId`; 5 wrong attempts lock the victim's `otp_attempt` for 30 min, repeatable; reset path doesn't auto-reset after expiry. | `OtpAttempt.kt`, `UserAuthenticationService.kt:136-160,177-207` | Lockout poisoning + registration/reset DoS. |

### 7.3 Medium

| # | Finding | Location | Impact |
|---|---|---|---|
| R2-M1 | `CUSTOMER` guard is universal (`isCustomerOrHigher = true`), so any `requireRole(...,CUSTOMER,...)` admits every role — the intent "exclude admin/seller" is unenforceable. | `Enums.kt:189`, `RouteAuthDsl.kt:66` | False sense of role separation; future role types silently gain all `CUSTOMER` guards. |
| R2-M2 | **Deleting a category CASCADE-wipes every product** (and by chain their images/inventory/reviews/cart/wishlist/reservations) or 500s once order items exist (`order_item.product_id` not cascaded). | `V8__...:80-82`, `ProductCategoryRepositoryImpl.kt:53` | Destructive or unpredictable admin delete. |
| R2-M3 | **Dashboard revenue counts unpaid orders and ignores refunds** (`status != CANCELED` only); "outOfStock" counts `ProductStatus.OUT_OF_STOCK`, which is never set anywhere. | `DashboardRepositoryImpl.kt:26-31,47,62-68` | Revenue/today/avg overstated for every unpaid order. |
| R2-M4 | **Cart: duplicate add → 409 instead of quantity merge; no unique `(user_id, product_id)`; no ACTIVE/stock validation on add or quantity update.** | `CartRepositoryImpl.kt:25-40,68-82`, `V1__...:347` | 409s, race-prone duplicate rows, carts with unorderable items. |
| R2-M5 | **Wishlist/review duplicate prevention is race-prone** (pre-check only; non-unique indexes). | `WishListRepositoryImpl.kt:23-34`, `ReviewRatingRepositoryImpl.kt:63-66`, `V1__...:358,391-392` | Concurrent inserts create duplicate rows. |
| R2-M6 | **Product caches stale after sales/rating changes.** `products:best-selling`/`products:detail:*` invalidated only on create/update/delete + view-count; order placement/review mutations never invalidate. | `ProductCatalogService.kt:21-22,40-41`, `OrderRepositoryImpl.kt:216`, `ReviewRatingRepositoryImpl.kt:74,88,97` | Best-selling lists and detail pages show stale data up to 300 s TTL. |
| R2-M7 | **MDC `requestId` never removed** → previous request's id leaks into unrelated logs on thread reuse. | `RequestTracing.kt:12-18` | Corrupted log correlation. |
| R2-M8 | **Rate limiting keyed on spoofable `X-Forwarded-For`/`X-Real-IP`** with no proxy trust config. | `CallExt.kt:18-22`, `ConfigureRateLimit.kt:54-62` | Header rotation bypasses AUTH/OTP/SEARCH buckets (brute force, scraping). |
| R2-M9 | **PENDING shops visible in public listings; `GET /shops/{id}` returns any status.** | `ShopRepositoryImpl.kt:94-116`, `ShopRoutes.kt:26-30` | Unapproved business data disclosed before approval. |
| R2-M10 | **Catalog shows products of non-APPROVED shops** (queries filter only `ProductStatus.ACTIVE`). | `ProductRepositoryImpl.kt:171-173,423-437` | Users see items they can't purchase. |
| R2-M11 | **`updateStock` cannot SET stock to 0** (the `quantity <= 0` guard runs for `SET` too); max stock and `min<=max` never enforced. | `InventoryService.kt:29-41`, `InventoryRepositoryImpl.kt:43-66` | Operational gaps (can't zero stock; stock can exceed configured max). |
| R2-M12 | **Policy consent not enforced at registration** (no consent field, nothing persisted). | `RegisterRequest.kt:15`, `UserAuthenticationService.kt:41-81` | Compliance/GDPR gap. |
| R2-M13 | **No policy update/deactivate routes** (`updatePolicy`/`deactivatePolicy` unreachable). | `PolicyRoutes.kt:32-52`, `PolicyService.kt:19-33` | Policies can never be amended or retired. |
| R2-M14 | **Coupon has no per-user limit and usage rows aren't deduped**; `CouponUsage` `(couponId,userId)` index is non-unique; multi-shop checkouts create one row per order but `usageCount += 1`. | `Coupon.kt:47-49`, `OrderRepositoryImpl.kt:103-119,241` | One user can exhaust a coupon; usage accounting inconsistent. |
| R2-M15 | **Refund amount accepts negative/zero values.** | `RefundRequestService.kt:82-86`, `RefundRequestRepositoryImpl.kt:144` | Nonsensical negative refund records. |
| R2-M16 | **`shipRefund` bypasses the refund transition matrix** (no `APPROVED→SHIPPED` edge) and doesn't tie to order state. | `RefundRequestService.kt:99-111`, `Enums.kt:28-38` | Inconsistent refund state machine. |
| R2-M17 | **`StockReservationCleanup` auto-cancel skips side effects** (no coupon reversal, no seller sales/commission reversal, no `OrderStatusHistory`). | `StockReservationCleanup.kt:43-60` | Metrics drift vs normal cancellation. |
| R2-M18 | **`reservedQuantity` inventory column is dead** — never written; reservations are only `StockReservation` rows. | `Inventory.kt:74,92` | UI always shows 0 reserved; no real available-stock accounting. |
| R2-M19 | **Public coupon lookup leaks discount configuration** (type/value/min-order/limit). | `CouponRoutes.kt:22-25`, `CouponRepositoryImpl.kt:58-60` | Info disclosure / coupon enumeration. |
| R2-M20 | **`change-password` is not rate-limited** → unauthenticated-ish brute force of `oldPassword` by a session holder. | `AuthRoutes.kt:117-123`, `ConfigureRateLimit.kt` | Password-guessing within an existing session. |
| R2-M21 | **OTP resend silently drops emails after the rate limit** (API still returns `OTP_SENT`). | `EmailSender.kt:20-34`, `UserAuthenticationService.kt:53-59` | Users stranded without OTP and no feedback. |
| R2-M22 | **`RoleAuthorizationPlugin.onCall` responds but doesn't terminate routing** — the route handler still executes (side effects run) before the committed-response error surfaces. | `RouteAuthDsl.kt:21-37` | Defense-in-depth gap; relies on service-layer re-checks. |
| R2-M23 | **Checkout summary vs placed-order totals can differ by a cent** (per-shop tax rounding vs aggregate). | `OrderRepositoryImpl.kt:231` vs `:300` | Customer charged differently than the quoted summary. |
| R2-M24 | **Concurrent cancel/status update can double-restore stock** (cancelability read outside the transaction; no in-tx status guard). | `OrderRepositoryImpl.kt:476-503` | Stock drifts upward on concurrent cancellations. |
| R2-M25 | **Brand listing is auth-gated while categories are public; no public shop-category listing exists.** | `BrandRoutes.kt:20`, `ShopCategoryRoutes.kt:15-45` | Inconsistent public API surface; onboarding can't fetch shop categories. |
| R2-M26 | **Shop approve/reject/suspend/activate have no transition validation**; `activateShop` silently no-ops unless SUSPENDED. | `ShopRepositoryImpl.kt:144-150` | Nonsensical admin states (re-approve, reject a live shop). |
| R2-M27 | **`blacklisted_token` table grows unbounded and stores full JWTs in plaintext.** | `BlacklistedToken.kt:9-12`, `AuthRepositoryImpl.kt:269-275` | Storage growth + token-material exposure in DB. |
| R2-M28 | **SUPER_ADMIN can freely promote/demote SUPER_ADMINs** (only self-change blocked); `superAdminAuth` primitive is unused — ADMIN ≡ SUPER_ADMIN in practice. | `Enums.kt:191-197`, `RouteAuthDsl.kt:72` | No admin/separation or escalation audit. |

### 7.4 Low / design gaps

- **Login timing + state enumeration:** non-existent account returns 401 without running BCrypt (timing side-channel); deactivated/unverified return distinct 400s vs wrong-password 401; `remainingAttempts` disclosed to unauthenticated callers. `UserAuthenticationService.kt:101-128`.
- **Lockout DoS on non-existent accounts:** `recordFailedAttempt` writes `login_attempt` rows for arbitrary (email, role); rows never cleaned. `AuthRepositoryImpl.kt:81-114`.
- **`getProductsByUser` hides a seller's own inactive products; no product status-update path exists** (`OUT_OF_STOCK` never set). `ProductRepositoryImpl.kt:181-185`.
- **`getProductDetail` serves any status** (no ACTIVE filter). `ProductRepositoryImpl.kt:187-189`.
- **`getCartSummary` picks an arbitrary inventory row** for products in multiple shops. `CartRepositoryImpl.kt:115-118`.
- **Dashboard:** "today" order count includes CANCELED (`DashboardRepositoryImpl.kt:39`); `getRecentActivity` embeds user emails (`:162`); `getTopProducts` N+1 (`:144`).
- **`generateOTP` off-by-one** (max value never produced). `SecurityUtils.kt:9-13`.
- **OTP expiry uses server-local time** vs DB UTC. `UserAuthenticationService.kt:51,165`.
- **`suspendRetryQuery` retries all exceptions** incl. validation (register race surfaces as `OTP_ALREADY_SENT` after retries; latency). `TransactionExt.kt:39-66`.
- **Logout requires a still-valid access token** (expired → 401 before logout). `AuthRoutes.kt:98-111`.
- **`getInventoryForUpdate` fine; low-stock listing includes OUT_OF_STOCK; `stock == min` classified LOW_STOCK.** `InventoryRepositoryImpl.kt:76`, `Enums.kt:167-171`.
- **Dead `createOrder` direct-order endpoint** — defined, never routed, no tax/shipping/coupon/reservations. `OrderRepositoryImpl.kt:321-417`.
- **`register` doubles as OTP resend gated by 10-min expiry; no dedicated throttled resend endpoint.**

### 7.5 Missing functionality (confirmed absent)

- Payment gateway / real charge / webhooks — `createPayment` only writes a DB row.
- Invoice generation; refund to original payment method; partial payments/partial refunds.
- Order cancellation window; stock reorder/alert automation; multi-currency (orders always `"USD"` default).
- Per-user coupon limits; account deletion/closure; email-change flow; admin-forced password reset; MFA; password expiry; remember-me; session-revocation UI.
- Seller approval gate (PENDING status is ignored); consent enforcement at registration; policy update/deactivate routes.
- Category/brand/shop/product public-read consistency (shop-category listing, public brands).

### 7.6 Round-2 priority fix list

**R2-P0 (fix first — payment/inventory integrity):** R2-C1 payment bypass (server-derived status + ownership) → C2 inventory ownership → C3 payment IDOR → C5 `order.status = PAID` in finalize + PAID requires payment → C8 product/no-shop approval gate.

**R2-P1 (money correctness):** C4 refund dedup (cumulative cap + unique orderItem) → C6 coupon/seller-metric reversal on cancel → C7 verified-purchase reviews → H4 cancel-PAID inconsistency → H5 refund-on-unpaid → H6 per-item refund tracking → M3 dashboard revenue → M23 summary/placeOrder cents.

**R2-P2 (auth/session):** H1 token revocation on password/role change → H2 DB role re-check in `ConfigureAuth` → H3 lockout check in refresh → H7 self-deactivation guard → H8/H9 reset-enumeration + shared OTP counter → M20 change-password rate limit → M21 OTP resend.

**R2-P3 (data integrity/UX):** M2 category delete semantics → M4/M5 unique constraints (cart/wishlist/review/coupon_usage) → M6 cache invalidation on sales/rating → M7 MDC cleanup → M9/M10 shop/product visibility by APPROVED → M11 stock SET-0 + max enforcement → M12/M13 consent + policy routes → M24 concurrency-safe cancel → M26 shop transition validation.

**R2-P4 (hardening):** M8 forwarded-header trust → M19 coupon lookup → M22 role-plugin finish → M27 blacklist purge/hash → M28 SUPER_ADMIN separation + `superAdminAuth` adoption.

### 7.7 Round-2 fixes applied

All items below are implemented, compiling and ktlint-clean (`./gradlew compileKotlin compileTestKotlin ktlintCheck`). Verified 2026-08-01.

**Critical — resolved ✅**
- **R2-C1 / R2-C3 (payment bypass + read IDOR):** `PaymentRequest.status` no longer trusted — `PaymentService.createPayment` derives `PaymentStatus.COMPLETED` server-side, rejects non-owners (`Message.Payments.NOT_ORDER_OWNER`), keeps the idempotency/order-match + overpayment checks, and `getPaymentById`/`getPaymentsByOrderId` take `(callerUserId, userType)` and enforce owner-or-admin (`ensurePaymentViewAccess`). Added `ApplicationCall.requireUserType()`. `PaymentRoutes.kt`, `PaymentService.kt`, `PaymentRepository(+Impl).kt`, `AuthExt.kt`.
- **R2-C2 (cross-tenant inventory):** new `InventoryAccess(isSellerOwner, isAdmin)` fact + `getInventoryShopId`/`getProductShopId`/`getSellerShopId`; all service methods now take `userId`/`userType` and enforce owner-or-admin; `getLowStockProducts` is scoped to the seller's own shop. `InventoryRoutes.kt`, `InventoryService.kt`, `InventoryRepository(+Impl).kt`, `Message.Inventory`.
- **R2-C5 (finalize never set `PAID`):** `finalizeOrderPayment` now sets `order.status = OrderStatus.PAID` + `paymentStatus=COMPLETED` + reservations FINALIZED. `PaymentRepositoryImpl.kt`.
- **R2-C4 / R2-H5 / R2-H6 / R2-M15 (refund money safety):** `createRefundRequest` now requires a COMPLETED payment (`RefundOrderAccess.isOrderPaid`) and blocks re-request when a `REFUNDED` refund exists; `RefundAccess` carries `alreadyRefundedAmount` (all active + REFUNDED) and the service caps the new amount at `itemTotal − otherRefunds` and rejects non-positive amounts; order `paymentStatus` becomes `PARTIALLY_REFUNDED` until every item is refunded (new enum value, VARCHAR storage — no migration needed). `Enums.kt`, `RefundRequestRepository(+Impl).kt`, `RefundRequestService.kt`.
- **R2-C6 (no reversal on cancel):** `applyOrderCancellation` now reverses coupon `usageCount`/deletes `CouponUsage` rows and subtracts seller `totalSales`/`totalCommission` (floored at 0). `OrderRepositoryImpl.kt`.
- **R2-C7 (reviews without purchase / self-rating):** new `ReviewCreateAccess(isVerifiedPurchase, isProductSeller)`; reviews now require a DELIVERED/RECEIVED order and block the product's shop owner; `is_verified_purchase` is set true. `ReviewRatingRepository(+Impl).kt`, `ReviewRatingService.kt`.
- **R2-C8 (no-shop / non-approved products):** `ProductCreateAccess` now carries `shopStatus`; `createProduct` requires a seller with an owned, `APPROVED` shop; public catalog queries (`getProducts`, `getProductsByCategory`, featured/best-selling/hot-deals, and raw-SQL search + facets) only return products from `APPROVED` shops. `ProductCrudService.kt`, `ProductRepository(+Impl).kt`, `Message.Products`.

**High — resolved ✅**
- **R2-H1 (tokens survive password/role change):** `changePassword`, `resetPassword`, `changeUserType` now call `revokeAllUserTokens`. `UserAuthenticationService.kt`.
- **R2-H2 (JWT claim trusted):** `ConfigureAuth.validate` loads the user from DB and issues `JwtTokenRequest(..., user.userType.name)` from the DB row, so role/status changes take effect immediately. `ConfigureAuth.kt`.
- **R2-H3 (locked account refresh):** `refreshAccessToken` rejects (and revokes) refresh when the user's login attempt is locked. `UserAuthenticationService.kt`.
- **R2-H4 (cancel-PAID inconsistency):** removed `PAID → CANCELED` from `OrderStatus.canTransitionTo`; paid orders must go through the refund flow. `Enums.kt`.
- **R2-H7 (self-deactivation):** `deactivateUser` now blocks `currentUserId == targetUserId`. `UserAuthenticationService.kt`.
- **R2-H8 (reset enumeration):** `forgotPassword` no longer reveals account existence — missing users and role mismatches return the generic OTP-sent response without an email. `UserAuthenticationService.kt`.
- **R2-H9 (shared OTP counter):** `otp_attempt` now tracks attempts per `(user_id, purpose)` via new `OtpPurpose` (REGISTRATION/RESET); repo/service thread the purpose through every counter call. `V9__...sql`, `OtpAttempt.kt`, `AuthRepository(+Impl).kt`, `UserAuthenticationService.kt`.

**Medium — resolved ✅**
- **R2-M2 (category delete wipe):** `deleteCategory` blocks deletion while products reference the category (`Message.Categories.IN_USE`). `ProductCategoryRepositoryImpl.kt`.
- **R2-M3 (dashboard revenue counts unpaid):** revenue/today/avg queries now require `paymentStatus = COMPLETED`. `DashboardRepositoryImpl.kt`.
- **R2-M4 (cart 409 / duplicate rows):** `createCart` merges quantity on an existing row; V9 adds a unique `(user_id, product_id)` index. `CartRepositoryImpl.kt`, `V9__...sql`.
- **R2-M5 (race-prone wishlist/review):** V9 adds unique indexes on `wishlist` and `review_rating` `(user_id, product_id)`. `V9__...sql`.
- **R2-M6 (stale product caches):** order placement/cancel and review add/update/delete invalidate `CacheKeys.PRODUCTS_PATTERN`. `OrderService.kt`, `ReviewRatingService.kt`.
- **R2-M7 (MDC leak):** a `Call`-stage interceptor removes `requestId` from MDC after completion. `RequestTracing.kt`.
- **R2-M9 (PENDING shops visible):** public `getShops`/`getShopsByCategory` now return only `APPROVED` shops (admin status views untouched). `ShopRepositoryImpl.kt`.
- **R2-M10 (non-approved shop products):** see R2-C8 — all public catalog queries filter to approved shops.
- **R2-M11 (stock SET-0 / max):** `updateStock` allows `SET 0`, rejects only negative / non-SET-zero, enforces `maximumStockLevel`, and `createOrUpdateInventory` validates `min ≤ max`. `InventoryService.kt`.
- **R2-M20 (change-password brute force):** `PATCH change-password` is now wrapped in the per-user `WRITE` rate-limit zone. `AuthRoutes.kt`.
- **R2-M21 (OTP resend):** `forgotPassword` reuses a still-valid reset OTP instead of silently dropping the resend. `UserAuthenticationService.kt`.
- **R2-M26 (shop transitions):** approve/reject/suspend/activate now validate the current status (`invalidStatus`, `ALREADY_APPROVED`/`ALREADY_SUSPENDED`). `ShopRepositoryImpl.kt`.

**Still open (out of scope of this pass):** R2-M1, R2-M8, R2-M12, R2-M13, R2-M14 (partially — unique coupon_usage index added in V9), R2-M16, R2-M17, R2-M18, R2-M19, R2-M22, R2-M23, R2-M24, R2-M25, R2-M27, R2-M28, and the 7.4 Low items.
