# Decision 0004: keep the API gateway in a separate application

Status: accepted for the learner's explicit API-gateway interview lesson,
2026-10-07. Learner execution and full request-path explanation remain pending.

## Context

Orderflow is one MVC/JPA application with product and order packages and a shared
transactional database. The learner wants a practical Spring Boot API-gateway
example with production failure reasoning. Adding gateway responsibilities to a
business controller would conceal the network boundary; splitting product/order
storage would introduce unrelated distributed transactions and expand the lesson.

## Decision

Add an independent Maven application under `gateway/`, using Java 21, Spring Boot
4.1.1, Spring Cloud BOM 2025.1.3, and its Gateway Server WebFlux 5.0.3. Keep the
existing application and its blocking JPA transactions in their own JVM. The
Cloud release explicitly [supports Boot 4.1](https://spring.io/blog/2026/08/20/spring-cloud-2025-1-3-has-been-released/).
Retain the root build rather than converting it into an aggregator in this lesson;
verify the two applications with separate commands.

Bind the gateway to loopback port 8090 by default. Explicitly forward product and
order path families to a configured fixed backend URL without path rewriting.
Do not proxy learning endpoints. Generate an edge request ID, bound connection
and response waiting, translate recognized connection/DNS failures to 502, and
preserve the native response-timeout 504. Expose only the gateway health endpoint
with no details.

Disable Gateway retries by omitting a Retry filter and Reactor Netty's transport
retry by setting `disableRetry(true)`. An order can commit before a response is
lost; transparent replay would risk a second business effect. Durable service-side
idempotency is the appropriate later protection. A gateway timeout cannot roll
back another application's database transaction.

## Consequences and alternatives

The boundary can be started, tested, failed, and deployed independently. The
gateway stays free of blocking business/database work, and the backend retains
its current API and transaction contract. Costs include another process, hop,
configuration surface, and failure point. Separate verification is required;
running only root `./dev verify` does not verify the gateway.

A single service could reasonably use direct access or a reverse proxy supplied
by its deployment platform; an application gateway is not inherently necessary.
Spring Cloud Gateway's MVC variant is another possible choice. We choose WebFlux
here to learn a dedicated reactive proxy and its event-loop constraint while
preserving the familiar MVC business service. This is not a claim that reactive
code is always faster or that MVC gateways are invalid.

The fixed URL is sufficient for this lesson and does not demonstrate discovery
or load balancing. No authentication, TLS, rate limiter, circuit breaker,
distributed tracing, ingress restrictions, or high-availability deployment is
installed by this decision. Local direct backend access remains possible, so the
gateway is not yet an enforced security boundary. Full-body latency, cancellation,
capacity, and streaming behavior need additional evidence before deployment.

See the [lesson](../API-GATEWAY.md) and [verification record](../labs/INTERVIEW-api-gateway.md).
