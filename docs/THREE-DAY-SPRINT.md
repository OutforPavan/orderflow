# Three-day guided sprint

Requested on 2026-09-25: three days, three hours per day. Each session below is
exactly 180 minutes, including a ten-minute break. Day labels are session numbers;
specific start times have not been agreed. No extra learner homework is assumed.

Working preference: build one narrow end-to-end flow across the main topics.
On 2026-09-25 the learner asked to start implementation and finish Day 1 today,
so Day 1 is active. Completion still requires the learner practice and review below.

**Update, 2026-09-26:** the learner requested an explanation of every existing
class and configuration before proceeding. Pause new feature implementation and
work through [CODE-WALKTHROUGH.md](CODE-WALKTHROUGH.md) in small groups. Review
understanding and answer questions before resuming the feature blocks. This
request takes priority over the earlier pace; calendar time does not advance a
learning checkpoint.

## What finishing this sprint means

Target: a Java 21 Orderflow slice that accepts a validated request, stores an order
and reserves stock atomically, enforces access rules, caches product reads, and
publishes an event to a separate notification application. The learner runs key
failure drills and explains the observed behavior and design tradeoffs.

This is an accelerated project milestone. All 95 PDF questions and 14 broader
requirements remain in [PDF-COVERAGE.md](PDF-COVERAGE.md). Their complete practical
coverage and senior interview depth cannot credibly be promised in nine hours
from fundamentals. No item becomes Covered merely because its topic appears here.
The full [roadmap](ROADMAP.md) remains the completion contract.

## How we use the time

- Work in feature blocks: short explanation, prediction, implementation, failure
  and recovery, then one or two interview follow-ups.
- The trainer prepares boilerplate, requests, fixtures, and focused checks during
  active work; the learner traces the important code, runs it, and explains it.
- Keep the sequential [notebook](TECHNICAL-NOTEBOOK.md) and evidence records in
  parallel. Use them for reference instead of reading long notes during a session.
- Commit tested increments with their notes. Preserve unfinished learner edits.
- Shared experiments may support several PDF questions, but each question still
  needs its own relevant evidence and reviewed explanation.

## Day 1: request to durable order

| Elapsed time | Work | Evidence to collect |
| --- | --- | --- |
| 00:00-00:20 | Close the missing-service experiment; restore wiring; trace DI and one configuration override. | Startup failure and recovery; identify who resolves the constructor argument. |
| 00:20-01:00 | Build product REST operations using request/response records, validation, deliberate status codes, and consistent errors. | Valid creation/read plus invalid input and missing-product responses. |
| 01:00-01:40 | Add PostgreSQL, a migration, entity/repository/service separation, and persistence. | Data survives restart; inspect SQL and one managed-entity update. |
| 01:40-01:50 | Break. | — |
| 01:50-02:40 | Place an order and reserve inventory in one service transaction; inject a failure. | Query database state after success and rollback; prove no partial order/stock update. |
| 02:40-03:00 | Run the checks and explain request-to-database flow. | Learner explanation, reviewed follow-ups, commit and progress record. |

Keep the domain small: one product per order initially, no payment integration.
Java records, exceptions, collections, and immutable request values are introduced
where used. This does not complete the JVM/concurrency curriculum.

## Day 2: correctness and access

| Elapsed time | Work | Evidence to collect |
| --- | --- | --- |
| 00:00-00:35 | Compare a proxied transaction call with self-invocation; explore one rollback-rule change and flush versus commit. | Actual committed rows after each selected case; explain the boundary. |
| 00:35-01:20 | Add Spring Security and test authentication, role authorization, and order ownership. | Successful access, unauthenticated denial, forbidden access, and cross-customer denial. |
| 01:20-01:30 | Break. | — |
| 01:30-02:05 | Compete for the final stock item; implement one optimistic-locking strategy with a defined conflict response. | Coordinated concurrent test and no overselling in the tested scenario. |
| 02:05-02:35 | Cache product reads locally; observe hits, stale data, and invalidation after updates. | Repeatable stale-value reproduction and verification of the selected fix. |
| 02:35-03:00 | Combined drill and interview review. | Defend transaction placement, access checks, and cache limitations; commit evidence. |

Choose the authentication mechanism for our actual client and explain CSRF/CORS
decisions. Do not invent a token protocol to save time. A local cache exercise
does not demonstrate distributed-cache consistency or outage behavior.

## Day 3: messaging and one service boundary

| Elapsed time | Work | Evidence to collect |
| --- | --- | --- |
| 00:00-00:35 | Introduce a real Kafka topic, event key, producer, consumer group, and offset through an order event. | Observe published/consumed records and explain the chosen ordering boundary. |
| 00:35-01:20 | Run a separate notification application with its own persisted notification projection and processed-event IDs. | A business event crosses a process boundary; deduplication and the projection write share a local transaction. |
| 01:20-01:30 | Break. | — |
| 01:30-02:10 | Replay an event; stop/restart the consumer and inspect pending work and recovery. | One persisted notification effect for a repeated event; observed replay/recovery and offsets. |
| 02:10-02:30 | Trace the database/Kafka dual-write gap and design a transactional outbox. | Identify an unsafe failure window and the proposed recovery path. Full outbox implementation remains follow-up work. |
| 02:30-03:00 | End-to-end demonstration and a short mock interview. | Explain architecture, one failure, tradeoffs, and remaining gaps; commit the sprint evidence. |

The notification is a stored local projection, not an actual email or payment.
Inventory remains with orders, keeping their transaction local. The initial event
publication path must be labeled as a teaching stage with a dual-write limitation;
it must not be described as reliable publication until the outbox lab is verified.
Kafka transaction support alone is not proof of atomic database-plus-broker work.

## Setup and time limits

Java 21 and the baseline build were verified in Lesson 002. A read-only check on
2026-09-25 found Rancher Desktop and its Docker client outside the shell PATH.
That client could not connect to its selected daemon socket. PostgreSQL and Kafka
runtime readiness has therefore not been established; we have not changed the
runtime, started containers, or downloaded images in this planning step.

Before the corresponding feature block, verify the local runtime, ports, startup,
cleanup, and dependency downloads. Trainer preparation can happen alongside
teaching, but any setup requiring learner attention counts within the three hours.
If setup or understanding needs more time, explicitly reduce a later block and
record it as pending. Do not hide overtime or silently substitute an in-memory
test for a PostgreSQL/Kafka failure experiment.

At each session end record actual time, work verified, learner understanding,
open questions, and the next priority. Reserve the final review even if a feature
is unfinished; neither the deadline nor passing tests establishes understanding.

## Work retained after the sprint

These remain required by the full tracker unless separately demonstrated:

- Full bean selection/lifecycle and configuration matrices; broader REST contracts.
- JPA relationships, N+1, fetching/pagination, query plans; complete propagation
  and isolation cases; other locking strategies; concurrent HTTP idempotency.
- Session/token and CSRF/CORS depth; distributed-cache races, TTL, stampedes, outages.
- Kafka lag, rebalances, schema changes, poison messages, retries; complete durable
  outbox implementation and crash/recovery cases.
- Payment reconciliation, sagas, HTTP resilience, circuit breakers and bulkheads.
- JVM/GC/JFR, executors, ThreadLocal, virtual threads; load and capacity, deployment,
  environment differences, scheduling, and incident-response matrices.
- Remaining question-specific reviews and the full capstone.

## Technical references for the selected drills

- [Constructor resolution](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)
- [Transactional proxy and rollback behavior](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Spring Security servlet architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [Spring Kafka transaction boundaries](https://docs.spring.io/spring-kafka/reference/kafka/transactions.html)
