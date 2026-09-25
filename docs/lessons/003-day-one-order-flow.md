# Lesson 003 — From HTTP request to durable order

**Checkpoint:** Day 1 implementation prepared for guided practice. Your missing-service prediction was correct: Spring resolves constructor dependencies. Live drills and the explanations below remain pending; prepared code does not complete Day 1 or the PDF questions.

Use the [180-minute plan](../THREE-DAY-SPRINT.md#day-1-request-to-durable-order), including its break. Record actual study time and observations in [PROGRESS.md](../PROGRESS.md).

Before starting any application exercise, run `./scripts/db start` from the
repository root. The application now needs PostgreSQL. Keep only your intended
development application instance running on port 8080.

## 1. Configuration: change behavior without editing Java

`LearningService` now receives typed `learning.message` configuration. Start normally, inspect `/api/learning/status`, then stop and restart with:

```sh
./dev spring-boot:run -Dspring-boot.run.arguments=--learning.message=Day1
```

Predict which message appears. A command-line property overrides the application file in this setup. `@ConfigurationProperties` binds configuration into a Java value; constructor injection supplies that value to the service. A property-file edit does not automatically refresh an already-running ordinary application. [Boot configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)

## 2. REST: input, business operation, output

Start the database and application:

```sh
./scripts/db start
./dev spring-boot:run
```

Open [requests/day1.http](../../requests/day1.http) in IntelliJ. Run requests sequentially: successful creation stores the product ID for later requests. Follow this path through controller, service, entity, and repository:

```text
JSON → request record → controller → service → repository → PostgreSQL
JSON ← response record ← controller ← service
```

A DTO defines the HTTP contract. A JPA entity represents stored state. Keeping them separate lets the service control allowed changes and prevents persistence details from defining the API.

You can also create the first product from a terminal and use the returned ID:

```sh
curl -i http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical keyboard","price":1250.00,"stock":10}'
```

Before running each request, predict its result:

| Request | Expected result |
| --- | --- |
| Valid product creation | `201`, response body and `Location` |
| Read the saved product | `200` |
| Blank name, negative price, or negative stock | `400` |
| Read a nonexistent positive ID | `404` |
| Order more units than available | `409`; unchanged stock |

`@Valid` asks MVC to validate the converted request against constraints before normal controller execution. It cannot prove a product exists or stock is sufficient; the service checks those rules. The advice maps selected failures into `ProblemDetail` responses, keeping status and JSON error structure deliberate. [MVC validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html), [error responses](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

## 3. Persistence: observe what survives

Flyway applies versioned SQL migrations. Hibernate checks the resulting schema with `ddl-auto=validate`; it does not create or silently update our tables. Inspect the migration before the entities. [Database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html)

Stop and restart only the application, then retrieve the same product. Explain why its ID and data remain. For SQL logging, start with `--spring.profiles.active=sql` as the run argument. Update its price and inspect the SQL. The service loads a **managed entity** and changes its price without another `save` call. Hibernate tracks that change in the persistence context and synchronizes it with SQL during flush. This requires the managed entity and active write transaction used here. [JPA transaction boundaries](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html)

We use `BigDecimal` and `numeric(12,2)` for unit prices, with matching input limits. An order snapshots the server-side price: a subsequent product-price change must not rewrite an existing order. A Java record is a compact data carrier; it does not make mutable objects referenced by its components deeply immutable.

## 4. Transactions: prove the failure path

Order placement checks available stock, reduces it, and persists an order within one service transaction. A call from the controller crosses Spring's transactional proxy. The transaction manager coordinates the database transaction around that call. Normal return leads to a commit attempt; an unchecked failure escaping the boundary triggers rollback under our default rules. [Transaction semantics](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

Inspect the successful order and its product: remaining stock must equal original stock minus quantity. Then run:

```sh
./dev verify
```

In `OrderFlowIntegrationTest`, trace `rollsBackExecutedStockSqlWhenTheOrderInsertFails`. It installs an `AFTER INSERT` trigger **only in the separate test database**. The stock update is flushed first; during order insertion, the trigger verifies the changed stock and raises an exception. After the service transaction ends, JDBC reads verify original stock and unchanged order count. The test removes its trigger afterward.

Predict the database result before reading assertions. **Flush sends pending SQL; commit completes the transaction.** Executed stock SQL can still roll back. This drill does not claim that an order insert successfully completed before a later application exception. No failure-switch endpoint exists in the application.

## Explain before marking practice complete

<details>
<summary>Why put @Transactional on the service?</summary>

The business operation spans stock and order writes. One surrounding boundary makes them succeed or roll back together; isolated repository transactions would not define that whole operation.

</details>

<details>
<summary>Why can a price change persist without save()?</summary>

The entity was loaded inside the write transaction and remains managed. Dirty checking notices its changed state and flushes an update. This does not apply to arbitrary detached objects.

</details>

<details>
<summary>Does @Transactional prevent two buyers taking the last item?</summary>

No. Both transactions may read the same available quantity before either commits. Day 2 adds coordinated concurrent tests and locking. This Day 1 implementation is not proven safe for competing orders.

</details>

<details>
<summary>Does every exception roll back?</summary>

Not under our default rules: unchecked exceptions and errors do; checked exceptions normally do not. Proxy boundaries, caught exceptions, and rule overrides also matter. Those variations are Day 2 drills.

</details>

Pending learner evidence: configuration override; valid and invalid requests; restart persistence; dirty-checking SQL; order success and rollback; your explanation of each boundary. These are first exercises in B04/B05/A01/D00/D01/J04 and parts of EXT01–05, not whole-family completion.
