# Data Retention & Privacy Operations

This document defines the intended retention and deletion policy for personal data. It must be
reviewed by legal/accounting before production use.

## Principles

- Collect only what is needed for the order, account, payment, tax and support flows.
- Pseudonymize or anonymize when the business purpose ends.
- Retain financial/tax records only as long as legally required.
- Never use deleted-account data for marketing or analytics.
- Backups follow the same retention schedule and are not selectively edited.

## Recommended retention

| Data category | Default retention | Notes |
|---|---|---|
| Active account/profile | While account is active | Erasure request starts anonymization |
| Orders, payments, refunds, invoices, payouts, tax | 7 years after the financial year | Accounting/tax/chargeback obligations |
| Shipping addresses on orders | 7 years | Needed for disputes/returns; direct identifiers may be redacted |
| Support tickets/messages | 3 years after closure | Remove/anonymize on erasure unless needed for a dispute |
| Analytics events | 13 months | Aggregate reports may be kept longer without identifiers |
| Webhook inbox payloads (payment/carrier) | 90 days | Retain only the processing status after that |
| Notification inbox | 1 year | User-deletable |
| Cart, wishlist, browsing data | While active / 30 days idle | Deleted on erasure |
| Passkeys and social identities | Until removed or account erased | Credentials are deleted on erasure |
| Wallet ledger / gift-card redemptions | 7 years | Financial audit trail; account identifiers pseudonymized |
| B2B accounts/quotes/POs | 7 years | Contract/tax records |
| Seller reviews/disputes | 3 years after resolution | Anonymize author on erasure |
| Audit logs | 2 years | Security/legal investigations |
| Backups | 35 days rolling | Not selectively editable |

## Erasure exceptions

Account erasure anonymizes the account and removes self-service data. The following may be retained
in pseudonymous form where required:

- orders, payments, refunds, invoices, tax and payout records
- chargeback/dispute evidence
- fraud/security records required for investigations
- records subject to a legal hold

Support tickets, passkeys, wallet account/ledger, digital entitlements, analytics events and social
identities are removed or anonymized. Order contact PII and delivery snapshots are cleared.

## Data-subject requests

- Export endpoint returns account/profile, addresses, orders, payments, refunds, consents, reviews,
  notifications and additional wallet/subscription/support/passkey/B2B/analytics metadata.
- Erasure requires the account password and revokes all refresh tokens.
- Operators should record DSAR completion in the audit log.

## Operational jobs

Recommended scheduled jobs:

- purge analytics events older than 13 months
- purge webhook inbox rows older than 90 days
- expire/clear idle carts and wishlists
- delete expired passkey challenges (in-memory today)
- revoke digital entitlements whose subscription period ended
- expire and anonymize support tickets after the retention window

## Open decisions

- Exact jurisdiction-specific financial retention periods.
- Whether wallet ledgers must be retained after erasure (currently deleted).
- Whether support transcripts must be retained for dispute/legal holds.
- Backup restore procedures for re-applying erasure requests.
