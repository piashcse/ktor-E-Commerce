# Full Codebase Bug & Logic Audit

**Commit:** `a7a04d4` (plus the audit fixes in the working tree)
**Date:** 2026-11-19
**Scope:** all `src/main/kotlin`, Flyway migrations, route/auth wiring and tests.
**Method:** full-suite run, static analysis, targeted domain review, and manual
verification of every item below against the source.

## Verification

| Check | Result |
|---|---|
| PostgreSQL-backed suite | **654/654 pass**, 0 failures, 0 errors |
| No-database suite | **654 tests**, 0 failures, 27 integration skips |
| Instruction / branch / line / class | **59.43% / 31.60% / 69.50% / 87.14%** |
| CI coverage gate | **PASS**, 0 violations |
| `detekt` + `ktlintCheck` | **PASS** |
| `git diff --check` | **PASS** |

---

## 1. Bugs fixed in this audit pass

### Orders / checkout / inventory / shipping

- **Reservation cleanup could wedge forever.** A cancellation exception inside
  the single cleanup transaction rolled back the entire batch, so one order with
  a captured payment blocked all later expirations. Cleanup now processes each
  order in its own transaction and logs per-order failures.
  `service/StockReservationCleanup.kt`
- **Direct `status=PAID` bypassed the payment pipeline.** Admin status updates
  accepted any completed payment, set `paymentStatus=COMPLETED`, and skipped
  payout/loyalty/digital-delivery/reservation finalization. The endpoint now
  rejects direct `PAID`; confirmation must go through the payment pipeline.
  `feature/order/OrderRepositoryImpl.kt`
- **Order never reached DELIVERED after a failed/canceled parcel.** The
  `allDelivered` check counted `CANCELED`/`FAILED` shipments. It now ignores
  those terminal-failure rows.
  `feature/shipping/ShipmentService.kt`
- **Duplicate shipping-label requests could return 500.** The unique
  `(order_id, idempotency_key)` index is now handled by re-reading and returning
  the winner instead of propagating the constraint error.
  `feature/shipping/ShipmentService.kt`
- **`inStockOnly` was a status filter, not an inventory filter.** Public product
  listing returned ACTIVE products with zero stock while search correctly
  filtered inventory. Listing now requires an inventory row with stock > 0.
  `feature/product/ProductRepositoryImpl.kt`
- **Variant creation left no inventory bucket.** Checkout of a new variant failed
  with `Inventory.NOT_FOUND`. Creating a variant now creates a zero-stock bucket
  in every active warehouse; listing variants now reports real stock/inStock
  instead of `status == ACTIVE`.
  `feature/product/ProductVariantService.kt`, `database/entities/ProductVariant.kt`
- **Coupon tax base included ineligible lines and wallet/gift payments.**
  Coupon/promotion discounts are now persisted per eligible line, and
  wallet/gift-card amounts are excluded from the taxable base.
  `feature/order/OrderRepositoryImpl.kt`
- **Duplicate order idempotency requests returned 500.** A unique-key race is now
  caught, and the original order responses are re-read and returned.
  `feature/order/OrderRepositoryImpl.kt`
- **Shop/cancellation changes did not invalidate the catalog cache.** Suspended
  shops and cleanup-driven cancellations could serve stale product/shop data for
  the cache TTL. Invalidation now happens in the shop status path and the
  cleanup/refund shared path.
  `feature/shop/ShopRepositoryImpl.kt`, `service/StockReservationCleanup.kt`,
  `feature/refund_request/RefundRequestRepositoryImpl.kt`
- **Low-stock events were published before commit.** Inventory writes now enqueue
  `LowStockEvent` through the outbox rather than `EventBus.publish`.
  `feature/inventory/InventoryRepositoryImpl.kt`

### Payments / refunds / subscriptions

- **Admin could confirm a failed Stripe payment.** `confirmPayment` now rejects
  `FAILED` non-manual payments; only PENDING/manual flows can be confirmed.
  `feature/payment/PaymentRepositoryImpl.kt`
- **Multiple live provider intents per order.** A second intent is now returned
  only when the amount matches; otherwise the request is rejected before any
  gateway call. This closes the double-capture path.
  `feature/payment/PaymentRepositoryImpl.kt`
- **`REFUND_UPDATED` could be marked PROCESSED before reconciliation.** Refund
  webhooks are now reconciled first; only then is the event marked
  PROCESSED/IGNORED, so a failure stays retryable.
  `feature/payment/PaymentRepositoryImpl.kt`
- **Subscription webhooks did not verify amount/currency.** Invoice payment now
  rejects a provider amount/currency mismatch.
  `feature/subscription/SubscriptionBillingService.kt`,
  `feature/payment/PaymentRepositoryImpl.kt`
- **Dunning grace was not honored for digital access.** `PAST_DUE` subscriptions
  retain entitlement access until `graceUntil`.
  `feature/subscription/DigitalDeliveryService.kt`
- **Concurrent billing could double-invoice.** `bill()` re-checks
  `currentPeriodEnd` after locking the subscription.
  `feature/subscription/SubscriptionBillingService.kt`
- **Manual/no-card subscriptions never expired.** A manual renewal past the grace
  period now expires and revokes access instead of staying `PAST_DUE` forever.
  `feature/subscription/SubscriptionBillingService.kt`
- **Loyalty clawback was clamped at zero.** Refunding an order whose points were
  already spent erased the debt; the balance may now go negative and is
  recovered from future earnings.
  `feature/loyalty/LoyaltyService.kt`
- **Gift-card refunds were not locked.** Refund reversal now locks the redemption
  and card rows.
  `feature/loyalty/LoyaltyService.kt`
- **ORIGINAL refunds on COD silently marked money as returned.** An
  `ORIGINAL` refund now requires a completed provider payment; otherwise the
  request must use `BANK_TRANSFER`.
  `feature/refund_request/RefundRequestRepositoryImpl.kt`

### Auth / RBAC / uploads / operations

- **API keys bypassed exact-role gates.** A seller API key could pass
  `customerOnlyAuth`. API-key authorization now honors `exactMatch` using the
  key owner's role.
  `plugin/RouteAuthDsl.kt`
- **A non-super admin could strip another admin/super-admin's roles.** Admin
  account role mutations now require a super admin actor.
  `feature/rbac/RbacService.kt`
- **Refresh-token reuse did not invalidate access tokens.** Reuse detection now
  increments `tokenVersion`.
  `feature/auth/AuthRepositoryImpl.kt`
- **Refund evidence upload re-read a consumed multipart stream.** The validated
  bytes are now passed through to `uploadRefundImage`.
  `feature/refund_request/RefundRequestRoutes.kt`, `service/UploadService.kt`
- **Admin audit events could be lost.** `AdminAudit` now writes to the outbox
  transactionally instead of fire-and-forget `EventBus.publish`.
  `event/AdminAudit.kt`
- **Review notifications could be lost.** Review-created and seller-reply events
  now use the outbox.
  `feature/review_rating/ReviewRatingRepositoryImpl.kt`
- **Email failures were swallowed by the notification subscriber.** External
  dispatch failures now propagate to the outbox retry/dead-letter policy while
  payout capture still runs.
  `event/subscriber/NotificationSubscriber.kt`
- **Promotion end dates were truncated to midnight.** Promotion `endsAt` parsing
  now uses `endOfDay = true`, matching coupons.
  `feature/promotion/PromotionService.kt`
- **Tax zone selection ignored region specificity.** A region-specific zone now
  outranks a country-wide fallback regardless of priority.
  `feature/tax/TaxService.kt`
- **Sitemap materialized whole tables.** SQL `LIMIT` is now applied before
  `toList()`.
  `feature/cms/CmsService.kt`
- **Internal support notes counted as first response.** SLA first-response is
  now set only for customer-visible replies.
  `feature/support/SupportService.kt`
- **Suspended shops could still use POS.** POS sale/session opening now requires
  an APPROVED shop.
  `feature/omnichannel/OmnichannelService.kt`
- **Outbox claim lease could expire mid-batch.** The lease was raised from 5 to
  15 minutes to cover a full 50-row drain.
  `service/OutboxPoller.kt`

---

## 2. Conciseness and performance pass

- **Product funnel** no longer performs per-order item queries or per-product
  name lookups. Orders, order items and product names are batch-loaded once.
  Conversion now uses distinct purchased orders against distinct view sessions
  and caps at 100% instead of comparing units with sessions.
  `feature/analytics/AnalyticsService.kt`
- **Cart stock preview** now filters inventory to active warehouses and reuses
  the same stock semantics as checkout instead of summing inactive rows.
  `feature/cart/CartRepositoryImpl.kt`
- **Variant listing** batch-loads stock buckets instead of returning
  `inStock = status == ACTIVE`.
  `feature/product/ProductVariantService.kt`
- **Coupon/promotion tax allocation** is persisted per eligible line once,
  removing duplicate proportional-discount work in the tax pass.
- The audit/review event paths now reuse the outbox publisher rather than
  duplicating fire-and-forget `EventBus.publish` calls.
- `ProductVariantService.createVariant` creates the inventory buckets in the
  same transaction, avoiding a second seller inventory call.

## 3. Remaining confirmed issues (not fixed)

These are genuine issues that need a product/schema decision or a larger
refactor. They do **not** currently fail the test suite or coverage gate.

### High / money-sensitive

1. **Refunds rewrite already-PAID seller payout rows.**
   `feature/refund_request/RefundRequestRepositoryImpl.kt` subtracts the refund
   from every `SellerPayout` row for the order, regardless of status. If the
   seller was already paid, the historical paid amount is reduced with no
   clawback ledger/payout request. A refund after payout should create a negative
   clawback row and leave PAID rows immutable.

2. **Provider refund still happens before the local commit.**
   `prepareProviderRefund` calls the gateway before the transaction that stores
   `providerRefundId` and reverses stock/payout. The provider idempotency key
   (`refund:<refundId>`) makes Stripe retries safe, but a DB failure after the
   network call leaves a window where a non-idempotent provider could refund
   twice. A `REFUND_PENDING_PROVIDER` state + provider worker is the robust fix.

3. **Partially paid orders cannot be canceled.**
   `OrderCancellation.cancel` blocks whenever *any* completed payment exists, not
   only a full payment. A deposit/partial capture leaves a CONFIRMED order that
   can neither be canceled nor auto-expired. Cancellation needs an
   auto-refund/partial-refund path.

4. **Coupon `usageCount` counts split orders, not checkouts.**
   `consumeCoupon` increments by `orders.size`. A single multi-shop checkout can
   exceed a `usageLimit=1` coupon. This is a product decision: either count one
   usage per checkout, or document that usage is per split order.

5. **Payout refund/reversal and multi-currency accounting need a ledger.**
   Payout statements are now currency-scoped, but there is no combined
   cross-currency statement and no negative clawback row for paid sellers.

### Medium

6. **Promotion reversal is checkout-wide.** Cancelling one split order releases
   the whole checkout's promotion usage while sibling orders keep the discount.
   `feature/promotion/PromotionService.kt`.
7. **Seller promotions are ignored for multi-shop carts.** Only platform-wide
   promotions are eligible when the cart spans shops.
   `feature/promotion/PromotionService.kt`.
8. **Hidden/moderated reviews permanently block re-review.** The duplicate check
   and unique `(user_id, product_id)` index include HIDDEN/REJECTED rows. Users
   cannot submit a corrected review.
9. **Targeted notification send ignores per-event preferences.** The inbox row
   and delivery channel use the global stored channel rather than
   `NotificationPreferencesService.resolve(userId, type)`.
10. **Analytics conversions mix units with sessions.** Purchased units are
    divided by viewing sessions; conversion can exceed 100%. The funnel also
    performs per-order/per-product lookups (`N+1`).
11. **Warehouse deactivation strands stock.** Deactivating a warehouse with
    inventory hides that stock from `effectiveStock`; no transfer/blocking rule
    exists. Restores may fall back to another warehouse or fail if none remains.
12. **Cart stock preview disagrees with checkout.** Cart sums all inventory rows
    (including inactive warehouses), while checkout uses active warehouses and
    variant-level stock.
13. **`shipOrder` creates no Shipment row.** Manual tracking numbers cannot be
    matched by carrier tracking webhooks; updates are silently ignored.
14. **Webhook endpoints have no body-size cap or dedicated rate limit.**
    `receiveText()` buffers the whole payload before signature verification.
15. **Upload quotas are global, multi-part requests can upload extra files, and
    support/API-key writes lack rate limits.**
16. **Profile update still accepts PII in query parameters** (government ID,
    address, postcode) and the existing log redaction list does not cover all of
    them.
17. **Registration reveals verified/unverified account existence** through
    distinct `USER_EXISTS` / `OTP_ALREADY_SENT` responses.
18. **Provider refunds with no local `RefundRequest` are acknowledged but cannot
    create a ledger entry**, because the provider event has no order/item
    mapping.

### Low / hygiene

19. `StockAlertTable` still declares the old `(user_id, product_id)` unique index
    in the Exposed entity while Flyway replaced it with variant-aware indexes.
    Runtime uses Flyway, but any Exposed schema-sync would regress it.
20. `NotificationTable` entity default is `IN_APP` while V9’s DB default is
    `EMAIL`; all current inserts set the channel explicitly.
21. Many integration tests remain one large flow per domain; splitting them
    would improve failure isolation and coverage precision.

---

## 4. Items checked and found correct

- JWT issuer/audience/expiry, DB user revalidation, `tokenVersion`, blacklist
  cache, and token-bound rate-limit keys.
- Permission gates on admin route groups and seller ownership checks on
  inventory, products, shops, orders, refunds, payouts, warehouse and support
  attachments.
- Server-owned payment status, signature verification, amount/currency matching,
  webhook dedupe and refund idempotency keys.
- Atomic order placement, `FOR UPDATE` stock checks, coupon/gift-card locks,
  payment charge leases and outbox-acknowledged delivery.
- BigDecimal money handling, explicit UTC serialization, and currency-scoped FX
  fail-closed behavior.
- Migration ordering and the `V1..V69` schema path on PostgreSQL 14/16.

---

## 5. Recommended next order

1. Refund-after-payout clawback ledger (money).
2. Provider refund pending-state/worker (money reliability).
3. Partial-payment cancellation/auto-refund (state machine).
4. Promotion checkout-wide reversal and seller-scoped promotions (discounts).
5. Review re-submission + notification preferences (user flows).
6. Analytics SQL aggregation + warehouse deactivation policy (reporting/stock).
7. Webhook body limits/rate limits + upload quotas (availability).
8. Split the remaining giant integration tests (test maintainability).
