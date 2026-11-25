# Contributing

Thanks for contributing to Ktor-E-Commerce.

## Before you start

- Read [docs/development.md](docs/development.md) for setup and test commands.
- For security issues, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.
- Keep pull requests focused: one feature, fix or refactor per PR.

## Workflow

1. Fork the repository and create a branch from `main`.
2. Make the change and add/update tests.
3. Run the full check locally:

   ```bash
   ./gradlew ktlintCheck detekt test
   ```

4. Update documentation when behavior, configuration or endpoints change. Do not hand-edit generated OpenAPI output.
5. Open a pull request using the template and describe the behavior change, not just the files touched.

## Code guidelines

- Route files must not import Exposed; persistence belongs in repositories.
- Throw `AppException` subtypes instead of building ad-hoc error responses.
- Money uses `BigDecimal` and `Money`/`PricingService`; never `Double`.
- Business-critical events go through the transactional outbox.
- New configuration must be added to `.env.example`, `DotEnvConfig`, and the operations docs.
- Add indexes and constraints in new Flyway migrations; never edit an applied migration.

## Tests

- Route tests: HTTP status, auth scopes and response shape.
- Unit tests: pure logic and calculations.
- Integration tests: real PostgreSQL through `PostgresTestSupport`.
- The CI coverage gate is enforced; add tests rather than lowering `COVERAGE_MIN`.

## Commit messages

Use a conventional prefix where possible: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`. Explain the reason in the body when the diff is not self-explanatory.
