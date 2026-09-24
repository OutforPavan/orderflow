# Orderflow learning roadmap

Build an order and inventory application with Java 21, beginning with Spring Boot
4.1.1. Learn one concept at a time by explaining it, implementing a small change
together, and observing the result. Later lessons may change as questions arise.

## Working rhythm

1. Describe one problem and predict the behavior.
2. Explain the concept using the code currently in the repository.
3. Implement one small change together.
4. Run the application or a focused test; inspect failures when relevant.
5. Explain the mechanism, a failure mode, a tradeoff, and the evidence.
6. Review the explanation and commit the tested, finished increment to GitHub.

Advance when the learner can explain and apply the concept. Committed code alone
does not establish understanding. Keep unfinished questions in `PROGRESS.md`.

## First lessons

| Lesson | One concept | Small exercise |
| --- | --- | --- |
| 001 | Application bootstrap and one HTTP endpoint | Run `/api/learning/status`, change its message, and trace the request |
| 002 | Constructor injection | Introduce a small service and explain who creates and connects its instances |
| 003 | External configuration | Configure a value and observe which configuration wins |
| 004 | REST resources and DTOs | Introduce an in-memory product catalog and describe its HTTP contract |
| 005 | Input validation | Reject invalid product input and explain where validation runs |
| 006 | Consistent error responses | Turn one expected failure into an intentional HTTP response |
| 007 | Test boundaries | Compare a plain Java unit test with an HTTP-focused Spring test |

Gate: explain the request path, beans, dependency injection, component scanning,
starters, and auto-configuration using this application rather than definitions.

## Persistence and correctness

| Lesson | One concept | Required observation |
| --- | --- | --- |
| 008 | Database persistence and migrations | A product survives restart; the schema change is reproducible |
| 009 | JPA entity lifecycle | Inspect SQL and explain the persistence context and dirty checking |
| 010 | Transaction atomicity | Order creation and inventory reservation roll back together |
| 011 | Transaction proxies | Demonstrate why self-invocation can bypass transactional advice |
| 012 | Rollback rules and propagation | Predict one exception scenario and one nested service call |
| 013 | Flush versus commit | Observe when SQL executes and when a transaction becomes committed |
| 014 | Relationships and fetching | Reproduce and fix an N+1 query without hiding its query cost |
| 015 | Isolation and concurrent updates | Reproduce a race between two orders for the last item |
| 016 | Locking | Prevent overselling and defend the selected locking approach |
| 017 | Request idempotency | Retry an order request without creating a second order |

Gate: diagnose a failed order from SQL and transaction behavior; explain atomicity,
isolation, concurrency, retries, and the limits of the chosen guarantees.

## Security, operation, and scale

| Stage | Separate lessons, introduced gradually | Exit evidence |
| --- | --- | --- |
| Spring Security | Filter chain; authentication; authorization; ownership checks; session/token tradeoffs | Explain identity propagation and test unauthenticated, forbidden, and cross-customer requests |
| Operations | Structured logs; health; metrics; deployment configuration | Diagnose an introduced failure from evidence and distinguish liveness from readiness |
| Cache | Cache-aside reads; keys and TTL; invalidation; cache failures | Demonstrate stale data and defend consistency and failure behavior |
| Kafka | Events; topics and partitions; consumer groups; offsets; duplicate processing | Explain ordering and delivery semantics and demonstrate an idempotent consumer |
| Reliable publication | Database/message dual-write failure; transactional outbox; recovery | Reproduce the failure and show eventual recovery without duplicate business effects |
| Microservices | Boundary selection; one service extraction; timeouts; eventual consistency; compensation | Defend the extraction against its operational cost and explain remote failure handling |

The interview target is sound engineering judgment at a senior level. For each
stage, explain what was built, why it behaves that way, what can fail, and which
experiment supports the claim. Completing a checklist does not guarantee mastery.
