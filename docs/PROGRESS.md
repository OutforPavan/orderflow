# Learning progress

## Current position

- Environment and minimal application baseline: prepared.
- Baseline: Java 21, Spring Boot 4.1.1, Maven, one HTTP endpoint.
- Repository: [OutforPavan/orderflow](https://github.com/OutforPavan/orderflow).
- Lesson 001: learner confirmed receiving the endpoint JSON on 2026-09-25;
  message-change exercise and reviewed explanation remain pending.
- Lesson 002: constructor-injection implementation prepared and trainer-verified.
- Understanding demonstrated: explanations have not been assessed yet.
- Current next step: learner traces the controller/service constructor wiring and
  performs the guided missing-service exercise before advancing further.

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

Record the learner's actual explanations and doubts here after the exercise.
Implement and explain small steps together; publish tested, finished increments
through commits to the GitHub repository as part of the learning workflow.
