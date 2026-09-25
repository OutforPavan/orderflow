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
