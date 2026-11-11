# Complete API Endpoint Index

Canonical index of every HTTP operation exposed by the service, mirroring the OpenAPI
document served by Swagger UI.

**Operations:** 345  
**Tags:** 35  
**Base path:** `/api/v1` unless noted otherwise

> The live contract is available at `/swagger` and `/swagger/documentation.yaml`.
> Update this file when routes change.

## Operation summary

| Tag | Operations |
|---|---:|
| [Auth](#auth) | 21 |
| [Profile](#profile) | 3 |
| [Shop](#shop) | 13 |
| [Shop Category](#shop-category) | 4 |
| [Brand](#brand) | 5 |
| [Product Category](#product-category) | 5 |
| [Product Sub Category](#product-sub-category) | 4 |
| [Product](#product) | 26 |
| [Recommendation](#recommendation) | 1 |
| [Review](#review) | 8 |
| [Wishlist](#wishlist) | 5 |
| [Cart](#cart) | 6 |
| [Checkout](#checkout) | 8 |
| [Order](#order) | 13 |
| [Payment](#payment) | 5 |
| [Coupon](#coupon) | 9 |
| [Shipping Method](#shipping-method) | 4 |
| [Shipment](#shipment) | 5 |
| [Inventory](#inventory) | 6 |
| [Warehouse](#warehouse) | 4 |
| [Refund](#refund) | 8 |
| [RMA](#rma) | 8 |
| [Notification](#notification) | 10 |
| [KYC](#kyc) | 5 |
| [Payout](#payout) | 10 |
| [Tax](#tax) | 16 |
| [Fraud](#fraud) | 13 |
| [Loyalty](#loyalty) | 9 |
| [Localization](#localization) | 5 |
| [Analytics](#analytics) | 4 |
| [Seller Tools](#seller-tools) | 14 |
| [Subscription](#subscription) | 13 |
| [B2B](#b2b) | 15 |
| [Omnichannel](#omnichannel) | 6 |
| [CMS & SEO](#cms--seo) | 10 |
| [Support](#support) | 9 |
| [FX](#fx) | 2 |
| [API Keys](#api-keys) | 3 |
| [Feature Flags](#feature-flags) | 2 |
| [Outbox](#outbox) | 3 |
| [Policy](#policy) | 6 |
| [Consent](#consent) | 5 |
| [Privacy](#privacy) | 2 |
| [Dashboard](#dashboard) | 7 |
| [Audit Log](#audit-log) | 2 |
| [Health](#health) | 3 |

## Auth

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `PUT` | `/api/v1/admin/auth/{userId}/activate` | Admin - activate a user account | Admin |
| `PUT` | `/api/v1/admin/auth/{userId}/change-user-type` | Admin - Change user type | Admin |
| `PUT` | `/api/v1/admin/auth/{userId}/deactivate` | Admin - Deactivate a user account | Admin |
| `PUT` | `/api/v1/auth/change-password` | Change password for authenticated user | Authenticated |
| `POST` | `/api/v1/auth/forgot-password` | Request password reset OTP | Public |
| `POST` | `/api/v1/auth/login` | Login | Public |
| `POST` | `/api/v1/auth/logout` | Logout authenticated user | Authenticated |
| `POST` | `/api/v1/auth/otp-verification` | Verify account OTP | Public |
| `POST` | `/api/v1/auth/refresh-token` | Refresh access token | Public |
| `POST` | `/api/v1/auth/register` | Register | Public |
| `POST` | `/api/v1/auth/social/google` | Google social login | Public |
| `POST` | `/api/v1/auth/passkeys/login/options` | Start passkey login | Public |
| `POST` | `/api/v1/auth/passkeys/login` | Finish passkey login | Public |
| `GET` | `/api/v1/auth/passkeys` | List my passkeys | Authenticated |
| `POST` | `/api/v1/auth/passkeys/register/options` | Start passkey registration | Authenticated |
| `POST` | `/api/v1/auth/passkeys/register` | Finish passkey registration | Authenticated |
| `DELETE` | `/api/v1/auth/passkeys/{id}` | Delete a passkey | Authenticated |
| `POST` | `/api/v1/auth/mfa/setup` | Start TOTP MFA setup | Authenticated |
| `POST` | `/api/v1/auth/mfa/enable` | Enable TOTP MFA | Authenticated |
| `POST` | `/api/v1/auth/mfa/disable` | Disable TOTP MFA | Authenticated |
| `POST` | `/api/v1/auth/reset-password` | Reset password with OTP | Public |

## Profile

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/profile` | Retrieve the authenticated user's profile information | Authenticated |
| `PUT` | `/api/v1/profile` | Update the authenticated user's profile information | Authenticated |
| `POST` | `/api/v1/profile/image-upload` | Upload a profile image | Authenticated |

## Shop

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `PUT` | `/api/v1/admin/shops/activate/{id}` | Admin - Activate a suspended shop | Admin |
| `PUT` | `/api/v1/admin/shops/approve/{id}` | Admin - Approve a pending shop application | Admin |
| `PUT` | `/api/v1/admin/shops/reject/{id}` | Admin - Reject a shop application | Admin |
| `GET` | `/api/v1/admin/shops/status` | Admin - Retrieve shops filtered by status | Admin |
| `PUT` | `/api/v1/admin/shops/suspend/{id}` | Admin - Suspend an active shop | Admin |
| `GET` | `/api/v1/seller/shops` | Seller - Retrieve owned shops | Seller |
| `POST` | `/api/v1/seller/shops` | Seller - Create a new shop | Seller |
| `PUT` | `/api/v1/seller/shops/{id}` | Seller - Update shop details | Seller |
| `POST` | `/api/v1/seller/shops/{id}/image` | Seller - Upload a shop logo | Seller |
| `GET` | `/api/v1/shops/category/{categoryId}` | Retrieve shops by category (public) | Public |
| `GET` | `/api/v1/shops/featured` | Retrieve featured shops (public) | Public |
| `GET` | `/api/v1/shops/public` | Retrieve public shops with filters (public) | Public |
| `GET` | `/api/v1/shops/{id}` | Retrieve detailed information about a specific shop | Public |

## Shop Category

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/admin/shop-categories` | Admin - Create a new shop category | Admin |
| `PUT` | `/api/v1/admin/shop-categories/{id}` | Admin - Update an existing shop category name | Admin |
| `DELETE` | `/api/v1/admin/shop-categories/{id}` | Admin - Permanently delete a shop category | Admin |
| `GET` | `/api/v1/shop-categories` | Retrieve a paginated list of all shop categories | Public |

## Brand

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/admin/brands` | Admin - Create a new brand | Admin |
| `PUT` | `/api/v1/admin/brands/{id}` | Admin - Update an existing brand | Admin |
| `DELETE` | `/api/v1/admin/brands/{id}` | Admin - Delete a brand | Admin |
| `POST` | `/api/v1/admin/brands/{id}/image` | Admin - Upload a brand logo | Admin |
| `GET` | `/api/v1/brands` | Retrieve a paginated list of all brands (public) | Public |

## Product Category

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/admin/product-categories` | Admin - Create a new product category | Admin |
| `PUT` | `/api/v1/admin/product-categories/{id}` | Admin - Update an existing product category name | Admin |
| `DELETE` | `/api/v1/admin/product-categories/{id}` | Admin - Permanently delete a product category | Admin |
| `POST` | `/api/v1/admin/product-categories/{id}/image` | Admin - Upload a category image | Admin |
| `GET` | `/api/v1/product-categories` | Retrieve a paginated list of all product categories | Public |

## Product Sub Category

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/admin/product-subcategories` | Admin - Create a new product subcategory | Admin |
| `PUT` | `/api/v1/admin/product-subcategories/{id}` | Admin - Update an existing product subcategory name | Admin |
| `DELETE` | `/api/v1/admin/product-subcategories/{id}` | Admin - Permanently delete a product subcategory | Admin |
| `GET` | `/api/v1/product-subcategories` | Retrieve subcategories for a specific category | Public |

## Product

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `DELETE` | `/api/v1/admin/products/{id}` | Admin - Permanently delete any product | Admin |
| `GET` | `/api/v1/products` | Retrieve a paginated list of products with optional filters | Public |
| `GET` | `/api/v1/products/recommendations` | Best-seller recommendations | Public |
| `GET` | `/api/v1/products/search` | Search for products by name with fuzzy matching, faceted aggregation, and ranking | Public |
| `GET` | `/api/v1/products/suggest` | Autocomplete over product names, categories and brands | Public |
| `GET` | `/api/v1/products/{id}` | Get product detail | Public |
| `GET` | `/api/v1/products/{id}/also-bought` | Frequently bought together | Public |
| `GET` | `/api/v1/products/{id}/options` | List product options | Public |
| `GET` | `/api/v1/products/{id}/related` | Related products | Public |
| `GET` | `/api/v1/products/{id}/variants` | List product variants | Public |
| `GET` | `/api/v1/stock-alerts` | List my back-in-stock alerts | Authenticated |
| `POST` | `/api/v1/stock-alerts` | Subscribe to a back-in-stock alert | Authenticated |
| `DELETE` | `/api/v1/stock-alerts/{productId}` | Remove a back-in-stock alert | Authenticated |
| `GET` | `/api/v1/seller/products` | Seller - Retrieve seller products | Seller |
| `POST` | `/api/v1/seller/products` | Create product | Seller |
| `POST` | `/api/v1/seller/products/image-upload` | Seller - Upload a product image | Seller |
| `PUT` | `/api/v1/seller/products/{id}` | Update product | Seller |
| `DELETE` | `/api/v1/seller/products/{id}` | Delete product | Seller |
| `POST` | `/api/v1/seller/products/{id}/options` | Create option dimension | Seller |
| `DELETE` | `/api/v1/seller/products/{id}/options/{optionId}` | Delete option dimension | Seller |
| `POST` | `/api/v1/seller/products/{id}/options/{optionId}/values` | Add option value | Seller |
| `DELETE` | `/api/v1/seller/products/{id}/options/{optionId}/values/{valueId}` | Delete option value | Seller |
| `POST` | `/api/v1/seller/products/{id}/variants` | Create product variant | Seller |
| `PUT` | `/api/v1/seller/products/{id}/variants/{variantId}` | Update product variant | Seller |
| `DELETE` | `/api/v1/seller/products/{id}/variants/{variantId}` | Delete product variant | Seller |
| `PUT` | `/api/v1/seller/products/{id}/variants/{variantId}/options` | Attach option values to a variant | Seller |

## Recommendation

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/recommendations` | Personalized recommendations | Authenticated |

## Review

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/reviews` | Retrieve reviews and ratings for a specific product | Public |
| `POST` | `/api/v1/reviews` | Submit a new review and rating for a product | Authenticated |
| `PUT` | `/api/v1/reviews/{id}` | Update an existing review and rating | Authenticated |
| `DELETE` | `/api/v1/reviews/{id}` | Delete a review and rating | Authenticated |
| `PUT` | `/api/v1/reviews/{id}/images` | Replace review images (max 8) | Authenticated |
| `POST` | `/api/v1/reviews/{id}/helpful` | Vote a review helpful | Authenticated |
| `GET` | `/api/v1/admin/reviews` | Admin: list reviews by status | Admin |
| `PUT` | `/api/v1/admin/reviews/{id}/status` | Admin: moderate a review | Admin |

## Wishlist

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/wishlists` | List wishlist | Authenticated |
| `POST` | `/api/v1/wishlists` | Add a product to the authenticated user's wishlist | Authenticated |
| `GET` | `/api/v1/wishlists/check` | Check if a specific product is in the user's wishlist | Authenticated |
| `POST` | `/api/v1/wishlists/move-to-cart` | Move a wishlist product to the cart | Authenticated |
| `DELETE` | `/api/v1/wishlists/remove` | Remove from wishlist | Authenticated |

## Cart

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/carts` | Get cart | Authenticated |
| `POST` | `/api/v1/carts` | Add item to cart | Authenticated |
| `DELETE` | `/api/v1/carts/all` | Clear cart | Authenticated |
| `DELETE` | `/api/v1/carts/remove` | Remove cart item | Authenticated |
| `GET` | `/api/v1/carts/summary` | Cart summary | Authenticated |
| `PUT` | `/api/v1/carts/update` | Update cart quantity | Authenticated |

## Checkout

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/checkout/guest` | Guest checkout | Public |
| `POST` | `/api/v1/checkout/place-order` | Place order | Authenticated |
| `GET` | `/api/v1/checkout/shipping-address` | List shipping addresses | Authenticated |
| `POST` | `/api/v1/checkout/shipping-address` | Create shipping address | Authenticated |
| `PUT` | `/api/v1/checkout/shipping-address/{id}` | Update shipping address | Authenticated |
| `DELETE` | `/api/v1/checkout/shipping-address/{id}` | Delete shipping address | Authenticated |
| `GET` | `/api/v1/checkout/shipping-method` | List shipping methods | Authenticated |
| `POST` | `/api/v1/checkout/summary` | Checkout summary | Authenticated |

## Order

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/orders` | Admin: list orders | Admin |
| `GET` | `/api/v1/orders/{id}/invoice` | Printable order invoice | Authenticated |
| `GET` | `/api/v1/orders/{id}/packing-slip` | Printable packing slip | Authenticated |
| `PATCH` | `/api/v1/admin/orders/status/{id}` | Admin - Update the status of any order | Admin |
| `POST` | `/api/v1/admin/orders/{id}/cancel` | Admin - Cancel any order | Admin |
| `POST` | `/api/v1/admin/orders/{id}/shipping` | Admin - Mark a paid order as shipped | Admin |
| `GET` | `/api/v1/orders` | List my orders | Authenticated |
| `GET` | `/api/v1/orders/cursor` | Cursor-paginated order history | Authenticated |
| `PATCH` | `/api/v1/orders/status/{id}` | Update order status | Customer / Seller |
| `GET` | `/api/v1/orders/track` | Guest order tracking | Public |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel order | Authenticated |
| `POST` | `/api/v1/orders/{id}/shipping` | Seller: mark order shipped | Authenticated |
| `GET` | `/api/v1/seller/orders` | Seller: list orders | Seller |

## Payment

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/admin/payments/{id}/confirm` | Admin - confirm a pending payment as completed | Admin |
| `POST` | `/api/v1/payments` | Create a payment intent for an order. Status is server-owned. | Authenticated |
| `POST` | `/api/v1/payments/webhook/{provider}` | Provider webhook (signature verified) | Public |
| `GET` | `/api/v1/payments/order/{orderId}` | Retrieve all payments for a specific order | Authenticated |
| `GET` | `/api/v1/payments/{id}` | Retrieve payment details by ID | Authenticated |

## Coupon

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/coupons` | Admin - Retrieve a list of all coupons | Admin |
| `POST` | `/api/v1/admin/coupons` | Admin - Create a new discount coupon | Admin |
| `PUT` | `/api/v1/admin/coupons/{id}` | Admin - Update an existing coupon | Admin |
| `DELETE` | `/api/v1/admin/coupons/{id}` | Admin - Delete a coupon | Admin |
| `GET` | `/api/v1/coupons/{code}` | Retrieve detailed information about a coupon by its code | Public |
| `GET` | `/api/v1/admin/promotions` | Admin - List automatic promotions | Admin |
| `POST` | `/api/v1/admin/promotions` | Admin - Create an automatic promotion | Admin |
| `PUT` | `/api/v1/admin/promotions/{id}` | Admin - Update a promotion | Admin |
| `DELETE` | `/api/v1/admin/promotions/{id}` | Admin - Deactivate a promotion | Admin |

## Shipping Method

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/shipping-methods` | Admin - Retrieve all shipping methods | Admin |
| `POST` | `/api/v1/admin/shipping-methods` | Admin - Create a new shipping method | Admin |
| `PUT` | `/api/v1/admin/shipping-methods/{id}` | Admin - Update an existing shipping method | Admin |
| `DELETE` | `/api/v1/admin/shipping-methods/{id}` | Admin - Delete a shipping method | Admin |

## Shipment

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/orders/{id}/shipping-quotes` | Get live carrier rates for an order | Authenticated |
| `GET` | `/api/v1/orders/{id}/shipments` | List an order's parcels | Authenticated |
| `POST` | `/api/v1/orders/{id}/shipments` | Purchase a label and allocate order lines | Seller / Admin |
| `GET` | `/api/v1/shipments/{id}` | Retrieve a shipment | Authenticated |
| `POST` | `/api/v1/shipments/webhook/{provider}` | Carrier tracking webhook (signature verified) | Public |

## Inventory

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/inventories` | Admin: list inventory | Admin |
| `POST` | `/api/v1/seller/inventories` | Create or update inventory | Seller |
| `GET` | `/api/v1/seller/inventories/low-stock` | List low-stock inventory | Seller |
| `GET` | `/api/v1/seller/inventories/product/{productId}` | Get product inventory | Seller |
| `GET` | `/api/v1/seller/inventories/shop/{shopId}` | List shop inventory | Seller |
| `PUT` | `/api/v1/seller/inventories/stock/{productId}` | Update stock | Seller |

## Warehouse

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/seller/warehouses` | List warehouses | Seller |
| `POST` | `/api/v1/seller/warehouses` | Create warehouse | Seller |
| `PUT` | `/api/v1/seller/warehouses/{id}` | Update warehouse | Seller |
| `DELETE` | `/api/v1/seller/warehouses/{id}` | Deactivate warehouse | Seller |

## Refund

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/refund-requests/order/{orderId}` | Admin - Get all refund requests for an order | Admin |
| `PUT` | `/api/v1/admin/refund-requests/{id}/status` | Admin - Update refund status | Admin |
| `GET` | `/api/v1/refund-requests/order/{orderId}` | Get refund requests for an order | Authenticated |
| `GET` | `/api/v1/refund-requests/{id}` | Get refund request details | Authenticated |
| `GET` | `/api/v1/refund-requests/{id}/evidence/{fileName}` | Get private refund evidence for an authorized refund request | Authenticated |
| `POST` | `/api/v1/refund-requests/{id}/ship` | Mark an approved refund as shipped | Authenticated |
| `POST` | `/api/v1/refund-requests/{orderId}` | Create a refund request for an order item | Authenticated |
| `PUT` | `/api/v1/seller/refund-requests/{id}/status` | Seller - Update refund request status | Seller |

## RMA

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/rma-requests` | Admin: list RMAs | Admin |
| `PUT` | `/api/v1/admin/rma-requests/{id}/status` | Admin: update RMA status | Admin |
| `GET` | `/api/v1/rma-requests` | List my RMAs | Authenticated |
| `POST` | `/api/v1/rma-requests` | Create return/exchange request | Authenticated |
| `POST` | `/api/v1/rma-requests/{id}/cancel` | Cancel RMA | Authenticated |
| `POST` | `/api/v1/rma-requests/{id}/ship` | Mark RMA as shipped | Authenticated |
| `GET` | `/api/v1/seller/rma-requests` | Seller: list RMAs | Seller |
| `PUT` | `/api/v1/seller/rma-requests/{id}/status` | Seller: update RMA status | Seller |

## Notification

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | List notifications | Authenticated |
| `GET` | `/api/v1/notifications/cursor` | Cursor-paginated notifications | Authenticated |
| `GET` | `/api/v1/notifications/preferences` | Get global notification channel | Authenticated |
| `PUT` | `/api/v1/notifications/preferences` | Set global notification channel | Authenticated |
| `GET` | `/api/v1/notifications/preferences/{eventType}` | Get per-event channel | Authenticated |
| `PUT` | `/api/v1/notifications/preferences/{eventType}` | Set per-event channel | Authenticated |
| `POST` | `/api/v1/notifications/read-all` | Mark all notifications read | Authenticated |
| `POST` | `/api/v1/notifications/send` | Send notification | Admin / Seller |
| `GET` | `/api/v1/notifications/unread-count` | Unread notification count | Authenticated |
| `POST` | `/api/v1/notifications/{id}/read` | Mark notification read | Authenticated |

## KYC

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/sellers` | Admin: list KYC submissions | Admin |
| `PUT` | `/api/v1/admin/sellers/{sellerId}/reject` | Admin: reject KYC | Admin |
| `PUT` | `/api/v1/admin/sellers/{sellerId}/verify` | Admin: verify KYC | Admin |
| `GET` | `/api/v1/seller/kyc` | Get my KYC status | Seller |
| `POST` | `/api/v1/seller/kyc` | Submit KYC | Seller |

## Payout

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/payouts` | Admin: list payouts | Admin |
| `GET` | `/api/v1/admin/payouts/export` | Admin: export payouts CSV | Admin |
| `POST` | `/api/v1/admin/payouts/{id}/approve` | Admin: approve payout | Admin |
| `POST` | `/api/v1/admin/payouts/{id}/pay` | Admin: mark payout paid | Admin |
| `POST` | `/api/v1/admin/payouts/{id}/reject` | Admin: reject payout | Admin |
| `GET` | `/api/v1/seller/payouts` | Seller: list payouts | Seller |
| `GET` | `/api/v1/seller/payouts/balance` | Seller: available balance | Seller |
| `POST` | `/api/v1/seller/payouts/request` | Seller: request payout | Seller |
| `GET` | `/api/v1/seller/payouts/statement` | Seller: payout statement | Seller |
| `GET` | `/api/v1/seller/payouts/statement/export` | Seller: export statement CSV | Seller |

## Tax

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/tax-rules` | List tax rules | Admin |
| `POST` | `/api/v1/admin/tax-rules` | Create or update tax rule | Admin |
| `PUT` | `/api/v1/admin/tax-rules` | Upsert tax rule | Admin |
| `DELETE` | `/api/v1/admin/tax-rules/{id}` | Delete tax rule | Admin |
| `GET` | `/api/v1/admin/tax-classes` | List product tax classes | Admin |
| `POST` | `/api/v1/admin/tax-classes` | Create a tax class | Admin |
| `PUT` | `/api/v1/admin/tax-classes/{id}` | Update a tax class | Admin |
| `DELETE` | `/api/v1/admin/tax-classes/{id}` | Delete a tax class | Admin |
| `GET` | `/api/v1/admin/tax-zones` | List tax zones/nexus | Admin |
| `POST` | `/api/v1/admin/tax-zones` | Create a tax zone | Admin |
| `PUT` | `/api/v1/admin/tax-zones/{id}` | Update a tax zone | Admin |
| `DELETE` | `/api/v1/admin/tax-zones/{id}` | Delete a tax zone | Admin |
| `GET` | `/api/v1/admin/tax-exemptions` | List customer/business exemptions | Admin |
| `POST` | `/api/v1/admin/tax-exemptions` | Create an exemption | Admin |
| `DELETE` | `/api/v1/admin/tax-exemptions/{id}` | Delete an exemption | Admin |
| `PUT` | `/api/v1/admin/products/{id}/tax-class` | Assign a product tax class | Admin |

## Fraud

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/fraud/rules` | List velocity rules | Admin |
| `POST` | `/api/v1/admin/fraud/rules` | Create a velocity rule | Admin |
| `PUT` | `/api/v1/admin/fraud/rules/{id}` | Update a velocity rule | Admin |
| `DELETE` | `/api/v1/admin/fraud/rules/{id}` | Delete a velocity rule | Admin |
| `GET` | `/api/v1/admin/fraud/blocklist` | List blocklist entries | Admin |
| `POST` | `/api/v1/admin/fraud/blocklist` | Add a blocklist entry | Admin |
| `DELETE` | `/api/v1/admin/fraud/blocklist/{id}` | Delete a blocklist entry | Admin |
| `GET` | `/api/v1/admin/fraud/high-risk` | List review/blocked payments | Admin |
| `POST` | `/api/v1/admin/payments/{id}/risk` | Record AVS/CVV results | Admin |
| `GET` | `/api/v1/admin/chargebacks` | List chargebacks | Admin |
| `POST` | `/api/v1/admin/chargebacks` | Record a chargeback | Admin |
| `GET` | `/api/v1/admin/chargebacks/{id}` | Retrieve a chargeback | Admin |
| `PUT` | `/api/v1/admin/chargebacks/{id}` | Update chargeback status/outcome | Admin |

## Loyalty

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/wallet` | Get loyalty points, store credit and referral code | Authenticated |
| `GET` | `/api/v1/wallet/ledger` | List wallet ledger entries | Authenticated |
| `GET` | `/api/v1/wallet/gift-cards/{code}` | Check a gift-card balance | Authenticated |
| `GET` | `/api/v1/referrals` | List my referrals | Authenticated |
| `POST` | `/api/v1/referrals/claim` | Claim a referral code | Authenticated |
| `GET` | `/api/v1/admin/gift-cards` | List gift cards | Admin |
| `POST` | `/api/v1/admin/gift-cards` | Issue a gift card | Admin |
| `DELETE` | `/api/v1/admin/gift-cards/{id}` | Void a gift card | Admin |
| `POST` | `/api/v1/admin/wallets/{userId}/credit` | Credit store balance | Admin |

## Localization

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/i18n/locales` | List supported locales and default currencies | Public |
| `GET` | `/api/v1/i18n/messages` | Get a translated API message catalog | Public |
| `GET` | `/api/v1/admin/products/{id}/translations` | List product translations | Admin |
| `PUT` | `/api/v1/admin/products/{id}/translations/{locale}` | Create/update a product translation | Admin |
| `DELETE` | `/api/v1/admin/products/{id}/translations/{locale}` | Delete a product translation | Admin |

## Analytics

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/analytics/events` | Ingest storefront funnel events | Public |
| `GET` | `/api/v1/admin/analytics/funnel` | Conversion funnel report | Admin |
| `GET` | `/api/v1/admin/analytics/attribution` | UTM/referrer attribution report | Admin |
| `GET` | `/api/v1/admin/analytics/products` | Product conversion report | Admin |

## Seller Tools

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/sellers/{id}/reviews` | List active seller reviews | Public |
| `POST` | `/api/v1/seller-reviews` | Review a seller after delivery | Customer |
| `GET` | `/api/v1/seller/reviews` | Seller: list reviews received | Seller |
| `GET` | `/api/v1/admin/seller-reviews` | Admin: list seller reviews | Admin |
| `PUT` | `/api/v1/admin/seller-reviews/{id}/status` | Admin: moderate a seller review | Admin |
| `GET` | `/api/v1/disputes` | List my disputes | Authenticated |
| `POST` | `/api/v1/disputes` | Open a dispute | Authenticated |
| `GET` | `/api/v1/admin/disputes` | Admin: list disputes | Admin |
| `PUT` | `/api/v1/admin/disputes/{id}` | Admin: resolve/update a dispute | Admin |
| `GET` | `/api/v1/admin/commission-rules` | List commission rules | Admin |
| `POST` | `/api/v1/admin/commission-rules` | Create a commission rule | Admin |
| `PUT` | `/api/v1/admin/commission-rules/{id}` | Update a commission rule | Admin |
| `DELETE` | `/api/v1/admin/commission-rules/{id}` | Delete a commission rule | Admin |
| `GET` | `/api/v1/seller/commission` | Seller: effective commission rate | Seller |

## Subscription

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/subscription-plans` | List active subscription plans | Public |
| `GET` | `/api/v1/subscriptions` | List my subscriptions | Authenticated |
| `GET` | `/api/v1/subscriptions/{id}/invoices` | List my subscription invoices | Authenticated |
| `POST` | `/api/v1/subscriptions` | Subscribe to a plan | Authenticated |
| `POST` | `/api/v1/subscriptions/{id}/cancel` | Cancel at period end | Authenticated |
| `GET` | `/api/v1/admin/subscription-plans` | Admin: list plans | Admin |
| `POST` | `/api/v1/admin/subscription-plans` | Admin: create a plan | Admin |
| `PUT` | `/api/v1/admin/subscription-plans/{id}` | Admin: update a plan | Admin |
| `DELETE` | `/api/v1/admin/subscription-plans/{id}` | Admin: deactivate a plan | Admin |
| `GET` | `/api/v1/admin/subscriptions` | Admin: list subscriptions | Admin |
| `POST` | `/api/v1/admin/subscriptions/run-billing` | Admin: run renewal billing | Admin |
| `GET` | `/api/v1/library` | List my digital purchases | Authenticated |
| `GET` | `/api/v1/library/{token}/download` | Redeem a digital download | Authenticated |

## B2B

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/b2b/account` | Get my B2B account | Authenticated |
| `POST` | `/api/v1/b2b/account` | Apply for/update a B2B account | Authenticated |
| `GET` | `/api/v1/b2b/quotes` | List my B2B quotes | Authenticated |
| `POST` | `/api/v1/b2b/quotes` | Request a B2B quote | Authenticated |
| `GET` | `/api/v1/b2b/quotes/{id}` | Get a B2B quote | Authenticated |
| `POST` | `/api/v1/b2b/quotes/{id}/accept` | Accept a quote and create a PO | Authenticated |
| `GET` | `/api/v1/b2b/purchase-orders` | List my purchase orders | Authenticated |
| `GET` | `/api/v1/b2b/purchase-orders/{id}` | Get a purchase order | Authenticated |
| `GET` | `/api/v1/seller/b2b/quotes` | Seller: list quote requests | Seller |
| `PUT` | `/api/v1/seller/b2b/quotes/{id}` | Seller: price/send/reject a quote | Seller |
| `GET` | `/api/v1/seller/b2b/purchase-orders` | Seller: list purchase orders | Seller |
| `POST` | `/api/v1/seller/b2b/purchase-orders/{id}/fulfill` | Seller: fulfill a purchase order | Seller |
| `GET` | `/api/v1/admin/b2b/accounts` | Admin: list B2B accounts | Admin |
| `PUT` | `/api/v1/admin/b2b/accounts/{userId}` | Admin: approve/update credit terms | Admin |
| `GET` | `/api/v1/admin/b2b/purchase-orders` | Admin: list purchase orders | Admin |

## Omnichannel

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/stores` | Find nearby pickup stores | Public |
| `GET` | `/api/v1/seller/pickup-orders` | Seller: list pickup orders | Seller |
| `POST` | `/api/v1/seller/orders/{id}/pickup/ready` | Seller: mark pickup ready | Seller |
| `POST` | `/api/v1/seller/orders/{id}/pickup/complete` | Seller: verify pickup code and complete | Seller |
| `GET` | `/api/v1/seller/pos/sales` | Seller: list POS sales | Seller |
| `POST` | `/api/v1/seller/pos/sales` | Seller: create an in-store POS sale | Seller |

## CMS & SEO

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/cms/pages` | List published CMS pages | Public |
| `GET` | `/api/v1/cms/pages/{slug}` | Get a published page | Public |
| `GET` | `/api/v1/admin/cms/pages` | Admin: list CMS pages | Admin |
| `POST` | `/api/v1/admin/cms/pages` | Admin: create a CMS page | Admin |
| `PUT` | `/api/v1/admin/cms/pages/{id}` | Admin: update a CMS page | Admin |
| `DELETE` | `/api/v1/admin/cms/pages/{id}` | Admin: archive a CMS page | Admin |
| `GET` | `/api/v1/seo/metadata` | Get SEO metadata for an entity | Public |
| `PUT` | `/api/v1/admin/seo/metadata` | Admin: upsert SEO metadata | Admin |
| `GET` | `/sitemap.xml` | XML sitemap | Public |
| `GET` | `/robots.txt` | Robots policy | Public |

## Support

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/support/tickets` | List my support tickets | Authenticated |
| `POST` | `/api/v1/support/tickets` | Open a support ticket | Authenticated |
| `GET` | `/api/v1/support/tickets/{id}` | Get a ticket and public replies | Authenticated |
| `POST` | `/api/v1/support/tickets/{id}/messages` | Reply to a ticket | Authenticated |
| `POST` | `/api/v1/support/tickets/{id}/close` | Close my ticket | Authenticated |
| `GET` | `/api/v1/admin/support/tickets` | Admin: list support tickets | Admin |
| `GET` | `/api/v1/admin/support/tickets/{id}` | Admin: ticket with internal notes | Admin |
| `POST` | `/api/v1/admin/support/tickets/{id}/messages` | Admin: reply or add internal note | Admin |
| `PUT` | `/api/v1/admin/support/tickets/{id}` | Admin: update status/priority/assignment | Admin |

## FX

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/fx-rates` | List FX rates | Admin |
| `PUT` | `/api/v1/admin/fx-rates/{targetCurrency}` | Set FX rate | Admin |

## API Keys

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/api-keys` | List API keys | Authenticated |
| `POST` | `/api/v1/api-keys` | Create API key | Authenticated |
| `DELETE` | `/api/v1/api-keys/{id}` | Revoke API key | Authenticated |

## Feature Flags

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/feature-flags` | List feature flags | Admin |
| `PUT` | `/api/v1/admin/feature-flags/{key}` | Set feature flag | Admin |

## Outbox

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/outbox/dead-letters` | List dead letters | Admin |
| `POST` | `/api/v1/admin/outbox/dead-letters/{id}/requeue` | Requeue dead letter | Admin |
| `GET` | `/api/v1/admin/outbox/summary` | Outbox summary | Admin |

## Policy

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/admin/policies` | Admin - Create a new policy document or new version | Admin |
| `GET` | `/api/v1/admin/policies/by-id/{id}` | Admin - Retrieve a policy document by ID | Admin |
| `POST` | `/api/v1/admin/policies/deactivate/{id}` | Admin - Deactivate a policy document | Admin |
| `PUT` | `/api/v1/admin/policies/{id}` | Admin - Update an existing policy document | Admin |
| `GET` | `/api/v1/admin/policies/{policyType}/history` | Admin - Retrieve all versions of a specific policy type | Admin |
| `GET` | `/api/v1/policies/{policyType}` | Retrieve the latest active version of a policy by type | Public |

## Consent

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/policy-consents` | Admin - Retrieve consent records filtered by user and/or policy type | Admin |
| `GET` | `/api/v1/policy-consents` | Retrieve all consent records for the authenticated user | Authenticated |
| `POST` | `/api/v1/policy-consents/consent` | Record user consent for a specific policy document | Authenticated |
| `DELETE` | `/api/v1/policy-consents/{id}` | Revoke a consent record (owner or admin) | Owner / Admin |
| `GET` | `/api/v1/policy-consents/{policyType}` | Check if the user has consented to a specific policy type | Authenticated |

## Privacy

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/privacy/erase` | Erase/anonymize the authenticated account (password required) | Authenticated |
| `GET` | `/api/v1/privacy/export` | Export all personal data held for the authenticated user | Authenticated |

## Dashboard

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/dashboard` | Get summary dashboard stats (revenue, orders, users, products, shops) | Admin |
| `GET` | `/api/v1/admin/dashboard/activity` | Get recent activity feed (orders + user registrations) | Admin |
| `GET` | `/api/v1/admin/dashboard/orders` | Get order statistics with status distribution | Admin |
| `GET` | `/api/v1/admin/dashboard/revenue` | Get detailed revenue stats with optional date range and daily breakdown | Admin |
| `GET` | `/api/v1/admin/dashboard/top-products` | Get top-selling products sorted by sales volume | Admin |
| `GET` | `/api/v1/admin/dashboard/users` | Get user growth analytics over a period | Admin |
| `GET` | `/api/v1/seller/dashboard` | Seller dashboard | Seller |

## Audit Log

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/v1/admin/audit-logs` | Get paginated audit logs with optional filters | Admin |
| `GET` | `/api/v1/admin/audit-logs/{id}` | Get a single audit log entry by ID | Admin |

## Health

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/health` | Service health | Public |
| `GET` | `/health/live` | Liveness probe | Public |
| `GET` | `/health/ready` | Readiness probe | Public |
