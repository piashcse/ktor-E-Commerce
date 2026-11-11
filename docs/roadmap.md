# Product Roadmap — Contemporary E-Commerce Gaps

This roadmap tracks the gaps identified in the full codebase audit. Each epic is implemented on
its own branch so it can be reviewed and merged as an independent PR.

**Convention**

- Branch: `feature/<short-name>` or `fix/<short-name>` off `master`
- One epic per PR, with migration + tests + docs
- No cross-epic breaking changes
- Every PR updates this table (`Status`)

**Status legend:** ✅ implemented · 🚧 in progress · 📋 planned · ⏸ deferred

---

## Capability matrix

| # | Epic | Why it matters | Priority | Effort | Status | Branch |
|---|---|---|---|---|---|---|
| 1 | Real payment provider + webhooks + 3DS | Every order depends on payment capture; cards/wallets are default | P0 | High | ✅ | `feature/payment-provider` |
| 2 | Carrier integration + shipments + labels + tracking | Shipping status is the top post-purchase support driver | P0 | High | ✅ | `feature/shipping-carrier` |
| 3 | Tax zones/classes/exemptions + inclusive pricing | VAT/GST compliance blocks cross-border sales | P0 | High | ✅ | `feature/tax-engine` |
| 4 | Fraud/risk + chargeback workflow | Direct revenue loss and compliance risk | P0 | High | ✅ | `feature/fraud-risk` |
| 5 | Promotions engine | Automatic discounts/free-shipping thresholds drive conversion | P1 | Medium | ✅ | `feature/promotions-engine` |
| 6 | Review media + moderation | UGC improves trust/conversion; `ReviewStatus` already exists | P1 | Low | ✅ | `feature/review-media-moderation` |
| 7 | Search suggestions + facets + synonyms | Search users convert at the highest rate | P1 | Medium | ✅ | `feature/search-suggestions` |
| 8 | Order status/shipping notifications | Reduces WISMO support and improves repeat purchase | P1 | Low | ✅ | `feature/order-notifications` |
| 9 | Printable invoices + packing slips | Every order/return needs an operational document | P1 | Low | ✅ | `feature/printable-invoices` |
| 10 | Back-in-stock + price-drop alerts | High-ROI retention using existing outbox/preferences | P1 | Medium | ✅ | `feature/stock-alerts` |
| 11 | Social login + MFA/passkeys | Reduces account friction and improves security | P1 | Medium | 🚧 | `feature/mfa-totp` |
| 12 | Loyalty/referrals/gift cards/store credit | Retention and repeat-purchase levers | P2 | Medium | ✅ | `feature/loyalty-wallet` |
| 13 | i18n + localized checkout | Required for cross-border growth | P2 | Medium | ✅ | `feature/localization` |
| 14 | Funnel/attribution analytics | Needed to optimize conversion work | P2 | Medium | ✅ | `feature/analytics-funnel` |
| 15 | Seller reviews/disputes/commission rules | Marketplace trust and economics | P2 | Medium | ✅ | `feature/seller-tools` |
| 16 | Subscriptions + digital goods | New revenue models | P2 | High | ✅ | `feature/subscriptions` |
| 17 | B2B quotes/POs/net terms | Wholesale revenue | P3 | High | ✅ | `feature/b2b` |
| 18 | BOPIS/POS/store locator | Omnichannel | P3 | High | ✅ | `feature/omnichannel` |
| 19 | CMS/SEO/sitemap | Storefront acquisition | P3 | Medium | ✅ | `feature/content-seo` |
| 20 | Support tickets/chat | Customer service | P3 | Medium | 📋 | `feature/support` |

---

## Implemented baseline

The following are already production-grade and should not be rebuilt:

- JWT auth, refresh rotation, OTP, password reset, login lockout, API keys, RBAC
- Catalog, variants/options, images, PostgreSQL search, related/also-bought recommendations
- Cart, wishlist, guest checkout, addresses, shipping methods
- Orders, status history, tracking fields, cancellation, idempotency
- Coupons, tax rules, FX, inventory reservations, multi-warehouse
- Refunds and RMA/exchanges
- Reviews, helpful votes, verified-purchase flag
- Seller KYC, payouts, statements
- Notifications + per-event preferences
- Policy consent, GDPR export/erase
- Outbox/dead letters, feature flags, audit log, dashboards, metrics
- OpenAPI docs, CI, integration tests, coverage gates

---

## Epic acceptance criteria

### 1. Payment provider
- Provider adapter behind `PaymentGateway`
- Webhook route with signature verification
- 3DS/SCA result propagation
- Provider-side refunds
- Idempotent payment creation and confirmation

### 2. Shipping carrier
- `Shipment` entity with carrier/service/tracking/label/status
- Rate quote endpoint
- Label purchase endpoint
- Tracking webhook or poller
- Per-line/partial fulfillment

### 3. Tax engine
- Tax zones/nexus and product tax classes
- Inclusive vs exclusive pricing
- Business/customer exemptions
- Per-line tax breakdown

### 4. Fraud/risk
- Velocity rules and blocklists
- AVS/CVV result storage
- 3DS enforcement rules
- Chargeback/dispute records

### 5. Promotions
- Automatic percentage/fixed/free-shipping/BXGY rules
- Priority and stacking rules
- Cart/checkout evaluation
- Admin CRUD

### 6. Review media + moderation
- Review image table and endpoints
- Admin list + status update (`ACTIVE`/`HIDDEN`/`FLAGGED`)
- Ownership and moderation authorization

### 7. Search suggestions
- `GET /api/v1/products/suggest?q=`
- Product/category/brand suggestions
- Trigram-backed ranking and limit clamp

### 8. Order notifications
- `OrderStatusChangedEvent`
- Templates for shipped/delivered/canceled
- Subscriber + preference support

### 9. Printable invoices
- `GET /api/v1/orders/{id}/invoice`
- `GET /api/v1/orders/{id}/packing-slip`
- Owner/seller/admin authorization
- HTML suitable for print-to-PDF

### 10. Stock alerts
- Subscribe/unsubscribe endpoints
- Back-in-stock event on inventory replenishment
- Notification via existing dispatcher/preferences

### 11. Identity hardening
- TOTP MFA setup/enable/disable and login enforcement (done)
- Social login (planned)
- Passkeys/WebAuthn (planned)

---

## Delivery sequence

| Phase | Epics |
|---|---|
| 1. Launch-critical | 1, 2, 3, 4 |
| 2. Conversion | 5, 6, 7, 8, 9, 10 |
| 3. Trust & retention | 11, 12, 13, 14, 15 |
| 4. Expansion | 16, 17, 18, 19, 20 |
