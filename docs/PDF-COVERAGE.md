# PDF question coverage and completion requirements

All **95 questions/scenarios** in the three supplied PDFs are required learning
outcomes. They supplement the broader curriculum; they do not replace it.
This tracker records source identity, original numbering, practical work, and
the evidence needed to close each item.

**Current state: 95 mapped; 0 covered.** Some introductory reference notes and
baseline checks exist, but no PDF item has yet passed the full learner checkpoint.
Every PDF item below is planned. Mapping a question is not completing it.
The [Lesson 002 injection lab](labs/B03-001-constructor-injection.md) implements a
first part of EXT01 and a prerequisite for P3-Q81/P4-S20. Their full ambiguity
exercises and learner reviews remain pending.

The [three-day sprint](THREE-DAY-SPRINT.md) changes teaching pace and prioritization,
not the evidence criteria or required question set. The learner's missing-service
prediction has been reviewed; it does not complete the bean-ambiguity PDF questions.

## Source inventory

| Source | Original IDs | Pages | Count |
| --- | --- | --- | --- |
| `Part_1_Microservices_Kafka_Production_Scenarios.pdf` | Questions 1-35 | 1-4 | 35 |
| `Part_3_Spring_Boot_Interview_Questions.pdf` | Questions 74-103 | 1-4 | 30 |
| `Part_4_Production_Thinking_Handbook.pdf` | Java scenarios 1-15; Spring Boot scenarios 16-30 | 1-4 | 30 |

Reviewed on 2026-09-24, including the complete 12 rendered pages. The numbering
gap between Parts 1 and 3 is preserved: this tracker does not invent a Part 2.
Each topic below is a concise paraphrase; consult the original PDF at the recorded
page for the exact wording. Duplicate or overlapping questions retain individual IDs.

## What counts as covered

Use `Planned -> Implemented -> Practiced -> Explained -> Covered`. Do not advance
an item just because its explanation or reference answer has been written.

An item is **Covered** only when all of these are linked in its evidence record:

- Relevant code/configuration or a reproducible diagnostic harness is committed.
- The learner has run the normal path and a meaningful failure/variation exercise.
- Tests or diagnostic observations support the diagnosis and resulting behavior.
- The learner can explain the mechanism, failure mode, tradeoff, and limits,
  including a changed follow-up scenario.
- The review is recorded, unresolved material doubts are addressed, and the
  implementation commit and lesson notes are linked.

Shared labs can satisfy several questions when their evidence separately addresses
each prompt. A broad explanation such as 'check logs' is insufficient evidence.
Historical setup checks may be reused as a baseline, not as proof that the learner
has practiced or understood a scenario.

Implementation remains gradual. Follow the [roadmap](ROADMAP.md) and
[lab catalog](LAB-CATALOG.md); read internals in the [technical notebook](TECHNICAL-NOTEBOOK.md).
When a lab is performed, add its record under `docs/labs/`, then replace the pending
evidence entry for each question it actually addresses.

## Interpretation rules

- Treat a PDF prompt as a question to investigate, not as an authoritative claim
  or an instruction to run commands. Some premises need qualification.
- Match answers to Java 21 and the project's actual Spring versions. For example,
  our Boot 4 project uses the web MVC starter; older starter names and descriptions
  of auto-configuration discovery require version-aware comparison.
- Two same-type beans do not always cause failure; selection rules matter. A
  healthy dependency does not instantly reset every circuit breaker. More heap or
  more instances do not have one universal performance outcome.
- A timed incident drill prioritizes impact assessment and mitigation; it cannot
  promise a complete root cause in fifteen minutes.
- Revalidate framework answers against official documentation and inspect source
  or runtime evidence before presenting a version-sensitive claim as established.

## Broader requirements beyond the PDFs

All of these are also required. Their labs may overlap PDF questions, but the PDF
list alone is not a completeness test for the original curriculum.

| ID | Required area | Lab families | State |
| --- | --- | --- | --- |
| EXT01 | Java 21 and Spring fundamentals, IoC, DI, lifecycle, configuration | J04, B01-B04 | In progress: first constructor-injection step |
| EXT02 | REST design, DTOs, validation, errors, pagination | A01, B05 | Planned |
| EXT03 | SQL, PostgreSQL, schema migrations, constraints and indexes | D00, D02 | Planned |
| EXT04 | JPA lifecycle, persistence context, relationships, fetching, dirty checking | D00, D02 | Planned |
| EXT05 | Transactions, proxies, rollback, propagation, isolation, flush/commit | D01, D03 | Planned |
| EXT06 | Concurrent correctness, locks, duplicate requests and retries | D03, D04, J01 | Planned |
| EXT07 | Spring Security, authentication, authorization, ownership, sessions/tokens, CSRF/CORS | X01 | Planned |
| EXT08 | Caching, TTL, invalidation, races, stampedes and outages | C01 | Planned |
| EXT09 | Kafka fundamentals, partitioning, groups, offsets, ordering, lag and schema evolution | K01, K02 | Planned |
| EXT10 | Reliable event publication, outbox, idempotent consumption and recovery | K02, K03 | Planned |
| EXT11 | Microservice boundaries, contracts, resilience and distributed workflows | X02, R01, R02 | Planned |
| EXT12 | Observability, deployment, capacity, scheduling and incident diagnosis | O01-O04, S01 | Planned |
| EXT13 | Unit/integration/contract testing, reproducible environments and delivery | X03 | Planned |
| EXT14 | JVM/GC, memory domains, executors, futures, locks, ThreadLocal and virtual threads | J01-J04 | Planned |

The project is complete when all 95 PDF items and all 14 broader requirements
meet their evidence criteria, and the final capstone in the lab catalog has been
reviewed. Add new requirements explicitly when we agree to expand the curriculum.

## Question index

The IDs below remain stable even if we change lesson order. Lab-family links go
to the shared catalog; each entry supplies the question-specific exercise.

## Part 1 - Microservices, Kafka and production

| ID | PDF page | Topic | Labs | State |
| --- | --- | --- | --- | --- |
| [P1-Q01](#p1-q01) | 1 | Diagnosing an abrupt API slowdown | O02, O01 | Planned |
| [P1-Q02](#p1-q02) | 1 | Concurrent duplicate order creation | D04, D03 | Planned |
| [P1-Q03](#p1-q03) | 1 | Idempotent handling of repeated Kafka deliveries | K02, D01 | Planned |
| [P1-Q04](#p1-q04) | 1 | Checkout behavior when a participating service fails | R02, R01 | Planned |
| [P1-Q05](#p1-q05) | 1 | API latency outside SQL execution | O02, D01 | Planned |
| [P1-Q06](#p1-q06) | 1 | Intermittent failures confined to production conditions | O03, O04 | Planned |
| [P1-Q07](#p1-q07) | 1 | Recovering a successful payment with missing confirmation | R02, K03, D04 | Planned |
| [P1-Q08](#p1-q08) | 1 | Timeouts despite low CPU utilization | J01, D01, R01 | Planned |
| [P1-Q09](#p1-q09) | 2 | Uneven traffic and service-specific capacity | O02, O03 | Planned |
| [P1-Q10](#p1-q10) | 2 | Increasing Kafka consumer lag | K01, K02, O02 | Planned |
| [P1-Q11](#p1-q11) | 2 | Connection exhaustion with a responsive database | D01, O02 | Planned |
| [P1-Q12](#p1-q12) | 2 | Horizontal scaling without throughput gains | O02, D01, D03 | Planned |
| [P1-Q13](#p1-q13) | 2 | Locating the source of stale reads | C01, D02, O01 | Planned |
| [P1-Q14](#p1-q14) | 2 | Tracing a request through six service hops | O01, J03 | Planned |
| [P1-Q15](#p1-q15) | 2 | Protecting the API from a very slow external dependency | R01, J01 | Planned |
| [P1-Q16](#p1-q16) | 2 | Preventing duplicate effects from retries | D04, R02, D01 | Planned |
| [P1-Q17](#p1-q17) | 2 | Containing cascading dependency failures | R01 | Planned |
| [P1-Q18](#p1-q18) | 2 | Investigating a sudden increase in memory use | J02, O02 | Planned |
| [P1-Q19](#p1-q19) | 3 | Diagnosing incidents without application exceptions | O01, O04 | Planned |
| [P1-Q20](#p1-q20) | 3 | Time-boxed incident triage and mitigation | O04, O01, O03 | Planned |
| [P1-Q21](#p1-q21) | 3 | Latency regression following deployment | O03, O02 | Planned |
| [P1-Q22](#p1-q22) | 3 | Classifying causes of connection-pool saturation | D01, D03, O02 | Planned |
| [P1-Q23](#p1-q23) | 3 | Reproducing performance differences between local and deployed environments | O02, O03 | Planned |
| [P1-Q24](#p1-q24) | 3 | Investigating intermittent HTTP 500 responses | B05, O01, O04 | Planned |
| [P1-Q25](#p1-q25) | 3 | Diagnosing retained memory and eventual process failure | J02, J03 | Planned |
| [P1-Q26](#p1-q26) | 3 | Isolating a slow dependency from the rest of the API | R01, J01 | Planned |
| [P1-Q27](#p1-q27) | 3 | Consistent updates under concurrent writers | D03, D01 | Planned |
| [P1-Q28](#p1-q28) | 3 | Cache freshness after writes and concurrent reads | C01, D01 | Planned |
| [P1-Q29](#p1-q29) | 4 | Duplicate scheduled work after adding replicas | S01, D04 | Planned |
| [P1-Q30](#p1-q30) | 4 | Client timeouts while health checks pass | O01, O03, R01 | Planned |
| [P1-Q31](#p1-q31) | 4 | Profiling non-database work in a slow response | O02, J02, J01 | Planned |
| [P1-Q32](#p1-q32) | 4 | Correlating logs across service boundaries | O01, J03 | Planned |
| [P1-Q33](#p1-q33) | 4 | Handling a sudden tenfold traffic increase | O02, O03, R01 | Planned |
| [P1-Q34](#p1-q34) | 4 | Comparing staging and production configuration and infrastructure | O03, B04 | Planned |
| [P1-Q35](#p1-q35) | 4 | Diagnosing crashes that occur only under load | J02, O02, O04 | Planned |

<a id="p1-q01"></a>

<details>
<summary>P1-Q01 - Diagnosing an abrupt API slowdown</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 1, original number 1.

**Lab families:** [O02](LAB-CATALOG.md#o02), [O01](LAB-CATALOG.md#o01). **State:** Planned.

**Live practice / implementation:** Run a repeatable order-creation load test and inject database, downstream HTTP, and CPU delays in separate runs; use request metrics, traces, and a profiler to locate the dominant delay before changing code.

**Required evidence:** For each run, preserve throughput, error rate, p50/p95/p99 latency, and a trace or profile supporting the diagnosis; rerun the same workload after the targeted fix and explain any remaining bottleneck.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q02"></a>

<details>
<summary>P1-Q02 - Concurrent duplicate order creation</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 1, original number 2.

**Lab families:** [D04](LAB-CATALOG.md#d04), [D03](LAB-CATALOG.md#d03). **State:** Planned.

**Live practice / implementation:** Send simultaneous create-order requests using one idempotency key; enforce a database uniqueness constraint with transactional request registration, payload fingerprinting, and defined replay/conflict responses.

**Required evidence:** A concurrent integration test proves one order and one business effect; identical retries return the documented result, conflicting payload reuse is rejected, and a later legitimate order with a different key succeeds.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q03"></a>

<details>
<summary>P1-Q03 - Idempotent handling of repeated Kafka deliveries</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 1, original number 3.

**Lab families:** [K02](LAB-CATALOG.md#k02), [D01](LAB-CATALOG.md#d01). **State:** Planned.

**Live practice / implementation:** Implement a unique processed-event record in the same database transaction as inventory reservation; terminate the consumer after the database commit but before offset commit, then restart and replay the event.

**Required evidence:** Observe redelivery with only one inventory reservation and one processed-event record; force a database rollback and prove the event remains retryable. Explain why a database transaction and Kafka offset commit still permit redelivery.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q04"></a>

<details>
<summary>P1-Q04 - Checkout behavior when a participating service fails</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 1, original number 4.

**Lab families:** [R02](LAB-CATALOG.md#r02), [R01](LAB-CATALOG.md#r01). **State:** Planned.

**Live practice / implementation:** Model checkout states and stop inventory or payment during the workflow; implement bounded calls, durable pending/failure states, and idempotent recovery or compensation appropriate to each completed step.

**Required evidence:** Failure drills show a bounded client response, no falsely completed order, visible pending work, and eventual recovery or compensation after the dependency returns; repeated recovery does not repeat a business effect.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q05"></a>

<details>
<summary>P1-Q05 - API latency outside SQL execution</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 1, original number 5.

**Lab families:** [O02](LAB-CATALOG.md#o02), [D01](LAB-CATALOG.md#d01). **State:** Planned.

**Live practice / implementation:** Keep individual SQL statements fast while independently adding connection acquisition wait, a slow downstream call, and costly DTO serialization; instrument the request stages rather than using SQL duration as total database time.

**Required evidence:** A measured request timeline separates queue/pool wait, SQL execution, downstream work, and response processing; identify the injected cause in each run and confirm its removal reduces end-to-end latency.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q06"></a>

<details>
<summary>P1-Q06 - Intermittent failures confined to production conditions</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 1, original number 6.

**Lab families:** [O03](LAB-CATALOG.md#o03), [O04](LAB-CATALOG.md#o04). **State:** Planned.

**Live practice / implementation:** Deploy the same application artifact into two local environments with controlled differences in CPU/memory limits, configuration, dependency latency, and data volume; introduce one hidden fault for a comparison drill.

**Required evidence:** Produce a redacted environment comparison and timestamped incident timeline, reproduce the failure under the relevant condition, and validate a safe mitigation with error and latency measurements.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q07"></a>

<details>
<summary>P1-Q07 - Recovering a successful payment with missing confirmation</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 1, original number 7.

**Lab families:** [R02](LAB-CATALOG.md#r02), [K03](LAB-CATALOG.md#k03), [D04](LAB-CATALOG.md#d04). **State:** Planned.

**Live practice / implementation:** Make the payment stub commit a payment and drop its response; persist the payment attempt and provider idempotency key, reconcile unknown outcomes against provider status, and publish local order-state changes through an outbox.

**Required evidence:** After the lost response, the order stays in an explicit pending state; repeated reconciliation and delayed/duplicate callbacks converge to one paid order and one charge, with durable audit evidence of the transition.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q08"></a>

<details>
<summary>P1-Q08 - Timeouts despite low CPU utilization</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 1, original number 8.

**Lab families:** [J01](LAB-CATALOG.md#j01), [D01](LAB-CATALOG.md#d01), [R01](LAB-CATALOG.md#r01). **State:** Planned.

**Live practice / implementation:** Create separate low-CPU timeout cases using exhausted worker threads, a saturated connection pool, and a slow remote call; capture thread dumps and queue/pool measurements while load is active.

**Required evidence:** Correlate blocked/waiting thread stacks and resource metrics with each injected cause; demonstrate that bounded waits and the targeted resource or code fix restore responsiveness without relying on CPU alone.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q09"></a>

<details>
<summary>P1-Q09 - Uneven traffic and service-specific capacity</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 9.

**Lab families:** [O02](LAB-CATALOG.md#o02), [O03](LAB-CATALOG.md#o03). **State:** Planned.

**Live practice / implementation:** Generate a hot endpoint or tenant and, separately, skew load-balancer distribution across replicas; measure per-service and per-instance demand, then adjust routing or independently scale the constrained service.

**Required evidence:** Charts distinguish expected differences between service workloads from accidental imbalance among replicas; a repeat load run demonstrates the chosen routing/capacity change improves the overloaded path within database and downstream capacity limits.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q10"></a>

<details>
<summary>P1-Q10 - Increasing Kafka consumer lag</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 10.

**Lab families:** [K01](LAB-CATALOG.md#k01), [K02](LAB-CATALOG.md#k02), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Induce lag with slow event processing, a hot partition, repeated failure of one event, and rebalances in separate runs; compare partition lag, processing duration, assignment, and incoming versus completed event rates.

**Required evidence:** Record the signature and specific remediation for each case; show lag draining after recovery and demonstrate that adding consumers beyond available partitions does not create additional active partition consumers in the group.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q11"></a>

<details>
<summary>P1-Q11 - Connection exhaustion with a responsive database</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 11.

**Lab families:** [D01](LAB-CATALOG.md#d01), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Hold connections during a slow HTTP call inside a transaction and, in an isolated fault fixture, fail to close a borrowed JDBC connection; compare these with a healthy short-transaction baseline.

**Required evidence:** Pool active/idle/pending metrics, connection acquisition time, transaction timing, and diagnostic stacks explain both failures while simple database probes remain fast; removing the leak and reducing transaction scope restore pool reuse.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q12"></a>

<details>
<summary>P1-Q12 - Horizontal scaling without throughput gains</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 12.

**Lab families:** [O02](LAB-CATALOG.md#o02), [D01](LAB-CATALOG.md#d01), [D03](LAB-CATALOG.md#d03). **State:** Planned.

**Live practice / implementation:** Load-test one, two, and four application instances against a fixed-capacity database and a contended inventory row; measure shared database capacity, lock wait, aggregate pool connections, and downstream limits.

**Required evidence:** A throughput/latency comparison locates the shared limiting resource and shows why more instances stop helping; validate a targeted query, contention, or admission-control improvement without merely increasing every pool.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q13"></a>

<details>
<summary>P1-Q13 - Locating the source of stale reads</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 13.

**Lab families:** [C01](LAB-CATALOG.md#c01), [D02](LAB-CATALOG.md#d02), [O01](LAB-CATALOG.md#o01). **State:** Planned.

**Live practice / implementation:** Compare direct database reads, a fresh persistence context, a previously loaded JPA entity, and a cached API response after an update; attach row versions and cache hit/miss observations to the experiment.

**Required evidence:** A read-path table identifies which layer serves the old value and why; targeted refresh/invalidation fixes that layer, and the learner distinguishes this result from separately investigated replica lag or client caching.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q14"></a>

<details>
<summary>P1-Q14 - Tracing a request through six service hops</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 14.

**Lab families:** [O01](LAB-CATALOG.md#o01), [J03](LAB-CATALOG.md#j03). **State:** Planned.

**Live practice / implementation:** Build a local six-hop request path using Orderflow services and small downstream fixtures; propagate tracing context through HTTP and one asynchronous boundary and export spans to a local trace viewer.

**Required evidence:** One trace displays the expected parent/child relationships, timings, and injected failing hop; correlated logs resolve to that trace, and tests confirm concurrent requests do not share trace or logging context accidentally.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q15"></a>

<details>
<summary>P1-Q15 - Protecting the API from a very slow external dependency</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 15.

**Lab families:** [R01](LAB-CATALOG.md#r01), [J01](LAB-CATALOG.md#j01). **State:** Planned.

**Live practice / implementation:** Make a downstream stub delay for 20 seconds; configure connection and response timeouts, an overall request budget, bounded retries only for safe operations, and a limited-concurrency bulkhead.

**Required evidence:** Under concurrent load, responses finish within the documented budget or fail explicitly, queued/in-flight work stays bounded, and an unrelated endpoint remains responsive; verify whether client cancellation actually releases local resources.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q16"></a>

<details>
<summary>P1-Q16 - Preventing duplicate effects from retries</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 16.

**Lab families:** [D04](LAB-CATALOG.md#d04), [R02](LAB-CATALOG.md#r02), [D01](LAB-CATALOG.md#d01). **State:** Planned.

**Live practice / implementation:** Drop an order or payment response after its business operation commits, then retry with the same stable idempotency key; separate retryable transient failures from validation errors and reconcile unknown payment outcomes.

**Required evidence:** A fault test observes multiple attempts but one order/payment effect; conflicting key reuse is rejected, attempt limits are respected, and the stored outcome can be returned after process restart.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q17"></a>

<details>
<summary>P1-Q17 - Containing cascading dependency failures</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 17.

**Lab families:** [R01](LAB-CATALOG.md#r01). **State:** Planned.

**Live practice / implementation:** Slow one dependency under load and add timeouts, a circuit breaker, per-dependency bulkheads, and a shared retry budget; test the breaker open, half-open, and recovery behavior.

**Required evidence:** Metrics demonstrate bounded calls into the failing dependency, protected capacity for healthy dependencies, and controlled recovery; the learner explains why a circuit breaker alone does not limit all concurrent in-flight work.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q18"></a>

<details>
<summary>P1-Q18 - Investigating a sudden increase in memory use</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 2, original number 18.

**Lab families:** [J02](LAB-CATALOG.md#j02), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Compare a high-allocation large-response workload with a deliberately retained-object fixture; inspect heap occupancy after collection, allocation profiles, GC activity, process RSS, and container memory accounting.

**Required evidence:** Measurements distinguish transient allocation pressure from sustained retention and identify the relevant objects or memory category; a same-workload rerun confirms the chosen fix reduces the observed pressure.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q19"></a>

<details>
<summary>P1-Q19 - Diagnosing incidents without application exceptions</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 19.

**Lab families:** [O01](LAB-CATALOG.md#o01), [O04](LAB-CATALOG.md#o04). **State:** Planned.

**Live practice / implementation:** Inject hangs, incorrect successful responses, and proxy-side request failures in separate drills; collect user-visible success/latency metrics, dependency traces, platform events, and thread/resource observations.

**Required evidence:** Each drill is detected through a concrete signal even when application error logs are empty; a timeline ties that signal to the injected failure and validates recovery from the client perspective.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q20"></a>

<details>
<summary>P1-Q20 - Time-boxed incident triage and mitigation</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 20.

**Lab families:** [O04](LAB-CATALOG.md#o04), [O01](LAB-CATALOG.md#o01), [O03](LAB-CATALOG.md#o03). **State:** Planned.

**Live practice / implementation:** Run a 15-minute local incident drill with a hidden injected fault; assess impact, check recent changes, preserve evidence, communicate the current hypothesis, and choose a safe mitigation before deeper diagnosis.

**Required evidence:** Submit a timestamped incident log with measured impact, tested hypotheses, mitigation and recovery checks, remaining uncertainty, and follow-up work. Success requires disciplined triage and evidence, not a guaranteed root-cause discovery within 15 minutes.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q21"></a>

<details>
<summary>P1-Q21 - Latency regression following deployment</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 21.

**Lab families:** [O03](LAB-CATALOG.md#o03), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Deploy a controlled query or configuration regression, annotate the release time, and compare artifact/configuration changes, request mix, query counts, resource limits, and warm-up behavior against the previous version.

**Required evidence:** Before/after measurements narrow the regression; a compatible rollback or corrective deployment restores the baseline under the same workload, with data/schema compatibility checked before rollback.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q22"></a>

<details>
<summary>P1-Q22 - Classifying causes of connection-pool saturation</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 22.

**Lab families:** [D01](LAB-CATALOG.md#d01), [D03](LAB-CATALOG.md#d03), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Exercise slow queries, lock-blocked transactions, connection leaks, and excessive concurrency one at a time; correlate pool acquisition/usage metrics with database sessions, query execution, and lock waits.

**Required evidence:** A diagnostic table links each fault to its measured signature and corrective action; connection demand is estimated against transaction duration and concurrency, and the selected fix succeeds without blindly enlarging the pool.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q23"></a>

<details>
<summary>P1-Q23 - Reproducing performance differences between local and deployed environments</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 23.

**Lab families:** [O02](LAB-CATALOG.md#o02), [O03](LAB-CATALOG.md#o03). **State:** Planned.

**Live practice / implementation:** Run the same artifact and representative dataset locally and in constrained containers; vary concurrency, network delay, CPU quota, memory, and connection settings while keeping a recorded baseline.

**Required evidence:** A controlled comparison identifies the factor responsible for the reproduced slowdown, includes request latency and resource evidence, and verifies the correction under deployment-like conditions.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q24"></a>

<details>
<summary>P1-Q24 - Investigating intermittent HTTP 500 responses</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 24.

**Lab families:** [B05](LAB-CATALOG.md#b05), [O01](LAB-CATALOG.md#o01), [O04](LAB-CATALOG.md#o04). **State:** Planned.

**Live practice / implementation:** Inject data-dependent failures and a fault on only one replica; group 500 responses by route, deployment, instance, trace, and exception type, then reproduce a failing request and implement a focused correction.

**Required evidence:** A trace connects a client-visible 500 to its failing code path or dependency; the regression test covers the triggering input, error responses expose no stack traces or secrets, and a repeated load run confirms the failure disappears.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q25"></a>

<details>
<summary>P1-Q25 - Diagnosing retained memory and eventual process failure</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 25.

**Lab families:** [J02](LAB-CATALOG.md#j02), [J03](LAB-CATALOG.md#j03). **State:** Planned.

**Live practice / implementation:** Introduce an unbounded collection and a separate ThreadLocal cleanup fault under repeated load; capture GC trends, heap histograms/dumps, and process termination evidence, then remove the retaining references and bound growth.

**Required evidence:** Heap analysis traces representative retained objects to GC roots; after the fix, post-GC occupancy stabilizes under a soak run, and the learner distinguishes a Java out-of-memory failure from a container memory-limit kill.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q26"></a>

<details>
<summary>P1-Q26 - Isolating a slow dependency from the rest of the API</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 26.

**Lab families:** [R01](LAB-CATALOG.md#r01), [J01](LAB-CATALOG.md#j01). **State:** Planned.

**Live practice / implementation:** Route calls to two dependencies through a shared executor, slow one to demonstrate cross-impact, then introduce separate bounded concurrency limits, bounded queues where applicable, timeouts, and explicit rejection behavior.

**Required evidence:** The protected dependency and unrelated route retain their documented latency/error targets while the faulty path sheds excess work; executor/queue metrics verify that isolation does not merely relocate unbounded waiting.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q27"></a>

<details>
<summary>P1-Q27 - Consistent updates under concurrent writers</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 27.

**Lab families:** [D03](LAB-CATALOG.md#d03), [D01](LAB-CATALOG.md#d01). **State:** Planned.

**Live practice / implementation:** Reproduce a lost update and inventory oversell using synchronized concurrent requests; compare JPA version-based optimistic locking, pessimistic locking, and a conditional atomic stock update with database constraints.

**Required evidence:** Concurrency tests preserve the stock invariant and expose the expected conflict/deadlock outcomes; explain bounded retry rules, transaction boundaries, and the workload trade-offs of each implemented strategy.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q28"></a>

<details>
<summary>P1-Q28 - Cache freshness after writes and concurrent reads</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 3, original number 28.

**Lab families:** [C01](LAB-CATALOG.md#c01), [D01](LAB-CATALOG.md#d01). **State:** Planned.

**Live practice / implementation:** Implement cache-aside reads and transaction-aware invalidation, then force a reader/writer race, a rolled-back update, and an invalidation failure across two instances; add TTL or version-aware safeguards matched to the freshness requirement.

**Required evidence:** Tests document the actual consistency guarantee, show rolled-back writes do not publish uncommitted values, and demonstrate bounded staleness or version-safe behavior under the forced race; failure recovery and cache metrics are visible.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q29"></a>

<details>
<summary>P1-Q29 - Duplicate scheduled work after adding replicas</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 4, original number 29.

**Lab families:** [S01](LAB-CATALOG.md#s01), [D04](LAB-CATALOG.md#d04). **State:** Planned.

**Live practice / implementation:** Run two replicas with the same scheduled reconciliation job; implement a shared lease/lock or atomic work claiming and idempotent job effects, then test worker death, lease expiry, and a job that outlives its lease.

**Required evidence:** Logs first demonstrate per-instance scheduling; the corrected design prevents duplicate durable effects and recovers abandoned work, including an overlap drill that explains why a distributed lease alone cannot promise exactly-once execution.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q30"></a>

<details>
<summary>P1-Q30 - Client timeouts while health checks pass</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 4, original number 30.

**Lab families:** [O01](LAB-CATALOG.md#o01), [O03](LAB-CATALOG.md#o03), [R01](LAB-CATALOG.md#r01). **State:** Planned.

**Live practice / implementation:** Keep a lightweight health endpoint green while inducing reverse-proxy timeout, request-queue saturation, and slow business dependencies in separate local drills; compare external synthetic requests with liveness, readiness, and request-path signals.

**Required evidence:** Client, proxy, and application timestamps locate each timeout boundary; the chosen timeout/readiness/capacity correction restores the business request without treating a successful health response as proof of user-visible availability.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q31"></a>

<details>
<summary>P1-Q31 - Profiling non-database work in a slow response</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 4, original number 31.

**Lab families:** [O02](LAB-CATALOG.md#o02), [J02](LAB-CATALOG.md#j02), [J01](LAB-CATALOG.md#j01). **State:** Planned.

**Live practice / implementation:** Use a fast-query endpoint with a large object graph, expensive mapping/serialization, synchronous log delay, and a saturated worker queue in separate variants; collect profiles and request-stage timings.

**Required evidence:** Attribute the dominant cost to CPU work, allocation/GC, blocking I/O, or queueing using the relevant evidence; compare response size, allocation, throughput, and latency before and after one targeted correction.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q32"></a>

<details>
<summary>P1-Q32 - Correlating logs across service boundaries</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 4, original number 32.

**Lab families:** [O01](LAB-CATALOG.md#o01), [J03](LAB-CATALOG.md#j03). **State:** Planned.

**Live practice / implementation:** Add structured service/instance/request fields and trace/span correlation to logs; propagate tracing context through HTTP, an executor, and later Kafka, with correct cleanup and a defined sampling strategy.

**Required evidence:** A saved query reconstructs one request across services and asynchronous work; concurrent tests show no context leakage, and the learner explains what remains observable when a trace is unsampled or logging context is absent.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q33"></a>

<details>
<summary>P1-Q33 - Handling a sudden tenfold traffic increase</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 4, original number 33.

**Lab families:** [O02](LAB-CATALOG.md#o02), [O03](LAB-CATALOG.md#o03), [R01](LAB-CATALOG.md#r01). **State:** Planned.

**Live practice / implementation:** Run a staged 1x-to-10x load experiment and measure saturation, database/downstream budgets, queue growth, instance startup time, and retry amplification; add admission control and scale only where measured capacity allows it.

**Required evidence:** Document the sustainable rate and first limiting resource; show bounded resource use and explicit overload responses at excess load, then demonstrate whether additional replicas actually improve completed-request throughput.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q34"></a>

<details>
<summary>P1-Q34 - Comparing staging and production configuration and infrastructure</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 4, original number 34.

**Lab families:** [O03](LAB-CATALOG.md#o03), [B04](LAB-CATALOG.md#b04). **State:** Planned.

**Live practice / implementation:** Introduce a deployment-only override or profile mismatch and compare effective configuration, artifact/JDK versions, schema/migrations, data scale, network/dependency settings, and CPU/memory limits between two environments.

**Required evidence:** A redacted configuration/environment diff identifies the injected difference and its property source; startup validation or a deployment check catches it afterward, and the same artifact works once the intended effective settings are restored.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p1-q35"></a>

<details>
<summary>P1-Q35 - Diagnosing crashes that occur only under load</summary>

**Source:** `Part_1_Microservices_Kafka_Production_Scenarios.pdf`, page 4, original number 35.

**Lab families:** [J02](LAB-CATALOG.md#j02), [O02](LAB-CATALOG.md#o02), [O04](LAB-CATALOG.md#o04). **State:** Planned.

**Live practice / implementation:** Ramp load in a constrained container with one seeded failure at a time, such as unbounded task retention, memory-limit termination, or thread exhaustion; preserve process exit details, platform events, GC/JFR data, and resource trends.

**Required evidence:** Reproduce a failure threshold, support the diagnosis with termination and runtime evidence, and validate the targeted correction with a repeat ramp and soak test; record capacity limits and unresolved uncertainty instead of inferring the cause from the last log line.

**Execution record, implementation commit, and learner review:** pending.

</details>

## Part 3 - Spring Boot

| ID | PDF page | Topic | Labs | State |
| --- | --- | --- | --- | --- |
| [P3-Q74](#p3-q74) | 1 | Auto-configuration selection | B01 | Planned |
| [P3-Q75](#p3-q75) | 1 | Web starter dependency and runtime effects | B01, B06 | Planned |
| [P3-Q76](#p3-q76) | 1 | Defaults and explicit overrides | B01, B04 | Planned |
| [P3-Q77](#p3-q77) | 1 | Configuration file loading | B04 | Planned |
| [P3-Q78](#p3-q78) | 1 | Version-specific application startup sequence | B01, B02, B06 | Planned |
| [P3-Q79](#p3-q79) | 1 | Component scanning versus the Boot application annotation | B01, B02 | Planned |
| [P3-Q80](#p3-q80) | 1 | Embedded Tomcat selection and construction | B01, B06 | Planned |
| [P3-Q81](#p3-q81) | 1 | Resolving multiple dependency candidates | B03 | Planned |
| [P3-Q82](#p3-q82) | 2 | Profile-dependent configuration | B04 | Planned |
| [P3-Q83](#p3-q83) | 2 | Factory metadata loading and current auto-configuration imports | B01, B06 | Planned |
| [P3-Q84](#p3-q84) | 2 | Java configuration and optional XML integration | B01, B03 | Planned |
| [P3-Q85](#p3-q85) | 2 | Controller response-body and view semantics | B05 | Planned |
| [P3-Q86](#p3-q86) | 2 | Managed dependency versions | B01 | Planned |
| [P3-Q87](#p3-q87) | 2 | Bean construction, initialization, and destruction | B02 | Planned |
| [P3-Q88](#p3-q88) | 2 | External configuration sources and binding | B04, O03 | Planned |
| [P3-Q89](#p3-q89) | 2 | Properties and YAML configuration collisions | B04 | Planned |
| [P3-Q90](#p3-q90) | 2 | Actuator auto-configuration and endpoint exposure | B01, O01 | Planned |
| [P3-Q91](#p3-q91) | 2 | Configuration classes and bean-method interception | B03 | Planned |
| [P3-Q92](#p3-q92) | 3 | Conditional DataSource creation | B01, D01 | Planned |
| [P3-Q93](#p3-q93) | 3 | Startup runners and readiness | B06 | Planned |
| [P3-Q94](#p3-q94) | 3 | Persistence exception translation versus HTTP error handling | D01, B05 | Planned |
| [P3-Q95](#p3-q95) | 3 | Direct configuration imports and auto-configuration selection | B01, B03 | Planned |
| [P3-Q96](#p3-q96) | 3 | Auto-configuration exclusions and consequences | B01 | Planned |
| [P3-Q97](#p3-q97) | 3 | Boot capabilities and distributed-service responsibilities | O03, R01, O01 | Planned |
| [P3-Q98](#p3-q98) | 3 | Executable Boot archive layout and launch | B06 | Planned |
| [P3-Q99](#p3-q99) | 3 | Default logging stack and configuration | O01, B04 | Planned |
| [P3-Q100](#p3-q100) | 3 | Server-port property precedence | B04, B06 | Planned |
| [P3-Q101](#p3-q101) | 3 | Servlet MVC request and response execution | B05 | Planned |
| [P3-Q102](#p3-q102) | 3 | Cloud deployment capabilities and limits | O03, O01 | Planned |
| [P3-Q103](#p3-q103) | 4 | Diagnosing representative application performance mistakes | O02, D02, J02 | Planned |

<a id="p3-q74"></a>

<details>
<summary>P3-Q74 - Auto-configuration selection</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 1, original number 74.

**Lab families:** [B01](LAB-CATALOG.md#b01). **State:** Planned.

**Live practice / implementation:** Enable the condition report, then change one supported configuration property or matching bean and compare the relevant candidate's outcomes.

**Required evidence:** Before/after condition-report entries explain why the chosen configuration or bean is applied or rejected.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q75"></a>

<details>
<summary>P3-Q75 - Web starter dependency and runtime effects</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 1, original number 75.

**Lab families:** [B01](LAB-CATALOG.md#b01), [B06](LAB-CATALOG.md#b06). **State:** Planned.

**Live practice / implementation:** Inspect the existing Boot 4.1.1 spring-boot-starter-webmvc dependency graph and isolate a reversible embedded-server dependency removal to observe its startup effect.

**Required evidence:** Dependency graph, condition report, and startup result connect the starter to available classes and server infrastructure; document the PDF's older starter-web wording.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q76"></a>

<details>
<summary>P3-Q76 - Defaults and explicit overrides</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 1, original number 76.

**Lab families:** [B01](LAB-CATALOG.md#b01), [B04](LAB-CATALOG.md#b04). **State:** Planned.

**Live practice / implementation:** Run the application with its defaults, then override one property and one conditional infrastructure bean in a controlled exercise.

**Required evidence:** Behavioral differences and the condition report identify the default, override mechanism, and maintenance tradeoff.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q77"></a>

<details>
<summary>P3-Q77 - Configuration file loading</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 1, original number 77.

**Lab families:** [B04](LAB-CATALOG.md#b04). **State:** Planned.

**Live practice / implementation:** Bind a harmless named setting from application.properties and trace configuration-data loading and property-source creation with the debugger.

**Required evidence:** A recorded breakpoint trace and resolved property origin show when the setting enters the Environment and reaches the bound object.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q78"></a>

<details>
<summary>P3-Q78 - Version-specific application startup sequence</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 1, original number 78.

**Lab families:** [B01](LAB-CATALOG.md#b01), [B02](LAB-CATALOG.md#b02), [B06](LAB-CATALOG.md#b06). **State:** Planned.

**Live practice / implementation:** Trace the current JVM launch through environment preparation, configuration processing, context refresh, server lifecycle, runners, and readiness.

**Required evidence:** An ordered trace tied to Boot 4.1.1 and Framework 7.0.9 distinguishes server creation from start and states the launch mode and customization limits.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q79"></a>

<details>
<summary>P3-Q79 - Component scanning versus the Boot application annotation</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 1, original number 79.

**Lab families:** [B01](LAB-CATALOG.md#b01), [B02](LAB-CATALOG.md#b02). **State:** Planned.

**Live practice / implementation:** Move a demonstration component outside the default scan scope, then restore discovery using an explicit scan boundary and inspect the composed Boot annotation.

**Required evidence:** Bean-presence checks and the annotation trace separate application component discovery from configuration and auto-configuration imports.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q80"></a>

<details>
<summary>P3-Q80 - Embedded Tomcat selection and construction</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 1, original number 80.

**Lab families:** [B01](LAB-CATALOG.md#b01), [B06](LAB-CATALOG.md#b06). **State:** Planned.

**Live practice / implementation:** Inspect matching servlet-server conditions, locate the server-factory bean, and trace its server creation and subsequent lifecycle start.

**Required evidence:** Resolved dependency, condition, factory type, and runtime port observations explain why this application uses Tomcat.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q81"></a>

<details>
<summary>P3-Q81 - Resolving multiple dependency candidates</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 1, original number 81.

**Lab families:** [B03](LAB-CATALOG.md#b03). **State:** Planned.

**Live practice / implementation:** Create two implementations for one constructor dependency, observe an unresolved ambiguity, and compare explicit qualifier and primary-candidate variants; separately inspect candidate-name matching.

**Required evidence:** A failure trace and successful injection assertions identify the actual selection rule rather than assuming that absence of a qualifier always fails.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q82"></a>

<details>
<summary>P3-Q82 - Profile-dependent configuration</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 82.

**Lab families:** [B04](LAB-CATALOG.md#b04). **State:** Planned.

**Live practice / implementation:** Add harmless development and test profile overrides plus one profile-gated demonstration bean, then launch with each profile and with no explicit profile.

**Required evidence:** Resolved settings, active-profile output, and bean-presence assertions show which configuration applies in each run.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q83"></a>

<details>
<summary>P3-Q83 - Factory metadata loading and current auto-configuration imports</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 83.

**Lab families:** [B01](LAB-CATALOG.md#b01), [B06](LAB-CATALOG.md#b06). **State:** Planned.

**Live practice / implementation:** Trace one current SpringFactoriesLoader extension lookup and separately inspect AutoConfigurationImportSelector reading AutoConfiguration.imports resources.

**Required evidence:** Observed resource names and call stacks distinguish surviving spring.factories uses from Boot 4.1.1 auto-configuration candidate discovery.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q84"></a>

<details>
<summary>P3-Q84 - Java configuration and optional XML integration</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 84.

**Lab families:** [B01](LAB-CATALOG.md#b01), [B03](LAB-CATALOG.md#b03). **State:** Planned.

**Live practice / implementation:** Register a demonstration bean using a configuration class and, in a separate reversible exercise, import an XML definition for an equivalent bean.

**Required evidence:** Bean lookup or injection assertions show both paths and support a correction to the premise that Boot completely removes XML support.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q85"></a>

<details>
<summary>P3-Q85 - Controller response-body and view semantics</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 85.

**Lab families:** [B05](LAB-CATALOG.md#b05). **State:** Planned.

**Live practice / implementation:** Compare a small endpoint under RestController with Controller variants with and without ResponseBody, including a String return value.

**Required evidence:** HTTP assertions and return-value-handler observations distinguish body conversion from view-name handling and resulting error paths.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q86"></a>

<details>
<summary>P3-Q86 - Managed dependency versions</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 86.

**Lab families:** [B01](LAB-CATALOG.md#b01). **State:** Planned.

**Live practice / implementation:** Inspect the effective POM and dependency tree, follow an inherited version to Boot's managed dependencies, and compare a temporary explicit override before reverting it.

**Required evidence:** Effective build metadata proves version selection, dependency inclusion, and override impact are distinct mechanisms.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q87"></a>

<details>
<summary>P3-Q87 - Bean construction, initialization, and destruction</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 87.

**Lab families:** [B02](LAB-CATALOG.md#b02). **State:** Planned.

**Live practice / implementation:** Add an isolated demonstration bean with constructor, initialization, and destruction observations, then inspect post-processing and close its context normally.

**Required evidence:** An ordered lifecycle trace distinguishes definition registration, instance creation, callbacks, possible post-processing, and managed shutdown.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q88"></a>

<details>
<summary>P3-Q88 - External configuration sources and binding</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 88.

**Lab families:** [B04](LAB-CATALOG.md#b04), [O03](LAB-CATALOG.md#o03). **State:** Planned.

**Live practice / implementation:** Supply the same harmless setting through a packaged file, external file, environment variable, and command-line argument across controlled runs.

**Required evidence:** A precedence matrix records each effective value and origin, and a deployment-style run uses the same artifact with different external settings.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q89"></a>

<details>
<summary>P3-Q89 - Properties and YAML configuration collisions</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 89.

**Lab families:** [B04](LAB-CATALOG.md#b04). **State:** Planned.

**Live practice / implementation:** Create a controlled same-location collision between application.properties and application.yml, then compare a different-location or profile-specific collision.

**Required evidence:** Effective values and property origins document the applicable precedence without claiming that file format alone outranks every other source.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q90"></a>

<details>
<summary>P3-Q90 - Actuator auto-configuration and endpoint exposure</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 90.

**Lab families:** [B01](LAB-CATALOG.md#b01), [O01](LAB-CATALOG.md#o01). **State:** Planned.

**Live practice / implementation:** Add Actuator in its planned phase, inspect endpoint-related conditions, and vary endpoint availability and HTTP exposure settings using local-only diagnostic data.

**Required evidence:** Condition reports, bean inspection, and HTTP checks distinguish endpoint implementation, availability, exposure, and access behavior.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q91"></a>

<details>
<summary>P3-Q91 - Configuration classes and bean-method interception</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 2, original number 91.

**Lab families:** [B03](LAB-CATALOG.md#b03). **State:** Planned.

**Live practice / implementation:** Compare full Configuration, proxyBeanMethods=false, and a registered non-configuration bean-factory class while directly invoking one Bean method from another.

**Required evidence:** Instance identity checks and debugger inspection demonstrate when interception preserves managed references and when ordinary Java calls create additional objects.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q92"></a>

<details>
<summary>P3-Q92 - Conditional DataSource creation</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 92.

**Lab families:** [B01](LAB-CATALOG.md#b01), [D01](LAB-CATALOG.md#d01). **State:** Planned.

**Live practice / implementation:** During the persistence phase, inspect DataSource configuration with a driver and connection settings, then introduce a custom DataSource and a controlled invalid-configuration variant.

**Required evidence:** Connection checks, bean type, matching/backoff conditions, and failure diagnostics connect dependencies and settings to the actual data source.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q93"></a>

<details>
<summary>P3-Q93 - Startup runners and readiness</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 93.

**Lab families:** [B06](LAB-CATALOG.md#b06). **State:** Planned.

**Live practice / implementation:** Add a harmless CommandLineRunner that records arguments and completion, then introduce a controlled delay and failure in isolated runs.

**Required evidence:** Runner/event ordering and process outcomes show its startup purpose, readiness implications, and failure propagation.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q94"></a>

<details>
<summary>P3-Q94 - Persistence exception translation versus HTTP error handling</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 94.

**Lab families:** [D01](LAB-CATALOG.md#d01), [B05](LAB-CATALOG.md#b05). **State:** Planned.

**Live practice / implementation:** Trigger a controlled persistence constraint failure, inspect the exception boundary, and separately map an application exception to an HTTP response using an exception handler.

**Required evidence:** Exception-class and HTTP-contract assertions identify the responsible Spring persistence and MVC mechanisms without treating them as one Boot feature.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q95"></a>

<details>
<summary>P3-Q95 - Direct configuration imports and auto-configuration selection</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 95.

**Lab families:** [B01](LAB-CATALOG.md#b01), [B03](LAB-CATALOG.md#b03). **State:** Planned.

**Live practice / implementation:** Import a small explicit configuration class and compare its registration path with the selector imported by EnableAutoConfiguration.

**Required evidence:** Bean definitions and import-selector traces explain the relationship between the general import mechanism and Boot's conditional candidate selection.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q96"></a>

<details>
<summary>P3-Q96 - Auto-configuration exclusions and consequences</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 96.

**Lab families:** [B01](LAB-CATALOG.md#b01). **State:** Planned.

**Live practice / implementation:** Exclude one verified current auto-configuration class in an isolated exercise, observe the missing infrastructure or fallback behavior, then restore it.

**Required evidence:** The exclusion report and a targeted bean or behavior assertion show the exact effect and any dependent startup failure.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q97"></a>

<details>
<summary>P3-Q97 - Boot capabilities and distributed-service responsibilities</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 97.

**Lab families:** [O03](LAB-CATALOG.md#o03), [R01](LAB-CATALOG.md#r01), [O01](LAB-CATALOG.md#o01). **State:** Planned.

**Live practice / implementation:** When the planned service split is justified, run two independently configured services and inject a slow or unavailable downstream service while observing telemetry.

**Required evidence:** Deployment and failure observations distinguish Boot-provided application infrastructure from explicitly designed network resilience and operational behavior.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q98"></a>

<details>
<summary>P3-Q98 - Executable Boot archive layout and launch</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 98.

**Lab families:** [B06](LAB-CATALOG.md#b06). **State:** Planned.

**Live practice / implementation:** Inspect the packaged executable JAR and an ordinary project JAR, compare manifests and dependency layout, and launch each using its appropriate classpath or launcher.

**Required evidence:** Archive listings and successful launch commands explain Boot's nested libraries and loader without conflating them with every shaded or fat JAR format.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q99"></a>

<details>
<summary>P3-Q99 - Default logging stack and configuration</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 99.

**Lab families:** [O01](LAB-CATALOG.md#o01), [B04](LAB-CATALOG.md#b04). **State:** Planned.

**Live practice / implementation:** Inspect resolved logging dependencies, add a parameterized application log, and change only its package log level through external configuration.

**Required evidence:** Dependency and runtime output identify the facade, implementation, effective level, and difference between application logs and Boot's condition-report debug switch.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q100"></a>

<details>
<summary>P3-Q100 - Server-port property precedence</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 100.

**Lab families:** [B04](LAB-CATALOG.md#b04), [B06](LAB-CATALOG.md#b06). **State:** Planned.

**Live practice / implementation:** Set conflicting server ports through a config file, environment variable, and command line in controlled launches, then test a random-port configuration.

**Required evidence:** Property origins, startup-reported bound ports, and HTTP requests demonstrate precedence and distinguish the configured port from a randomly assigned actual port.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q101"></a>

<details>
<summary>P3-Q101 - Servlet MVC request and response execution</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 101.

**Lab families:** [B05](LAB-CATALOG.md#b05). **State:** Planned.

**Live practice / implementation:** Trace a request through the servlet entry, DispatcherServlet, handler mapping and adapter, controller invocation, and response conversion; compare a missing route and unsupported media type.

**Required evidence:** Debugger observations and HTTP tests explain the successful JSON path and where each controlled failure changes the response.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q102"></a>

<details>
<summary>P3-Q102 - Cloud deployment capabilities and limits</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 3, original number 102.

**Lab families:** [O03](LAB-CATALOG.md#o03), [O01](LAB-CATALOG.md#o01). **State:** Planned.

**Live practice / implementation:** Package and run the service in the planned container environment with external settings, health/readiness observations, resource limits, and a termination signal.

**Required evidence:** A reproducible deployment and shutdown record demonstrates useful capabilities and operational limitations rather than an unqualified claim of framework preference.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p3-q103"></a>

<details>
<summary>P3-Q103 - Diagnosing representative application performance mistakes</summary>

**Source:** `Part_3_Spring_Boot_Interview_Questions.pdf`, page 4, original number 103.

**Lab families:** [O02](LAB-CATALOG.md#o02), [D02](LAB-CATALOG.md#d02), [J02](LAB-CATALOG.md#j02). **State:** Planned.

**Live practice / implementation:** Introduce isolated representative faults such as excessive database round trips, avoidable per-request allocation, or a constrained resource pool, then profile and fix one fault at a time.

**Required evidence:** Repeatable workload results, latency/throughput measurements, query counts or runtime profiles, and a before/after comparison substantiate each diagnosis without claiming an exhaustive ranking.

**Execution record, implementation commit, and learner review:** pending.

</details>

## Part 4 - Java runtime and Spring Boot production

| ID | PDF page | Topic | Labs | State |
| --- | --- | --- | --- | --- |
| [P4-J01](#p4-j01) | 1 | Progressive slowdown without visible errors | J02, O02 | Planned |
| [P4-J02](#p4-j02) | 1 | High latency with low CPU | J01, O02 | Planned |
| [P4-J03](#p4-j03) | 1 | Out-of-memory failures outside ordinary heap exhaustion | J02 | Planned |
| [P4-J04](#p4-j04) | 1 | Heap sizing can alter latency and memory pressure | J02, O02 | Planned |
| [P4-J05](#p4-j05) | 1 | Requests can queue while another pool has idle threads | J01, O02 | Planned |
| [P4-J06](#p4-j06) | 1 | Allocation changes increase garbage-collection pressure | J02, O02 | Planned |
| [P4-J07](#p4-j07) | 1 | Process lifetime after the main method returns | J01 | Planned |
| [P4-J08](#p4-j08) | 1 | Parallel streams can add overhead or contention | J01, O02 | Planned |
| [P4-J09](#p4-j09) | 2 | Gradually rising memory usage | J02 | Planned |
| [P4-J10](#p4-j10) | 2 | Logging can consume latency and capacity | O01, J02, O02 | Planned |
| [P4-J11](#p4-j11) | 2 | ThreadLocal lifetime and request-context leakage | J03 | Planned |
| [P4-J12](#p4-j12) | 2 | Executor failures can remain unobserved | J01, O01 | Planned |
| [P4-J13](#p4-j13) | 2 | Retries can amplify an outage | R01, O02 | Planned |
| [P4-J14](#p4-j14) | 2 | Load-dependent deadlocks | J01, O04 | Planned |
| [P4-J15](#p4-j15) | 2 | More application instances can worsen a shared bottleneck | D01, O02, O03 | Planned |
| [P4-S16](#p4-s16) | 2 | Local and production configuration diverge | B04, O03, O04 | Planned |
| [P4-S17](#p4-s17) | 2 | Production latency differs from local latency | D02, O01, O02 | Planned |
| [P4-S18](#p4-s18) | 2 | A properties-file edit does not determine the effective value | B04 | Planned |
| [P4-S19](#p4-s19) | 3 | Timeouts with low CPU point to waits or constrained resources | D01, O02, R01 | Planned |
| [P4-S20](#p4-s20) | 3 | Ambiguous dependency candidates prevent startup | B03 | Planned |
| [P4-S21](#p4-s21) | 3 | Transaction annotations do not guarantee rollback on every path | D01 | Planned |
| [P4-S22](#p4-s22) | 3 | Database-pool exhaustion under load | D01, D02, O02 | Planned |
| [P4-S23](#p4-s23) | 3 | Scheduled work competes with request processing | S01, J01, O02 | Planned |
| [P4-S24](#p4-s24) | 3 | Container execution changes environment and resource limits | O03, J02, B04 | Planned |
| [P4-S25](#p4-s25) | 3 | A deployment may still route users to old behavior | O03, C01, O01 | Planned |
| [P4-S26](#p4-s26) | 3 | Missing production logs require an end-to-end delivery check | O01, B04, O03 | Planned |
| [P4-S27](#p4-s27) | 3 | Asynchronous work can move or worsen the bottleneck | J01, J03, O02 | Planned |
| [P4-S28](#p4-s28) | 3 | Circuit-breaker recovery depends on state and probe policy | R01, O01 | Planned |
| [P4-S29](#p4-s29) | 3 | Additional resources do not remove every limiting factor | O02, D03, R01 | Planned |
| [P4-S30](#p4-s30) | 4 | Defending and testing a consequential Spring Boot design choice | B04, D01, O03 | Planned |

<a id="p4-j01"></a>

<details>
<summary>P4-J01 - Progressive slowdown without visible errors</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 1, original number 1.

**Lab families:** [J02](LAB-CATALOG.md#j02), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Add an intentionally retained, bounded collection of request payload samples to a lab endpoint. Run a fixed-rate workload long enough to compare early and late windows, then remove the retention and rerun.

**Required evidence:** Compare throughput, p95 latency, live heap after GC, allocation rate, and JFR samples across both runs; connect the measured retained objects to the slowdown rather than treating time alone as proof of a leak.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j02"></a>

<details>
<summary>P4-J02 - High latency with low CPU</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 1, original number 2.

**Lab families:** [J01](LAB-CATALOG.md#j01), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Route a lab endpoint through a deliberately slow HTTP stub using a bounded worker pool. Generate enough concurrent requests to fill the pool while collecting thread dumps and latency measurements.

**Required evidence:** Show waiting or blocked stacks, pool occupancy, queue depth, downstream latency, and CPU together; demonstrate which wait dominates by changing only the stub delay.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j03"></a>

<details>
<summary>P4-J03 - Out-of-memory failures outside ordinary heap exhaustion</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 1, original number 3.

**Lab families:** [J02](LAB-CATALOG.md#j02). **State:** Planned.

**Live practice / implementation:** In a separate resource-limited JVM, set a small MaxDirectMemorySize and retain direct ByteBuffers until that limit is reached. Record the exception text and heap usage, then release references and repeat with a bounded allocation strategy.

**Required evidence:** Produce the direct-buffer allocation failure while ordinary heap still has headroom. Explain the measured memory domain and distinguish heap, direct/native memory, metaspace, and native-thread failures without assuming all OutOfMemoryError cases are heap leaks.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j04"></a>

<details>
<summary>P4-J04 - Heap sizing can alter latency and memory pressure</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 1, original number 4.

**Lab families:** [J02](LAB-CATALOG.md#j02), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Run an allocation-heavy workload in isolated JVMs with two explicit heap sizes, the same collector, warm-up, input, and bounded environment. Capture GC logs and JFR for each run; optionally add container memory pressure as a separate controlled variable.

**Required evidence:** Compare pause distributions, GC frequency, live-set size, throughput, and RSS. Report the observed result even if the larger heap helps; explain why a larger heap is not guaranteed to worsen or improve every workload.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j05"></a>

<details>
<summary>P4-J05 - Requests can queue while another pool has idle threads</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 1, original number 5.

**Lab families:** [J01](LAB-CATALOG.md#j01), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Build an endpoint that hands work to a separate small executor with a bounded queue while the servlet executor has spare capacity. Saturate the downstream executor and expose both executors' measurements.

**Required evidence:** Identify exactly where requests wait using queue size, active-thread counts, and stack samples. Show that idle threads in one pool cannot automatically execute another pool's work, then validate a targeted capacity or admission-control change.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j06"></a>

<details>
<summary>P4-J06 - Allocation changes increase garbage-collection pressure</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 1, original number 6.

**Lab families:** [J02](LAB-CATALOG.md#j02), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Add a lab-only payload transformation that creates repeated intermediate arrays or strings. Compare it with a version that avoids unnecessary copies under identical load and response semantics.

**Required evidence:** Use allocation profiles, GC logs, and latency distributions to connect a specific call site to changed allocation volume and pauses; confirm response equality and record whether the live set also changed.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j07"></a>

<details>
<summary>P4-J07 - Process lifetime after the main method returns</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 1, original number 7.

**Lab families:** [J01](LAB-CATALOG.md#j01). **State:** Planned.

**Live practice / implementation:** Write a small Java 21 program that starts a non-daemon platform-thread executor, submits work, and returns from main without shutdown. Run it with a timeout, capture its thread dump, then add orderly executor shutdown.

**Required evidence:** Show the surviving non-daemon thread and the process's exit after cleanup. Distinguish these threads from Java 21 virtual threads, which are daemon threads and do not themselves keep the JVM alive.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j08"></a>

<details>
<summary>P4-J08 - Parallel streams can add overhead or contention</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 1, original number 8.

**Lab families:** [J01](LAB-CATALOG.md#j01), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Implement sequential and parallel versions of the same bounded computation, verify equal results, and benchmark after warm-up across small and large inputs. Add a separate variant that blocks on a local stub to inspect common-pool behavior.

**Required evidence:** Report elapsed-time distributions, CPU, worker activity, and competing-request latency. Attribute regressions to measured overhead, blocking, or resource contention rather than assuming parallelism is always faster or always unsafe.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j09"></a>

<details>
<summary>P4-J09 - Gradually rising memory usage</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 9.

**Lab families:** [J02](LAB-CATALOG.md#j02). **State:** Planned.

**Live practice / implementation:** Create separate bounded experiments for an unbounded-by-design retention map and a bounded cache, using a capped test dataset. Collect successive heap histograms or dumps after comparable load and GC opportunities.

**Required evidence:** Show retained-object growth and a reference path in the retention case, and stabilization in the bounded case. Distinguish rising committed heap or RSS from evidence of a growing live set.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j10"></a>

<details>
<summary>P4-J10 - Logging can consume latency and capacity</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 10.

**Lab families:** [O01](LAB-CATALOG.md#o01), [J02](LAB-CATALOG.md#j02), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Add high-volume structured logging to a lab endpoint and route it through a deliberately slow local sink. Compare synchronous output with a bounded asynchronous configuration and a reduced-volume baseline.

**Required evidence:** Measure request latency, log throughput, allocation, queue occupancy, and any dropped events. Explain the latency-versus-delivery tradeoff and prove that moving logs to a queue does not create unlimited sink capacity.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j11"></a>

<details>
<summary>P4-J11 - ThreadLocal lifetime and request-context leakage</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 11.

**Lab families:** [J03](LAB-CATALOG.md#j03). **State:** Planned.

**Live practice / implementation:** Use a single reused executor thread to process two synthetic users. Set a ThreadLocal in the first task and deliberately omit cleanup, then repeat using try/finally removal and explicit context propagation for asynchronous work.

**Required evidence:** Demonstrate the second task observing stale context before the fix, then assert isolation across tasks and cleanup on exceptional paths. Explain why thread-bound storage is neither request scope nor automatic asynchronous propagation.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j12"></a>

<details>
<summary>P4-J12 - Executor failures can remain unobserved</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 12.

**Lab families:** [J01](LAB-CATALOG.md#j01), [O01](LAB-CATALOG.md#o01). **State:** Planned.

**Live practice / implementation:** Submit a Callable that throws and initially discard its Future. Then observe completion with Future.get or an explicit completion/reporting path, and compare with a task sent using execute.

**Required evidence:** Capture the surfaced exception and a failure counter or structured event. Explain how submit stores failure for observation, how execute differs, and how the caller or monitoring system learns that work failed.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j13"></a>

<details>
<summary>P4-J13 - Retries can amplify an outage</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 13.

**Lab families:** [R01](LAB-CATALOG.md#r01), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Use a failing local dependency with a request counter. Compare nested immediate retries against a single retry owner with a bounded attempt budget, exponential backoff, jitter, timeout, and concurrency cap.

**Required evidence:** Report upstream calls per original request, peak concurrency, completion latency, and queue/rejection behavior. Show the multiplicative effect of nested retries and the bounded failure behavior after the change.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j14"></a>

<details>
<summary>P4-J14 - Load-dependent deadlocks</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 14.

**Lab families:** [J01](LAB-CATALOG.md#j01), [O04](LAB-CATALOG.md#o04). **State:** Planned.

**Live practice / implementation:** In an isolated process, coordinate two tasks with barriers so they acquire two locks in opposite orders. Capture a deadlock report, stop the lab process, then impose consistent lock ordering and rerun repeated concurrent attempts.

**Required evidence:** Provide the cyclic wait graph and the matching thread-dump ownership details, followed by bounded successful completion with the fix. Explain why low-concurrency local testing may miss the same ordering defect.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-j15"></a>

<details>
<summary>P4-J15 - More application instances can worsen a shared bottleneck</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 15.

**Lab families:** [D01](LAB-CATALOG.md#d01), [O02](LAB-CATALOG.md#o02), [O03](LAB-CATALOG.md#o03). **State:** Planned.

**Live practice / implementation:** Run a fixed aggregate workload against one and then multiple application instances sharing a deliberately capacity-limited database. Keep per-instance pool sizes unchanged first, then apply an explicit total connection budget.

**Required evidence:** Compare aggregate connections, DB wait time, throughput, p95 latency, and errors. Show whether extra instances increase downstream contention and validate the connection-budget change under the same total workload.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s16"></a>

<details>
<summary>P4-S16 - Local and production configuration diverge</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 16.

**Lab families:** [B04](LAB-CATALOG.md#b04), [O03](LAB-CATALOG.md#o03), [O04](LAB-CATALOG.md#o04). **State:** Planned.

**Live practice / implementation:** Start the same packaged application with two environment profiles. Inject one controlled difference at a time, such as a missing required property, unreachable dependency URL, or incompatible file path, and build a startup-diagnosis checklist from actual failures.

**Required evidence:** Capture startup logs, active profiles, safe property-origin information, and dependency reachability for each failure. Fix the root cause and show the identical artifact starting successfully without exposing secret values.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s17"></a>

<details>
<summary>P4-S17 - Production latency differs from local latency</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 17.

**Lab families:** [D02](LAB-CATALOG.md#d02), [O01](LAB-CATALOG.md#o01), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Compare a local-sized dataset with a much larger seeded dataset and introduce controlled dependency latency. Instrument an order-read endpoint to separate database, remote-call, and application time.

**Required evidence:** Use traces, query counts/plans, and latency percentiles to locate the changed cost. Make one targeted query or call-path improvement and show its effect with the production-like dataset and latency retained.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s18"></a>

<details>
<summary>P4-S18 - A properties-file edit does not determine the effective value</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 2, original number 18.

**Lab families:** [B04](LAB-CATALOG.md#b04). **State:** Planned.

**Live practice / implementation:** Bind a harmless demonstration property, set competing values in a properties file, profile file, environment variable, and command-line argument, and restart through a scripted matrix. Include a file edit without restart to test the chosen loading behavior.

**Required evidence:** Record the effective value and winning property source in every case. Explain precedence and the absence of automatic runtime rebinding in this setup, then show the intended value after the correct source or restart is changed.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s19"></a>

<details>
<summary>P4-S19 - Timeouts with low CPU point to waits or constrained resources</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 19.

**Lab families:** [D01](LAB-CATALOG.md#d01), [O02](LAB-CATALOG.md#o02), [R01](LAB-CATALOG.md#r01). **State:** Planned.

**Live practice / implementation:** Hold a database connection in a slow lab operation until a small connection pool is exhausted, then drive additional requests with finite acquisition and HTTP timeouts. Collect synchronized pool and request measurements.

**Required evidence:** Show connection waiters, active connections, timed-out requests, and waiting stack locations while CPU remains low. Demonstrate the effect of shortening transaction work or limiting admission instead of merely adding threads.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s20"></a>

<details>
<summary>P4-S20 - Ambiguous dependency candidates prevent startup</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 20.

**Lab families:** [B03](LAB-CATALOG.md#b03). **State:** Planned.

**Live practice / implementation:** Introduce two implementations of one service interface and inject it through a single constructor. Observe startup failure, then compare explicit @Qualifier selection with a documented @Primary default.

**Required evidence:** Capture the ambiguity diagnostic and prove with an application test which implementation is selected under each configuration. Explain why arbitrary bean deletion or matching names accidentally is a fragile remedy.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s21"></a>

<details>
<summary>P4-S21 - Transaction annotations do not guarantee rollback on every path</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 21.

**Lab families:** [D01](LAB-CATALOG.md#d01). **State:** Planned.

**Live practice / implementation:** Build database-backed tests for an external proxied call, a same-object self-call, an unchecked exception, a checked exception, and a swallowed exception. Apply a deliberate rollback rule or service boundary change for the intended contract.

**Required evidence:** Query persisted rows after each completed transaction and record the difference between proxy interception and rollback rules. Confirm the final contract with real transaction boundaries rather than relying on annotation presence.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s22"></a>

<details>
<summary>P4-S22 - Database-pool exhaustion under load</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 22.

**Lab families:** [D01](LAB-CATALOG.md#d01), [D02](LAB-CATALOG.md#d02), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Create separate failure modes for a long transaction, a slow query, and an intentionally unclosed raw JDBC connection in an isolated lab. Drive bounded load and enable suitable pool diagnostics; fix each cause independently.

**Required evidence:** Correlate active/idle/waiting pool counts, acquisition timeouts, transaction/query duration, and any leak warnings. Show connections returning and load recovering after each fix; explain that a leak warning alone is not proof of a permanently leaked connection.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s23"></a>

<details>
<summary>P4-S23 - Scheduled work competes with request processing</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 23.

**Lab families:** [S01](LAB-CATALOG.md#s01), [J01](LAB-CATALOG.md#j01), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Add a scheduled batch operation that shares a constrained executor or database budget with HTTP work. Compare a dedicated bounded scheduler plus an explicit batch concurrency limit, then run two instances to reveal duplicate scheduling.

**Required evidence:** Measure API p95 latency, scheduler delay, pool pressure, and number of job executions. Show that thread-pool isolation alone does not isolate shared DB or CPU capacity, and validate the chosen single-execution policy across instances.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s24"></a>

<details>
<summary>P4-S24 - Container execution changes environment and resource limits</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 24.

**Lab families:** [O03](LAB-CATALOG.md#o03), [J02](LAB-CATALOG.md#j02), [B04](LAB-CATALOG.md#b04). **State:** Planned.

**Live practice / implementation:** Package the application into a container and compare its startup with local execution using explicit CPU/memory limits, environment values, filesystem permissions, and dependency hostnames. Change one discrepancy per experiment.

**Required evidence:** Record image/JDK identity, effective resource limits, safe configuration origins, and startup or connectivity errors. Explain and fix observed differences such as container-local localhost or unwritable paths without treating Docker itself as the root cause.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s25"></a>

<details>
<summary>P4-S25 - A deployment may still route users to old behavior</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 25.

**Lab families:** [O03](LAB-CATALOG.md#o03), [C01](LAB-CATALOG.md#c01), [O01](LAB-CATALOG.md#o01). **State:** Planned.

**Live practice / implementation:** Deploy two local application revisions with a visible build identifier behind a controllable proxy. Deliberately leave one old instance in rotation, then separately enable a stale response cache to compare the symptoms.

**Required evidence:** Correlate request/build identifiers, active instances, image digests, and cache-hit behavior. Demonstrate consistent new responses after correcting routing or cache policy, and prove a deliberate rollback returns the expected revision.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s26"></a>

<details>
<summary>P4-S26 - Missing production logs require an end-to-end delivery check</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 26.

**Lab families:** [O01](LAB-CATALOG.md#o01), [B04](LAB-CATALOG.md#b04), [O03](LAB-CATALOG.md#o03). **State:** Planned.

**Live practice / implementation:** Send a uniquely tagged synthetic request and break the logging path in separate runs using an elevated log threshold, an incorrect appender destination, and a stopped local collector. Restore each setting independently.

**Required evidence:** Trace the marker from application emission through stdout/file output to collection, identifying the first missing stage. Verify effective profile/log level and collector recovery without logging credentials or personal data.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s27"></a>

<details>
<summary>P4-S27 - Asynchronous work can move or worsen the bottleneck</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 27.

**Lab families:** [J01](LAB-CATALOG.md#j01), [J03](LAB-CATALOG.md#j03), [O02](LAB-CATALOG.md#o02). **State:** Planned.

**Live practice / implementation:** Move a slow lab operation to @Async with an explicitly bounded executor. Compare synchronous and asynchronous versions at the same arrival rate, inject overload, and define rejection, error reporting, and context propagation behavior.

**Required evidence:** Measure submission latency separately from end-to-end completion latency, plus queue depth, worker utilization, errors, and rejected tasks. Show whether capacity increased or work merely accumulated, including correct context on success and failure.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s28"></a>

<details>
<summary>P4-S28 - Circuit-breaker recovery depends on state and probe policy</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 28.

**Lab families:** [R01](LAB-CATALOG.md#r01), [O01](LAB-CATALOG.md#o01). **State:** Planned.

**Live practice / implementation:** Use a local stub that fails and later recovers. Configure an observable breaker with a known open wait, half-open probe allowance, and slow-call/failure thresholds. Compare recovery with new calls versus an idle period and a configured automatic transition if supported.

**Required evidence:** Record state transitions, attempted and blocked calls, probe outcomes, and timing. Explain the actual library's transition policy and why a healthy upstream alone does not instantly reset local breaker state.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s29"></a>

<details>
<summary>P4-S29 - Additional resources do not remove every limiting factor</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 3, original number 29.

**Lab families:** [O02](LAB-CATALOG.md#o02), [D03](LAB-CATALOG.md#d03), [R01](LAB-CATALOG.md#r01). **State:** Planned.

**Live practice / implementation:** Build one benchmark dominated by a serialized critical section or contended database row and another limited by a dependency's fixed rate cap. Increase application workers or instances before making a targeted change to the limiting path.

**Required evidence:** Compare throughput scaling, tail latency, lock/dependency wait, and CPU. Identify the measured serial or external limit and show which design change alters it; do not infer a CPU bottleneck from slow responses alone.

**Execution record, implementation commit, and learner review:** pending.

</details>

<a id="p4-s30"></a>

<details>
<summary>P4-S30 - Defending and testing a consequential Spring Boot design choice</summary>

**Source:** `Part_4_Production_Thinking_Handbook.pdf`, page 4, original number 30.

**Lab families:** [B04](LAB-CATALOG.md#b04), [D01](LAB-CATALOG.md#d01), [O03](LAB-CATALOG.md#o03). **State:** Planned.

**Live practice / implementation:** Choose an explicit database connection-pool budget per instance and document its assumptions in an architecture decision. Run a multi-instance load test, deliberately exceed the shared DB connection budget, then revise the configuration and rehearse rollback.

**Required evidence:** Present the decision, rejected alternatives, measured failure threshold, revised capacity calculation, and recovery evidence. Explain the tradeoff using the implemented configuration and observed behavior rather than claiming a setting is universally production-safe.

**Execution record, implementation commit, and learner review:** pending.

</details>

## Source fingerprints

SHA-256 hashes identify the exact supplied editions used for this mapping.
They allow a later revision to be compared without confusing question numbers.

- `Part_1_Microservices_Kafka_Production_Scenarios.pdf`: `d49fe275022c56db1029aed8571ffdeea1a01ab5b90e99ca5c49355e0a7abf7a`
- `Part_3_Spring_Boot_Interview_Questions.pdf`: `0d755bedc73bc282bc0b3bb9e622ca576e65ea964db75312f873a6a8f52a9052`
- `Part_4_Production_Thinking_Handbook.pdf`: `c1927a12ecf23db74ec7c30a62600e5de2f401c1bbc5115c469453b896bb7eb0`
