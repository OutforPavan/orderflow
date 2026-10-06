# Learning progress

## Current position

- Environment and minimal application baseline: prepared.
- Baseline: Java 21, Spring Boot 4.1.1, Maven, one HTTP endpoint.
- Repository: [OutforPavan/orderflow](https://github.com/OutforPavan/orderflow).
- Lesson 001: learner confirmed receiving the endpoint JSON on 2026-09-25;
  message-change exercise and reviewed explanation remain pending.
- Lesson 002: constructor-injection implementation prepared and trainer-verified.
- Understanding demonstrated: missing-service startup prediction reviewed as
  correct; refine the lookup owner to Spring's container. On 2026-09-27 the learner
  correctly identified input/validation control as a request-DTO benefit; API
  independence from the entity and validation execution were explained as refinements.
- Current pace: [three-day sprint](THREE-DAY-SPRINT.md), three hours per day.
- Current implementation: Day 1 product/order APIs, external configuration,
  PostgreSQL migrations, JPA, and transaction rollback. Trainer verification passed:
  43 tests plus real HTTP requests and application-restart persistence.
- Current learner evidence: product creation returned 201, Location `/api/products/1`,
  name `Keyboard`, price 1250.00, and stock 10 (response shared on 2026-09-26).
  The learner subsequently reported restart persistence and an order POST, sharing
  a product response with stock 8. The actual order JSON has not been supplied.
- Current next step, 2026-09-29: explain Spring Security first in response to the
  learner's interview question, then select the client/authentication mechanism
  for a small implementation. [Security reference](SPRING-SECURITY.md) is prepared;
  no security feature is installed. Earlier class/configuration walkthrough,
  validation prediction, and unobserved failure/recovery checkpoints remain open.

## Baseline verification — 2026-09-24

- Built and packaged with Eclipse Temurin Java 21.0.12.1 and Maven 3.9.16.
- `./dev verify`: 2 tests passed, no failures or skipped tests.
- Started the packaged JAR and called the endpoint over localhost: HTTP 200,
  `application/json`, and the expected response body.
- Stopped the verification server after the check.
- These checks establish the baseline works; the learner checkpoint remains open.

The baseline endpoint is `GET /api/learning/status` and returns:

```json
{"application":"orderflow","message":"Learning Spring Boot one step at a time"}
```

## Evidence to collect for lesson 001

- [x] Learner starts the application and calls the endpoint (self-reported JSON response, 2026-09-25).
- [ ] Learner changes the message and observes the new response.
- [ ] Learner traces bootstrap, request mapping, method execution, and JSON response.
- [ ] Learner explains Spring Boot's contribution and identifies remaining questions.

## Discussion and reference notes - 2026-09-24

- Discussed how Spring creates the controller: IoC, component scanning,
  managed beans, default singleton scope, and the request mapping.
- Prepared deeper startup, bean-lifecycle, and HTTP-processing reference notes
  in [TECHNICAL-NOTEBOOK.md](TECHNICAL-NOTEBOOK.md), including follow-up answers.
- Open learner question: what happens if `@RestController` is removed and no
  alternative registration is added? The learner's answer is still pending.
- Indexed all 95 questions from the three supplied PDFs in
  [PDF-COVERAGE.md](PDF-COVERAGE.md), with practical labs and evidence criteria.
- PDF items covered: 0/95. Broader requirements reviewed complete: 0/14.
  These are planning counts, not a judgment about the learner's prior experience.
- Next teaching step remains the Lesson 001 exercise and explanation; the new
  documentation does not advance the lesson checkpoint automatically.

## Lesson 002 implementation - 2026-09-25

- Added `LearningService`; `LearningController` requires it through its sole
  constructor. The service supplies the existing message, and the HTTP contract
  remains unchanged.
- `./dev verify` on Java 21: 3 tests passed, 0 failures/errors/skips.
- The missing-dependency test registered only the controller and verified context
  startup fails with `UnsatisfiedDependencyException` caused by
  `NoSuchBeanDefinitionException`. That expected failure is a passing test.
- A separate packaged-application localhost request returned HTTP 200,
  `application/json`, and the expected two fields. The verification server was stopped.
- [Lesson instructions](lessons/002-constructor-injection.md) and
  [lab evidence](labs/B03-001-constructor-injection.md) are ready for learner practice.
- EXT01 is in progress. PDF items covered remain 0/95; same-type bean ambiguity
  questions P3-Q81 and P4-S20 are still planned for a later B03 exercise.
- Learner execution of Lesson 002, constructor explanation, and failure comparison
  remain pending. The user's request to begin the next implementation did not
  mark earlier unanswered checkpoints complete.

## Open questions and next steps

### Looping caller and duplicate order protection - 2026-10-06

- Prepared technical notebook chapter 13 and interview follow-ups distinguishing
  API admission controls from durable order idempotency. Covered authenticated
  caller quotas, concurrent duplicates, lost responses after commit, changing keys,
  and domain uniqueness. No rate/throughput numbers are measured results.
- Proposed shared enforcement and one database transaction for key/order/stock/result;
  current product locking alone does not deduplicate requests. No new protection
  is installed, no attack diagnosed, and no API/DB/load checks were run this turn.
- Linked references for P1-Q02, P1-Q16, and P1-Q33; all remain Planned. Learner
  practice/review, earlier checkpoints, and completion counts remain unchanged.

### CAP and Orderflow scaling interview preparation - 2026-10-06

- Prepared technical notebook chapter 12: CAP during partitions, application
  scaling, burst handling, cache techniques/races, database optimization, and
  shard-key tradeoffs using a hypothetical flash sale.
- Inspected the current database stock lock and five-connection pool configuration;
  distinguished existing behavior from future caches, replicas, messaging,
  idempotency, and sharding. No production deployment or measured capacity is claimed.
- Added interviewer follow-ups and references for P1-Q12, P1-Q28, P1-Q33, and P4-J15.
  Their states remain Planned; learner answers and live drills are pending.
- Documentation only. No application changes, load tests, or database operations.
  Earlier learner checkpoints, security choice, and all coverage criteria remain open.

### Percentile and Future/CompletableFuture follow-ups - 2026-10-06

- Explained p95/p99 using an illustrative request sample; no measurements taken.
- Learner clarified the next question as Future versus CompletableFuture.
  Prepared the comparison, composition/error/threading details, and a standalone
  Java 21 example under docs/examples. Trainer execution produced the expected
  product list and combined page; no database or HTTP requests were made.
- No application async behavior was introduced and no performance gain is claimed.
  Learner practice, earlier checkpoints, and full coverage criteria remain pending.

### OrderEntry performance interview preparation - 2026-10-06

- Learner requested a senior-engineer answer to a Capgemini question about a slow
  OrderEntry landing page. Prepared a diagnosis-first explanation and conditional
  fixes in technical notebook chapter 10 and the interview notes.
- Distinguish usable order form from a painted shell; correlate frontend and
  backend evidence, optimize the critical path, preserve price/stock correctness,
  then verify tail latency and errors under representative load.
- This is hypothetical interview preparation. No page was inspected, application
  code changed, performance test run, or improvement measured. The Spring Security
  implementation/client selection and earlier learner checkpoints remain pending.
- Documentation-only update; PDF coverage and broader completion counts unchanged.

### Spring Security explanation requested - 2026-09-29

- Learner reports an interviewer asked for Spring Security in the order/inventory
  project and explicitly requests an in-depth explanation first, covering
  authentication, authorization, and implementation.
- Prepared [SPRING-SECURITY.md](SPRING-SECURITY.md): request/filter flow, credential
  verification, bean roles, password handling, action/resource authorization,
  Basic/session/JWT choices, CSRF/CORS, security errors, and acceptance cases.
- Inspected the current `fe874e3` baseline: six endpoints, no security dependency
  or account model, and no order owner. Pricing has added pessimistic stock/price
  locking and strict DTO input; preserve those behaviors. The latest historical
  build record is 74 passing tests on 2026-09-28, not new security verification.
- Asked which client to secure first (IntelliJ/curl username/password, browser
  session, or JWT resource server). No response recorded. Basic is used only as
  the reference example; its product-only rules deny orders pending ownership.
- This increment adds explanation/reference documentation only. No app source,
  dependency, migration, credential, running server, or database was changed.
  Reference code was reviewed, not compiled or run. Security remains unimplemented;
  live practice and learner explanation are pending. Coverage stays 0/95 and 0/14.

### Request DTO answer reviewed - 2026-09-27

- Learner identified control over request fields, types, size, and nullability as
  benefits of `CreateProductRequest`. Exact answer and review are in
  [INTERVIEW-NOTES.md](INTERVIEW-NOTES.md).
- Clarified that entities can also have validation annotations; the separate DTO
  defines operation-specific input independently of persistence fields. Our
  request has no generated ID, and mapping to the new entity is explicit.
- Explained JSON conversion versus validation. Next prediction: valid JSON with
  negative price; does validation allow execution to reach the product service?
  The learner's prediction and live validation experiment remain pending.
- New features remain paused for the class/configuration walkthrough. This is a
  documentation-only review; no tests, API requests, or database changes were run.
  PDF coverage remains 0/95 and broader requirements complete remain 0/14.

### Restart report and request to understand the code - 2026-09-26

- Learner reports product 1 survived application restart and that stock went from
  10 to 8 after an order POST. Supplied product JSON: ID 1, Keyboard, price 1250.00,
  stock 8; HTTP 200 dated `Sat, 26 Sep 2026 02:50:33 GMT`.
- The phrase "order response with quantity 10" needs a terminology correction:
  product `stock` is current availability; order `quantity` is units bought in one
  order. The guided request asked for 2. Stock 8 is consistent with that request
  given the prior stock 10 and no other changes, but actual order ID, quantity,
  total, and POST response have not been reviewed.
- Learner explicitly asked to understand every existing class and configuration
  before proceeding. The trainer implemented too broad a slice before explaining
  it. Pause feature work and use the new file-by-file guide, request traces, and
  follow-up questions alongside the sequential technical notebook.
- Guide preparation and code audit do not mark the class walkthrough or learner
  explanations complete. Next discussion: object ownership and the product flow,
  then orders/transactions, configuration/errors, and test evidence.
- This increment changes documentation only. No application tests, API requests,
  or database mutations were run for this walkthrough. Historical 43-test and
  smoke-test results remain dated to the implementation verification.
- PDF coverage remains 0/95; broader requirements complete remain 0/14.

### First learner product request - reported 2026-09-26

- Learner supplied the response to the guided create-product request: HTTP 201,
  `Location: /api/products/1`, and JSON with ID 1, name Keyboard, price 1250.00,
  stock 10. The supplied HTTP Date header was `Fri, 25 Sep 2026 16:36:01 GMT`.
- This records a learner-reported successful create request, not an independent
  database inspection or evidence of completed restart/validation/transaction drills.
- Explained the created status, resource location, and returned fields. The learner's
  own explanation and the transaction prediction remain pending.
- Continue with product 1. Order totals depend on its price at order creation;
  do not assume the optional price-update request has been performed.
- Full PDF coverage remains 0/95; no broader requirement is marked complete.

### Day 1 implementation and verification - 2026-09-25

- Implemented the Day 1 feature scope requested by the learner: externalized
  message, product REST/validation/error contracts, PostgreSQL/Flyway/JPA,
  dirty-checking price change, and atomic single-product order creation.
- Preserved the learner's committed `LearningService - ` message prefix from
  baseline `a5c72db`, moving the value into configuration and aligning its HTTP test.
- Prepared project-local PostgreSQL 17.11 with a verified installer checksum;
  `orderflow` and `orderflow_test` are separate. Generated settings and data stay ignored.
- `./dev verify`: 43 tests, 0 failures/errors/skips. The rollback trigger verified
  the stock UPDATE was visible before raising the order-insert failure; after the
  service transaction ended, stock was unchanged and no order was stored.
- `python3 scripts/day1-smoke.py`: actual HTTP create/read/update, validation,
  404/409, server-calculated totals, restart persistence, and environment/CLI
  configuration overrides passed. Only its two temporary app processes were stopped.
- The project-local PostgreSQL server remains running for learner practice.
  Use `./scripts/db stop` after stopping the development application.
- [Day 1 lab evidence](labs/DAY1-order-flow.md), [lesson](lessons/003-day-one-order-flow.md),
  and [IntelliJ requests](../requests/day1.http) are ready.
- Learner Day 1 runs and explanations remain pending, including the transaction
  prediction requested during implementation. Do not mark the study day complete
  solely from these trainer checks. Actual learner study minutes are not measured.
- EXT01-05 are in progress; 0/95 PDF items and 0/14 broader areas are fully covered.

### Learner review and accelerated pace - 2026-09-25

- Learner correctly predicted that removing `@Service` prevents startup because
  the controller requires `LearningService`. The container resolves dependencies;
  the controller itself does not search for a bean. See the exact answer and review
  in [INTERVIEW-NOTES.md](INTERVIEW-NOTES.md).
- The local service initially had its annotation removed and its message changed.
  A later inspection showed `@Service` restored, with the message edit retained.
  These learner edits were preserved. They do not prove an observed startup failure
  or successful recovery; the learner's diagnostic and runtime results remain pending.
- Adopted a working three-session plan with 180 minutes each, including breaks.
  The breadth-first option is a stated working assumption pending learner preference.
  Nine-hour sprint completion and all-95-question curriculum completion are distinct.
- Rancher Desktop's Docker client was found, but its selected daemon was unreachable.
  Database/broker runtime readiness still needs verification during preparation.
- This increment changes documentation only. No new application feature or passing
  build is claimed for the learner's currently modified working tree.
- PDF questions Covered: 0/95; broader requirements complete: 0/14.
- Session minutes used: not measured yet. No elapsed study time is inferred from chat.

Record the learner's actual explanations and doubts here after the exercise.
Implement and explain small steps together; publish tested, finished increments
through commits to the GitHub repository as part of the learning workflow.


## Interview preparation slice — 2026-09-28

The learner explicitly requested implementation of product discounts and paid priority fees, plus broad architect-interview preparation. This narrowly resumes feature work for that request; earlier class walkthrough and learner checkpoints remain open. The learner chose priority fees after the discount.

See [pricing walkthrough](INTERVIEW-PRICING.md) and [design decision](decisions/0003-order-pricing-snapshots.md). Java 8 compatibility applies to the five dependency-free pricing/demo classes; the application remains Java 21 / Boot 4. Paid service choice is not verified membership. Broader security/distributed-system topics are prepared reference material, not completed implementations or learner-demonstrated skills.

Verification is recorded in [pricing evidence](labs/INTERVIEW-pricing.md). No PDF item or broader curriculum requirement is marked Covered by this preparation; learner practice and reviewed answers remain pending. Actual learner study time is not measured.
