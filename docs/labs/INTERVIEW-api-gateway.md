# API gateway: first routing and failure exercise

Date: 2026-10-07 (Asia/Kolkata).
Baseline: `2e1bd6e`.
Scope: EXT11, a first R01/X02 exercise; no full PDF question completion claimed.

## Requirement and mechanism

Run an independent Spring Cloud Gateway on loopback port 8090, forwarding only
product/order paths to the existing MVC backend on 8080. Keep the path, query,
method, body, status and relative Location contract. Add a generated request ID,
a 1-second connection timeout, a 3-second response wait, and no automatic retry.
Connection/DNS establishment failures map to 502; response timeout returns 504.
The gateway's health endpoint does not prove downstream readiness.

See [the lesson](../API-GATEWAY.md),
[design decision](../decisions/0004-separate-api-gateway.md),
[gateway sources](../../api-gateway/src/main/java/com/outforpavan/orderflow/gateway),
[configuration](../../api-gateway/src/main/resources/application.yml),
and [practice requests](../../requests/gateway.http).

## Trainer verification

Gateway verification on Java 21.0.12.1, Spring Boot 4.1.1, Spring Cloud
2025.1.3 / Gateway 5.0.3: `./dev -f gateway/pom.xml verify` passed **18 tests**,
0 failures/errors/skips, and produced the executable gateway JAR. These ran
against temporary loopback HTTP fixtures on random ports, not PostgreSQL.

Observed: product/order paths, query, methods, JSON bodies and relative Location
are forwarded; backend 400/404/409/500 responses are retained; unconfigured routes
and unexposed management paths return 404 without forwarding. Generated UUIDs
replace multiple forged caller/backend IDs and survive gateway error responses.
A 500ms test-only response limit beats a fixture's 2-second delay and returns 504.
A TCP port reserved without a listener returns 502 while gateway health stays UP.
A fixture consumes a POST then drops its response: exactly one downstream request
was recorded. This simulates an uncertain response, not a database commit.

The first execution was blocked by sandbox socket restrictions. An authorized
rerun exposed an overly strict health JSON assertion: Boot 4.1 also returns health
group names. The corrected test checks status UP and absence of component/details
instead of exact JSON text; all 18 then passed. This was a test assumption fix.

Existing backend regression: `./scripts/db start` followed by `./dev verify`
passed **74 tests**, 0 failures/errors/skips, and packaged the application. The
project-local PostgreSQL remained available for learner practice.

A separate read-only packaged-app smoke check started the actual MVC backend and
the gateway on random loopback ports. Product and order GETs for a nonexistent
ID passed through the gateway and returned exactly the same PostgreSQL-backed
404 bodies as direct backend calls. Gateway request IDs and local health were
verified, and `/api/learning/status` returned 404 through the gateway while the
backend endpoint was reachable. Only GETs were sent; no product/order rows were
created by this smoke check. Both temporary application processes were stopped.
The gateway JAR came from the verified staged build whose source matches the
installed gateway; backend verification ran in the actual project directory.

Reproduce the same read-only behavior after starting both apps with the lesson's
commands: compare direct port 8080 and gateway port 8090 GETs for
`/api/products/9223372036854775807` and `/api/orders/9223372036854775807`, inspect
`X-Request-Id`, then compare `/api/learning/status` on both ports. Product/order
creation through the real gateway remains the learner's next practice step;
creation forwarding was verified against the controlled HTTP fixture.

Review also corrected removed Boot 4 `server.error.*` settings to the actual
`spring.web.error.*` namespace and clarified that our request-ID log entry's
omission of query strings does not control separate framework error logs.
The final gateway verification passed all 18 tests after those corrections.

## Learner evidence and next experiment

Prediction supplied in chat: "No—first check the outcome or use an idempotency key".
Reviewed as correct: response loss/timeout does not prove rollback, and duplicate
prevention must cover the persisted order/stock effect. Current Orderflow does not
implement idempotency. A gateway timeout also does not guarantee backend cancellation.

Next: run both apps, create/read a product and order via the gateway, observe the
request ID, then compare an unconfigured route (404) with an unavailable backend
(502). Explain route vs predicate vs filter and why `/api` is retained.
Learner live practice and reviewed request-flow explanation remain pending.
No study time measured; no coverage item marked Covered.

## Limits

Local teaching foundation, not an internet-ready deployment. Authentication,
resource authorization, TLS, backend network isolation, distributed rate limiting,
circuit breakers, load balancing, tracing, and load/capacity validation are absent.
Request logging occurs at response commitment and measures time to headers, not
full streaming-body completion. This does not instrument backend logs or implement
W3C distributed tracing. The response wait is not a complete end-to-end deadline.
No traffic capacity or performance improvement is claimed.
