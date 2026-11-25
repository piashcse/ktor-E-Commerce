# Development Guide

## Requirements

- JDK 17 or newer
- PostgreSQL 16 (or Docker for Testcontainers-based tests)
- Gradle wrapper is checked in; no global Gradle install is required

## Run locally

```bash
cp .env.example .env      # fill DB_PASSWORD, JWT_SECRET, etc.
./gradlew run
```

Flyway applies migrations on startup. The API listens on `PORT` (default `8080`); Swagger UI is served at `/swagger`.

## Build and static analysis

```bash
./gradlew build            # compile + test + lint + detekt
./gradlew ktlintCheck      # formatting
./gradlew ktlintFormat     # auto-fix formatting and import order
./gradlew detekt           # static analysis
```

Do not relax `config/detekt/detekt.yml` thresholds to silence a finding; split the code or document the suppression locally.

## Tests

| Layer | Location | What it covers |
|---|---|---|
| Route tests | `src/test/kotlin/com/piashcse/feature/<domain>` | HTTP status, auth scopes, response shape with mocked dependencies |
| Unit tests | alongside the code they test | pure logic, money/tax calculations, mappers, services |
| Integration tests | `src/test/kotlin/com/piashcse/database` | real PostgreSQL via `DB_*` or Testcontainers |

Integration tests use `PostgresTestSupport`, which reuses one container per JVM. They prefer `DB_*` variables when present and are skipped locally when neither a database nor Docker is available. In CI, `COVERAGE_ENFORCE_DB=true` fails the build instead of skipping.

```bash
./gradlew test
./gradlew test --tests 'com.piashcse.feature.coupon.*'
```

Coverage is written to `build/reports/jacoco/test/html/index.html`; the CI gate is configured by `COVERAGE_MIN` in `.github/workflows/ci.yml`.

## Database migrations

- Migrations live in `src/main/resources/db/migration`.
- Name files `V<next>__<snake_case_description>.sql`.
- Never edit an applied migration; add a new one.
- Keep migrations safe for rolling deploys: add columns as nullable or with defaults, backfill, then enforce constraints in a later migration.
- Seed/repeatable data belongs in `R__*.sql`.

## Adding a feature

1. Create `feature/<domain>/` with route, request/response DTOs, service and repository interface.
2. Add Exposed queries to the repository, not the route.
3. Register the repository and service in the matching `di/*Module.kt` file.
4. Wire the route in `ConfigureRouting` under the customer, seller or admin tree.
5. Document the endpoint in `docs/` and update OpenAPI descriptions.
6. Add a route test and, for persistence changes, an integration test.
7. Run `./gradlew build`.

## Conventions

- All errors throw `AppException` subtypes; never hand-build error maps in routes.
- Use `Money`/`PricingService` for money math and `AppClock` for time.
- Use the transactional outbox for business-critical events.
- Keep route files free of persistence imports. `LayeringTest` enforces this.
