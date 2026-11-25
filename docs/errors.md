# Error Handling & Status Codes

All API endpoints in **Ktor-E-Commerce** adhere to consistent, RESTful error handling principles modeled after industry standards (such as Stripe, GitHub, and RFC 7807 Problem Details).

---

## Response Envelope Philosophy

- **Direct Data on Success**: Successful requests return the domain payload or resource array directly (HTTP `200` or `201`). No redundant `{ "isSuccess": true, "data": ... }` wrapping.
- **Message Acknowledgement**: Actions with no resource payload return `200 OK` with `{ "message": "..." }`. Delete endpoints follow this convention rather than `204 No Content`.
- **Structured JSON on Failure**: Whenever a client or server error occurs (HTTP `4xx` or `5xx`), a uniform JSON error payload is returned with a machine-readable code, human-readable message, and optional field errors.

---

## Standard Error Schema

All errors are produced by `ApiError.of(...)` and include RFC 9457-style aliases in addition to the canonical `message`/`code` fields:

```json
{
  "message": "Resource not found",
  "code": "NOT_FOUND",
  "errors": null,
  "requestId": "req-9c8f61aa-7023",
  "type": "NOT_FOUND",
  "title": "Resource not found",
  "status": 404
}
```

| Field | Description |
|---|---|
| `message` | Human-readable, safe to display |
| `code` | Stable machine-readable code (`BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `VALIDATION_FAILED`, `SERVICE_UNAVAILABLE`, `INTERNAL_SERVER_ERROR`) |
| `errors` | Field-level errors for validation failures, otherwise `null` |
| `requestId` | Correlation ID also written to logs and the `X-Request-Id` response header |
| `type` / `title` / `status` | RFC 9457 compatibility aliases |

---

## Validation Error Schema (HTTP 400)

Request validation and business-rule failures both return `400 Bad Request`. There is no separate `422` mapping. When individual fields fail validation, `errors` carries field-level feedback:

```json
{
  "message": "Validation failed",
  "code": "VALIDATION_FAILED",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    }
  ],
  "requestId": "req-9c8f61aa-7023",
  "type": "VALIDATION_FAILED",
  "title": "Validation failed",
  "status": 400
}
```

---

## HTTP Status Codes Reference

The following HTTP status codes are systematically returned across all API endpoints:

| Status Code | Meaning | Typical Trigger | Example Message |
|:---|:---|:---|:---|
| <span class="status-pill s200">200 OK</span> | Success | Resource retrieved or updated successfully | N/A (Payload returned) |
| <span class="status-pill s200">201 Created</span> | Resource Created | New entity successfully persisted | N/A (Created entity returned) |
| <span class="status-pill s200">204 No Content</span> | Not used | Reserved for future endpoints; current deletes return `200` with a message body | — |
| <span class="status-pill s400">400 Bad Request</span> | Validation / Client Error | Malformed JSON, missing mandatory fields, invalid parameters | `"Invalid email or password"` |
| <span class="status-pill s401">401 Unauthorized</span> | Missing / Expired Auth | Missing or expired JWT access token in `Authorization` header | `"Authentication required"` |
| <span class="status-pill s403">403 Forbidden</span> | Insufficient Permissions | Valid identity, but account type or assigned RBAC permissions lack access | `"Insufficient permissions"` |
| <span class="status-pill s404">404 Not Found</span> | Resource Missing | Non-existent ID or unmapped route path | `"Product not found"` |
| <span class="status-pill s409">409 Conflict</span> | State Conflict | Duplicate unique key (e.g., user email already registered, SKU duplicate) | `"User already exists with this email"` |
| <span class="status-pill s400">400 Bad Request</span> | Validation / Business Rule Failure | Malformed JSON, missing fields, invalid parameters, or a violated business constraint | `"Requested quantity exceeds available stock"` |
| <span class="status-pill s400">429 Too Many Requests</span> | Rate Limit Exceeded | Auth or checkout brute-force limits triggered | `"Too many requests. Please try again later."` |
| <span class="status-pill s500">500 Internal Server Error</span> | Server Fault | Unexpected unhandled exception logged with request correlation ID | `"Internal server error"` |

---

## Client Error Handling Guidelines

1. **Check the HTTP Status First**: Differentiate between client remediation (`4xx`) and system degradation (`5xx`).
2. **Inspect Field-Level Errors**: If status is `400 Bad Request`, check if the `errors` array is present to map inline validation messages directly to UI form inputs.
3. **Handle Token Expiration**: When receiving `401 Unauthorized`, trigger the token refresh flow (`POST /api/v1/auth/refresh-token`) before retrying the original request.
4. **Idempotency on Network Failure**: For mutating requests (like placing an order or charging a payment), pass the `Idempotency-Key` header to safely retry without duplicate transactions.
