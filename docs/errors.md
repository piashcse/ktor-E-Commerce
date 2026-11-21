# Error Handling & Status Codes

All API endpoints in **Ktor-E-Commerce** adhere to consistent, RESTful error handling principles modeled after industry standards (such as Stripe, GitHub, and RFC 7807 Problem Details).

---

## Response Envelope Philosophy

- **Direct Data on Success**: Successful requests return the domain payload or resource array directly (HTTP `200`, `201`, or `204 No Content`). No redundant `{ "isSuccess": true, "data": ... }` wrapping.
- **Structured JSON on Failure**: Whenever a client or server error occurs (HTTP `4xx` or `5xx`), a uniform JSON error payload is returned with human-readable messaging and contextual validation errors.

---

## Standard Error Schema

For general errors (e.g. `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error`), the response schema is:

```json
{
  "message": "Resource not found"
}
```

---

## Validation Error Schema (HTTP 400)

When incoming request parameters, path variables, or body fields fail validation rules (e.g., format violations, missing required fields, or constraint failures), an `errors` array provides field-level feedback:

```json
{
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    }
  ]
}
```

---

## HTTP Status Codes Reference

The following HTTP status codes are systematically returned across all API endpoints:

| Status Code | Meaning | Typical Trigger | Example Message |
|:---|:---|:---|:---|
| <span class="status-pill s200">200 OK</span> | Success | Resource retrieved or updated successfully | N/A (Payload returned) |
| <span class="status-pill s200">201 Created</span> | Resource Created | New entity successfully persisted | N/A (Created entity returned) |
| <span class="status-pill s200">204 No Content</span> | Success (No Body) | Resource deleted or action completed without return body | Empty |
| <span class="status-pill s400">400 Bad Request</span> | Validation / Client Error | Malformed JSON, missing mandatory fields, invalid parameters | `"Invalid email or password"` |
| <span class="status-pill s401">401 Unauthorized</span> | Missing / Expired Auth | Missing or expired JWT access token in `Authorization` header | `"Authentication required"` |
| <span class="status-pill s403">403 Forbidden</span> | Insufficient Permissions | Valid identity, but account type or assigned RBAC permissions lack access | `"Insufficient permissions"` |
| <span class="status-pill s404">404 Not Found</span> | Resource Missing | Non-existent ID or unmapped route path | `"Product not found"` |
| <span class="status-pill s409">409 Conflict</span> | State Conflict | Duplicate unique key (e.g., user email already registered, SKU duplicate) | `"User already exists with this email"` |
| <span class="status-pill s400">422 Unprocessable</span> | Business Rule Failure | Syntactically correct input violating business constraints (e.g., insufficient stock) | `"Requested quantity exceeds available stock"` |
| <span class="status-pill s400">429 Too Many Requests</span> | Rate Limit Exceeded | Auth or checkout brute-force limits triggered | `"Too many requests. Please try again later."` |
| <span class="status-pill s500">500 Internal Server Error</span> | Server Fault | Unexpected unhandled exception logged with request correlation ID | `"Internal server error"` |

---

## Client Error Handling Guidelines

1. **Check the HTTP Status First**: Differentiate between client remediation (`4xx`) and system degradation (`5xx`).
2. **Inspect Field-Level Errors**: If status is `400 Bad Request`, check if the `errors` array is present to map inline validation messages directly to UI form inputs.
3. **Handle Token Expiration**: When receiving `401 Unauthorized`, trigger the token refresh flow (`POST /api/v1/auth/refresh-token`) before retrying the original request.
4. **Idempotency on Network Failure**: For mutating requests (like placing an order or charging a payment), pass the `Idempotency-Key` header to safely retry without duplicate transactions.
