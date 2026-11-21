# API Keys

Create scoped API keys that inherit the owner's effective permissions.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| <span class="method-badge get">GET</span> | `/api/v1/api-keys` | List keys (prefixes only; secrets are never returned) |
| <span class="method-badge post">POST</span> | `/api/v1/api-keys` | Create a key |
| <span class="method-badge delete">DELETE</span> | `/api/v1/api-keys/{id}` | Revoke a key |

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
## Endpoint Details

### 1. List API keys

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/api-keys</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/api-keys' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Example",
    "prefix": "string",
    "scopes": [
      "string"
    ],
    "isActive": true,
    "lastUsedAt": "2024-05-06T12:00:00",
    "expiresAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---

### 2. Create API key

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/api-keys</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/api-keys' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "scopes": [
    "string"
  ],
  "expiresAt": "2024-05-06T12:00:00"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "prefix": "string",
  "scopes": [
    "string"
  ],
  "key": "pending-orders",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 3. Revoke API key

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/api-keys/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/api-keys/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---
