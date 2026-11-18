# API Keys

Scoped API keys allow integrations to call permitted endpoints without a user JWT. Keys use the same `Authorization: Bearer <key>` header as JWTs.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/api-keys` | List keys (prefixes only; secrets are never returned) |
| `POST` | `/api/v1/api-keys` | Create a key |
| `DELETE` | `/api/v1/api-keys/{id}` | Revoke a key |

All three require `api_key:manage`.

Create body:

```json
{
  "name": "warehouse-sync",
  "scopes": ["inventory:manage", "order:manage"],
  "expiresAt": "2027-01-01T00:00:00"
}
```

Scopes accept the stable permission codes (`order:manage`, `inventory:manage`) or legacy
`Permission` enum names (`ORDER_MANAGE`, `INVENTORY_MANAGE`). Codes are preferred.

The plaintext `key` is returned **only once** in `ApiKeyCreatedResponse`.

## Effective permissions

A key can only receive scopes the owner currently holds. Scopes are re-checked against the
owner's **effective permissions** on every authenticated request, so a role change narrows an
existing key immediately.

Inspect the owner's roles and permissions with:

```
GET /api/v1/me/permissions
```

Platform admins should use `/api/v1/admin/rbac/*` to manage roles; see [RBAC](rbac.md).

Keys can be revoked immediately; lookups use a hash of the key, never the plaintext. Revoking
returns:

```json
{ "message": "API key revoked" }
```
