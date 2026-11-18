-- Unified RBAC: platform roles/permissions and shop memberships.
-- Backward compatible: "user".user_type remains the account kind until the
-- legacy SUPER_ADMIN value is retired.

ALTER TABLE "user" ADD COLUMN IF NOT EXISTS authz_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS auth_permission (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    code        VARCHAR(120) NOT NULL,
    resource    VARCHAR(60)  NOT NULL,
    action      VARCHAR(60)  NOT NULL,
    description TEXT,
    is_system   BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX IF NOT EXISTS auth_permission_code_unique ON auth_permission(code);

CREATE TABLE IF NOT EXISTS auth_role (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    scope_type  VARCHAR(20)  NOT NULL DEFAULT 'GLOBAL',
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX IF NOT EXISTS auth_role_code_unique ON auth_role(code);
CREATE INDEX IF NOT EXISTS auth_role_scope_idx ON auth_role(scope_type);

CREATE TABLE IF NOT EXISTS auth_role_permission (
    role_id       VARCHAR(50) NOT NULL REFERENCES auth_role(id) ON DELETE CASCADE,
    permission_id VARCHAR(50) NOT NULL REFERENCES auth_permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS auth_user_role (
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    role_id     VARCHAR(50) NOT NULL REFERENCES auth_role(id) ON DELETE CASCADE,
    assigned_by VARCHAR(50) REFERENCES "user"(id) ON DELETE SET NULL,
    assigned_at TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS auth_user_role_user_idx ON auth_user_role(user_id);

CREATE TABLE IF NOT EXISTS shop_member (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    shop_id     VARCHAR(50) NOT NULL REFERENCES shop(id) ON DELETE CASCADE,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_owner    BOOLEAN     NOT NULL DEFAULT FALSE,
    invited_by  VARCHAR(50) REFERENCES "user"(id) ON DELETE SET NULL,
    joined_at   TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS shop_member_shop_user_unique ON shop_member(shop_id, user_id);
CREATE INDEX IF NOT EXISTS shop_member_user_idx ON shop_member(user_id, status);

CREATE TABLE IF NOT EXISTS shop_member_role (
    member_id VARCHAR(50) NOT NULL REFERENCES shop_member(id) ON DELETE CASCADE,
    role_id   VARCHAR(50) NOT NULL REFERENCES auth_role(id) ON DELETE CASCADE,
    PRIMARY KEY (member_id, role_id)
);

CREATE TABLE IF NOT EXISTS shop_invitation (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    shop_id     VARCHAR(50)  NOT NULL REFERENCES shop(id) ON DELETE CASCADE,
    email       VARCHAR(255) NOT NULL,
    role_ids    TEXT         NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    invited_by  VARCHAR(50) REFERENCES "user"(id) ON DELETE SET NULL,
    expires_at  TIMESTAMP    NOT NULL,
    accepted_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS shop_invitation_token_unique ON shop_invitation(token_hash);
CREATE INDEX IF NOT EXISTS shop_invitation_shop_idx ON shop_invitation(shop_id, status);

CREATE TABLE IF NOT EXISTS member_warehouse (
    member_id    VARCHAR(50) NOT NULL REFERENCES shop_member(id) ON DELETE CASCADE,
    warehouse_id VARCHAR(50) NOT NULL REFERENCES warehouse(id) ON DELETE CASCADE,
    PRIMARY KEY (member_id, warehouse_id)
);

-- Permission catalog mirrors the previous enum so existing route gates keep
-- working. Granular codes can be added without a deploy.
INSERT INTO auth_permission (id, code, resource, action, description, is_system)
VALUES
    ('perm-user-manage', 'user:manage', 'user', 'manage', 'Invite, update, suspend and assign roles to users', TRUE),
    ('perm-catalog-manage', 'catalog:manage', 'catalog', 'manage', 'Manage categories, brands and products', TRUE),
    ('perm-order-manage', 'order:manage', 'order', 'manage', 'Read and intervene in orders', TRUE),
    ('perm-payment-manage', 'payment:manage', 'payment', 'manage', 'Manage payments and payment webhooks', TRUE),
    ('perm-refund-manage', 'refund:manage', 'refund', 'manage', 'Read and decide refunds and RMA', TRUE),
    ('perm-payout-manage', 'payout:manage', 'payout', 'manage', 'Manage seller payouts', TRUE),
    ('perm-coupon-manage', 'coupon:manage', 'coupon', 'manage', 'Manage coupons and promotions', TRUE),
    ('perm-shop-manage', 'shop:manage', 'shop', 'manage', 'Approve, suspend and update shops', TRUE),
    ('perm-inventory-manage', 'inventory:manage', 'inventory', 'manage', 'Manage stock, warehouses and transfers', TRUE),
    ('perm-policy-manage', 'policy:manage', 'policy', 'manage', 'Manage legal policies and consent settings', TRUE),
    ('perm-dashboard-read', 'dashboard:read', 'dashboard', 'read', 'View dashboards and queues', TRUE),
    ('perm-audit-read', 'audit:read', 'audit', 'read', 'View audit logs', TRUE),
    ('perm-notification-send', 'notification:send', 'notification', 'send', 'Send platform notifications', TRUE),
    ('perm-tax-manage', 'tax:manage', 'tax', 'manage', 'Manage tax rules, classes, zones and exemptions', TRUE),
    ('perm-fx-manage', 'fx:manage', 'fx', 'manage', 'Manage FX rates', TRUE),
    ('perm-outbox-manage', 'outbox:manage', 'outbox', 'manage', 'Operate the outbox and dead letters', TRUE),
    ('perm-api-key-manage', 'api_key:manage', 'api_key', 'manage', 'Create and revoke API keys', TRUE),
    ('perm-feature-flag-manage', 'feature_flag:manage', 'feature_flag', 'manage', 'Toggle feature flags', TRUE),
    ('perm-role-manage', 'role:manage', 'role', 'manage', 'Create roles and assign permissions', TRUE),
    ('perm-support-manage', 'support:manage', 'support', 'manage', 'Manage support tickets', TRUE),
    ('perm-fraud-manage', 'fraud:manage', 'fraud', 'manage', 'Review fraud, risk and chargebacks', TRUE),
    ('perm-cms-manage', 'cms:manage', 'cms', 'manage', 'Manage CMS pages and SEO', TRUE),
    ('perm-subscription-manage', 'subscription:manage', 'subscription', 'manage', 'Manage subscription plans and billing', TRUE),
    ('perm-review-manage', 'review:manage', 'review', 'manage', 'Moderate reviews and replies', TRUE),
    ('perm-shipping-manage', 'shipping:manage', 'shipping', 'manage', 'Manage shipping methods and shipments', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth_role (id, code, name, description, scope_type, is_system)
VALUES
    ('role-platform-super-admin', 'platform:super_admin', 'Super Admin', 'Break-glass platform access. Prefer time-bound elevation over permanent assignment.', 'GLOBAL', TRUE),
    ('role-platform-admin', 'platform:admin', 'Platform Admin', 'Full platform access except feature flags.', 'GLOBAL', TRUE),
    ('role-platform-ops', 'platform:ops', 'Platform Operations', 'Users, shops and seller operations.', 'GLOBAL', TRUE),
    ('role-platform-catalog', 'platform:catalog', 'Catalog Manager', 'Categories, brands, products and inventory.', 'GLOBAL', TRUE),
    ('role-platform-orders', 'platform:orders', 'Order Operations', 'Orders, refunds, RMA and shipping.', 'GLOBAL', TRUE),
    ('role-platform-finance-approver', 'platform:finance_approver', 'Finance Approver', 'Approve refunds and payouts.', 'GLOBAL', TRUE),
    ('role-platform-finance-payer', 'platform:finance_payer', 'Finance Payer', 'Pay payouts and manage payment webhooks.', 'GLOBAL', TRUE),
    ('role-platform-support', 'platform:support', 'Support Agent', 'Support tickets.', 'GLOBAL', TRUE),
    ('role-platform-content', 'platform:content', 'Content Manager', 'CMS, SEO, policies and coupons.', 'GLOBAL', TRUE),
    ('role-platform-auditor', 'platform:auditor', 'Auditor', 'Read-only audit and dashboards.', 'GLOBAL', TRUE),
    ('role-platform-developer', 'platform:developer', 'Platform Developer', 'Feature flags, outbox and integrations.', 'GLOBAL', TRUE),
    ('role-shop-owner', 'shop:owner', 'Shop Owner', 'Full shop access and staff management.', 'SHOP', TRUE),
    ('role-shop-manager', 'shop:manager', 'Shop Manager', 'Shop settings, catalog, orders and reports.', 'SHOP', TRUE),
    ('role-shop-catalog', 'catalog:manager', 'Catalog Manager', 'Products, variants and inventory.', 'SHOP', TRUE),
    ('role-shop-fulfillment', 'order:fulfillment', 'Fulfillment', 'Orders, packing, shipping and RMA.', 'SHOP', TRUE),
    ('role-shop-inventory', 'inventory:manager', 'Inventory Manager', 'Stock, warehouses and transfers.', 'SHOP', TRUE),
    ('role-shop-finance', 'finance:viewer', 'Finance Viewer', 'Payouts, invoices and commission.', 'SHOP', TRUE),
    ('role-shop-support', 'support:agent', 'Support Agent', 'Shop tickets and review replies.', 'SHOP', TRUE),
    ('role-shop-analyst', 'analyst', 'Analyst', 'Sales and traffic reports.', 'SHOP', TRUE),
    ('role-shop-warehouse', 'warehouse:operator', 'Warehouse Operator', 'Receive, pick, pack and pickup.', 'SHOP', TRUE),
    ('role-shop-pos', 'pos:cashier', 'POS Cashier', 'POS sales and sessions.', 'SHOP', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-super-admin', id FROM auth_permission
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-admin', id FROM auth_permission WHERE code <> 'feature_flag:manage'
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-ops', id FROM auth_permission WHERE code IN ('user:manage', 'shop:manage', 'dashboard:read', 'audit:read')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-catalog', id FROM auth_permission WHERE code IN ('catalog:manage', 'inventory:manage')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-orders', id FROM auth_permission WHERE code IN ('order:manage', 'refund:manage', 'shipping:manage')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-finance-approver', id FROM auth_permission WHERE code IN ('payment:manage', 'refund:manage', 'payout:manage', 'tax:manage', 'fx:manage')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-finance-payer', id FROM auth_permission WHERE code IN ('payment:manage', 'payout:manage')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-support', id FROM auth_permission WHERE code = 'support:manage'
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-content', id FROM auth_permission WHERE code IN ('cms:manage', 'policy:manage', 'coupon:manage', 'notification:send')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-auditor', id FROM auth_permission WHERE code IN ('audit:read', 'dashboard:read')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-platform-developer', id FROM auth_permission WHERE code IN ('feature_flag:manage', 'outbox:manage', 'api_key:manage')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-owner', id FROM auth_permission WHERE code IN (
    'order:manage', 'refund:manage', 'payout:manage', 'shop:manage',
    'inventory:manage', 'dashboard:read', 'notification:send', 'api_key:manage'
)
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-manager', id FROM auth_permission WHERE code IN (
    'order:manage', 'shop:manage', 'inventory:manage', 'dashboard:read'
)
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-catalog', id FROM auth_permission WHERE code IN ('catalog:manage', 'inventory:manage')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-fulfillment', id FROM auth_permission WHERE code IN ('order:manage', 'shipping:manage')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-inventory', id FROM auth_permission WHERE code = 'inventory:manage'
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-finance', id FROM auth_permission WHERE code IN ('payout:manage', 'dashboard:read')
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-support', id FROM auth_permission WHERE code = 'support:manage'
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-analyst', id FROM auth_permission WHERE code = 'dashboard:read'
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-warehouse', id FROM auth_permission WHERE code = 'inventory:manage'
ON CONFLICT DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT 'role-shop-pos', id FROM auth_permission WHERE code = 'order:manage'
ON CONFLICT DO NOTHING;

-- Existing admins keep their effective access; SUPER_ADMIN is mapped to the
-- break-glass role until the legacy enum value is removed.
INSERT INTO auth_user_role (user_id, role_id, assigned_at)
SELECT id, 'role-platform-super-admin', NOW() FROM "user" WHERE user_type = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO auth_user_role (user_id, role_id, assigned_at)
SELECT id, 'role-platform-admin', NOW() FROM "user" WHERE user_type = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Existing sellers become shop owners so membership queries work immediately.
INSERT INTO shop_member (id, shop_id, user_id, status, is_owner, joined_at)
SELECT 'sm-' || seller.id, seller.shop_id, seller.user_id, 'ACTIVE', TRUE, NOW()
FROM seller
WHERE seller.shop_id IS NOT NULL
ON CONFLICT (shop_id, user_id) DO NOTHING;

INSERT INTO shop_member_role (member_id, role_id)
SELECT member.id, 'role-shop-owner'
FROM shop_member member
WHERE member.is_owner = TRUE
ON CONFLICT DO NOTHING;
