# Interview notebook

Use this notebook for explanations the learner has practiced and reviewed.
An implemented feature is evidence to discuss, not proof of understanding.

For trainer-prepared explanations and collapsible reference answers, use the
[technical notebook](TECHNICAL-NOTEBOOK.md). Keep this file for the learner's own
attempts, corrections, and reviewed answers. Link each relevant PDF question ID
from [the coverage tracker](PDF-COVERAGE.md) when recording an answer.

## Answer structure

1. **Problem:** What requirement or observed failure motivated the change?
2. **Mechanism:** What happens at runtime, and which component owns it?
3. **Failure:** Under what conditions does the behavior break or become surprising?
4. **Tradeoff:** Why this design, and when would another choice be better?
5. **Evidence:** Which experiment, test, SQL statement, or metric supports the claim?

Aim for a clear two-minute explanation, then answer deeper follow-up questions.
Distinguish what this project demonstrates from production experience.

## Lesson 001 — application bootstrap and HTTP endpoint

Status: awaiting the learner's explanation.

- What happens when `OrderflowApplication.main` runs?
- How does a request reach the method serving `/api/learning/status`?
- How does the method's return value become JSON?
- Which behavior comes from the application code and which from Spring Boot?
- What would you inspect first if the endpoint returned 404?

Learner's answer, reviewed corrections, and supporting observations: pending.


## Lesson 002 - constructor injection

Status: implementation demonstrated by the trainer; one learner prediction reviewed.
Related scope: EXT01 and the first B03 exercise. P3-Q81/P4-S20 concern ambiguity
and are not yet completed by this single-candidate example.

- Which object depends on which, and who supplies the constructor argument?
- Why is `@Autowired` unnecessary on this particular constructor?
- What changes if the required service is not registered?
- Why can `@WebMvcTest` with an explicit service import differ from full startup?
- What does `final` guarantee, and what does it not guarantee?

### Reviewed prediction - 2026-09-25

Prompt: If `@Service` is removed while the controller still requires the service,
would the application start successfully?

Learner's answer:

> if we remove @`Service` then application won't start because controller is having a bean dependency in constructor and that bean `LearningService` could not be found by controller.

Review: the predicted result and required-dependency reasoning are correct for
our current scan-based application. Precision correction: **Spring's container**
cannot resolve the service while creating the controller; the controller does not
perform a bean lookup. With no alternative registration, creation fails during
startup. Merely having the class in the source tree does not register a bean.

Local annotation removal and its later restoration were observed, but the learner's
startup diagnostic and successful runtime recovery have not been reported. Other questions above remain
pending. Follow-up to revisit: would explicit registration through `@Bean` or
`@Import` change the outcome? Reference answers remain in
[Lesson 002](lessons/002-constructor-injection.md).

## Day 1 - request to durable order

Status: implementation verified; learner shared product creation, reports restart
persistence, and supplied a later product response with stock 8 after an order POST.
New features are paused for the requested class/configuration walkthrough. A first
DTO-design answer was reviewed on 2026-09-27; remaining practical drills and
explanations are pending.
Use [the lesson](lessons/003-day-one-order-flow.md) and
[observed verification](labs/DAY1-order-flow.md) as references.

- Why keep a request DTO separate from a JPA entity?
- Where does request validation run, and why also keep database constraints?
- Who implements `ProductRepository`, and what does Hibernate do?
- Why can `changePrice` persist an update without another `save` call?
- Why does the order service own the transaction instead of only the controller
  or each repository method independently?
- A product starts with stock 10; reserving 3 succeeds but saving the order fails.
  What stock remains after one transaction rolls back, and why?
- How do the tests prove that stock SQL ran, and that the observation is after
  the actual service transaction ended?
- Why does this atomicity test not prove protection against overselling or duplicate retries?

The transaction prediction was requested during implementation; response and
review are pending. No Day 1 understanding or full PDF scenario is marked complete.

### First product response - reported 2026-09-26

Learner supplied HTTP 201, `Location: /api/products/1`, and the JSON product
`{id: 1, name: Keyboard, price: 1250.00, stock: 10}`. The response body is recorded
verbatim in the lab evidence. This is an execution result, not an interview answer.
Next explanation to review: why should the same product remain after restarting
only the Java application? The learner's answer is pending.

### Code and configuration walkthrough requested - 2026-09-26

Learner reports the product survived restart and stock decreased from 10 to 8
after an order POST. The supplied HTTP 200 JSON is a product response, not an
order response. Review the distinction between available `stock` and an order's
`quantity`; do not infer a reviewed order ID, quantity, or total from this evidence.

Learner asked to understand every existing class and configuration because the
implementation had moved ahead of the explanations. Use
[CODE-WALKTHROUGH.md](CODE-WALKTHROUGH.md) for the prepared file-by-file answers.
First prompts to review, one at a time:

- Why do `CreateProductRequest`, `Product`, and `ProductResponse` exist separately?
- Which objects does Spring create, and which does our code or JSON/JPA infrastructure create?
- Trace the existing product POST through its classes and identify where the transaction completes.

First DTO-design answer reviewed below on 2026-09-27. Other answers remain pending.
The trainer's reference guide is not a substitute for learner evidence.

### Request DTO reasoning reviewed - 2026-09-27

Prompt: Why would accepting `Product` directly as POST input be less controlled
than accepting `CreateProductRequest`?

Learner's answer:

> I think because accepting `CreateProductRequest` gives us more control in request attribute properties ,size and their data types ,null , non null.

Review: correctly identifies control over accepted fields, their types, and
request validation rules. Refinement: an entity can also carry validation
annotations. A separate request type defines this operation's input independently
of the persistence model. Our create request accepts name, price, and stock; it
has no product ID component. The service constructs a new entity from those
explicit values, and the database generates its ID. This is not a claim that all
unknown JSON properties necessarily produce an error.

The trainer explained the next distinction: `@RequestBody` asks for JSON conversion;
`@Valid` requests validation of the converted object before the controller body
runs. Field constraints do not execute merely because the record is constructed.

Next checkpoint: for valid JSON containing `"price": -10`, what do `@RequestBody`
and `@Valid` each do, and will `ProductService.create()` run?
Learner prediction and review: pending. No new runtime experiment was performed;
this answer does not complete the walkthrough, Day 1, or a PDF scenario.


## Interview preparation slice — 2026-09-28

The learner explicitly requested implementation of product discounts and paid priority fees, plus broad architect-interview preparation. This narrowly resumes feature work for that request; earlier class walkthrough and learner checkpoints remain open. The learner chose priority fees after the discount.

See [pricing walkthrough](INTERVIEW-PRICING.md) and [design decision](decisions/0003-order-pricing-snapshots.md). Java 8 compatibility applies to the five dependency-free pricing/demo classes; the application remains Java 21 / Boot 4. Paid service choice is not verified membership. Broader security/distributed-system topics are prepared reference material, not completed implementations or learner-demonstrated skills.

Verification is recorded in [pricing evidence](labs/INTERVIEW-pricing.md). No PDF item or broader curriculum requirement is marked Covered by this preparation; learner practice and reviewed answers remain pending. Actual learner study time is not measured.
