# Users, roles and permissions

Status: **foundation implemented** (schema, effective-permission resolution, platform role APIs, seller membership APIs).

Scope: platform operators, sellers and customers. B2B company memberships are intentionally out of scope for this phase.

## 1. Model

Authorization is split into three layers:

| Layer | Question | Stored in |
|---|---|---|
| Account type | What kind of identity is this? | `"user".user_type` |
| Membership | Which shop/platform does the user act in? | `shop_member`, `auth_user_role` |
| Role + permission | What may they do? | `auth_role`, `auth_permission`, `auth_role_permission` |

### Account types

```
CUSTOMER
SELLER
ADMIN
```

Rules:

- `ADMIN` only grants access to the admin console. Permissions come from roles.
- `SUPER_ADMIN` is retained as a **legacy enum value** and mapped to the `platform:super_admin` role. It will be removed after existing rows and clients are migrated.
- Guest checkout uses `user.is_guest`, not an account type.
- Staff is never an account type. A staff member is `user + membership + role`.

## 2. Tables

| Table | Purpose |
|---|---|
| `auth_permission` | Permission catalog (`resource:action`) |
| `auth_role` | Platform (`GLOBAL`) and shop (`SHOP`) roles |
| `auth_role_permission` | Role → permission mapping |
| `auth_user_role` | Platform role assignment |
| `shop_member` | User membership in a shop |
| `shop_member_role` | Shop role assignment |
| `shop_invitation` | Pending staff invitation (token hash, expiry) |
| `member_warehouse` | Optional warehouse scoping for shop staff |
| `"user".authz_version` | Invalidates cached permissions |

## 3. Permission codes

The existing `Permission` enum now carries a stable code:

| Enum | Code |
|---|---|
| `USER_MANAGE` | `user:manage` |
| `CATALOG_MANAGE` | `catalog:manage` |
| `ORDER_MANAGE` | `order:manage` |
| `PAYMENT_MANAGE` | `payment:manage` |
| `REFUND_MANAGE` | `refund:manage` |
| `PAYOUT_MANAGE` | `payout:manage` |
| `COUPON_MANAGE` | `coupon:manage` |
| `SHOP_MANAGE` | `shop:manage` |
| `INVENTORY_MANAGE` | `inventory:manage` |
| `POLICY_MANAGE` | `policy:manage` |
| `DASHBOARD_VIEW` | `dashboard:read` |
| `AUDIT_VIEW` | `audit:read` |
| `NOTIFICATION_SEND` | `notification:send` |
| `TAX_MANAGE` | `tax:manage` |
| `FX_MANAGE` | `fx:manage` |
| `OUTBOX_MANAGE` | `outbox:manage` |
| `API_KEY_MANAGE` | `api_key:manage` |
| `FEATURE_FLAG_MANAGE` | `feature_flag:manage` |
| `ROLE_MANAGE` | `role:manage` |
| `SUPPORT_MANAGE` | `support:manage` |
| `FRAUD_MANAGE` | `fraud:manage` |
| `CMS_MANAGE` | `cms:manage` |
| `SUBSCRIPTION_MANAGE` | `subscription:manage` |
| `REVIEW_MANAGE` | `review:manage` |
| `SHIPPING_MANAGE` | `shipping:manage` |
| `B2B_MANAGE` | `b2b:manage` |

Custom roles may include codes that do not have an enum constant yet. Route gates still use enum constants, so new route-level checks require a code constant; business-level checks can use raw codes through the resolver.

## 4. Effective permissions

```
effective(user) =
  union(permissions of active platform roles)
  ∪ union(permissions of active shop memberships)
```

Rules:

- If the user has **no assignments**, the legacy `UserType.defaultPermissions()` mapping is used for customers and sellers. This keeps existing behavior working.
- Platform admins are the exception: an `ADMIN`/`SUPER_ADMIN` with no role assignment gets **no permissions**, so an orphaned admin can never fall back to full access.
- If the user has **any assignment**, the result is exact. An assigned role with no permissions grants nothing.
- Results are cached by `userId + authz_version` for 60 seconds.
- Any role assignment or membership change increments `authz_version`, so the next request resolves fresh permissions.

## 5. Authentication flow

`ConfigureAuth` now:

1. Validates the JWT and loads the user from the database.
2. Resolves effective permissions through `RbacService`.
3. Builds `JwtTokenRequest` with `permissions` and `authzVersion`.

`JwtTokenRequest.hasPermission()`:

- JWT: checks the resolved permission set (code or legacy enum name).
- API key: checks explicit scopes (legacy enum names or codes).
- Fallback: role defaults only when no resolved set was supplied.

API keys are validated against the owner's effective permissions at creation **and** at authentication, so a role change narrows an existing key.

## 6. Seeded roles

### Platform (`GLOBAL`)

| Role | Notes |
|---|---|
| `platform:super_admin` | All permissions; break-glass |
| `platform:admin` | All except `feature_flag:manage` |
| `platform:ops` | Users, shops, dashboards, audit |
| `platform:catalog` | Catalog and inventory |
| `platform:orders` | Orders, refunds, shipping |
| `platform:finance_approver` | Payments, refunds, payouts, tax, FX |
| `platform:finance_payer` | Payments and payouts |
| `platform:support` | Support tickets |
| `platform:content` | CMS, policies, coupons, notifications |
| `platform:auditor` | Audit and dashboards (read-only) |
| `platform:developer` | Feature flags, outbox, API keys |

### Shop (`SHOP`)

`shop:owner`, `shop:manager`, `catalog:manager`, `order:fulfillment`, `inventory:manager`, `finance:viewer`, `support:agent`, `analyst`, `warehouse:operator`, `pos:cashier`.

## 7. API

OpenAPI tag: `RBAC`.

### Platform

```
GET    /api/v1/admin/rbac/permissions
GET    /api/v1/admin/rbac/roles?scope=GLOBAL|SHOP
POST   /api/v1/admin/rbac/roles
GET    /api/v1/admin/rbac/roles/{id}
PUT    /api/v1/admin/rbac/roles/{id}
DELETE /api/v1/admin/rbac/roles/{id}
PUT    /api/v1/admin/rbac/roles/{id}/permissions
GET    /api/v1/admin/rbac/users/{id}/roles
PUT    /api/v1/admin/rbac/users/{id}/roles
GET    /api/v1/admin/rbac/users/{id}/effective-permissions
```

All platform RBAC routes require `role:manage`.

Authorization rules enforced by the service:

- A non-super admin can only grant permissions they already hold.
- Only a super admin can grant `platform:super_admin` or edit system roles.
- The last super admin cannot be removed.
- Role and membership changes bump `authz_version` and are audited.

### Seller

```
GET    /api/v1/seller/members
POST   /api/v1/seller/members/invite
PUT    /api/v1/seller/members/{id}/roles
DELETE /api/v1/seller/members/{id}
```

The seller must own the shop. The invitation token is returned once; deliver it by email.

### Shared

```
GET  /api/v1/me/permissions
POST /api/v1/invitations/accept
```

## 8. Migration

`V66__rbac_and_seller_members.sql`:

1. Adds `user.authz_version`.
2. Creates the RBAC and shop-membership tables.
3. Seeds the permission catalog and all system roles.
4. Backfills `SUPER_ADMIN` → `platform:super_admin`.
5. Backfills `ADMIN` → `platform:admin`.
6. Creates owner memberships for existing sellers with a shop.

`V67__b2b_permission.sql` adds the `b2b:manage` permission and grants it to
`platform:admin` and `platform:super_admin`.

Backward compatibility:

- Existing admin JWTs keep working because permissions are resolved server-side.
- Existing API keys keep working because scopes accept both enum names and codes.
- `SUPER_ADMIN` remains accepted by `adminAuth` until the enum value is retired.

## 9. Admin route gates

The following admin groups now require explicit permissions instead of relying on `adminAuth` alone:

| Group | Permission |
|---|---|
| `admin/auth`, `admin/users` | `user:manage` |
| `admin/rbac` | `role:manage` |
| `admin/brands`, categories, products | `catalog:manage` |
| `admin/inventories` | `inventory:manage` |
| `admin/orders` | `order:manage` |
| `admin/payments`, webhooks, gift cards, wallets | `payment:manage` |
| `admin/refunds`, `admin/rma-requests` | `refund:manage` |
| `admin/payouts`, commission rules | `payout:manage` |
| `admin/fraud`, `admin/chargebacks` | `fraud:manage` |
| `admin/support/tickets`, `admin/disputes` | `support:manage` |
| `admin/cms`, `admin/seo` | `cms:manage` |
| `admin/policies`, policy consents | `policy:manage` |
| `admin/shipping-methods` | `shipping:manage` |
| `admin/subscriptions`, plans | `subscription:manage` |
| `admin/reviews`, seller reviews | `review:manage` |
| `admin/feature-flags` | `feature_flag:manage` |
| `admin/outbox` | `outbox:manage` |
| `admin/tax-*` | `tax:manage` |
| `admin/fx-rates` | `fx:manage` |
| `admin/audit-logs` | `audit:read` |
| `admin/dashboard`, queues, analytics | `dashboard:read` |
| `admin/b2b` | `b2b:manage` |

Mixed customer/seller/admin routes use `requireAdminPermission(...)` so a narrowly-scoped admin cannot bypass the permission gate through a customer or seller endpoint. `sellerAuth` is exact `SELLER` only; admins use the admin console.

## 10. Behavior changes from the old model

- `sellerAuth` now allows **exact `SELLER`** accounts only. Admins use the admin console instead of inheriting seller routes.
- Customer order detail, refund evidence and invoice routes no longer give admins implicit access. Admin access is checked with `requireAdminPermission(...)` (`order:manage` / `refund:manage`) or through the admin routes.
- `notifications/send` requires `notification:send` instead of any admin/seller role.
- Feature flags now require `feature_flag:manage`; a plain `ADMIN` no longer gets implicit access (only `SUPER_ADMIN`/`platform:developer` do).
- Invoice, packing-slip and shipment endpoints require `order:manage` for admin principals.
- B2B admin routes require `b2b:manage`.
- Changing a role's permissions increments `authz_version` for every holder of that role, so cached permissions are invalidated immediately.

## 11. Known limitations / next phase

- Seller **service** authorization still uses `requireSellerByUserId` and `sellerOwnsShop`. Invited staff have a membership, but seller endpoints that require a `seller` row will reject them. The next phase is to replace those checks with `requireShopPermission(user, shopId, permission)`.
- Invited staff should be `SELLER` accounts to enter the seller center in this phase. A `CUSTOMER` invitee can accept and receive a membership, but the seller portal gate will reject them until membership-based portal access lands.
- Route gates are still coarse (`CATALOG_MANAGE`, not `catalog:read` / `catalog:write`). Granular route gates can be added incrementally.
- `platform:finance_approver` and `platform:finance_payer` currently share the coarse `payout:manage` code. Split into `payout:approve` and `payout:pay` when payout endpoints are migrated.
- Invitation emails are not sent yet; the token is returned by the API.
- `SUPER_ADMIN` should be removed from `UserType` after a data backfill and client migration.
- B2B company memberships are out of scope.

## 12. Code layout

| File | Responsibility |
|---|---|
| `RbacModels.kt` | API request/response DTOs |
| `RbacService.kt` | Permission resolution, roles, platform assignments |
| `ShopMembershipService.kt` | Shop staff memberships and invitations |
| `RbacSupport.kt` | Shared role/member lookup and mapping helpers |
| `constants/Rbac.kt` | Role scope, membership/invitation status and seeded ids |
| `RbacRoutes.kt` | Admin, seller member, `/me` and invitation routes |

Shared utilities: `utils/common/Hashing.kt` and `Iterable<String>.entityIDs(...)` in `StringExt.kt`.

## 13. Verification

- `./gradlew build` passes: **637 tests, 0 failures**.
- `mkdocs build --strict` passes.
- The endpoint index documents **383 operations / 47 tags**, including the 16 RBAC operations.
- `./gradlew detekt` reports only the four pre-existing structural warnings.
- `V66` was applied against a fresh PostgreSQL schema; seeds produced 25 permissions, 21 roles and 98 role-permission links. `V67` adds `b2b:manage` and two role links (26 permissions, 100 links).
- `platform:admin` intentionally excludes `feature_flag:manage`.
- `RbacPrincipalTest` covers permission codes, resolved permission checks, legacy fallback and API-key scopes.
