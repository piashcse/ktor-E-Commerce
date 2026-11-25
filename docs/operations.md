# Operations Guide

## Probes and observability

| Endpoint | Purpose | Response |
|---|---|---|
| `GET /health` | Process/version summary | `{"status":"UP","version":"..."}` |
| `GET /health/live` | Liveness | `{"message":"UP"}` |
| `GET /health/ready` | Primary + replica `SELECT 1` | `200 READY` or `503` `ApiError` |
| `GET /metrics` | Prometheus scrape | Plain text |

`/metrics` is intentionally internal:

- If `METRICS_TOKEN` is set, callers must send `X-Metrics-Token`.
- If it is empty, only loopback requests are allowed and only when `TRUST_PROXY=false` and `FORCE_HTTPS=false`.
- In production (behind a proxy/TLS), always set `METRICS_TOKEN`.

Standard JVM/HTTP metrics are registered through Micrometer, plus application counters such as outbox dead letters, payment webhooks and email delivery. Logs include `[req=<id>]`; the same ID is returned as `X-Request-Id`.

## Configuration

Configuration is loaded from the environment (`.env` locally). `.env.example` is the canonical list. The most important groups:

### Core

| Variable | Default | Notes |
|---|---|---|
| `PORT` / `HOST` | `8080` / `localhost` | HTTP bind |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | localhost / 5432 / ktor-ecommerce / postgres / postgres | Primary database |
| `DB_READ_URL` | unset | Optional read replica; readiness checks it |
| `DB_POOL_SIZE` / `DB_MIN_IDLE` / `DB_CONNECTION_TIMEOUT_MS` | `10` / `2` / `30000` | Hikari pool |
| `JWT_SECRET` | — | Required; generate with `openssl rand -base64 48` |
| `ALLOWED_ORIGINS` | local origins | Comma-separated CORS allowlist |
| `FORCE_HTTPS` | `false` | Emit HSTS only behind TLS termination |
| `TRUST_PROXY` | `false` | Trust `X-Forwarded-*`; enable only behind a sanitizing proxy |
| `REDIS_URL` | unset | Optional Redis cache and shared rate limiter; falls back to in-process state if unreachable |

### Providers

| Variable | Notes |
|---|---|
| `PAYMENT_PROVIDER` | `manual` (COD) or `stripe` |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Required when Stripe is enabled |
| `SHIPPING_PROVIDER` | `manual` or `shippo` |
| `SHIPPO_API_TOKEN`, `CARRIER_WEBHOOK_SECRET` | Required when Shippo is enabled |
| `UPLOAD_DIR` | Local upload root (default `uploads`) |
| `EMAIL_HOST`, `EMAIL_PORT`, `EMAIL_USERNAME`, `EMAIL_PASSWORD`, `EMAIL_SSL` | SMTP delivery |

### Limits and storefront

| Variable | Notes |
|---|---|
| `RATE_LIMIT_*` | `limit:refillMinutes` per scope (auth, OTP, refresh, write, search, seller/admin write, general) |
| `FX_MAX_AGE_HOURS` | Reject stale FX rates; `0` disables |
| `FREE_SHIPPING_THRESHOLD` | Storefront shipping rule |
| `RETURN_WINDOW_DAYS` | Refund/RMA window |
| `REQUIRE_POLICY_CONSENT` | Require all policies accepted before checkout |
| `PUBLIC_BASE_URL`, `STOREFRONT_URL` | Links in emails and responses |

## Deployment

A `Dockerfile` and `docker-compose.yml` are provided.

1. Build: `docker build -t ktor-ecom .`
2. Provide secrets via environment/secret manager, not a baked `.env`.
3. Point the container at PostgreSQL and run with a non-root user.
4. Set `METRICS_TOKEN`, `FORCE_HTTPS=true` and `TRUST_PROXY=true` when behind TLS/proxy.
5. Mount `UPLOAD_DIR` on persistent storage, or replace local uploads with object storage before scaling horizontally.
6. Probes: `/health/live` for liveness, `/health/ready` for readiness.

## Background work

| Worker | Responsibility |
|---|---|
| `OutboxPoller` | Claims outbox rows (60-minute stale-claim cutoff), publishes to `EventBus`, retries, dead-letters |
| `AsyncWorker` | Image processing and other queued background tasks |
| `StockReservationCleanup` | Releases expired reservations |
| `MaintenanceCleanup` | Retention cleanup for old operational rows |
| `SubscriptionBillingWorker` | Renewals, dunning and invoice generation |

Each background worker acquires a Postgres advisory lock at startup, so only one replica runs the outbox poller, billing loop or cleanup jobs. Set `REDIS_URL` to share the cache and rate limits. The `EventBus` remains in-process, so outbox events are handled by the poller leader; if the leader exits, restart/redeploy to re-elect (runtime re-election is not implemented).

## Runbook

### Outbox backlog or dead letters

1. `GET /api/v1/admin/outbox/summary` — check pending and dead-letter counts.
2. `GET /api/v1/admin/outbox/dead-letters` — inspect the error and payload.
3. Fix the cause (provider down, malformed payload, subscriber bug).
4. `POST /api/v1/admin/outbox/dead-letters/{id}/requeue` (or bulk requeue).
5. Watch the pending count return to zero.

### Payment webhook failures

Provider webhooks return `500` with an `ApiError` so the provider retries. Repeated `500`s usually mean:

- the payment/refund row is not visible yet (ordering issue), or
- signature verification fails (wrong `STRIPE_WEBHOOK_SECRET`).

Confirm the provider secret and re-send the event from the provider dashboard after the local row exists.

### Stale FX rates

If checkout rejects quotes, check `/api/v1/fx-rates` and the last `fx_rate` update. Either refresh rates or raise `FX_MAX_AGE_HOURS` temporarily. Do not disable staleness in production without a documented decision.

### Failed payouts

Seller payouts are captured by `PayoutLedger`. Check `GET /api/v1/admin/payouts` for `FAILED` rows, inspect the provider reference, then retry the payout action. Ledger writes are idempotent per order.

### Read replica down

`/health/ready` returns `503` when the replica is unreachable. Remove `DB_READ_URL` and restart to fall back to the primary, or restore replica connectivity. Do not let readiness stay green with a broken replica.

## Backup and restore

- Back up PostgreSQL with `pg_dump`/PITR; the database is the source of truth.
- `UPLOAD_DIR` needs a matching filesystem backup.
- Outbox/dead-letter rows are part of the database backup; replaying after a restore may re-deliver events, so subscribers must remain idempotent.
- Test restores regularly; a backup that has never been restored is not verified.
