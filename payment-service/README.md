# Payment service

This application owns payment outcomes in its own PostgreSQL database and listens
on HTTPS port 8083. It imports the platform's shared servlet security configuration.
TLS requires a certificate signed by the local private CA. Internal operations
additionally require the `order-service` certificate identity. Public status reads
require a valid Keycloak access token and enforce customer ownership or the admin
role. An unknown order and another customer's order both return 404.

## API boundary

| Operation | Authorized caller | Result |
|---|---|---|
| `POST /internal/payments` | Order service certificate | Create or replay a payment outcome |
| `GET /internal/payments/{orderId}` | Order service certificate | Reconcile a previously submitted operation |
| `GET /api/payments/status/{orderId}` | Owner JWT or admin JWT, through trusted transport | Read payment status |

There is no public charge or refund endpoint. The gateway forwards access tokens;
the service validates them itself. Authentication at the gateway does not replace
resource-level authorization in this service.

The internal create request contains `orderId`, `customerId`, `amount`, and
`paymentMethodReference`. The order service supplies the authenticated customer's
subject and the server-calculated total. Money uses a single lab currency and at
most two decimal places; a multicurrency product needs an explicit currency field,
currency-specific rounding, and currency included in the idempotency fingerprint.

`demo-approved` succeeds and `demo-declined` declines. Other references decline as
unsupported simulator input. Responses contain `orderId`, `customerId`, `amount`,
`status`, `paymentId`, and nullable `reason`. The simulator resolves immediately;
`PENDING` is reserved for a future provider adapter, not a simulated success.

## Durable idempotency and transaction boundaries

1. Validate the request and normalize the amount to two decimal places.
2. Start a local database transaction and take a PostgreSQL transaction-scoped
   advisory lock derived from the order UUID. This serializes the same operation
   across instances, including when no payment row exists yet.
3. If a durable outcome exists, compare the request fingerprint. Identical input
   returns the saved response, while changed amount, customer, or method returns
   409. Numerically identical `1.0` and `1.00` produce the same fingerprint.
4. Otherwise call the provider adapter. The supplied adapter inserts a simulated
   debit into a ledger for approved requests, then the service stores its outcome.
5. Commit the outcome and ledger together. Unique order IDs in both tables provide
   a second durable guard against duplicate effects. The transaction lock is
   released on commit or rollback.

If the HTTP response is lost after commit, replaying the same operation returns
the original `paymentId` without another simulated debit. A database outage fails
the call; no success fallback fabricates a payment. The order Saga retains the
pending step and retries with the same stable order ID.

**This is an explicit local provider simulator. It never moves real money.** The
ledger and payment outcome share one database, so their commit is atomic. A real
bank/provider HTTP operation cannot participate in that transaction. Replacing
the adapter requires provider-side idempotency keys, a durable pending-operation
state, independently committed network attempts, and reconciliation of unknown
outcomes/webhooks. Do not put a remote charge inside this transaction and assume
rollback can reverse it. Refunds, capture/authorization separation, reconciliation
workers, and PCI card handling are outside this lab.

## Evidence and limits

`PaymentIntegrationTest` uses the dedicated PostgreSQL test database. It checks
concurrent duplicates, competing different requests, durable declines, ownership,
numeric normalization, and a deliberate failure between simulated debit and
outcome persistence. `PaymentStatusControllerTest` checks that ownership uses JWT
subject and admin permission uses an authority, not a username.

Shared security tests and the root HTTPS smoke harness verify JWT signatures,
roles, mTLS boundaries, and end-to-end requests; controller unit tests alone do
not establish those properties. Actual executed results belong in the root lab
verification record.

This service takes a consistency-first position for payment mutations: it does
not acknowledge an effect when the authoritative database is unreachable. That
is a design choice under communication failure, not proof of a replicated CP
database or a way to enable all three CAP properties. No multi-node database
failover or replica consistency guarantee is implemented here.
