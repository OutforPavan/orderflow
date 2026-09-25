# B03 / 001 - A required service supplied through a constructor

Date: 2026-09-25.
State: implemented and trainer-verified; learner practice and explanation pending.

## Scope and baseline

This is the first small constructor-injection exercise under EXT01. It prepares
for P3-Q81 and P4-S20, whose multiple-candidate scenarios are still unimplemented.
Neither PDF question is marked covered by this exercise.

Baseline commit: `7b5df71` (application behavior originally established in `3513f6a`).
Implementation checkpoint: [lesson-002-constructor-injection](https://github.com/OutforPavan/orderflow/tree/lesson-002-constructor-injection).

## Requirement and prediction

The controller should receive a Spring-managed service through its constructor,
then obtain the existing message from that service. Expected API behavior:
HTTP 200 and the same `application`/`message` JSON fields. A registered controller
with no candidate for its required service should fail during normal eager creation.

## Code changed

- [LearningService](../../src/main/java/com/outforpavan/orderflow/learning/LearningService.java):
  an `@Service` component whose `message()` method supplies the text.
- [LearningController](../../src/main/java/com/outforpavan/orderflow/learning/LearningController.java):
  one constructor accepting the service and a final reference used by `status()`.
- [MVC contract test](../../src/test/java/com/outforpavan/orderflow/learning/LearningControllerTest.java):
  explicitly imports the real service in its focused MVC context.
- [Missing-dependency test](../../src/test/java/com/outforpavan/orderflow/learning/LearningDependencyInjectionTest.java):
  registers only the controller in an isolated annotation-based context.

## Observed verification

Environment: Eclipse Temurin Java 21.0.12.1, Maven Wrapper 3.9.16,
Spring Boot 4.1.1 / Framework 7.0.9. No new dependency was added.

Command from the repository root:

```sh
./dev verify
```

Observed: **3 tests, 0 failures, 0 errors, 0 skipped**; executable JAR packaged.

The negative test observed `UnsatisfiedDependencyException` with
`NoSuchBeanDefinitionException` as its root cause. Its diagnostic identified
constructor parameter 0 and the missing `LearningService` type. The exception is
the expected assertion, so the test passes. The context closes after the test.

The existing context-load test passed with normal application component scanning.
The MVC contract test passed with the explicitly imported service.

A separate trainer smoke check launched the packaged JAR on an automatically
assigned loopback port, called the endpoint, and observed:

```json
{"application":"orderflow","message":"Learning Spring Boot one step at a time"}
```

HTTP status was 200 and content type was `application/json`. The temporary
verification server was then stopped. To repeat through the normal development
workflow, run `./dev spring-boot:run` and call
`curl -i http://localhost:8080/api/learning/status` in another terminal.

## What this does and does not establish

The tests exercise normal wiring, the HTTP contract, and failure when a required
service is absent from an explicitly configured context. The negative test does
not physically remove `@Service`; the guided full-application experiment for that
change is still pending. The MVC slice's explicit `@Import` can register the service
even without its component stereotype, so it cannot alone prove scanning works.

This small service exists to make dependency injection visible. Its responsibility
is deliberately tiny; it is not a claim that every constant needs a service class.
Interface-based alternatives, ambiguous candidates, qualifiers, optional injection,
and circular dependencies remain later exercises.

## Learner checkpoint

- [x] Learner reported receiving the original endpoint's JSON before this refactor.
- [ ] Learner restarts the updated application and traces controller-to-service calls.
- [ ] Learner predicts and observes missing-service failure in full application startup.
- [ ] Learner restores the annotation and verifies recovery.
- [ ] Learner explains constructor selection, dependency resolution, and test boundaries.

Review outcome: pending. Use the [guided lesson](../lessons/002-constructor-injection.md)
and record the learner's own explanation in [INTERVIEW-NOTES.md](../INTERVIEW-NOTES.md).
