# Privacy Policy Consent Documentation Merged

This documentation has been merged into the main [Privacy Policy API documentation](privacy-policy.md). Please refer to the main privacy-policy.md file for all privacy policy and consent-related endpoints.
## Error Handling

This API follows industry-standard error handling patterns (Stripe, GitHub, OpenAI):

### Success Responses
- **HTTP status code indicates success** (200, 201, 204)
- **Response body contains data directly** (no wrapper object)
- No `isSuccess` or `statusCode` fields needed

### Error Responses

**Standard Error (400/401/403/404/500):**
```json
{
  "message": "Error description"
}
```

**Validation Error (400):**
```json
{
  "message": "Validation failed",
  "errors": [
    {"field": "email", "message": "Invalid email format"},
    {"field": "password", "message": "Password must be at least 8 characters"}
  ]
}
```

### Common Error Codes

| Status Code | Description | Example Message |
|-------------|-------------|-----------------|
| `400` | Bad Request | `"Invalid email or password"` |
| `401` | Unauthorized | `"Authentication required"` |
| `403` | Forbidden | `"Insufficient permissions"` |
| `404` | Not Found | `"Product not found"` |
| `409` | Conflict | `"User already exists with this email"` |
| `500` | Internal Server Error | `"Internal server error"` |

All error messages are centralized and consistent across all endpoints.
