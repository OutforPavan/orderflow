# Lesson 001 — Start a Spring Boot application

**One goal:** run a Java application that answers an HTTP request, and identify
the code we wrote versus the infrastructure Spring Boot supplies.

This lesson describes the first baseline (`lesson-001-start`). Since Lesson 002,
the controller receives a `LearningService` and delegates message creation to it.
Use [Lesson 002](002-constructor-injection.md) for the current wiring. The original
message-change exercise below is still available using the service's message method.

## The problem

A client asks our program for information over HTTP. Java supplies the language
and runtime, but a web application also needs a server, request routing, and a
way to turn Java values into an HTTP response.

Spring Framework provides infrastructure including an object container and
Spring MVC request handling. Spring Boot assembles that infrastructure with
dependency starters and conditional configuration. It still runs as a Java program.

## Read these pieces together

`pom.xml` is Maven's build description. `java.version` targets Java 21.
The Boot parent manages compatible dependency versions; the web MVC starter
brings in the web stack, including an embedded Tomcat server.

`OrderflowApplication.main()` is the entry point:

```java
SpringApplication.run(OrderflowApplication.class, args);
```

This starts Spring's application context. Think of the context as the registry
and lifecycle manager for objects Spring manages; those objects are called beans.
With this project's dependencies and configuration it also starts the web server.

`@SpringBootApplication` combines three capabilities: application configuration,
component scanning, and auto-configuration. Scanning discovers our annotated
classes under `com.outforpavan.orderflow`; auto-configuration conditionally supplies
infrastructure based on dependencies, configuration, and existing beans.

In `learning/LearningController.java`:

- `@RestController` makes the class discoverable as a controller whose return
  values become response bodies.
- `@GetMapping("/api/learning/status")` maps an HTTP GET request to `status()`.
- The Java record carries two values. Spring MVC's message conversion writes
  those values as JSON using the configured JSON support.

A record is a compact Java data carrier. It is useful here for a response;
it is not a JPA entity. We will discuss data modeling later.

The request path for this baseline is:

```text
curl → embedded Tomcat → Spring MVC DispatcherServlet
     → LearningController.status() → JSON response
```

We did not call `new LearningController()` in main. Spring discovers and manages
that controller. In Lesson 002 we will introduce a second object and learn how
Spring supplies it through a constructor: dependency injection.

## Run and observe

From the project directory, run `./dev spring-boot:run`. Observe the Java version
and server port in the startup log. From another terminal run:

```sh
curl -i http://localhost:8080/api/learning/status
```

Identify the status code, content type, and the JSON fields.
These are three different parts of the response.

## Exercise — we do this next, together

Before editing, predict what will change if the message text changes.

1. Change the returned message in `LearningService.message()` to `My first Spring Boot application` (the initial baseline stored it directly in the controller).
2. Restart the application and call the endpoint again. We have not added hot reload.
3. Run `./dev test`. Explain why the HTTP contract test now fails.
4. Update that test's message expectation to match the deliberate contract change.
5. Run `./dev verify`, then record the result and your explanation in the progress log.

Do not implement extra endpoints yet. The useful outcome is understanding this
small request path and the relationship between a change and its test.

## Understanding checkpoint

Answer in your own words:

1. Where does this Java program start?
2. Who creates `LearningController`, and how is it found?
3. What would you investigate if the process starts but this URL returns 404?
4. What does Spring Boot configure, and what application behavior did we write?

A strong answer traces the mechanism and offers a way to verify it. We will
review your answer before moving to constructor injection.

## What the baseline tests establish

`OrderflowApplicationTests` starts the Spring context and checks it can load.
`LearningControllerTest` uses a Spring MVC test slice to verify routing, status,
content type, and JSON values. MockMvc does not open a real network port.
Running the application and using curl separately verifies the real HTTP path.

## Official reading (optional after the experiment)

- [First application](https://docs.spring.io/spring-boot/tutorial/first-application/index.html)
- [What @SpringBootApplication enables](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html)
- [Package structure and component scanning](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html)
