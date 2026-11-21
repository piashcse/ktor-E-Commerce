# Policy Consent

User consent records for policies, with read-only admin views.

## Endpoints

| Method | Endpoint | Description | Authentication Required |
|---|---|---|---|
| <span class="method-badge get">GET</span> | `/api/v1/policy-consents` | List all consent records for the authenticated user | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/policy-consents/{policyType}` | Check consent for a policy type | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/policy-consents/consent` | Record consent for a specific policy document | Yes |
| <span class="method-badge delete">DELETE</span> | `/api/v1/policy-consents/{id}` | Revoke a consent record (owner or admin) | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/admin/policy-consents` | List consent records filtered by user and/or policy type | Yes (Admin) |
| <span class="method-badge get">GET</span> | `/api/v1/policy-consents/requirements` | Policies requiring consent | Yes |

### 1. Record consent

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/policy-consents/consent</span>
</div>

```json
{
  "policyId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}
```

Consent rows are append-only: re-consenting creates a new row with `grantedAt`, and
`DELETE /api/v1/policy-consents/{id}` sets `revokedAt` rather than deleting the audit trail.

### 2. Check consent

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/policy-consents/{policyType}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/policy-consents/PRIVACY_POLICY' \
  -H 'Authorization: Bearer <access_token>'
```

### 3. Admin filtering

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/policy-consents?userId=&policyType=</span>
</div>

When `REQUIRE_POLICY_CONSENT=true`, checkout rejects orders until the customer has consented to
every policy type where `requiresConsent` is true (`SHIPPING_POLICY` is excluded).
## Endpoint Details

### 4. Retrieve all consent records for the authenticated user

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/policy-consents</span>
</div>

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/policy-consents?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "policyId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "consentDate": "2024-05-06T12:00:00",
      "ipAddress": "123 Main St",
      "userAgent": "string",
      "revokedAt": "2024-05-06T12:00:00",
      "policyVersion": "string",
      "policyContentHash": "string"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 5. Revoke a consent record (owner or admin)

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/policy-consents/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/policy-consents/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 6. Policies requiring consent

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/policy-consents/requirements</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/policy-consents/requirements' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "required": [
    {
      "type": "CUSTOMER",
      "policyId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "title": "Example",
      "version": "1.0.0",
      "effectiveDate": "2024-05-06T12:00:00",
      "hasConsented": true
    }
  ],
  "complete": true
}
```

---
