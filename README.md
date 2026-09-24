# Orderflow

A Java 21 learning project that will grow into an order and inventory backend.
We start from fundamentals, explain and implement together, and add one concept at a time.

## Current checkpoint

Lesson 001 baseline: one Spring Boot application and one JSON endpoint.
The learner exercise and explanation are still pending.

- Java 21; Spring Boot 4.1.1; Maven Wrapper.
- `GET /api/learning/status` returns a fixed learning message.
- Tests cover application startup and the HTTP response contract.
- Future features are described in the [roadmap](docs/ROADMAP.md).

## Run on this machine

```sh
cd /Users/pavtiwar/orderflow
./dev --version
./dev verify
./dev spring-boot:run
```

In a second terminal:

```sh
curl -i http://localhost:8080/api/learning/status
```

Expected status: `200 OK`. Expected JSON:

```json
{"application":"orderflow","message":"Learning Spring Boot one step at a time"}
```

Stop the server with Ctrl+C. If port 8080 is occupied, run
`./dev spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
and use port 8081 in the URL.

The `dev` helper selects the project-local Java 21 JDK under
`.tools/java21/Contents/Home`, then invokes the standard Maven Wrapper.
It keeps Maven downloads under `.tools/`; these are ignored by Git.
The project Maven settings use the default public repositories, isolating this
learning project from machine-wide repository settings.
This machine uses Eclipse Temurin 21.0.12.1+1 (macOS Apple Silicon), verified
against the SHA-256 published by Adoptium when downloaded.

On another machine, install JDK 21, set `JAVA_HOME`, and run `./dev verify`
(macOS/Linux), or use `mvnw.cmd verify` on Windows.
The first run needs internet access for Maven and dependencies.

In IntelliJ IDEA, open `pom.xml`, choose JDK 21 as the Project SDK and Maven
runner JRE. On this machine the JDK is at
`/Users/pavtiwar/orderflow/.tools/java21/Contents/Home`.

## Learn in small steps

1. Read [Lesson 001](docs/lessons/001-first-spring-boot-application.md).
2. Run the baseline, predict the exercise outcome, and make the small change.
3. Review the result together and explain it in your own words.
4. Run the tests, update the [progress log](docs/PROGRESS.md), and commit/push.

Keep source code, tests, lessons, and design decisions in Git. Credentials,
local tool installations, IDE state, and generated build output stay outside commits.

## Learning records

The living notes are maintained alongside each lesson. The completion requirement
covers every question in the three supplied PDFs plus the broader curriculum;
each item needs live practice, implementation or diagnostic evidence, and a
reviewed explanation.

- [Technical notebook: internals and follow-up answers](docs/TECHNICAL-NOTEBOOK.md)
- [All 95 PDF questions and completion tracker](docs/PDF-COVERAGE.md)
- [Practical lab catalog](docs/LAB-CATALOG.md)
- [Roadmap](docs/ROADMAP.md)
- [Progress](docs/PROGRESS.md)
- [Interview practice](docs/INTERVIEW-NOTES.md)
- [Why we start with one application](docs/decisions/0001-start-with-one-application.md)

The aim is to explain mechanisms, diagnose failures, and defend tradeoffs with
evidence. Completing a checklist alone does not establish interview readiness.

## References

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot first application](https://docs.spring.io/spring-boot/tutorial/first-application/index.html)
- [Adoptium archive installation](https://adoptium.net/installation/archives)
