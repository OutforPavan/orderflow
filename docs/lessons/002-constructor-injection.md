# Lesson 002 — Constructor injection

**Checkpoint:** implementation is prepared for practice; the learner correctly predicted missing-service startup failure on 2026-09-25. The review clarified that Spring's container resolves the dependency. Live failure/recovery and the remaining explanations are pending. Lesson 001's outstanding checkpoint remains open. This is the first small step in lab B03, not completion of the whole lab or any PDF question.

## The problem and the change

The controller now asks a `LearningService` for the learning message. It still handles the HTTP route and constructs the response record. This small refactor makes object collaboration visible before we add business rules.

A **dependency** is an object another object needs to do its work. **Dependency injection** means supplying that collaborator from outside. Our controller receives its service through its constructor; Spring supplies the argument when creating the controller. [Spring dependency injection](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)

In [LearningService.java](../../src/main/java/com/outforpavan/orderflow/learning/LearningService.java):

```java
@Service
public class LearningService {
    public String message() {
        return "Learning Spring Boot one step at a time";
    }
}
```

In [LearningController.java](../../src/main/java/com/outforpavan/orderflow/learning/LearningController.java), the relevant parts are:

```java
private final LearningService learningService;

public LearningController(LearningService learningService) {
    this.learningService = learningService;
}

@GetMapping("/api/learning/status")
public LearningStatus status() {
    return new LearningStatus("orderflow", learningService.message());
}
```

`@Service` is a specialized `@Component`: it identifies an application service and makes the class eligible for component scanning. It does not automatically add transactions, make a method asynchronous, or expose an HTTP endpoint. Our service is inside the application's scan boundary. [Component stereotypes](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html)

## What Spring does internally

For this application, Spring registers definitions for both classes. To construct `LearningController`, it resolves the constructor's `LearningService` parameter. It creates and initializes the service if needed, then passes the managed reference to the controller constructor. The assignment stores that reference for later calls. The dependency is available during controller construction. [Dependency resolution](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html#beans-dependency-resolution)

```text
Startup: Spring obtains LearningService → constructs LearningController(service)
Request: controller.status() → service.message() → response record → JSON
```

This controller has exactly one constructor, so Spring uses it without `@Autowired`. Constructor selection and dependency resolution are separate: knowing which constructor to call does not guarantee that its required arguments exist. [Single-constructor rule](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)

`final` prevents reassigning the controller's service field after construction. It does not make the referenced object's state immutable or thread-safe. Our service currently holds no mutable fields. Also, Java permits a caller to pass `null` manually; the constructor syntax alone is not a universal non-null guarantee.

## Why the MVC test imports the service

The test uses:

```java
@WebMvcTest(LearningController.class)
@Import(LearningService.class)
```

`@WebMvcTest` creates a focused MVC test context and normally excludes ordinary services from scanning. `@Import` explicitly includes our real, small service so the controller can be constructed. `MockMvc` exercises MVC handling without starting a listening HTTP server. Full-application startup checks serve a different purpose: they verify the application's normal wiring. [Spring Boot MVC testing](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests)

## Guided exercise — predict, break, restore

This exercise is **pending**. From the project directory:

```sh
./dev verify
./dev spring-boot:run
```

In another terminal:

```sh
curl -i http://localhost:8080/api/learning/status
```

Expect HTTP 200 and the existing JSON message. Then:

1. Stop the application. Temporarily remove only `@Service` from `LearningService`.
2. Predict whether the application starts. Run the full application again and read the dependency failure, including the missing type and requesting constructor.
3. Restore `@Service`, rerun verification, and confirm the endpoint works.
4. Explain why this differs from removing `@RestController` with no alternative registration.

Expected distinction: the missing service leaves a discovered controller whose required dependency cannot be resolved, so normal eager startup fails. Removing the controller annotation removes its handler from this application's scan-based setup; the app can start, but that route returns 404. The MVC slice's explicit service import can still register the service without `@Service`, so use full startup for this experiment.

Record the actual diagnostic and your explanation before closing the learner checkpoint. An expected result written here is not an observed result.

## Follow-up questions

<details>
<summary>1. Which object depends on which?</summary>

`LearningController` depends on `LearningService`. The controller uses the service to obtain its message.

</details>

<details>
<summary>2. Who calls the controller constructor?</summary>

Spring calls it during managed bean creation and supplies the resolved service reference. It is ordinary Java construction coordinated by the container.

</details>

<details>
<summary>3. Why is @Autowired unnecessary here?</summary>

There is one constructor. Spring selects it automatically, then resolves its parameter.

</details>

<details>
<summary>4. Why use a constructor instead of creating the service inside status()?</summary>

The dependency becomes explicit and supplied externally. The controller can also be constructed in a plain unit test with a chosen collaborator.

</details>

<details>
<summary>5. Does final make the service thread-safe?</summary>

No. It fixes the field's reference; thread safety depends on the service's state and behavior.

</details>

<details>
<summary>6. Why might the MVC slice succeed while application startup fails?</summary>

They register different sets of beans. Our slice explicitly imports the service; normal application startup relies on its component registration.

</details>
