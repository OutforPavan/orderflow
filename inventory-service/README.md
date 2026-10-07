# Inventory service

InventoryService owns products, authoritative stock, and the reservation ledger.
It listens on HTTPS port 8082 and uses its own PostgreSQL database. Run it through
the root platform scripts, which generate local certificates and database settings.
The shared security library is imported by `InventoryApplication`; the service
requires a trusted client certificate even for its public HTTP APIs.

## API and authorization

| Endpoint | Caller | Result |
|---|---|---|
| `POST /api/products` | Keycloak admin through the gateway | Create product; 201 plus Location |
| `GET /api/products/{id}` | Keycloak customer/admin | Authoritative product and stock |
| `PATCH /api/products/{id}/price` | Keycloak admin | Change price under the product lock |
| `GET /api/inventory/availability/{id}` | Keycloak customer/admin | Advisory cached stock observation |
| `POST /internal/reservations` | OrderService client certificate | Reserve once or return a durable rejection |
| `GET /internal/reservations/{orderId}` | OrderService client certificate | Read the stored outcome |
| `POST /internal/reservations/{orderId}/release` | OrderService client certificate | Compensate once, or create cancellation tombstone |

Reservation input is `{orderId, productId, quantity}`. The result contains the
same identifiers, `unitPrice`, `status`, and optional `reason`. Status is
`RESERVED`, `REJECTED`, or `RELEASED`. Out-of-stock and missing-product results
are stored business outcomes, returned as HTTP 200 for OrderService to interpret;
changing an existing operation's product or quantity returns HTTP 409.

The internal identity is established from the validated client certificate. A
user JWT, including an admin JWT, cannot impersonate the OrderService certificate.
The gateway certificate also has no permission for `/internal/**`.

## Trace the reservation code

`InternalReservationController` validates JSON, then invokes the transactional
`ReservationService`. Its transaction acquires a PostgreSQL advisory lock for the
order UUID. A normal row lock alone cannot serialize the first request because
the reservation row does not exist yet. The UUID remains the primary key; the
advisory hash controls locking only, so a hash collision causes extra contention,
not an incorrect identity match.

The service returns any existing reservation, checking that its input matches.
For a new operation it locks the product with `SELECT ... FOR UPDATE`, reads the
current price and stock, and either stores a rejection or decrements stock and
stores the reservation. Both writes commit together. A database failure rolls
them both back. The transaction has a three-second timeout; the caller can still
time out before it sees the outcome and must reuse the same order UUID.

Price changes acquire the same product row lock. A reservation therefore stores
one committed unit-price snapshot; retrying later does not silently reprice the
order. No external HTTP operation runs while this database transaction is open.

`release` uses the same order lock. It restores stock only when transitioning
`RESERVED` to `RELEASED`, in one transaction. Repeated compensation has no extra
stock effect. If release arrives before reserve, it creates a `RELEASED`
tombstone. Its product, quantity, and price start as null. A later reserve binds
the first two fields for future collision checks but cannot consume stock.
Releasing a rejected operation changes its state without adding stock.

Do not expire these operation keys or tombstones casually: a delayed replay
after deletion can perform the business effect again. A retention policy needs
a replay horizon and coordinated archival policy. This lab keeps them durable.

## Why cache availability, not reservations?

`AvailabilityService` uses Caffeine with at most 1,000 entries, expiring five
seconds after insertion. Each response identifies `observedAt` and `cached`.
These are display hints, not promises that a checkout will succeed. Repeated
reads avoid a database round trip. Missing products and database failures are
not cached, and no fallback fabricates stock.

Successful local stock mutations evict after commit. Another process does not
share the in-memory cache, so it cannot evict this instance's entry; expiry limits
its cache lifetime. This is not strict cross-replica cache coherence or a proof
that an observation is globally at most five seconds old. Reservation writes
always bypass the cache and lock the authoritative database row.

A hit can serve its existing advisory observation while the database is
unreachable. An expired entry or miss propagates the database failure. Increasing
the TTL improves hit rate but makes the display less current. Redis would add
shared infrastructure and would still require an invalidation strategy; for this
short-lived, non-authoritative read it is not necessary.

## CAP boundary and limits

For inventory mutations the implementation favors correctness over accepting a
write without authoritative data: if it cannot reach its PostgreSQL writer, it
cannot reserve or restore stock. The order Saga waits and retries the same
operation instead of treating a fallback as a successful reservation.

This does not turn CAP into a Spring configuration switch. A single local
PostgreSQL writer does not demonstrate consensus, replicated-database failover,
or a distributed linearizability proof. The advisory cache intentionally permits
stale reads; the authoritative mutation path never trusts it. Saga compensation
restores a business invariant across services but is not a global ACID rollback.

## Verification

From the repository root, after the platform setup scripts:

```sh
./dev -pl inventory-service -am verify
```

The tests use the separate generated `inventory-service-test.properties` database;
never point that file at live data because the fixture truncates its tables.
`InventoryPersistenceTest` exercises real PostgreSQL concurrency, stable
idempotency, changed-input conflicts, immutable pricing, durable rejections,
compensation races, release-before-reserve, and rollback after an injected second
write failure. Cache tests verify hits, expiry with a controllable clock,
invalidation, size limits, and bypass of stale stock during reservations.
`AvailabilityFailureTest` injects a database failure to verify that an expired
entry does not turn into invented availability.

References: [PostgreSQL locking](https://www.postgresql.org/docs/current/explicit-locking.html)
and [Caffeine eviction](https://github.com/ben-manes/caffeine/wiki/Eviction).
