# Architecture Overview

Ktor-E-Commerce is a **modular monolith**: one deployable Ktor application, organized as vertical feature slices with shared infrastructure. This page describes the actual structure and the rules new code should follow.

## Package layout

```text
com.piashcse
├── Application.kt        # composition root (plugins, Koin, subscribers, workers)
├── config/               # .env loading and typed configuration
├── constants/            # cross-cutting enums, messages, permissions
├── database/             # Exposed tables/DAOs, connection setup, readiness
├── di/                   # repositoryModule, featureServiceModule, gatewayModule, authModule
├── event/                # domain events, in-process bus, outbox publisher, subscribers
├── feature/<domain>/     # routes + DTOs + services + repository for one domain
├── mapper/               # shared DAO → response DTO mapping
├── model/                # shared request/response DTOs
├── plugin/               # Ktor plugins, auth DSL, routing tree, health probes
├── service/              # shared domain/infrastructure services and background workers
└── utils/                # extensions, money, resilience, email, validation
```

Each `feature/<domain>` folder is the default home for new code. Keep route, use-case and persistence concerns in that folder unless the code is genuinely shared.

## Dependency rules

| Rule | Status |
|---|---|
| Route files never import `org.jetbrains.exposed.*` | Enforced by `LayeringTest` |
| Route files live under `feature/` | Enforced by `LayeringTest` |
| HTTP edge depends on services/repositories through Koin | Convention |
| Services return DTOs, not DAOs | Convention; a few legacy paths still expose DAOs internally |
| Cross-feature calls go through a service interface or an event | Convention |
| Payment depends on `service.payment` ports, not on subscription/refund features | Enforced by package layout |

Some services still use Exposed directly. That is a known transitional state, not the target. When touching such a service, prefer moving the query into the feature repository.

## Request lifecycle

1. `RequestTracing` assigns/propagates `X-Request-Id`.
2. Ktor plugins run in order: content negotiation, compression, CORS, call logging, metrics, status pages, rate limiting, auth.
3. The route resolves dependencies with `by inject()` and validates path/query/body input.
4. A service or repository runs the use case, usually inside `query {}` / `retryQuery {}`.
5. `ConfigureStatusPage` converts `AppException` into the canonical `ApiError` envelope.
6. Responses use direct payloads; actions without a payload return `{ "message": "..." }`.

## Transactions and consistency

- `query {}` runs a blocking Exposed transaction on the IO dispatcher.
- `retryQuery {}` retries only transient database failures. Keep external calls and event publication outside the retry window where possible.
- Business writes and their outbox rows must share one transaction so the event cannot be lost or published for rolled-back data.
- Order completion is centralized in `OrderCompletion` so payment, zero-total checkout, POS and B2B settlement apply loyalty, digital delivery, payouts and stock reservations consistently.

## Events

There are two delivery paths:

- **Transactional outbox** (durable): business events inserted in the same transaction as the state change. `OutboxPoller` relays them at least once. This is the required path for money, orders, refunds, email/OTP and seller payouts.
- **Direct `EventBus.publish`** (best effort): only for telemetry/UI events where loss is acceptable. Callers must not assume delivery.

Event type names and serializers are registered once in `OutboxPublisher`. Add a new event by adding one codec entry and a subscriber; do not create a second `when` block.

## Known architectural debt

- Some `*RepositoryImpl` classes (notably order checkout) still contain use-case orchestration. Extract use cases incrementally.
- `service/` mixes domain services, infrastructure adapters and background jobs. Prefer `infrastructure/` and `jobs/` subpackages when touching it.
- In-process state (rate limits, cache, event bus, workers) means the app currently assumes a single active instance for those features.
- Some services still hold Exposed queries; moving them into feature repositories is an incremental cleanup.

Payment no longer imports subscription or refund features: the gateway contract,
subscription billing and refund reconciliation live behind ports in
`service.payment`, with the feature modules providing the implementations.

## Decision records

- **ADR-001: Transactional outbox for critical events.** Chosen over publishing inside the request because a crash between commit and publish must not lose paid-order, refund or OTP events.
- **ADR-002: Provider adapters behind `PaymentGateway` / `CarrierGateway`.** Manual adapters keep local development and COD working without external credentials.
- **ADR-003: Modular monolith, single Gradle module.** Package boundaries come first; a multi-module split is deferred until boundaries are clean.
- **ADR-004: `ApiError` for every failure.** One shape reduces client branching and keeps request IDs in error responses.

## Related pages

- [Development](development.md)
- [Operations](operations.md)
- [API Contracts](api-contracts.md)
- [Outbox & DLQ](outbox.md)
