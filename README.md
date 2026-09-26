# Orderflow

A Java 21 learning project that will grow into an order and inventory backend.
We start from fundamentals, explain and implement together, and add one concept at a time.

## Current checkpoint

Day 1 implementation: product REST APIs, validated DTOs, PostgreSQL/Flyway/JPA,
and transactional order creation. Trainer verification on 2026-09-25 passed all
43 tests and the packaged HTTP/restart check. The learner created product 1,
reports that it survived application restart, and shared its subsequent stock-8
response after an order request. The actual order JSON and remaining drills have
not yet been reviewed.

**Current focus:** pause new features and explain the existing classes and
configuration using the [code walkthrough](docs/CODE-WALKTHROUGH.md). Working code
does not establish learner understanding.

The learner has run the original endpoint and correctly predicted missing-service
startup failure. See the [Day 1 lesson](docs/lessons/003-day-one-order-flow.md).

Current pace: [three days, three hours per day](docs/THREE-DAY-SPRINT.md), organized
around a narrow end-to-end implementation. Full PDF coverage remains tracked separately.

- Java 21; Spring Boot 4.1.1; Maven Wrapper.
- `GET /api/learning/status` returns an externally configured learning message.
- Product creation/read/price changes and order creation/read use a real PostgreSQL database.
- Focused MVC checks and database integration tests exercise validation and rollback.
- Future features are described in the [roadmap](docs/ROADMAP.md).

## Run on this machine

```sh
cd /Users/pavtiwar/orderflow
./dev --version
./scripts/db start
./dev verify
./dev spring-boot:run
```

In a second terminal:

```sh
curl -i http://localhost:8080/api/learning/status
```

Expected status: `200 OK`. Expected JSON:

```json
{"application":"orderflow","message":"LearningService - Learning Spring Boot one step at a time"}
```

Stop the server with Ctrl+C. If port 8080 is occupied, run
`./dev spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
and use port 8081 in the URL.

Open [requests/day1.http](requests/day1.http) in IntelliJ and run its requests in
order. Each run of the create-product request gives a new ID. The database helper
keeps local tools, data, and generated connection settings under ignored `.tools/`.
Its application and test databases are separate. After stopping the application,
use `./scripts/db stop` to stop only this project's database.

On this machine, PostgreSQL 17.11 is installed locally and ready. On another Mac,
run `./scripts/db-setup` once to install the pinned, checksum-verified distribution.
On another supported OS, install PostgreSQL 17 and set `ORDERFLOW_PG_BIN` to its
binary directory before `./scripts/db start`. The helper creates a local teaching
cluster whose owner role is a superuser; production role separation is not implemented.

After packaging, `python3 scripts/day1-smoke.py` verifies real HTTP behavior,
configuration overrides, and persistence across an application restart. It uses
`orderflow_test`, leaves labeled inspection rows, and stops its own app processes.

To see SQL, run `./dev spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=sql`.
To override the lesson message, run
`./dev spring-boot:run '-Dspring-boot.run.arguments=--learning.message=Configured-from-the-command-line'`.
Normal file edits take effect after restart in this project.

The `dev` helper selects the project-local Java 21 JDK under
`.tools/java21/Contents/Home`, then invokes the standard Maven Wrapper.
It keeps Maven downloads under `.tools/`; these are ignored by Git.
The project Maven settings use the default public repositories, isolating this
learning project from machine-wide repository settings.
This machine uses Eclipse Temurin 21.0.12.1+1 (macOS Apple Silicon), verified
against the SHA-256 published by Adoptium when downloaded.

On another machine, install JDK 21, set `JAVA_HOME`, prepare PostgreSQL as above,
and run `./dev verify`
(macOS/Linux), or use `mvnw.cmd verify` on Windows.
The first run needs internet access for Maven and dependencies.

In IntelliJ IDEA, open `pom.xml`, choose JDK 21 as the Project SDK and Maven
runner JRE. On this machine the JDK is at
`/Users/pavtiwar/orderflow/.tools/java21/Contents/Home`.

## Learn in small steps

1. Walk through the [current classes and configuration](docs/CODE-WALKTHROUGH.md) together before continuing [Day 1 practice](docs/lessons/003-day-one-order-flow.md); revisit [constructor injection](docs/lessons/002-constructor-injection.md) for object wiring.
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

- [Lesson 002 implementation and verification record](docs/labs/B03-001-constructor-injection.md)
- [Day 1 implementation and verification record](docs/labs/DAY1-order-flow.md)
- [Every current class and configuration: guided walkthrough](docs/CODE-WALKTHROUGH.md)
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
