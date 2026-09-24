# Learning progress

## Current position

- Environment and minimal application baseline: prepared.
- Baseline: Java 21, Spring Boot 4.1.1, Maven, one HTTP endpoint.
- Repository: [OutforPavan/orderflow](https://github.com/OutforPavan/orderflow).
- Lesson 001: awaiting the learner's exercise and explanation.
- Understanding demonstrated: not assessed yet.
- Next concept after lesson 001: constructor injection in lesson 002.

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

- [ ] Learner starts the application and calls the endpoint.
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

## Open questions and next steps

Record the learner's actual explanations and doubts here after the exercise.
Implement and explain small steps together; publish tested, finished increments
through commits to the GitHub repository as part of the learning workflow.
