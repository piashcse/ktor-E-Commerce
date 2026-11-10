# Policy Consent API

Records and audits a user's consent to policy documents (privacy policy, terms, cookies, etc.).
The admin view is read-only; consent is always written by the authenticated user.

**Base URL:** `http://localhost:8080`

## Endpoints

| Method | Endpoint | Description | Authentication Required |
|---|---|---|---|
| `GET` | `/api/v1/policy-consents` | List all consent records for the authenticated user | Yes |
| `GET` | `/api/v1/policy-consents/{policyType}` | Check consent for a policy type | Yes |
| `POST` | `/api/v1/policy-consents/consent` | Record consent for a specific policy document | Yes |
| `DELETE` | `/api/v1/policy-consents/{id}` | Revoke a consent record (owner or admin) | Yes |
| `GET` | `/api/v1/admin/policy-consents` | List consent records filtered by user and/or policy type | Yes (Admin) |

## Record consent

**`POST /api/v1/policy-consents/consent`**

```json
{
  "policyId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}
```

Consent rows are append-only: re-consenting creates a new row with `grantedAt`, and
`DELETE /api/v1/policy-consents/{id}` sets `revokedAt` rather than deleting the audit trail.

## Check consent

**`GET /api/v1/policy-consents/{policyType}`**

`policyType` is one of `PRIVACY_POLICY`, `TERMS_CONDITIONS`, `REFUND_POLICY`, `COOKIE_POLICY`,
`DISCLAIMER`, `EULA`, `SHIPPING_POLICY`.

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/policy-consents/PRIVACY_POLICY' \
  -H 'Authorization: Bearer <access_token>'
```

## Admin filtering

**`GET /api/v1/admin/policy-consents?userId=&policyType=`**

Both filters are optional; results are paginated with `page`/`perPage` or `limit`/`offset`.

## Checkout enforcement

When `REQUIRE_POLICY_CONSENT=true`, checkout rejects orders until the customer has consented to
every policy type where `requiresConsent` is true (`SHIPPING_POLICY` is excluded).
