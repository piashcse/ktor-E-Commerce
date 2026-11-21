# Feature Flags

Runtime switches that toggle platform capabilities without a redeploy.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| <span class="method-badge get">GET</span> | `/api/v1/admin/feature-flags` | List all flags |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/feature-flags/{key}` | Enable/disable a flag |

Update body:

```json
{
  "enabled": true,
  "description": "Allow checkout without an account"
}
```

## Known flags

| Key | Effect |
|---|---|
| `guest_checkout` | Enables `POST /api/v1/checkout/guest` |

Unknown flags default to disabled (`FeatureFlags.isEnabled` returns `false`).
## Endpoint Details

### 1. List feature flags

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/feature-flags</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/feature-flags' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "key": "pending-orders",
    "enabled": true,
    "description": "Looks good",
    "updatedAt": "2024-05-06T12:00:00"
  }
]
```

---

### 2. Set feature flag

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/feature-flags/{key}</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/feature-flags/new_checkout' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "enabled": true,
  "description": "Looks good"
}'
```

#### Response

```json
{
  "key": "pending-orders",
  "enabled": true,
  "description": "Looks good",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---
