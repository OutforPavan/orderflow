# ADR 0005: Four applications, local trust, and durable checkout orchestration

Date: 2026-10-07. Status: accepted for the explicitly requested interview lab.

## Context

The learner requested a runnable shopping system with a gateway, OrderService,
InventoryService, PaymentService, authentication, HTTPS, circuit breakers, Saga,
cache and CAP discussion. They selected Keycloak and locally generated certificates.
The former monolith and tests remain in `legacy-monolith/`; existing local databases
are not migrated or deleted. Separate lab databases start empty.

## Decision

- Root is a Maven aggregator. Four sibling modules produce executable Spring Boot
  JARs; `security-support` is a plain library. Each executable owns its source tree,
  configuration, process and lifecycle. `src/` belongs to one module and is not the
  right container for independently deployed Spring applications.
- Gateway handles routing, token checks, request IDs and bounded proxy waits.
  Every service independently validates issuer, signature, expiration, audience,
  roles and ownership. Keycloak signs user tokens. Only local CLI tests use direct
  password grants; a production user-facing client needs Authorization Code + PKCE.
- TLS protects every HTTP hop. A generated CA signs separate certificates; business
  listeners require client certificates. The internal authorization chain permits
  only the `order-service` certificate identity. Public endpoints require JWT even
  with a valid client certificate. No insecure trust manager or hostname bypass.
- Separate PostgreSQL databases and login roles own orders, reservations and payments.
  The single local cluster is a convenience and shared failure domain, not HA.
- Order acceptance atomically persists the request fingerprint and Saga state.
  A polling worker claims rows using `FOR UPDATE SKIP LOCKED` and an expiring lease.
  Updates check the lease owner to fence stale workers. No transaction spans HTTP.
- Every participant operation is durable and idempotent by order UUID. Definite
  payment decline schedules stock release. Timeout/unknown payment outcome retains
  PAYMENT_PENDING and retries the same operation. Compensation also retries durably.
  Explicit invalid participant requests go to REVIEW_REQUIRED for investigation.
- Resilience4j circuit breakers wrap each outgoing participant adapter. Timeouts
  bound waiting; OPEN short-circuits calls; persistent Saga backoff schedules retry.
  No fallback fabricates successful reservations or payments.
- Caffeine caches advisory stock availability for five seconds, bounded to 1,000
  entries. Reservation checks always lock and update the database. Local invalidation
  runs after commit; another replica can still show stale stock until its TTL expires.
- Payment provider is simulated. Its local ledger and payment result share a DB
  transaction. A real external provider requires its own idempotency API plus
  durable reconciliation/webhook handling; the local transaction cannot cover it.

## CAP interpretation and consequences

There is no CAP switch. The design prioritizes authoritative service invariants
(no overselling; one simulated charge per order) over completing writes during
loss of the required database/participant. Cached display reads trade freshness
for temporary availability. Checkout spans separate local commits and converges
through a Saga; it is not globally atomic or globally linearizable.

The lab has no replicated database quorum or failover protocol. It demonstrates
failure policies and invariants, not a formal proof that the whole product is CP
or AP. Partition tolerance describes behavior under communication loss; it is not
an optional feature that can simply be disabled in a distributed deployment.

Alternatives: keeping the monolith is operationally simpler but cannot demonstrate
these service boundaries. Kafka/outbox choreography is valuable for other workloads,
but this lab uses persisted polling orchestration so no broker/outbox dual-write is
introduced. Redis is unnecessary for a small advisory local cache. Platform-managed
PKI and identity are appropriate for deployment; a repository-local CA is suitable
for this explicit local learning environment only.

Operational limitations: fixed local URLs, one coordinator polling thread per
instance, no real payment processing, no automatic operator reconciliation endpoint,
no certificate rotation or HA identity/database deployment. Monitor aged pending
orders and REVIEW_REQUIRED, preserve idempotency records for the retry window, and
introduce controlled reconciliation before a production rollout.

Keycloak lifecycle detail: use normal `start` with explicitly selected `dev-file`
storage, `local` cache and `--http-enabled=false`. The development command forces
HTTP on and cannot implement an HTTPS-only listener. Explicit local file storage
remains a lab convenience, not a production HA identity/database setup.
