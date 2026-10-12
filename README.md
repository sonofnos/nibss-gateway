# nibss-gateway

A NIBSS NIP style payment gateway service for a fictional Nigerian bank: name enquiry, outward interbank transfer, transaction status query (TSQ), inward credit, and reversal, sitting behind AML/KYC screening, double-entry ledger postings, settlement reconciliation, and field-level encryption. Java 21, Spring Boot 4.1.1 (Spring Boot 3.x is no longer offered by start.spring.io as of the writing of this repo, so this runs on 4.1, same as the other Spring Boot services in this portfolio), PostgreSQL, Maven.

## What this is and is not

This is a portfolio project. There is no real NIBSS connection anywhere in this repo, no sponsor bank, no live institution code, no real AML/KYC vendor, and no real BVN lookup. Every third party NIBSS NIP itself, the AML screen, the KYC provider is a documented interface with a single deterministic in-process fake implementation. Stated plainly: this is built against a local simulator, not a live NIBSS connection, same honest-fake pattern as [naira-rails](https://github.com/sonofnos/naira-rails).

What is real: the Spring Boot service, the Postgres schema, the double-entry ledger, the AES-GCM field encryption, the HMAC webhook verification, the audit trail, and all 35 tests, 22 of them against a real Postgres via Testcontainers.

## NIBSS NIP operations implemented

| Operation | Endpoint |
|---|---|
| Name enquiry | `POST /api/nip/name-enquiry` |
| Outward transfer | `POST /api/nip/transfers` |
| TSQ (transaction status query) | `GET /api/nip/transfers/{clientReference}/tsq` |
| Inward credit (webhook) | `POST /api/nip/webhooks/inward-credits` |
| Reversal (webhook) | `POST /api/nip/webhooks/reversals` |

`FakeNibssNipClient` is the only implementation of `NibssNipClient`: a hardcoded mock bank registry (`com.sonofnos.nibssgateway.nip.FakeNibssNipClient`) covering a few fake banks and accounts, plus two magic account numbers used by tests to force a NIBSS timeout deterministically.

## Requery, never retry

NIBSS NIP's real failure mode: a funds transfer call can time out without telling you whether the money actually moved. The only safe way to find out is a transaction status query (TSQ) against the same session id never resubmitting the transfer, because if it already landed on NIBSS's side, resubmitting pays the recipient twice. naira-rails documents this same fact for NestJS/TypeScript against a simulated NIP client; this repo implements the identical rule in Java.

`OutwardTransferService.initiate()`:
1. Saves the transfer (idempotent on `clientReference` via a unique DB constraint).
2. Runs AML screening. A flagged transfer goes to `PENDING_REVIEW` and stops there no debit, no NIBSS call.
3. Resolves the destination name (name enquiry), debits the source account, posts the ledger entry, calls `sendFundsTransfer` **exactly once**.
4. On `NibssTimeoutException`, calls `queryTransactionStatus` (TSQ) instead of calling `sendFundsTransfer` again. `OutwardTransferServiceTest.timeoutTriggersRequeryNotResend` asserts `sendFundsTransfer` is invoked exactly once and `queryTransactionStatus` exactly once.
5. A `DEFINITE_FAILURE` outcome (direct or via requery) posts a correcting double-entry (credits the source account back, debits the suspense account) and marks the transfer `FAILED`. The original debit entry is never deleted.

## Ledger

Every money-moving operation posts two `LedgerEntry` rows a `DEBIT` and a `CREDIT` of equal amount under the same reference, never a bare balance update. `LedgerService.post()` locks both accounts with `SELECT ... FOR UPDATE` in ascending account-id order, regardless of which account is being debited or credited, so two transfers moving money in opposite directions between the same pair of accounts always take their locks in the same order and cannot deadlock each other.

A NIBSS NIP transfer always has one leg outside this bank's own books (the destination bank, or the originating bank on an inward credit). `SettlementAccounts.NIBSS_SETTLEMENT_SUSPENSE` is a single ledger account standing in for that outside world: outward transfers credit it, inward credits debit it, reversals post the opposite correction. It is the one account in the system allowed to go negative (`Account.allowNegative`) real customer accounts cannot.

`LedgerConcurrencyIntegrationTest` fires 20 concurrent transfers (10 each direction) between the same two accounts at a real Postgres and asserts `sum(debit) == sum(credit)` across all resulting ledger entries, with no deadlock and no lost update.

## AML / fraud screening, KYC

`AmlScreeningProvider` is a plain interface behind Spring DI. The only implementation, `FakeAmlScreeningProvider`, flags a transfer when the amount meets or exceeds a configured threshold or the destination account number is on a hardcoded test watchlist never a real sanctions, PEP, or fraud-model screen. Every outward transfer passes through it before anything else happens; a flagged transfer moves to `PENDING_REVIEW`, not silently blocked and not silently allowed.

`KycProvider` / `FakeKycProvider` follow the same pattern: a deterministic check against an 11-digit identifier shape, standing in for a BVN lookup, with no real KYC vendor behind it.

## Settlement reconciliation

`POST /api/reconciliation/runs` (multipart CSV upload) classifies every transfer this gateway recorded against a daily settlement file by client reference:

- **MATCHED** amounts agree, no action.
- **AMOUNT_MISMATCH** audit-logged (`RECONCILIATION_MISMATCH`) for manual review; the transfer's own status is left alone until a human confirms which side is wrong.
- **MISSING_IN_LEDGER** the settlement file has a reference this gateway never recorded; audit-logged (`RECONCILIATION_ORPHAN`) as a possible fraud or integration gap, never auto-written into the ledger.
- **MISSING_IN_SETTLEMENT** left pending; NIBSS settlement runs on a cycle, so this is normal for same-day transfers and is exactly the set of references a standalone TSQ should be run against if it stays missing past one cycle.

`ReconciliationIntegrationTest` drives the real endpoint and proves all four categories are produced from one settlement file.

## Security

- **HMAC-SHA256 webhooks**: `inward-credits` and `reversals` both require `X-NIBSS-Signature`, verified over the raw request body (not a re-serialized DTO) with `MessageDigest.isEqual` for constant-time comparison. Same pattern as `payments-collections-service`'s `WebhookSignatureService`.
- **Field-level encryption**: account numbers and BVN-equivalent identifiers are AES-GCM encrypted at rest via `EncryptedStringConverter`, a JPA `AttributeConverter`. Ciphertext is stored as `<keyVersion>:<iv>:<ciphertext>`, so the key version travels with the field itself. `EncryptionService`'s Javadoc documents the rotation procedure: add a new key version alongside the old one, flip `app.encryption.active-key-version`, and old rows keep decrypting under their original version until a background job re-encrypts them. Only one version (`v1`) is live in this repo; the procedure is documented, not exercised against a second real version, which is stated here plainly rather than implied.
- **Audit trail**: every state-changing operation (transfer initiated, screened, debited, requeried, reversed; inward credit applied; reconciliation exceptions) appends one row to `audit_log`, separate from and in addition to application logs. `AuditLogRepository` exposes no update or delete method, and `V6__create_audit_log.sql` adds Postgres rules that make `UPDATE`/`DELETE` on that table a no-op at the database level too.

## Idempotency

Every money-moving endpoint is idempotent on a unique DB constraint, not a prior lookup:

- Outward transfer: unique `transfers.client_reference`.
- Inward credit: unique `inward_credits.nibss_reference`.
- Reversal: unique `reversals.nibss_reference`.

Each service attempts the insert (and, for inward credits and reversals, the matching ledger posting) inside one transaction and catches `DataIntegrityViolationException` on the unique constraint violation, falling back to reading back the already-processed row. `InwardCreditIdempotencyIntegrationTest` replays the same NIBSS reference sequentially and under 10-way concurrency and asserts exactly one credit applies either way.

## Real bugs found and fixed

- **YAML leading-zero coercion silently broke the AML watchlist.** `app.aml.watchlist-account-numbers: 0000000001` in `application.yml`/`application-test.yml` without quotes. SnakeYAML (YAML 1.1) treats an unquoted leading-zero numeral as octal, so `0000000001` parsed as the integer `1`, which Spring then converted to the single-element list `["1"]`  not `["0000000001"]`. The watchlist test account never matched, so a transfer to it sailed through screening and hit `AccountNotFoundException` instead of going to `PENDING_REVIEW`, which an integration test caught immediately (the response's error JSON, deserialized back into a `Transfer`, failed because Jackson tried to map the error body's `"status": 404` field onto the `TransferStatus` enum by ordinal). Fixed by quoting the value (`"0000000001"`).
- **The settlement suspense account could not accept an inward credit.** The suspense account started at a balance of `0` with the same non-negative balance check as every real customer account. The first inward credit (which debits the suspense account) threw `InsufficientFundsException`, caught by `InwardCreditIdempotencyIntegrationTest`. A suspense/clearing account is not a real asset account it represents money already moved on NIBSS's side and must be allowed to go negative. Fixed by adding `Account.allowNegative`, true only for the suspense account, and skipping the balance check when it is set.
- **The suspense account could not be seeded by a Flyway migration.** `account_number` goes through the same AES-GCM `EncryptedStringConverter` as every other account, and the active encryption key only exists inside the running Spring context, not inside a SQL script. A literal ciphertext baked into a migration would only decrypt correctly under the exact key the migration was written against breaking in any environment using a different key (as the test profile does). Fixed by seeding the suspense account from `SuspenseAccountSeeder`, an `ApplicationRunner` that calls the real `EncryptionService` and upserts via `JdbcTemplate` (`ON CONFLICT DO NOTHING`) on every startup.
- **A reassigned local variable broke lambda capture.** `OutwardTransferService.initiate()` originally reassigned `transfer` inside a `try` block whose `catch` clause's lambda (`.orElseThrow(() -> alreadyExists)`) closed over that same variable `javac` correctly refused to compile it, since a variable referenced from a lambda must be effectively final. Fixed by splitting the newly constructed transfer (`newTransfer`) from the variable the rest of the method uses (`transfer`).
- **Mockito's default `TestRestTemplate` is not on the classpath in Spring Boot 4.** `spring-boot-starter-test` no longer transitively pulls in everything `TestRestTemplate` needs; integration tests originally written against it failed to compile (`package org.springframework.boot.test.web.client does not exist`). Rather than add back a specific HTTP client dependency, every integration test was written against `MockMvc` instead (`spring-boot-starter-webmvc-test`, whose `AutoConfigureMockMvc` also moved package, to `org.springframework.boot.webmvc.test.autoconfigure`, in Boot 4's module split) matching the pattern already used in `payments-collections-service`.

## Running it

```bash
docker compose up -d --wait   # Postgres :5438
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run
```

Swagger UI: http://localhost:8082/swagger-ui.html
Health: http://localhost:8082/actuator/health

Create an account:
```bash
curl -X POST localhost:8082/api/accounts -H 'Content-Type: application/json' -d '{
  "accountNumber": "1234567890", "bvn": "12345678901",
  "accountName": "TEST CUSTOMER", "bankCode": "000099", "openingBalance": 5000.00
}'
```

Initiate an outward transfer:
```bash
curl -X POST localhost:8082/api/nip/transfers -H 'Content-Type: application/json' -d '{
  "clientReference": "REF-001", "sourceAccountId": "<account id>",
  "destinationBankCode": "000001", "destinationAccountNumber": "0123456789",
  "amount": 500.00, "narration": "test"
}'
```

Example signed webhook:
```bash
BODY='{"nibssReference":"NIBSS-REF-1","destinationAccountId":"<account id>","sourceBankCode":"000002","sourceAccountNumber":"9988776655","amount":750.00}'
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$WEBHOOK_HMAC_SECRET" | sed 's/^.* //')
curl -X POST localhost:8082/api/nip/webhooks/inward-credits \
  -H "Content-Type: application/json" -H "X-NIBSS-Signature: $SIG" -d "$BODY"
```

## Tests

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean verify
```

35 tests total:

**Unit (JUnit 5 + Mockito, 24 tests):** `AccountTest` (credit/debit/insufficient-funds), `HmacSignatureServiceTest` (valid, tampered, wrong secret, blank), `EncryptionServiceTest` (round trip, non-determinism, null pass-through, key rotation, unknown key version), `FakeAmlScreeningProviderTest`, `FakeKycProviderTest`, `SettlementCsvParserTest`, `ReconciliationServiceTest` (all four categories, mocked repository), `OutwardTransferServiceTest` (requery-not-retry proof, definite-failure reversal, AML pending-review branch all against mocked `NibssNipClient`/`TransferRepository`/`PlatformTransactionManager`, no Spring context).

**Integration (Testcontainers, real Postgres, Docker required, 11 tests):** `LedgerConcurrencyIntegrationTest` (20 concurrent reciprocal transfers, ledger invariant), `OutwardTransferIntegrationTest` (ordinary success, timeout-then-success via requery, timeout-then-failure with reversal, AML pending-review, idempotent replay), `InwardCreditIdempotencyIntegrationTest` (sequential and 10-way concurrent replay), `ReversalIntegrationTest` (correcting entry + status transition), `ReconciliationIntegrationTest` (all four categories end to end through the real upload endpoint).

CI (`.github/workflows/ci.yml`) runs `mvn clean verify` on GitHub-hosted Ubuntu runners, which ship Docker preinstalled, so the Testcontainers tests run in CI exactly as they do locally no separate Postgres service container needed.

## Layout

```
domain/            Account, LedgerEntry, Transfer, InwardCredit, Reversal, AuditLogEntry
repository/        Spring Data repositories (unique constraints enforce idempotency)
nip/               NibssNipClient interface, FakeNibssNipClient, NIP response types
aml/               AmlScreeningProvider interface, FakeAmlScreeningProvider
kyc/               KycProvider interface, FakeKycProvider
ledger/            LedgerService (ordered-lock double-entry posting), SettlementAccounts, SuspenseAccountSeeder
reconciliation/    SettlementCsvParser, ReconciliationService, classification types
security/          EncryptionService (AES-GCM, versioned keys), EncryptedStringConverter, HmacSignatureService
audit/             AuditLogService
service/           OutwardTransferService, InwardCreditService, ReversalService, NameEnquiryService, TransactionStatusService, AccountService
web/               Controllers + DTOs
config/            Flyway (explicit migrate, Boot 4 dropped the auto-config), Jackson, encryption/webhook/AML properties
```

## Deployment

- GitHub: https://github.com/sonofnos/nibss-gateway
- CI: build + full test suite (Testcontainers included, Docker preinstalled on GitHub-hosted runners) plus a Docker image build step.
