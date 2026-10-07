# Question 1: API gateway with Spring Boot

Prepared for the Capgemini client-round interview, 2026-10-07. This lesson adds a
small working gateway foundation with production concerns made explicit. It is
not a production-ready deployment. See the [verification and practice record](labs/INTERVIEW-api-gateway.md)
for actual observations; reading this guide does not complete learner practice.

## 1. The problem it solves

An API gateway is the HTTP entry point that accepts a client request, selects a
backend, forwards the request, and returns the backend's response. It gives clients
a stable address and a place to apply shared traffic policies. Authentication,
quotas, and monitoring are common policies, but installing a gateway does not
automatically implement them.

Our first request path is:

```text
curl / IntelliJ
       |
       | POST http://127.0.0.1:8090/api/orders
       v
Gateway: Spring Cloud Gateway Server WebFlux / Netty
       | route match, request ID, bounded downstream wait
       | POST http://127.0.0.1:8080/api/orders
       v
Orderflow: Spring MVC -> OrderService -> JPA -> PostgreSQL
       | validate, lock stock, calculate price, commit order
       v
Response travels back through the gateway to the caller
```

The gateway does not calculate prices or update inventory. The existing
`OrderService` transaction still owns the stock change and order insert. There
is one business service behind this gateway; product and order routes do not
turn its two Java packages into separate microservices.

## 2. Three terms to explain in an interview

| Term | Meaning | Orderflow example |
| --- | --- | --- |
| Route | A named forwarding rule with a destination and matching conditions. | `orderflow-orders` targets the configured Orderflow URL. |
| Predicate | A condition that decides whether a request matches a route. | `Path=/api/orders,/api/orders/**` accepts the collection and nested paths. |
| Filter | Code that participates in request/response processing. | Add a generated request ID before forwarding and return that ID to the caller. |

Spring's route handler finds a matching route and executes its ordered filter
chain around the downstream call. [Gateway glossary](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/glossary.html),
[request flow](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/how-it-works.html).

Our `RequestIdFilter` specifically uses WebFlux's `WebFilter`, so it also covers
local health requests and unmatched paths. A Gateway `GlobalFilter` belongs to
the chain for matched gateway routes; it would not alone cover those local cases.
[Gateway global filters](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/global-filters.html).

## 3. Why the gateway has its own application

The existing application under `src/main/java/com/outforpavan/orderflow` uses
Spring MVC and blocking JPA/JDBC. The new [gateway](../gateway/pom.xml) is an
independently built and started Spring Boot application under `gateway/`, with
reactive WebFlux/Netty and no database dependency. HTTP connects them; the backend
does not have to become reactive. Do not call JPA, `Thread.sleep`, or `.block()`
inside a reactive gateway filter: occupying an event-loop thread delays unrelated
requests that share it.

The gateway POM uses Java 21, Boot 4.1.1, and Spring Cloud BOM 2025.1.3, which
manages Gateway 5.0.3. The [official release announcement](https://spring.io/blog/2026/08/20/spring-cloud-2025-1-3-has-been-released/)
confirms compatibility with Boot 4.1. Use the BOM rather than choosing unrelated
Cloud component versions. See [Gateway's WebFlux starter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/starter.html).

This adds another process, network hop, configuration, and possible failure point.
For a small single backend, a gateway is optional; here the separate application
makes that boundary observable for the requested lesson. The
[architecture decision](decisions/0004-separate-api-gateway.md) records the tradeoff.

## 4. Walk through the small implementation

Read these files in order:

| File | What to understand |
| --- | --- |
| [gateway/pom.xml](../gateway/pom.xml) | Independent build, Cloud BOM, Gateway WebFlux starter, Actuator, and tests. Root `./dev verify` and gateway verification are separate commands. |
| [GatewayApplication](../gateway/src/main/java/com/outforpavan/orderflow/gateway/GatewayApplication.java) | A second Boot entry point, started in a second JVM. |
| [application.yml](../gateway/src/main/resources/application.yml) | Listener, explicit routes, connection/response timeouts, and health exposure. |
| [RequestIdFilter](../gateway/src/main/java/com/outforpavan/orderflow/gateway/RequestIdFilter.java) | Generate a UUID, replace the incoming header, and register work immediately before response headers are committed. |
| [GatewayHttpClientConfiguration](../gateway/src/main/java/com/outforpavan/orderflow/gateway/GatewayHttpClientConfiguration.java) | Customize Gateway's existing HTTP client to disable transport retries. |
| [GatewayConnectionErrorHandler](../gateway/src/main/java/com/outforpavan/orderflow/gateway/GatewayConnectionErrorHandler.java) | Translate connection/DNS exceptions into HTTP 502 before Boot renders its error response. |

The configuration uses the Gateway 5 prefix
`spring.cloud.gateway.server.webflux`. It declares these route boundaries:

| Incoming path on port 8090 | Behavior |
| --- | --- |
| `/api/products` or `/api/products/**` | Forward to Orderflow, preserving the path. |
| `/api/orders` or `/api/orders/**` | Forward to Orderflow, preserving the path. |
| `/api/learning/**` | No gateway route: 404. The backend's existing learning endpoint still exists. |
| `/actuator/health` | Answered by the gateway's own Actuator. No backend proxy. |
| Other paths | No gateway route: normally 404. |

The predicates match paths, not a list of allowed HTTP methods. Backend controllers
still decide which methods and operations exist. There is no `StripPrefix` filter:
the backend already expects `/api/...`. A fixed `http://` destination does not
perform service discovery or client-side load balancing.

Defaults are `GATEWAY_ADDRESS=127.0.0.1`, `GATEWAY_PORT=8090`, and
`ORDERFLOW_URL=http://127.0.0.1:8080`. These are server/operator configuration, not
client-selected destinations. The local listener is intentionally loopback.

### Request ID and observable timing

Every incoming request gets a new UUID in `X-Request-Id`. The filter uses header
`set`, replacing any caller-provided value, and forwards the generated ID to
Orderflow. Its `beforeCommit` callback also sets the response header, replacing
any conflicting backend value. An ID helps correlate one request; it is neither
authentication nor an idempotency key. The backend receives it but this lesson
does not add backend logging or distributed tracing.

The `RequestIdFilter` log entry contains the generated ID, route ID, method, HTTP status, and
elapsed milliseconds. It omits bodies, query strings, and arbitrary client header
values. Framework error logs are separate and may include request URLs; log
redaction and retention need their own production review. The measurement ends
when response headers are about to be committed;
it is not a measurement of complete body delivery or the user's end-to-end
experience. A client disconnect before response commitment may produce no such
log. Full completion/cancellation observability is a later step.

### Failure behavior and the retry trap

| Situation | Result and meaning |
| --- | --- |
| Backend returns 201, 400, 404, 409, or 500 | Forward the backend status and body; do not manufacture a success response. |
| Connection refused or hostname cannot resolve | Error handler maps the recognized failure to 502; Boot renders the error. |
| Backend does not produce its response within the configured response wait | Gateway's native timeout gives 504. |
| Error after response headers are committed | Status cannot simply be replaced with a new 502 response. |

`connect-timeout: 1000` is a connection-establishment limit in milliseconds.
`response-timeout: 3s` bounds the configured downstream response wait. It is not
a guaranteed three-second deadline for complete streamed-body delivery or the
entire client experience, nor a cancellation/rollback guarantee in the backend.
The response timeout applies to the response stage of the routing flow;
see the [configuration reference](https://docs.spring.io/spring-cloud-gateway/reference/configprops.html),
[timeout units](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/http-timeouts-configuration.html),
and [Gateway 5.0.3 routing implementation](https://github.com/spring-cloud/spring-cloud-gateway/blob/v5.0.3/spring-cloud-gateway-server-webflux/src/main/java/org/springframework/cloud/gateway/filter/NettyRoutingFilter.java).

There is no Gateway `Retry` filter. We also set `HttpClient.disableRetry(true)`:
Reactor Netty has its own automatic retry for certain connection-reset failures,
separate from Gateway filters. [Reactor Netty API](https://projectreactor.io/docs/netty/release/api/reactor/netty/http/client/HttpClient.html#disableRetry(boolean)).

Imagine the backend commits an order, then the response is lost. A blind POST retry
can create a second order and reduce stock again. A 504 means the gateway stopped
waiting, not that the transaction rolled back. The existing stock lock serializes
updates but does not identify duplicate purchase intent. Durable idempotency in
the service/database is a later lesson; merely sending an `Idempotency-Key` header
does not protect the current implementation.

## 5. Run the real product/order flow

Run all commands from `/Users/pavtiwar/orderflow`. The database setup is documented
in the [README](../README.md). These requests create ordinary local practice data;
each new product/order POST creates another record.

Terminal 1, start the existing backend:

```sh
cd /Users/pavtiwar/orderflow
./scripts/db start
./dev spring-boot:run
```

Terminal 2, verify and start the separate gateway:

```sh
cd /Users/pavtiwar/orderflow
./dev -f gateway/pom.xml verify
./dev -f gateway/pom.xml spring-boot:run
```

Terminal 3, confirm gateway health and create/read a product through port 8090.
The examples use Python 3 only to extract returned IDs, so they do not assume a
particular database sequence value:

```sh
curl -i http://127.0.0.1:8090/actuator/health

product_json=$(curl --fail-with-body -sS http://127.0.0.1:8090/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Gateway practice keyboard","price":1250.00,"stock":10}')
printf '%s\n' "$product_json"
product_id=$(printf '%s' "$product_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -i "http://127.0.0.1:8090/api/products/$product_id" \
  -H 'X-Request-Id: caller-chosen-id'
```

Expect a product response and a generated UUID response header, different from
`caller-chosen-id`. Match that UUID to the gateway log. Then submit one order:

```sh
order_json=$(curl --fail-with-body -sS http://127.0.0.1:8090/api/orders \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":$product_id,\"quantity\":2,\"serviceLevel\":\"STANDARD\"}")
printf '%s\n' "$order_json"
order_id=$(printf '%s' "$order_json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -i "http://127.0.0.1:8090/api/orders/$order_id"
curl -i "http://127.0.0.1:8090/api/products/$product_id"
curl -i "http://127.0.0.1:8080/api/products/$product_id"
curl -i http://127.0.0.1:8090/api/learning/status
```

Expected observations: the order exists; the new product has stock 8 if there
were no other orders; direct and proxied reads describe the same stored product;
the learning URL through the gateway returns 404. Price calculations still follow
the backend's existing campaign settings. Stop on a failed create request rather
than blindly retrying it. Stop each application in its own terminal with Ctrl+C.

For an alternate local backend port, run the backend with
`./dev spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`, then start
the gateway with:

```sh
ORDERFLOW_URL=http://127.0.0.1:8081 ./dev -f gateway/pom.xml spring-boot:run
```

## 6. Predict a failure, then explain the evidence

First predict what happens to a product GET when the backend stops while the
gateway remains running. Stop only the backend, repeat the GET through 8090,
inspect the status/request ID/log, and restart the backend. Expected: recognized
connection refusal becomes 502. Gateway `/actuator/health` may still report UP:
it describes the gateway itself, not proof that orders can be created.

The automated gateway tests use a local HTTP fixture to exercise forwarding and
downstream failures without requiring PostgreSQL. The
[lab record](labs/INTERVIEW-api-gateway.md) identifies checks actually run and their
limits. A fixture check and a real backend exercise establish different evidence.

Checkpoint: explain route selection, which application commits the order, and
what a timeout tells the caller. The learner's answer to the retry prediction was
**“No—first check the outcome or use an idempotency key”**. That is a correct
conceptual prediction. Refine it: safe replay requires the backend to implement
durable key handling; the header alone has no effect in this lesson. Live learner
practice and explanation of the complete request path are still pending.

## 7. What a production rollout still needs

This local lesson implements explicit routing, request IDs, bounded downstream
waiting, deliberate connection errors, and limited health exposure. It does not
implement authentication, authorization, TLS termination, a Redis rate limiter,
circuit breakers, service discovery, load balancing, or durable idempotency.

A later production design must choose trusted identity and service authorization,
protect backend access against gateway bypass, and provide tested request-size,
concurrency, connection-pool, and timeout budgets. Deploy redundant gateway
instances behind suitable ingress/load balancing, secure management access, add
traces and alerting, and exercise overload, shutdown, and failure recovery.
These are design requirements to implement incrementally, not guarantees of this
commit. Only health is exposed by this gateway's Actuator configuration, and its
details are hidden. [Actuator exposure and health](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html).

Interview outline: “I put a separate Spring Cloud Gateway application in front of
the MVC service. Path predicates select product and order routes; shared request
processing generates an ID and applies bounded downstream waits. Business
validation and transactions remain in the service. I distinguish upstream HTTP
errors, connection failures, and timeouts, and avoid automatic write retries until
durable idempotency exists. Security, capacity limits, and high availability require
their own implementation and verification.” This describes the lesson accurately;
do not present it as personal production operating experience.
