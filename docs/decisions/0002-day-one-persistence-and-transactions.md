# 0002 — PostgreSQL and one order transaction

Date: 2026-09-25
Status: accepted for the Day 1 teaching slice

## Context

The accelerated sprint needs observable persistence and rollback in one small
order flow. A product has a price and available stock; an order initially contains
one product and quantity. We need to distinguish a successful Java method call,
executed SQL, and committed database state.

## Decision

- Use real PostgreSQL for the application and a separate test database. Mocks or
  an in-memory database cannot establish this PostgreSQL rollback behavior.
- Let Flyway own versioned schema changes; use Hibernate schema validation.
  Keep SQL constraints alongside HTTP validation and business checks.
- Keep request/response records separate from JPA entities. Store decimal unit prices
  with `BigDecimal` and `numeric(12,2)`; snapshot the ordered unit price.
- Keep product and order persistence in one application and database. The service
  method is the transaction boundary for reserving stock and saving the order.
- Demonstrate dirty checking when changing a managed product. Explicitly flush
  stock before the order insert in the order exercise so rollback evidence includes
  a database update that has already executed. This ordering adds database work;
  it is a teaching choice, not a rule to flush after every change.
- Inject the rollback failure through a trigger installed only by an integration
  test. Verify committed state afterward through JDBC. Add no production failure API.

## Consequences

Database setup is necessary, and the tests require PostgreSQL. Migrations are
reviewable schema history; entity/schema disagreement fails startup instead of
silently modifying persistent data. The test database and development data remain
separate. Test failures are observed after the service boundary, without an outer
test transaction hiding the commit or rollback result.

Atomicity does not solve concurrent stock checks. Day 1 deliberately has no locking
strategy; Day 2 must reproduce and fix the race before claiming concurrent correctness.
Security, caching, Kafka, and service extraction remain later steps. Separating
inventory now would turn this local transaction into a distributed consistency
problem before we have understood the local boundary.

See [the lesson](../lessons/003-day-one-order-flow.md),
[Spring transaction semantics](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html),
and [Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html).
