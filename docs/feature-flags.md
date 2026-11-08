# Feature Flags

Runtime switches backed by the `feature_flag` table. Super-admins can toggle capabilities without a redeploy.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/feature-flags` | List all flags |
| `PUT` | `/api/v1/admin/feature-flags/{key}` | Enable/disable a flag |

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
