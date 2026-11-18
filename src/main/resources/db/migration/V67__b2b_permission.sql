-- B2B admin routes were previously reachable by any ADMIN. Gate them with an
-- explicit permission without changing the B2B feature surface.
INSERT INTO auth_permission (id, code, resource, action, description, is_system)
VALUES ('perm-b2b-manage', 'b2b:manage', 'b2b', 'manage', 'Manage B2B accounts, quotes, purchase orders and invoices', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM auth_role role
JOIN auth_permission permission ON permission.code = 'b2b:manage'
WHERE role.code IN ('platform:super_admin', 'platform:admin')
ON CONFLICT DO NOTHING;
