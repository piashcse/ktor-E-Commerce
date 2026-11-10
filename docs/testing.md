# Testing & Coverage

The project uses a three-layer test strategy and enforces coverage in CI.

## Test layers

| Layer | Location | Database | Purpose |
|---|---|---|---|
| Unit | `src/test/kotlin/com/piashcse/utils`, `constants`, `plugin`, `feature/*` | No | Money math, validators, serializers, state machines, ranking, DTO rules |
| Route/contract | `src/test/kotlin/com/piashcse/feature/**/*RoutesTest.kt`, `TypedContractTest` | No (repositories mocked) | HTTP status, auth scopes, validation, JSON contracts, OpenAPI metadata |
| Integration | `src/test/kotlin/com/piashcse/database/*IntegrationTest.kt` | PostgreSQL | Flyway migrations, repositories, services, order/payment/refund/payout lifecycles, concurrency |

Integration tests prefer `DB_*` (CI service) and fall back to Testcontainers locally. They are
skipped only when neither is available; in CI they are mandatory.

## Commands

```bash
# Fast unit + route tests (integration tests skip without a DB)
./gradlew test

# Coverage report
./gradlew test jacocoTestReport
# HTML: build/reports/jacoco/test/html/index.html
# XML:  build/reports/jacoco/test/jacocoTestReport.xml

# Enforce the local gate (unit-only baseline)
./gradlew jacocoTestCoverageVerification
```

### Running the integration suites locally

```bash
# Example: dedicated local database
createdb ktor-ecom-test
DB_NAME=ktor-ecom-test ./gradlew test --tests 'com.piashcse.database.*'
```

The tests apply Flyway migrations, use unique suffixes for created rows, and are safe to re-run
against a dedicated test database. Do not point them at a shared/production database.

## Coverage policy

Coverage excludes generated/simple surface area (`ApplicationKt`, `di`, `database/entities`,
`model`, synthetic classes). Gates are split so a developer build without a database still has a
meaningful floor while CI enforces the integration-backed target.

### CI (PostgreSQL available)

```bash
COVERAGE_MIN=0.50 COVERAGE_ENFORCE_DB=true \
  ./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

| Scope | Instruction minimum |
|---|---:|
| Overall | 50% |
| `feature.order` | 60% |
| `feature.payment` | 60% |
| `feature.refund_request` | 70% |
| `feature.payout` | 55% |
| `feature.cart` | 70% |
| `feature.dashboard` | 80% |
| `feature.coupon` | 60% |
| `feature.review_rating` | 70% |
| `feature.profile` | 70% |
| `feature.policy` | 60% |

### Local (unit/route only)

| Scope | Instruction minimum |
|---|---:|
| Overall | 28% |
| Branch | 8% |
| `constants` | 90% |
| `utils.validator` | 90% |
| `utils.db` | 80% |
| `utils.resilience` | 75% |
| `utils.common` | 55% |
| `plugin` | 65% |
| `feature.common` | 30% |

## Current baseline

At the time of writing (CI configuration):

- **430 tests**
- **54.3% instruction**, **25.7% branch**, **59.3% line**, **78.1% class**

Known lower-coverage areas and next targets:

- `feature.auth` — login/lockout/refresh token edge cases
- `feature.product` — search facets, variant/option guards
- `event.subscriber` — notification/email/audit subscribers
- `utils.email` — SMTP failure and circuit-breaker paths
- `config` — environment validation and fail-fast behavior

## Adding tests

- Prefer unit tests for pure logic; avoid database setup unless the behavior lives in SQL/repositories.
- Use `RouteTestHelper.installTestInfra` + MockK for route tests.
- Use a unique suffix (`UUID`) for integration data and clean up when a test asserts global counts.
- Keep one `@Test` per behavior; prefer explicit names like `` `paid orders cannot be canceled without a refund` ``.
- Update `docs/endpoints.md`/`README.md` when adding or changing routes.
