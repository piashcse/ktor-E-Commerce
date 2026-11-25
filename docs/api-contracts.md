# API Contracts

## Versioning

All business endpoints are under `/api/v1`. Health and metrics endpoints are unversioned (`/health*`, `/metrics`). Breaking changes require a new version prefix; additive fields and new optional query parameters do not.

The generated OpenAPI document is the machine-readable contract. Use `/swagger` for interactive exploration.

## Success responses

- `200 OK` with the resource or list payload.
- `201 Created` with the created resource where applicable.
- Actions without a resource return `200 OK` with `{ "message": "..." }`.
- `204 No Content` is not currently used.

Lists use the pagination envelope `{ "data": [...], "metadata": { ... } }` for endpoints that return paginated results; single resources are returned directly.

## Errors

Every failure uses the `ApiError` shape (`message`, `code`, optional `errors`/`requestId`, plus RFC 9457 aliases). See [Error Handling](errors.md) for the schema and status mapping. Validation and business-rule failures both use `400`; there is no `422` mapping.

## Idempotency

Mutating endpoints that can be retried safely accept an `Idempotency-Key` header:

- order placement/checkout
- payment creation and confirmation
- refunds
- payout requests
- B2B invoice settlement

Reuse the same key when retrying the same logical operation. A different payload with the same key is a client error. Keys are scoped per user where the endpoint is user-scoped.

## Authentication

- `Authorization: Bearer <jwt>` for user, seller and admin endpoints.
- API keys use the `X-API-Key` header and are scope-checked like roles.
- Webhooks are public but verified with the provider signature header (`Stripe-Signature`, `X-Webhook-Signature`, `X-Carrier-Signature`).

## Pagination and filtering

- `limit` and `offset` are the canonical parameters (`page`/`size` aliases may be accepted on some endpoints).
- Response metadata includes total count and current page information where supported.
- Unknown query parameters are ignored; invalid values return `400`.

## Event catalog

Durable outbox event types (stable strings; do not rename without a migration/version):

| Aggregate | Event types |
|---|---|
| Orders | `ORDER_PLACED`, `ORDER_STATUS_CHANGED` |
| Payments/refunds | `PAYMENT_COMPLETED`, `REFUND_STATUS_CHANGED` |
| Returns | `RMA_STATUS_CHANGED` |
| Catalog | `BACK_IN_STOCK`, `LOW_STOCK` |
| Reviews | `SELLER_REVIEW_CREATED`, `PRODUCT_REVIEW_CREATED`, `REVIEW_REPLY` |
| Identity | `USER_REGISTERED`, `KYC_STATUS_CHANGED` |
| Sellers | `SHOP_STATUS_CHANGED`, `PAYOUT_STATUS_CHANGED`, `DISPUTE_CREATED` |
| Platform | `ADMIN_ACTION`, `SEND_EMAIL` |

Consumers must be idempotent: the outbox is at-least-once and an event can be replayed from dead letters.
