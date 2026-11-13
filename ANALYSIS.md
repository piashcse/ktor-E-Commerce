# ktor-ecom — Deep Analysis & Remediation Plan

> Static review of ~37k LOC of Kotlin, 55 Flyway migrations, routes/auth DSL, tests, docs, Docker and CI.
> Each item is implemented on a dedicated branch. Status: `[ ]` open, `[~]` partially implemented, `[x]` fixed.

**Baseline:** `master` @ `d182c4c`, `./gradlew compileKotlin` passes.

---

## 0. Executive Summary

The project has a strong skeleton: Ktor 3, Exposed, BigDecimal money, Flyway, JWT, an outbox, OpenAPI and a large route/test surface. The highest-risk areas are:

1. Read-replica configuration silently becomes the default Exposed database, breaking all writes.
2. API-key scopes are not enforced on role-gated routes.
3. MFA login writes a detached DAO outside a transaction.
4. Social login allows pre-account hijacking.
5. Provider refunds run before the DB transaction that records them.
6. Payout rejection strands seller earnings; refund/payout math is wrong.
7. Checkout totals, tax, coupons, wallet and split orders disagree.
8. Notification preferences and several advertised flags/features are not honored.
9. GDPR export/erasure is incomplete.
10. Large god classes and snake_case packages need refactoring.

---

## 1. Critical / High Findings

### 1.1 Security, auth, access control

- [x] **MFA login DAO write outside transaction.** `UserAuthenticationService.kt:118-120` writes `user.mfaLastUsedStep` after `findUserByEmailAndType()` commits. Exposed 1.6.1 `Entity.setValue(Column...)` calls `TransactionManager.current()` → 500 for MFA logins and no replay protection.
- [x] **API-key scopes ignored by `requireRole`.** `RouteAuthDsl.kt:44-54,145-185`; only two routes use `requirePermission`.
- [x] **Empty API-key scopes fail open.** `ApiKeyService.kt:45-61`; `JwtTokenRequest.kt:74-80`.
- [x] **Social login pre-hijack.** `SocialAuthService.kt:26-42,51-61` matches by email only and auto-verifies unverified password accounts without clearing the password.
- [x] **MFA/passkey enrollment needs password re-auth.** `AuthRoutes.kt:212-324`; a stolen access token/API key can enroll a new TOTP secret or passkey.
- [x] **Login lockout bypass via email case.** `AuthRepositoryImpl.kt:122-147` stores raw email, reads normalized email.
- [x] **Tax exemption trusts client-supplied `businessTaxId`.** `TaxService.kt:184-192`; `CheckoutRequest.kt:31`.
- [x] **JWT blacklist header parsing is case-sensitive.** `ConfigureAuth.kt:34-42,53-60`; `AuthRoutes.kt:217-219,239-243`.
- [x] **Guest checkout bypasses policy consent.** `CheckoutRoutes.kt:44-51,132-136`; `GuestCheckoutService.kt:103-115`.
- [x] **GDPR export/erase incomplete and customer-only.** `PrivacyService.kt:46,47-114,144-183`; `PrivacyRoutes.kt:17`.
- [ ] Refresh rotation not atomic with new-token storage. `AuthRepositoryImpl.kt:487-514`.
- [ ] Access tokens have no `jti`/version; only current token is revoked. `AuthRepositoryImpl.kt:276-288,354-358`.
- [ ] Passkey challenge is raw UUID, not base64url. `PasskeyService.kt:344-358`.
- [ ] MFA/reset OTPs stored plaintext. `User.kt:19-22`.
- [ ] MFA/passkey step-up endpoints lack rate limiting. `AuthRoutes.kt:288-324`.
- [ ] Account enumeration on pre-auth paths.

### 1.2 Money, checkout, orders

- [x] **Read replica becomes default database.** `ConfigureDataBase.kt:37,39-58`; Exposed `addDatabase` pushes, `primaryDatabase = default ?: first`.
- [x] **Wallet redemption double-subtracts points.** `OrderRepositoryImpl.kt:662-675` (line 673).
- [x] **Split-order wallet/gift reference is first order only.** `OrderRepositoryImpl.kt:649,664,672,679`.
- [x] **Shop/category coupons discount whole basket.** `OrderRepositoryImpl.kt:152-161,604-613`.
- [x] **Coupon limits counted per split order.** `OrderRepositoryImpl.kt:139-150,178-194`.
- [x] **Checkout summary/order shipping disagree.** `OrderRepositoryImpl.kt:856-866` vs `551-556`.
- [x] **Two tax engines (cart 5% vs TaxService).** `PricingService.kt:17,27`; `CartRepositoryImpl.kt:250-253`.
- [x] **Tax-inclusive totals do not reconcile.** `OrderRepositoryImpl.kt:560-561,728-734,902-906`; `InvoiceService.kt:75-79`.
- [x] **Seller payout reduced by wallet/gift discounts.** `NotificationSubscriber.kt:208-225`; `OrderRepositoryImpl.kt:692`.
- [x] **Gateway charged before final re-validation.** `PaymentRepositoryImpl.kt:224-246`.
- [x] **Paid order cancellation marks REFUNDED without refund.** `OrderCancellation.kt:51-53`.
- [x] **Pickup/warehouse stock restored to wrong warehouse.** `Inventory.kt:64-91,120-134`; `OrderCancellation.kt:70-73`; `RefundRequestRepositoryImpl.kt:194-198`.
- [x] **`OrderItem.discountAmount` always zero.** `OrderRepositoryImpl.kt:523`; `OrderMappers.kt:65`.
- [x] **Checkout formatted fields stale after discount tax recalculation.** `OrderRepositoryImpl.kt:970-1020`.
- [x] **Invalid status/date query params return 500.** `OrderRepositoryImpl.kt:1212,1225`; dashboard/analytics parse paths.
- [x] **Guest order tracking email case mismatch.** `OrderRepositoryImpl.kt:369,1064-1065`.
- [x] **`retryQuery` retries permanent DB errors.** `TransactionExt.kt:38-61`.
- [x] **`minOrderQuantity` / `freeShipping` unused.** `Product.kt:28,37`.
- [ ] Promotion usage never reversed. `OrderRepositoryImpl` promotion path; `OrderCancellation.kt:85-97`.
- [ ] Tax zone selection can ignore a specific non-nexus zone. `TaxService.kt:208-216`.
- [ ] Offset timestamps drop offset instead of converting to UTC.
- [ ] Money as `Double` in product filters.

### 1.3 Refunds, payouts, subscriptions

- [x] **Provider refund before DB transaction.** `RefundRequestRepositoryImpl.kt:238-288,290-341` (`:316-318`).
- [x] **Shipping refund capped per item.** `RefundRequestRepositoryImpl.kt:371-375,433-442`.
- [x] **Partial refund reduces seller payout by gross amount.** `RefundRequestRepositoryImpl.kt:204-217`.
- [x] **Partial-quantity refund blocks remainder.** `RefundRequestRepositoryImpl.kt:53,101-112`.
- [x] **Payout rejection strands earnings.** `PayoutRepositoryImpl.kt:75-95,318-339`.
- [x] **Payout request amount not tied to claimed rows.** `PayoutRepositoryImpl.kt:215-301`.
- [x] **Payout statement tax ignores period.** `PayoutRepositoryImpl.kt:117-122`.
- [x] **RMA marks COMPLETED while refund is PENDING; duplicate RMAs allowed.** `RmaService.kt:249-303,73-120`.
- [x] **Subscription activation trusts client `paymentReference`.** `SubscriptionService.kt:208-234`.
- [ ] Refund cap includes wallet/gift; partial refunds restore neither.
- [ ] Multi-currency payout balance mixes currencies.
- [ ] Subscription billing has no scheduler and no self-serve PENDING activation.
- [ ] Digital `maxDownloads` not enforced.

### 1.4 Infrastructure / operations

- [x] **Outbox is not claim-safe and dead-letters transient failures.** `OutboxPoller.kt:69-90,118-135`; `EventBus.kt:100-109`.
- [x] **SMTP has no connect/read timeout.** `EmailSender.kt:44-70`.
- [x] **Image decompression bomb guard runs after decode.** `ImageCompressor.kt:27-32`.
- [x] **Workers not awaited on shutdown; channel stays open.** `OutboxPoller.kt:40-43`; `AsyncWorker.kt:31-42`; `Application.kt:55-66`.
- [x] **EventBus subscribers never cleared.** `EventBus.kt:74-93,193-196`.
- [x] **Docker health check probes `/health/live`.** `Dockerfile:22-23`; `docker-compose.yml:43-47`.
- [x] **Signed download credentials leak into logs.** `ConfigureBasic.kt:187-216`; `SubscriptionRoutes.kt:132-153`.
- [x] **Cleanup omits dead-letter and passkey challenge tables.** `MaintenanceCleanup.kt:43-136`.
- [x] **Product detail cache invalidated before read.** `ProductRoutes.kt:80-84`.
- [x] **JWT secret reused for TOTP AES and download HMAC.** `docker-compose.yml:25-38`; `MfaSecretCipher.kt:43-46`; `DigitalDeliveryService.kt:54-56`.
- [x] **`.env.example` DB name mismatch; CI over-privileged; Trivy non-blocking.**
- [ ] Upload quota TOCTOU.
- [ ] `withTimeout` cannot interrupt blocking JDBC readiness probe.

### 1.5 Catalog / notifications / platform

- [x] **Public feeds leak pending-shop products.** `ProductRepositoryImpl.kt:551-585`.
- [x] **Inventory create/update lost update.** `InventoryRepositoryImpl.kt:118-142`.
- [x] **Wishlist variant products cannot move to cart.** `WishListRepositoryImpl.kt:83-99`.
- [x] **Wishlist allows deleted/inactive products.** `WishListRepositoryImpl.kt:26,48-50,86`.
- [x] **Stock-alert unique index prevents variant subscriptions.** `V30__stock_alerts.sql:16`.
- [x] **Notification default channel contradiction.** `NotificationPreferencesService.kt:58,68`; `NotificationProviders.kt:96`.
- [x] **Cleanup deletes notification preferences.** `MaintenanceCleanup.kt:95-98`.
- [x] **`POST /notifications/send` bypasses preferences/providers.** `NotificationRepositoryImpl.kt:158-165`.
- [x] **Policy future `effectiveDate` never activates.** `PolicyRepositoryImpl.kt:47-65,102-110`.
- [x] **Analytics consent defaults true and events are not user-linked.** `AnalyticsService.kt:61-65`; `AnalyticsRoutes.kt:20-26`.
- [x] **Fraud threshold 0 fires for everyone; `>=` off-by-one; refund rows counted; mixed-currency sums.** `FraudService.kt:57,158,313-324`.
- [x] **Chargeback amount unvalidated.** `ChargebackService.kt:26-32,83-92`.
- [x] **Dashboard top-product revenue ignores FX/discount; revenue ignores partial refunds.** `DashboardRepositoryImpl.kt:204-214,37-46`.
- [x] **Feature flags `require_policy_consent` and `tax_rules` unused.**
- [ ] SMS/push providers inert but selectable.
- [ ] Back-in-stock ignores `user.isActive`.
- [ ] Seller dashboard missing top products.
- [ ] Analytics funnel mixes sessions and orders.
- [ ] Shipment/PO/RMA/POS incomplete flows.

---

## 2. Logic Contradictions by Feature

| Feature | Contradiction |
|---|---|
| Payment integration | Admin confirm and subscription `paymentReference` complete payments without a provider charge. |
| Stock restoration | Restores to oldest warehouse, not the pickup warehouse. |
| Order state machine | Pickup sets `RECEIVED` from `PAID`, bypassing `canTransitionTo`. |
| Coupon scope | Scoped coupons discount the whole basket. |
| Multi-currency | B2B/POS hardcode USD; fraud/attribution/dashboard mix currencies. |
| Loyalty wallet | Split orders share the first order reference. |
| Seller payout | Wallet/gift card reduce seller earnings. |
| Notification prefs | API says IN_APP, dispatcher defaults EMAIL. |
| API keys | Scopes advertised but ignored by role routes. |
| Feature flags | Seeded flags are dead. |
| Consent | Guest checkout bypasses required consent. |
| Policy effective date | Future policies never activate. |
| GDPR | Export/erase incomplete. |
| Subscription | Trial/auto-renew/PENDING activation not implemented. |
| Digital delivery | `maxDownloads` not enforced. |
| B2B | Credit excludes invoices; dunning sends nothing; no payment ledger/tax/address. |
| POS | Idempotency not stored; card/PayPal sales are synthetic. |
| Shipping | Transient webhook failures return 200. |
| RMA | COMPLETED while refund is PENDING. |
| KYC | Verify without document/bank details. |
| Tax | Cart uses 5%, checkout uses zones; inclusive tax does not reconcile. |
| Product | `freeShipping` and `minOrderQuantity` unused. |
| Stock alerts | Unique index prevents multiple variants. |
| Wishlist | Variant products cannot move to cart. |
| Pagination | Comment says page/perPage wins; code gives limit/offset precedence. |

---

## 3. Incomplete Features

| Feature | Missing |
|---|---|
| Payment provider | Manual default; Stripe webhook/3DS/refund wiring partial; subscription/B2B bypass. |
| Subscription billing | No scheduler, no self-serve activation, no proration/upgrade/refund, no immediate cancel. |
| Digital delivery | `maxDownloads` enforcement, renewal reset, user-bound signed URLs. |
| SMS/push | Providers inert but selectable. |
| Notification inbox | No delete endpoint; 1-year retention not implemented. |
| B2B | PO cancel, invoice VOID/credit notes, reminders, tax, delivery address, stock reservation, payment ledger. |
| POS | Card processing, idempotency, CSV export, POS refunds, drawer refund accounting. |
| RMA | Replacement order creation, exchange shipping. |
| KYC | Document upload, URL ownership, audit trail. |
| Shipping | Label cancel/refund, rate quote persistence/validation. |
| Support | Seller-facing routes. |
| Feature flags | `require_policy_consent`, `tax_rules` dead. |
| Seller dashboard | Top products. |
| GDPR | Complete export/erase, analytics user linkage. |
| Analytics | User attribution, funnel dedupe, price-drop alerts. |
| Invoice | Invoice sequence, tax breakdown, seller tax ID, PDF. |
| Product data | `minOrderQuantity`, `freeShipping`, `barcode`, `weight`, `dimensions`, `reservedQuantity`. |
| Search | `RELEVANCE_FORMULA` unused in one path. |
| Refunds | Partial remainder, cumulative shipping/tax, wallet/gift restoration, provider idempotency. |
| Payouts | Request amount vs rows, multi-currency, rejection recovery, partial approval. |
| Inventory | `reservedQuantity`, warehouse-aware restoration. |

---

## 4. Refactor / Naming (Industry Standard)

### Packages

```
feature/audit_log            -> feature/audit
feature/refund_request       -> feature/refund
feature/review_rating        -> feature/review
feature/shipping_address     -> feature/address
feature/shipping_method      -> feature/shipping
feature/product_category     -> feature/catalog/category
feature/product_sub_category -> feature/catalog/subcategory
feature/shop_category        -> feature/shop/category
feature/stockalert           -> feature/stockalert (currently only one without underscore)
com.piashcse                 -> com.<org>.<app>
```

Target layout:

```
com.<org>.<app>
├── domain/          # entities, value objects, repository interfaces, domain services
├── application/     # use cases / orchestration
├── infrastructure/  # Exposed repositories, gateways, SMTP, storage
├── api/             # routes, DTOs, auth, error mapping
└── config/          # env, DI, migrations
```

### Classes

| Current | Recommended |
|---|---|
| `WishListDAO/Table/Repository` | `WishlistDAO/Table/Repository` |
| `OrderRepositoryImpl` (1,230 LOC) | `OrderQueryRepository`, `OrderPlacementService`, `OrderPricingService`, `OrderCancellationService`, `OrderFulfillmentService` |
| `B2bService` (798 LOC) | `B2bQuoteService`, `B2bPurchaseOrderService`, `B2bInvoiceService` |
| `OmnichannelService` (726 LOC) | `PosService`, `PickupService`, `StoreLocatorService` |
| `*RepositoryImpl` | `*ExposedRepository` / `*JdbcRepository` |
| `UserType` | `Role` |
| `PaymentRepositoryImpl` | `PaymentService` + `PaymentRepository` |
| `SellerToolsService` | `DisputeService`, `CommissionService`, `SellerReviewService` |
| `shopSellerRoutesV1` | `shopSellerRoutes` |
| `OrderStatus.RECEIVED` + `DELIVERED` | one terminal state |

### Data / contracts

- Replace raw status strings with enums (`PayoutStatus`).
- Use `Instant`/`OffsetDateTime` + PostgreSQL `timestamptz` everywhere.
- Introduce a `Money(amount, currency)` value object; remove `String`/`Double` money in DTOs and filters.
- Move inline DTOs out of services into `model/request` and `model/response`.
- One error contract (`ApiError`) and one pagination contract.
- Replace raw JDBC in `ProductRepositoryImpl`/`DashboardRepositoryImpl` with Exposed/queries.
- Split god files and enforce application-level transaction/saga boundaries.
- Store notification preferences in `notification_preference`, not fake `notification` rows.
- Use `uuid`/ULID IDs and plural snake_case table names; `order` is a reserved word.

---

## 5. Fix Order

1. Read-replica default database.
2. MFA transaction write.
3. API-key scope enforcement.
4. Gateway/refund transaction ordering.
5. Seller money math.
6. Wallet/split-order correctness.
7. Single tax engine and inclusive-tax reconciliation.
8. Coupon scope/usage.
9. Auth pre-hijack/session revocation.
10. Outbox claim semantics and shutdown.
11. Notification preferences.
12. Catalog/shipping/B2B/subscriptions/POS/GDPR.
13. Refactoring and renaming.

---

## 6. Implementation Branches (this pass)

| Branch | Base | Scope shipped | Verified |
|---|---|---|---|
| `fix/01-security-auth` | `master` | MFA-step transaction, API-key scope fail-closed, social pre-hijack, email-case lockout, bearer parsing, MFA password step-up, guest consent, tax exemption, privacy route role, read-replica default DB | `compileKotlin`, `compileTestKotlin`, auth/permission/MFA/guest tests |
| `fix/02-commerce-money` | `fix/01-security-auth` | wallet double-subtract, scoped coupon math, per-checkout coupon usage ref, shipping/free-shipping summary parity, line discount persistence, stale formatted fields, payment compensations, paid-cancel guard, warehouse-aware restore, transient retry, guest email lookup, order status validation | compile + commerce/product tests |
| `fix/03-refunds-payouts` | `fix/02-commerce-money` | provider refund pre-validation, cumulative shipping cap, partial-quantity refunds, net-of-commission payout reduction, payout rejection recovery, statement tax period, RMA completion ordering, subscription client-reference activation removal | compile + subscription/RMA/payment/tax tests |
| `fix/04-catalog-platform` | `fix/03-refunds-payouts` | public feed visibility, inventory lock/retry, wishlist variant support, stock-alert variant uniqueness, notification defaults/cleanup, policy effective-date, fraud threshold/FX/refund exclusion, chargeback validation, dashboard FX/top revenue, analytics consent default | compile + catalog/platform tests |
| `fix/05-infra-ops` | `fix/04-catalog-platform` | claim-safe outbox, EventBus lifecycle, AsyncWorker channel lifecycle, SMTP timeouts, header-only image dimension guard, health checks, log redaction, cache invalidation removal, dead-letter/passkey cleanup, env/compose/CI hardening | compile + outbox/service tests |
| `refactor/06-wishlist-naming` | `fix/05-infra-ops` | `WishList*` → `Wishlist*` across entities, API, mappers and tests | compile + wishlist/commerce tests |
| `fix/07-marketplace-money` | `refactor/06-wishlist-naming` | B2B invoice payment ledger/payout/digital, B2B credit exposure and re-pricing, POS idempotency and card guard, shipment idempotency/duplicate-line/status monotonicity, digital download quota | compile + B2B/omnichannel/shipment/subscription/notification tests |
| `fix/08-split-order-wallet` | `fix/07-marketplace-money` | wallet/credit/gift redemptions allocated per split order so cancellation/refund reverses only that order's share | compile + commerce/order/loyalty tests |
| `fix/09-access-token-version` | `fix/08-split-order-wallet` | per-user token version claim; password change/reset increments it and rejects older access tokens | compile + auth/identity/MFA tests |
| `fix/10-tax-promotions` | `fix/09-access-token-version` | promotion usage ledger/reversal, inclusive-tax invoice labels, offset-to-UTC normalization, coupon end-of-day, analytics date validation | compile + promotion/coupon/invoice/analytics/order tests |
| `fix/11-privacy-notifications` | `fix/10-tax-promotions` | complete GDPR export/erase tables, notification external delivery after commit, back-in-stock active-user guard, analytics purchase-funnel dedupe by session | compile + privacy/notification/analytics tests |
| `fix/12-subscriptions-payouts` | `fix/11-privacy-notifications` | autonomous daily subscription billing worker, per-currency payout balances and currency-aware requests | compile + payout/subscription/after-sales tests |
| `fix/13-cart-tax-dashboard` | `fix/12-subscriptions-payouts` | cart estimate uses `TaxService`/default address instead of hardcoded 5%, seller dashboard top products | compile + dashboard/commerce/pricing tests |

> The checkbox statuses above are the **target** end state. Several items remain partial or open; see section 7.

## 7. Remaining Work (not shipped in this pass)

- **Auth/session:** refresh-rotation atomicity, passkey base64url challenge, encrypted OTP storage, step-up rate limits, enumeration-resistant responses.
- **Orders/money:** tax-inclusive checkout-summary reconciliation, money-as-`BigDecimal` in filters.
- **Refunds/payouts:** wallet/gift restoration on partial refunds, partial payout withdrawals (currently full-balance only), subscription proration/plan changes.
- **Platform:** SMS/push providers, analytics user linkage, policy version consent snapshots.
- **Marketplace edges:** B2B PO cancel/VOID/credit notes/tax/address/reminders, POS card processing (now rejected explicitly), drawer refund accounting, shipment rate validation/cancel/label refund/webhook ack semantics, RMA replacement creation, KYC document validation, seller support routes.
- **Refactor:** full package rename (`audit_log`, `refund_request`, `review_rating`, `product_category`, …), `PayoutStatus` enum, god-class decomposition (`OrderRepositoryImpl`, `B2bService`, `OmnichannelService`), DTO relocation, raw-JDBC replacement, `Instant`/`timestamptz` migration.
