# API Keys

Scoped API keys allow integrations to call permitted endpoints without a user JWT. Keys use the same `Authorization: Bearer <key>` header as JWTs.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/api-keys` | List keys (prefixes only; secrets are never returned) |
| `POST` | `/api/v1/api-keys` | Create a key |
| `DELETE` | `/api/v1/api-keys/{id}` | Revoke a key |

Create body:

```json
{
  "name": "warehouse-sync",
  "scopes": ["INVENTORY_READ", "INVENTORY_WRITE"],
  "expiresAt": "2027-01-01T00:00:00"
}
```

The plaintext `key` is returned **only once** in `ApiKeyCreatedResponse`. Scopes are stored as permission names; requests are authorized against the key's scopes by `requirePermission`.

Keys can be revoked immediately; lookups use a hash of the key, never the plaintext.
