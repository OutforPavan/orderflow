# 0001 — Start with one application

Date: 2026-09-24
Status: accepted

## Context

Orderflow is a progressive learning project covering Spring Boot, JPA,
transactions, security, caching, Kafka, and eventually microservices. The learner
wants explanations and implementation together, beginning with fundamentals.

## Decision

Begin with one deployable Java 21 application using Spring Boot 4.1.1 and Maven.
The first lesson contains one static HTTP endpoint. Introduce a service when
constructor injection becomes the subject of lesson 002.

Add persistence, security, infrastructure, and application boundaries when their
requirements become concrete. Consider service extraction after observing the
costs and failure modes of communication between processes.

## Consequences

Each change can be explained using a small amount of existing code. Early setup
is simple, and later design choices have an observable reason. Service extraction
will require deliberate refactoring and introduce additional operational work.
