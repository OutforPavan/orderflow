# Practical lab catalog

These are **planned lab families**, each divided into small guided exercises.
They support the [95-question PDF tracker](PDF-COVERAGE.md) and the broader
[roadmap](ROADMAP.md). A lab ID is not a lesson number or a claim of completion.

We implement features in Orderflow as they become relevant. Experiments that
deliberately crash, deadlock, or exhaust a resource use separate, bounded local
processes with a timeout and cleanup. Failure modes do not become default behavior
of the main application. Payment scenarios use a simulated provider.

## The required exercise cycle

1. State the requirement and predict behavior, including the failure case.
2. Implement the smallest relevant code or configuration change together.
3. Run a repeatable request, test, load script, or injected-failure scenario.
4. Inspect evidence: assertions, SQL, traces, metrics, thread dumps, or profiles.
5. Apply and verify the fix; compare with the baseline under the same conditions.
6. Explain the mechanism, limits, alternatives, and tradeoffs in your own words.
7. Record the evidence and review, update the relevant question IDs, and commit.

Reading notes or watching a demonstration alone does not complete this cycle.
For an internals question, code/configuration experiments and debugger observations
can supply practical evidence without inventing an unnecessary business feature.

## Foundation labs

| ID | Family | Code or live experiment | Required evidence |
| --- | --- | --- | --- |
| <a id="b01"></a>B01 | Boot auto-configuration | Inspect dependency graph and effective POM; trace imports/conditions; override or exclude one eligible default; compare convention and explicit setup. | Matching condition report, selected bean assertions, and explanation of parent/starter/runtime boundaries. |
| <a id="b02"></a>B02 | Scanning and bean lifecycle | Move a component across scan boundaries; register it explicitly; trace construction, initialization, and shutdown; compare a bean with an MVC handler. | Discovery and lifecycle observations with predicted failures; explain definitions, instances, scopes, and concurrency. |
| <a id="b03"></a>B03 | Injection and configuration classes | Introduce a collaborator, ambiguous implementations, qualifier/primary choices, and configuration methods with and without method interception. | Startup failure/recovery and tests proving which collaborator or instance is selected. |
| <a id="b04"></a>B04 | Configuration and profiles | Bind typed properties; validate required values; vary file format, profile, environment, and CLI inputs; inspect safe property origins. | A reproducible precedence matrix and proof of the selected value, including restart behavior. |
| <a id="b05"></a>B05 | MVC request and error path | Trace dispatch and conversion; vary method/path/Accept; add validation and exception handling; compare controller styles. | HTTP contract checks, debugger observations, and documented 4xx/5xx behavior. |
| <a id="b06"></a>B06 | Server, startup hooks, and packaging | Inspect executable and ordinary JARs; run on alternate/random ports; add bounded runners; test startup failure and shutdown. | Archive/manifest inspection, runner/event order, actual listening port, and process outcomes. |
| <a id="a01"></a>A01 | API and domain boundaries | Grow an in-memory product catalog using DTOs, validation, deliberate status codes, pagination, and consistent error responses. | Unit and HTTP tests demonstrating public contracts and separation from internal domain representation. |
| <a id="j04"></a>J04 | Java 21 foundations | Use records, collections, equality, exceptions, and immutable values in the current feature; compare platform and virtual threads when blocking work exists. | Correctness tests and measured behavior; explain why virtual threads do not remove downstream capacity limits. |

## Persistence and security labs

| ID | Family | Code or live experiment | Required evidence |
| --- | --- | --- | --- |
| <a id="d00"></a>D00 | SQL, migrations, and JPA basics | Add PostgreSQL and repeatable migrations; implement entity persistence; observe identity, state transitions, persistence context, dirty checking, and relationships. | Data survives restart; schema builds from scratch; SQL and state observations match the code and constraints. |
| <a id="d01"></a>D01 | DataSource, transactions, and pool use | Inspect datasource creation; place an order and reserve stock atomically; vary proxy boundary, rollback rules, propagation, flush/commit, transaction duration, and raw-JDBC resource handling. | Database-backed assertions after real transaction boundaries; pool metrics and failure/recovery evidence. |
| <a id="d02"></a>D02 | Query correctness and cost | Reproduce N+1, poor fetching/pagination, missing indexes, large-result allocation, and slow queries on seeded data. | Query counts/plans, result-correctness assertions, and before/after latency with a stable workload. |
| <a id="d03"></a>D03 | Concurrent updates and locking | Coordinate competing orders for the last item; compare atomic updates, optimistic versions, and pessimistic locks; reproduce contention. | Deterministic concurrent tests preventing overselling; measured conflict/wait behavior and justified retries. |
| <a id="d04"></a>D04 | HTTP request idempotency | Persist idempotency keys and request fingerprints with uniqueness; retry concurrent requests and simulate a lost response after commit. | One intended business effect, stable retry response, and explicit behavior for a reused key with different input. |
| <a id="x01"></a>X01 | Spring Security | Add filter-chain configuration, authentication, authorization, ownership checks, password handling, and session or resource-server token verification as justified; explore CSRF/CORS for the selected client. | Positive and negative tests including 401/403 and cross-customer access; trace the security context and explain trust boundaries. |

## Runtime and operations labs

| ID | Family | Code or live experiment | Required evidence |
| --- | --- | --- | --- |
| <a id="j01"></a>J01 | Executors, queues, and locks | Implement bounded executors; observe failed futures, rejection, task lifetime, parallel-stream behavior, and an isolated reproducible deadlock. | Thread dumps, queue/active counts, visible error reporting, bounded completion, and cleanup. |
| <a id="j02"></a>J02 | JVM memory, GC, and profiling | Compare allocation and retention; inspect heap versus native/direct memory; run constrained heap-size and GC experiments; collect JFR. | Profiles, GC logs, retained-object evidence, and workload/resource metadata; distinguish diagnosis from speculation. |
| <a id="j03"></a>J03 | ThreadLocal and asynchronous context | Reuse a worker across synthetic users; reproduce missing cleanup; add context propagation and a bounded asynchronous operation. | Context isolation on success/failure; completion versus submission latency; queue pressure and failure reporting. |
| <a id="o01"></a>O01 | Logs, metrics, traces, and Actuator | Instrument business requests, pools, and downstream calls; trace across services; vary logging levels/sinks; expose deliberate health/readiness signals and secure management access. | Correlated evidence across components, trace/span propagation, metric interpretation, and detection of a broken logging path. |
| <a id="o02"></a>O02 | Load, latency, and bottlenecks | Maintain repeatable workload scripts; vary arrival rate, concurrency, data volume, queues, serialization, dependency latency, and shared capacity one variable at a time. | Throughput, error rate, p50/p95/p99 latency, saturation and resource data, plus a measured improvement with preserved correctness. |
| <a id="o03"></a>O03 | Deployment, Docker, and environment drift | Run an immutable artifact in a container; vary resources, profiles, DNS/URLs, permissions, instance count, routing, and revision; exercise termination and rollback. | Build/image identity, safe effective settings, traffic-to-version evidence, graceful behavior, and repeatable recovery. |
| <a id="o04"></a>O04 | Incident response | Introduce an undisclosed local fault with a time limit; establish impact, form hypotheses, gather evidence, mitigate, and write a short incident review. | Timestamped investigation, recovery signal, confirmed versus unconfirmed causes, and a regression check. Root cause is not guaranteed in 15 minutes. |
| <a id="s01"></a>S01 | Scheduling across instances | Run the same scheduled task on two instances; measure contention with APIs; introduce durable coordination, idempotent effects, and failure recovery. | Duplicate-execution reproduction, overlap/crash/lease-expiry checks, and explicit limits of the chosen guarantee. |

## Cache, messaging, and distributed-workflow labs

| ID | Family | Code or live experiment | Required evidence |
| --- | --- | --- | --- |
| <a id="c01"></a>C01 | Cache correctness | Add cache-aside product reads; reproduce stale entries, race conditions, TTL effects, a stampede, multi-instance differences, and cache unavailability. | Hit/miss and latency evidence, consistency tests after writes, bounded fallback, and a justified invalidation policy. |
| <a id="k01"></a>K01 | Kafka foundations and lag | Introduce events, keys, partitions, groups, offset positions, rebalances, producer acknowledgments, and schema changes; throttle consumers deliberately. | Observed partition assignment/order, offset and lag changes, scaling limits, and compatible event evolution. |
| <a id="k02"></a>K02 | Delivery, retries, and consumer idempotency | Crash around processing/offset commits; redeliver records; implement a durable deduplication boundary, retry limits, and poison-message handling. | Repeatable duplicate delivery with one intended business effect; restart recovery and an explicit explanation of guarantee boundaries. |
| <a id="k03"></a>K03 | Transactional outbox | Reproduce database/message dual-write loss; persist the event with the order; publish asynchronously; fail around publish/acknowledgment and resume. | Durable recovery, duplicate handling, eventual delivery evidence, and remaining operational limits. |
| <a id="r01"></a>R01 | Remote-call resilience | Use slow/failing local dependencies; configure deadlines, retry ownership/budgets, jitter, circuit-breaker recovery, bulkheads, and admission control. | Call counts, concurrent work, queue depth, breaker transitions, bounded latency, and isolation under failure. |
| <a id="r02"></a>R02 | Checkout workflow and payment reconciliation | Model explicit workflow states with a simulated payment provider; drop confirmations; retry callbacks; reconcile unknown outcomes; test compensation. | Durable state transitions, no accidental duplicate charge, restart recovery, and business rules for unresolved or irreversible steps. |
| <a id="x02"></a>X02 | Microservice boundaries and contracts | Extract one justified boundary from the working application; define data ownership, HTTP/event contracts, version compatibility, and independent deployment. | Contract and integration tests; demonstrated remote failures; architecture decision comparing benefits with operational cost. |
| <a id="x03"></a>X03 | Testing and delivery discipline | Grow unit, MVC, database, messaging, and service-contract checks; introduce isolated real-dependency integration tests and a build pipeline when relevant. | Repeatable builds, meaningful regression coverage, clean environment startup, and a deploy/rollback exercise. |

## Evidence record for a completed exercise

Add one focused record under `docs/labs/` when an exercise is actually performed.
These records do not exist yet. Use this template:

```markdown
# <lab ID> / <small exercise name>
Date:
Source question IDs and broader requirement IDs:
Application baseline commit:
Requirement and prediction:
Code/configuration changed (relative repository links):
Environment, versions, workload, and resource limits:
Reproduction command and expected failure:
Observed evidence before the fix:
Fix and rationale:
Verification command, assertions, and observed result:
Limitations and alternative designs:
Learner explanation and follow-up answers:
Review outcome and open questions:
Implementation commit or PR:
```

Commit reproducible scripts, tests, and concise results. Keep tool caches, secrets,
personal data, large heap dumps, and generated binaries out of the repository.
Share a small sanitized diagnostic excerpt or a reproducible generation command
when it is sufficient to substantiate the result.

## Final capstone

Once the relevant families are complete, run order placement under concurrent
load while injecting database, cache, downstream, and messaging failures one at
a time. Demonstrate correctness, recovery, observability, and a safe rollback.
Then answer unfamiliar follow-up scenarios and defend the design using collected
evidence. The question tracker and broader requirements must both be complete;
a working happy path alone does not finish the learning project.
