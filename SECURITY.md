# Security Policy

## Reporting a vulnerability

Do **not** open a public GitHub issue for security problems.

Report privately to the maintainer listed in the repository profile, or use GitHub's private vulnerability reporting for this repository. Include:

- affected version/commit,
- reproduction steps or a proof of concept,
- impact assessment (data exposure, auth bypass, payment manipulation, etc.),
- any suggested fix.

You will receive an acknowledgement as soon as possible. Please allow time for a fix and coordinated disclosure before publishing details.

## Supported versions

Security fixes target the latest released version on `main`.

## Deployment checklist

- Set a strong, unique `JWT_SECRET`; rotate it if it may have leaked.
- Set `METRICS_TOKEN` and keep `/metrics` internal.
- Enable `FORCE_HTTPS=true` behind TLS termination and `TRUST_PROXY=true` only behind a sanitizing proxy.
- Use provider webhook secrets (`STRIPE_WEBHOOK_SECRET`, `CARRIER_WEBHOOK_SECRET`) and verify signatures.
- Store credentials in a secret manager, never in `.env` committed to git.
- Restrict PostgreSQL network access and use least-privilege database users.
- Persist and back up `UPLOAD_DIR`; validate uploaded content types in routes (already enforced).
- Keep dependencies patched; CI runs Trivy on the built image.

## Known limitations

Rate limiting, cache, the event bus and background workers are in-process. Do not run multiple active instances without moving those concerns to shared infrastructure.
