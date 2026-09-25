# Day 1 - HTTP request to durable order

Date: 2026-09-25.
State: implemented and trainer-verified; learner practice and reviewed explanations pending.
Baseline: `a5c72db` (including the learner's restored service annotation and message edit).
Implementation checkpoint: [day-1-order-flow](https://github.com/OutforPavan/orderflow/tree/day-1-order-flow).

## Scope

Initial exercises in B04, B05, A01, D00, D01, J04 and parts of EXT01-05.
This does not close the full configuration, rollback-rule, concurrency, or performance
scenarios in the PDF tracker. No PDF question is marked Covered by this record.

| Feature | Source / exercise |
| --- | --- |
| Validated external configuration | [LearningProperties](../../src/main/java/com/outforpavan/orderflow/learning/LearningProperties.java) and [configuration tests](../../src/test/java/com/outforpavan/orderflow/learning/LearningConfigurationTest.java) |
| Product create/read/price update | [ProductController](../../src/main/java/com/outforpavan/orderflow/product/ProductController.java), [ProductService](../../src/main/java/com/outforpavan/orderflow/product/ProductService.java) |
| HTTP input and errors | [CreateProductRequest](../../src/main/java/com/outforpavan/orderflow/product/CreateProductRequest.java), [ApiExceptionHandler](../../src/main/java/com/outforpavan/orderflow/api/ApiExceptionHandler.java) |
| Schema and persistence | [Migration V1](../../src/main/resources/db/migration/V1__products_and_orders.sql), [Product](../../src/main/java/com/outforpavan/orderflow/product/Product.java) |
| One transaction for stock and order | [OrderService](../../src/main/java/com/outforpavan/orderflow/order/OrderService.java) |
| Live requests and SQL | [IntelliJ HTTP requests](../../requests/day1.http), [read-only SQL](../../requests/day1.sql) |
| Rollback after executed stock SQL | [OrderFlowIntegrationTest](../../src/test/java/com/outforpavan/orderflow/OrderFlowIntegrationTest.java) |

## Environment and setup observations

Java 21.0.12.1, Spring Boot 4.1.1, PostgreSQL 17.11 (Postgres.app 2.9.6),
Maven Wrapper 3.9.16. The native database replaces the unavailable Docker path for
this local exercise; it does not change global Docker/Kubernetes configuration.

The PostgreSQL distribution was downloaded from the publisher's GitHub release.
Its SHA-256 matched the release-asset digest before mounting/execution:
`b38bb00b8c8702a568270aab85995c550f7f93d1503b818efdc5ff9a519b7168`.
The temporary installer volume was detached after copying. The distribution,
cluster, generated password, connection properties, and logs remain under ignored
`.tools/`. Connection files have mode 600. PostgreSQL listens on `127.0.0.1:54329`.

`scripts/db start` created `orderflow` and `orderflow_test`; no global database or
existing user service was reset. `scripts/db-setup` and `scripts/db` pass Bash syntax
checks. The setup helper also reports the existing installation successfully.
The fresh-download/mount steps were executed directly during setup; the helper's
fresh-install branch was reviewed, not rerun against another installation.

The local cluster's owner is a development superuser. This is a teaching setup,
not production database-role provisioning. No generated password is committed.

## Verification actually performed

```sh
./scripts/db start
./dev verify
python3 scripts/day1-smoke.py
```

Observed Maven result: **43 tests, 0 failures, 0 errors, 0 skipped**, packaged JAR.

- MVC checks cover 201/Location, reads and price changes, 400 for missing/null/
  invalid fields, precision bounds, malformed JSON, fractional integer fields,
  and invalid path types; expected application failures map to 404/409.
- Bean/configuration checks cover required-dependency failure and validated
  external message binding. The learner's message prefix is preserved.
- Real PostgreSQL integration checks prove successful order/stock commit,
  insufficient-stock rejection without writes, managed price update without another
  `save`, stable historical order price, and rollback after an executed stock UPDATE.

The rollback test does not run inside a test-managed transaction. It creates a
test-only trigger specific to its product. During the order INSERT, the trigger
checks that stock is 7 (initially 10, reserved 3), then raises
`Injected Day 1 failure after stock UPDATE`. The application exception escapes the
service boundary. Subsequent JDBC queries see stock 10 and zero orders for that
product. Cleanup drops the trigger/function and removes only the test's own rows.
The expected database error in test logs is a passing failure-path assertion.

The packaged HTTP harness observed successful validation/error contracts and a
product initially priced 1250 with stock 10. A price change to 1199 followed by
an order for 2 yielded total 2398 and stock 8. An order for 100 returned 409.
After stopping and starting the application, the same product and order remained,
with price 1199, stock 8, and total 2398. Environment configuration won over the
file default, and a CLI value then won over that environment value.

Both temporary app processes were stopped. The harness leaves labeled demo rows
in `orderflow_test` for inspection; its SQL logs are in ignored `target/`.
The project PostgreSQL server remains available. The normal `orderflow` database
is separate and is migrated when the learner starts the development application.

## Meaningful limits

- Stock reservation has no locking/version check yet. Atomicity alone does not
  establish concurrent correctness; Day 2 must reproduce and fix the race.
- No request-idempotency guarantee, security, distributed cache, Kafka, or
  distributed transaction is implemented in this increment.
- A complete propagation/isolation/rollback-rule matrix and JPA relationships/
  fetching behavior remain further exercises.
- Trainer evidence is not evidence that the learner performed or explained a drill.

## Learner checkpoint

- [ ] Override the message and explain the winning source.
- [ ] Execute valid/invalid HTTP requests and identify where validation runs.
- [ ] Restart the application and retrieve the saved product/order.
- [ ] Change price and inspect the dirty-checking UPDATE.
- [ ] Run and explain successful order creation and the forced rollback test.
- [ ] Explain controller/service/repository responsibilities, persistence context,
  transaction boundary, flush versus commit, and current concurrency limitations.

Review and actual study duration: pending. Use the
[guided lesson](../lessons/003-day-one-order-flow.md) and keep the learner's answers
in [INTERVIEW-NOTES.md](../INTERVIEW-NOTES.md).
