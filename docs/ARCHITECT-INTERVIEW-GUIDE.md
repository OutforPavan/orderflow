# Architect interview preparation: backend, security, scale, and consistency

Prepared for the requested Capgemini senior-architect conversation with a banking client. Interview stated by the learner: 29 September 2026, 8 p.m. Study examples are proposed designs, not claims about the client's actual system or the learner's past production experience.

## How to use this guide

Start with the two implemented pricing scenarios. Then practise one order's journey through security, stock, payment, and recovery. For every concept, explain the problem, the mechanism, one failure case, the tradeoff, and how you would verify it. Do not memorise only definitions.

A useful answer structure is: **“The rule I need to preserve is ... Here is how I implement it ... If this step fails ... The tradeoff is ... I verify it by ...”** State an assumption where requirements are missing, and distinguish an implemented feature from a proposed design.

Implementation scope: Java 8-compatible monetary calculation integrated into the existing Java 21 / Spring Boot 4 application; database snapshots and product-row locking. Security, OAuth, TLS, priority scheduling, payment collection, distributed workflows and bulk processing below are interview preparation, not new implemented services.

Read in this order:

1. Pricing scenarios and their code/design decisions.
2. Security: identity, permission, OAuth, TLS, and concrete vulnerability fixes.
3. Architecture: business invariants, consistency, recovery, scale, and bulk work.
4. Oral practice prompts and a proposed study schedule.
5. The companion Java 8 code walkthrough for exact source listings.

# The two pricing scenarios: explanation, implementation, and interview defence

The agreed rule is **10% discount first, then a 5% or 10% priority fee on the discounted amount**. The implementation applies this during order creation. A campaign can cover all products or a server-configured set of product IDs. The same catalogue product supports standard and paid priority service without changing its catalogue price.

## Explain the problem before writing code

A product's catalogue price answers “What is the base price of this product?” An order's price answers “What did this customer agree to pay for these units under these rules?” Those are different facts. If I subtract a discount directly from the shared product entity, every customer can see the changed price, a second request may discount an already discounted price, and old orders become difficult to explain. Instead, I calculate a separate immutable breakdown and save it with the order.

For one unit priced at ₹1,000:

| Product qualifies for campaign? | Service | Subtotal | Discount | Fee base | Priority fee | Total |
|---|---|---:|---:|---:|---:|---:|
| Yes | Standard | 1,000.00 | 100.00 | 900.00 | 0.00 | 900.00 |
| Yes | Priority 5% | 1,000.00 | 100.00 | 900.00 | 45.00 | 945.00 |
| Yes | Priority 10% | 1,000.00 | 100.00 | 900.00 | 90.00 | 990.00 |
| No | Priority 5% | 1,000.00 | 0.00 | 1,000.00 | 50.00 | 1,050.00 |

A 10% discount followed by a 10% fee does not restore the original price: 1,000 × 0.90 × 1.10 = 990. The fee uses the reduced base. Two units double the subtotal, then the same line calculation applies.

The example uses one currency with two decimal places, represented as rupees in this explanation. The existing application does not store a currency column. Currency, tax, delivery, promotion dates, coupon budgets, refunds, payment collection, and fulfilment are separate requirements. A real financial system must record the currency and follow its approved precision and rounding rules.

## The business assumptions to say aloud

Before implementation, I would establish which products qualify, whether offers combine, whether priority fees apply before or after discounts, whether fees apply per line or per order, how amounts are rounded, and whether priority is available to every paying customer or only eligible members. We have explicitly chosen fee-after-discount.

Here, priority is an **optional paid service level** selected in the request: `STANDARD`, `PRIORITY_5`, or `PRIORITY_10`. The customer selects the service; the server determines its fixed percentage. Omitting the field chooses standard service. There is no implemented customer account or authentication model. Therefore this is not proof of membership-based priority entitlement.

If only selected customers are eligible, derive the customer from a validated login, load their eligibility from an authoritative account/profile service, and accept or reject their requested service level. Do not let a JSON `isPriority` flag establish privileged membership. If the customer has a standing preference, store their consent and resolve the choice from that profile. Disclose the final fee before accepting the order; an implementation must not silently add a surprise charge to an already accepted price.

## How the code is divided

| Class | One responsibility |
|---|---|
| `ProductDiscountPolicy` | Decide whether this product receives the 10% campaign discount |
| `ServiceLevel` | Define the supported paid options and their server-owned fee rates |
| `PriceCalculator` | Calculate one line's monetary breakdown from validated inputs |
| `PriceBreakdown` | Carry the immutable result without allowing later mutation |
| `PricingDemo` | Run examples without a web server or database |
| `PricingConfiguration` | Connect the pure Java classes to the existing Spring application |
| `OrderService` | Load and lock the product, price the request, reserve stock, and save the order in one local transaction |
| `PurchaseOrder` | Persist the accepted monetary snapshot and selected service level |

Separating eligibility from arithmetic makes a new promotion rule easier to change without changing stock handling or controllers. The current rule is small enough that it does not need a general-purpose rules engine. If promotions become varied, introduce a narrow `DiscountPolicy` interface with interchangeable implementations. This is the Strategy idea: the caller depends on a rule contract while different policies supply the decision. Composition lets product offers and paid service levels combine without a class for every combination. A Decorator can also compose adjustments, but implicit ordering is dangerous for money: make the calculation order explicit and auditable. Do not add design patterns merely to name them in an interview.

## Follow the calculation slowly

1. Validate the price, quantity, discount percentage, and service level.
2. Multiply catalogue unit price by quantity to obtain the line subtotal.
3. Calculate the percentage discount and round that amount to two decimal places.
4. Subtract the discount from the subtotal.
5. Calculate the priority fee on that remainder and round the fee.
6. Add the fee to the remainder and return all components.
7. Save these values with the order. Future reads use the saved snapshot.

`BigDecimal` supports decimal arithmetic with explicit rounding. Construct money from a decimal string such as `new BigDecimal("1000.00")`; constructing it from a binary floating-point value can carry an unexpected approximation. Use `compareTo` for numeric comparisons; `equals` also considers scale. In this example, `HALF_UP` is a chosen policy, not a universal banking rule. [Java 8 BigDecimal reference](https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html)

A useful rounding test is three units at 0.05 each. Subtotal is 0.15; the 10% discount is 0.015 rounded to 0.02; the remaining amount is 0.13; its 5% fee is 0.0065 rounded to 0.01; total is 0.14. Applying rounding separately per unit would produce a different answer. This is why “where do we round?” is part of the business contract.

The calculator is stateless and its results are immutable. Requests do not share a mutable total. The selected-product policy copies its input set so external code cannot change eligibility accidentally after configuration. Product lookup in this set has typical constant-time cost; pricing one line uses a fixed number of arithmetic operations. This is not a guarantee of bounded cost for arbitrarily huge numeric input, so API and database limits still matter.

## How it enters the existing order flow

The order request accepts product ID, positive quantity, and an optional service level. It does not accept the price, discount, fee rate, or final total. Unknown fields and numeric enum values are rejected. The product's price comes from the database, discount eligibility comes from server configuration, and the fee rate comes from the enum.

`OrderService` acquires a database write lock on the selected product row before reading the price and checking stock. Another order for that product waits, then sees the newly committed stock. Product price changes use the same locking path, avoiding a stale product update overwriting a stock change. Different product rows can still be processed independently. PostgreSQL releases these row locks when the transaction finishes; the work should remain short. [PostgreSQL row locks](https://www.postgresql.org/docs/current/explicit-locking.html)

The existing explicit flush sends the stock update to the database before saving the order. Flush is not commit. If inserting the order fails, the local transaction rolls back the stock update too. A payment provider call would not become part of that transaction merely by adding `@Transactional`; it needs a separate durable workflow and recovery strategy. [Spring transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

The database migration backfills older orders with their existing subtotal and zero discount/fee. It replaces the old `total = unit_price × quantity` rule with checks for subtotal, discount, fee, tier, and final total. Do not edit an already applied V1 migration: add a new migration so existing installations have an explicit upgrade path.

This is a learning-app migration: stop older application writers before deploying it. Old code cannot insert the new mandatory subtotal. On a large production table, plan incremental backfill, constraint validation, and an expand/backfill/contract rollout to control locking and compatibility. Do not claim this teaching migration is a zero-downtime rollout.

## Run the example in the application

The application remains Java 21 / Spring Boot 4.1.1. The five framework-independent pricing/demo source files use Java 8 language features and APIs. The Spring adapters, existing records, and framework dependencies are not a Java 8 application. Compatibility of the pricing code is checked with `javac --release 8`; this is not a claim that the full application was run on a Java 8 virtual machine.

To make every product eligible, start the app from `/Users/pavtiwar/orderflow` with:

```sh
./dev spring-boot:run -Dspring-boot.run.arguments=--pricing.discount-all-products=true
```

For selected products only, use the real product IDs:

```sh
./dev spring-boot:run '-Dspring-boot.run.arguments=--pricing.discount-product-ids=1,2'
```

The defaults enable no campaign, so existing standard requests retain their previous total. These startup settings affect new orders; they do not reprice saved orders. Stop an existing app process normally before starting another on the same port.

Create a test product:

```http
POST /api/products
Content-Type: application/json

{"name":"Interview keyboard","price":1000.00,"stock":10}
```

Use the actual returned ID in the order request:

```http
POST /api/orders
Content-Type: application/json

{"productId":1,"quantity":1,"serviceLevel":"PRIORITY_5"}
```

For an eligible product, the response includes these monetary fields:

```json
{
  "unitPrice": 1000.00,
  "subtotal": 1000.00,
  "discountPercent": 10.00,
  "discountAmount": 100.00,
  "prioritySurchargePercent": 5.00,
  "prioritySurchargeAmount": 45.00,
  "serviceLevel": "PRIORITY_5",
  "total": 945.00
}
```

This is an excerpt; the real response also contains identifiers, quantity, and creation time. JSON clients may display 945 instead of 945.00; the database monetary columns preserve the declared scale.

Run the five-file Java 8 compatibility example without Spring:

```sh
mkdir -p work/pricing-java8
.tools/java21/Contents/Home/bin/javac --release 8 -d work/pricing-java8 \
  src/main/java/com/outforpavan/orderflow/pricing/ServiceLevel.java \
  src/main/java/com/outforpavan/orderflow/pricing/PriceBreakdown.java \
  src/main/java/com/outforpavan/orderflow/pricing/PriceCalculator.java \
  src/main/java/com/outforpavan/orderflow/pricing/ProductDiscountPolicy.java \
  src/main/java/com/outforpavan/orderflow/pricing/PricingDemo.java
.tools/java21/Contents/Home/bin/java -cp work/pricing-java8 com.outforpavan.orderflow.pricing.PricingDemo
```

With an actual JDK 8, compile those same five files using its `javac` without `--release`, then run its `java`. The JDK 8 runtime run is an instruction, not an observed test result from this preparation.

## Questions that reveal architectural depth

**“Why not change the product price?”** “The product price is shared catalogue data. The offer and service fee belong to the customer's accepted order. I preserve both the original price and the adjustments so we can explain the invoice and calculate an appropriate refund.”

**“Why not add ten and subtract ten?”** “Percentages have bases. Here the fee applies to the discounted amount. For 1,000, the discount is 100 and the 10% fee is 90, giving 990. I confirm the order of operations and rounding before coding.”

**“What if there are many items?”** “Calculate each line's eligible discount, then apply the approved fee/tax allocation rules and sum exact decimal amounts. A basket-level coupon may require allocation across lines so later partial refunds still reconcile. Define deterministic handling of rounding remainders; do not independently round every calculation and hope the invoice balances.”

**“What if an order already exists?”** “For an unpaid draft, I can explicitly reprice under a controlled revision after showing the customer the new quote. An accepted or paid order must follow the change/refund policy; I would record an adjustment or refund linked to the original amounts rather than silently overwrite financial history. This implementation prices at creation and preserves the result.”

**“What if the campaign is limited to the first thousand redemptions?”** “A set of product IDs is insufficient. Reserve or consume a campaign entitlement using a durable atomic rule, record the redemption identity, and make retries return the same redemption. Decide whether cancellations restore quota. Cached eligibility cannot enforce a global redemption limit alone.”

**“Does a priority fee actually make the customer faster?”** “The implementation records the purchased service and fee. Faster fulfilment needs a scheduler or queue policy, reserved capacity, monitoring, and a remedy if the promise is missed. Merely charging an extra amount changes no execution order.”

**“What happens if a request is sent twice?”** “The current feature does not implement idempotency. Repeated POSTs can create separate orders while stock remains. Production checkout needs a durable customer-scoped request identity and the original result for safe retries. Locking stock solves a different problem.”

**“Would you use this for banking production as it stands?”** “It is a verified learning implementation of the pricing slice. It has no authentication, customer ownership, payment integration, durable queue, audit actor identity, order idempotency, or full currency/tax/refund model. I can explain exactly which guarantees are implemented and how I would add the remaining controls.”


---

# Security interview guide: explain it through an order

These are proposed production security controls. The implemented pricing application currently has no authentication, customer ownership checks, or Spring Security configuration. These explanations use an order application as the example. Architecture recommendations below are proposed design choices, not claims about the interviewer's systems. Sources were checked on 28 September 2026.

## A strong opening answer

“When a customer places an order, I need to establish who is calling, whether they may place that order, whether the price and customer benefits are legitimate, and whether processing the request preserves our business rules. I protect the connection, validate the caller, enforce permissions on the particular order, calculate money on the server, and record enough evidence to investigate the result. A successful login alone does not solve all those problems.”

## Authentication and authorization

**Authentication means establishing identity. Authorization means deciding what that identity may do.** A bank checks your identity at the entrance, but that does not allow you to open every customer's locker. Likewise, `orders:read` may permit calling the order API, but the service must also check that this customer owns order 123, or has a legitimate staff permission to access it. An unpredictable order identifier reduces guessing; it never replaces the permission check. Start with denied access and grant the minimum necessary permission. [OWASP authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)

For our application I would derive the customer identity from the validated security context, then load the customer's permitted account and service tier from trusted data. I would not trust `customerId`, `isPriority`, `discountPercent`, or an administrator flag just because they arrived in a JSON request. A customer can request priority service; the server decides eligibility and the fee. This distinction prevents the same pricing feature from becoming a privilege escalation vulnerability. [OWASP business logic guidance](https://cheatsheetseries.owasp.org/cheatsheets/Business_Logic_Security_Cheat_Sheet.html)

If an application owns password login, store password hashes using an appropriate password hashing algorithm and parameters, with a unique salt. Password hashing is intentionally expensive to slow offline guessing; reversible encryption and a single fast SHA-256 hash do not provide the same protection. Prefer an established identity provider where practical. For banking operations, the required multifactor and transaction checks depend on the customer's security policy and risk. [OWASP password storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

## Spring Security: what actually happens to a request

“Spring Security puts a sequence of security checks in front of the controller. A request must pass those checks before the business operation executes.”

The servlet container enters `DelegatingFilterProxy`, which connects it to Spring's `FilterChainProxy`. That component selects the **first matching** `SecurityFilterChain`. Its filters handle the configured security responsibilities, including exploit protection, authentication, and authorization. Authentication normally runs before the permission decision. Ordering matters: a broad chain declared first can accidentally prevent a more specific chain from being selected. Make the fallback policy explicit so a newly added endpoint does not unintentionally become public. [Spring Security architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)

For a bearer JWT request, the flow is:

1. The bearer-token filter extracts the token from the authorization header.
2. An `AuthenticationManager` delegates to the JWT authentication provider.
3. A `JwtDecoder` verifies and validates the token.
4. Verified claims become the caller's identity and authorities.
5. The request's security context carries that result for subsequent authorization and application code.

The issuer identifies who created the token; the audience identifies which API should accept it; scopes describe permitted actions. The service must require the intended issuer, expiration and other time checks, acceptable signing algorithm, and the intended audience. **Do not assume issuer configuration automatically configures your audience requirement.** Configure audience validation explicitly for the framework version in use. A correctly signed token intended for a different API should still be rejected. [Spring JWT resource server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

The `SecurityContextHolder` is commonly backed by thread-local storage. It is not a permanent global “currently logged-in user.” Be careful when moving work into asynchronous tasks: a worker needs an intentional identity propagation or service identity model. For username/password authentication, an appropriate provider checks credentials; for bearer-token authentication, the relevant provider checks the token. These are different authentication mechanisms behind the same framework abstractions. [Spring authentication architecture](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/architecture.html)

An API normally returns **401** when valid authentication is missing and **403** when an authenticated caller lacks permission. A login-oriented browser application may redirect instead. Configure consistent handlers so clients receive an appropriate response rather than a leaked stack trace. [OWASP REST security](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html)

**Candidate answer to “Is securing the gateway enough?”** “The gateway can reject obvious invalid requests and apply broad limits. The order service knows order ownership, tenant membership, and allowed state changes, so it must enforce those rules too. I also ensure internal calls cannot bypass trusted identity verification. Network location is not a business permission.”

## OAuth, OpenID Connect, and JWT without confusing them

These terms describe different things:

| Term | Plain-English meaning | Order example |
|---|---|---|
| OAuth 2.0 | A framework for obtaining limited permission to access an API | A client receives permission to create an order |
| OpenID Connect, or OIDC | An identity layer on OAuth for authenticating the end user | The shopping application learns who signed in |
| Access token | The credential presented to the API | Sent to the order service |
| ID token | An OIDC statement intended for the client about authentication | Used by the client to establish the login session |
| Refresh token | A credential used with the authorization server to obtain replacement access tokens | Kept away from ordinary order API calls |
| JWT | A structured token format; OAuth access tokens may use it | Contains signed claims about issuer, audience, expiry, and scope |

An ID token is not a substitute for an access token at the order API. OIDC defines ID-token validation, including issuer and audience checks, and nonce validation when applicable. [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)

For a user login, explain authorization code flow with PKCE:

1. The application starts a login request and supplies a challenge derived from a fresh random value called the code verifier, which it retains. This value is separate from any client secret.
2. The authorization server authenticates the user and obtains any required consent.
3. The browser returns with a short-lived authorization code.
4. The application exchanges that code together with its original secret. PKCE makes an intercepted code insufficient by itself.
5. The client uses the access token at the API; refresh happens at the authorization server under its policy.

Use exact registered redirect matching and appropriate request correlation protections. Avoid the implicit flow for new implementations and do not use the resource-owner password credentials grant. Protect refresh tokens with the applicable rotation or sender-constraining mechanism and detect reuse. [OAuth security best current practice, RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)

For a background inventory worker acting as itself, client credentials can represent the application rather than an end user. Keep client credentials in a secret-management system and narrowly scope the resulting permissions. An application credential does not automatically give the worker authority to impersonate any customer. For high-value integrations, sender-constrained tokens can bind token use to a client-held key or certificate, reducing the usefulness of a stolen token. [OWASP OAuth guidance](https://cheatsheetseries.owasp.org/cheatsheets/OAuth2_Cheat_Sheet.html)

**“Is a JWT encrypted?”** Usually the commonly used signed JWT is readable. Its signature detects modification; it does not hide the claims. Do not put passwords, full account details, or unnecessary personal information into it. A bearer token is usable by whoever possesses it, so protect it in transit, storage, and logs. Local JWT verification reduces dependence on a network lookup, but a revoked permission may remain effective until token expiry unless the system also checks revocation or current policy. An opaque token checked through introspection offers a different tradeoff: centrally checked state, with an additional availability and latency dependency. [OWASP JWT guidance](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_Cheat_Sheet.html)

**“The identity provider is unavailable; must every request fail?”** “New authentication may fail. Existing JWTs may remain verifiable using trusted cached keys while they are valid. A previously unseen key may require a key fetch and fail. I do not accept unverifiable tokens. For a high-risk action that requires a fresh policy or revocation decision, availability of that decision is part of the design.”

## CSRF and CORS: a common interview trap

Cross-site request forgery means another site tricks a browser into sending a request with credentials the browser attaches automatically. If the bank's session cookie is automatically included, the attacker may not need to know its value. Keep appropriate CSRF protection for cookie-authenticated state-changing requests. Use anti-CSRF tokens and origin checks where applicable; cookie `SameSite` settings help but should be considered within the full design.

An API that exclusively accepts a bearer token explicitly placed in the authorization header, without automatically attached browser credentials, has a different CSRF exposure. Therefore, **“stateless” is not by itself a justification to disable CSRF**. CORS controls which browser origins may read responses or perform certain cross-origin interactions; it does not authenticate callers and does not stop non-browser clients. [OWASP CSRF prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)

## SSL/TLS and mutual TLS

“TLS protects data while it travels between two endpoints. It helps prevent eavesdropping and undetected modification, and authenticates the server. SSL is the historical predecessor; production designs should use appropriate modern TLS versions.” Prefer TLS 1.3 where supported; carefully configured TLS 1.2 may be required for compatibility. Disable obsolete versions and weak configurations. Check the certificate's trusted issuer, validity, and hostname. A certificate signed by a trusted authority for a different hostname is not valid for this connection. [OWASP TLS guidance](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Security_Cheat_Sheet.html)

A simplified TLS 1.3 handshake: the client and server negotiate supported parameters, exchange key material, authenticate the server using its certificate and proof of key possession, and derive symmetric traffic keys. Those symmetric keys are shared temporary secrets that protect the application messages efficiently. The certificate itself is not the bulk-data encryption key. In mutual TLS, the server also authenticates the client using a certificate. It identifies the calling workload or client; the application must still decide which operation it may perform. TLS 1.3 early data can be replayed, so do not casually enable it for order creation or payment operations. [TLS 1.3 specification, RFC 8446](https://www.rfc-editor.org/rfc/rfc8446.html)

For an order system, I would map every connection: browser to gateway, gateway to order service, order service to payment provider, and service to database. If TLS ends at the gateway, that does not automatically protect the next connection. Operationally, I would automate certificate renewal, alert before expiry, rehearse key rotation, and restrict who may read private keys. Never “fix” a certificate error by disabling verification. TLS protects transport; it does not fix SQL injection, an authorization bug, or sensitive data printed into logs.

## OWASP remediation: explain a defect, the fix, and proof

The OWASP Top 10:2025 includes access control, misconfiguration, supply chain, cryptography, injection, insecure design, authentication, integrity, logging and alerting, and exceptional-condition handling. It is a useful risk map, not a complete application test plan. An architect should connect each risk to a real path through the product. [OWASP Top 10:2025](https://top10.owasp.org/2025/0x00_2025-Introduction/)

| Problem in an order system | Practical remediation | Evidence that the fix works |
|---|---|---|
| A customer changes `/orders/123` to another customer's identifier | Enforce ownership and tenant rules in the service and data access | Cross-customer and cross-tenant requests fail, including update and export paths |
| Product search text becomes part of executable SQL | Use bound parameters; allowlist permitted sort-field names | Malicious search text is treated as data and cannot alter the query |
| A JSON request includes `unitPrice: 1` or `isPriority: true` | Accept a small request DTO; derive trusted price and eligibility on the server | Extra privileged fields cannot change the result |
| A product description contains script markup | Use context-appropriate output encoding; sanitize deliberately supported HTML | The administration page renders the content safely without executing it |
| A product import accepts a URL pointing at internal infrastructure | Restrict destinations and schemes; validate resolved addresses and redirects; control outbound networking | Local, private, metadata, and redirect-based destinations are denied under the chosen policy |
| A huge upload or export exhausts memory | Set input, batch, concurrency, execution-time, and per-customer limits | Load tests demonstrate bounded memory and fair service |
| A library has an exploitable published defect | Identify deployed versions and reachable paths, upgrade to a fixed supported version, verify behavior | Dependency and image scans plus targeted regression checks |
| XML import resolves an external entity | Disable external entities and unnecessary DTD processing in every relevant parser | A hostile XML fixture cannot read files or fetch network resources |

References for the corresponding fixes: [SQL injection](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html), [mass assignment](https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html), [cross-site scripting](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html), [server-side request forgery](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html), [denial of service](https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html), [dependency management](https://cheatsheetseries.owasp.org/cheatsheets/Vulnerable_Dependency_Management_Cheat_Sheet.html), and [XML external entities](https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html).

**Banking-specific depth:** separate login from authorization of a particular sensitive transaction. A customer approving a transfer of 1,000 to beneficiary A must not accidentally authorize a later modified transfer of 10,000 to beneficiary B. Bind approval to significant transaction details, enforce the allowed sequence on the server, expire the approval, make it single use, and check it again at execution. Changing important details should invalidate the previous approval. This is a business-integrity control in addition to transport encryption and login. [OWASP transaction authorization](https://cheatsheetseries.owasp.org/cheatsheets/Transaction_Authorization_Cheat_Sheet.html)

For discount and fee changes, I would retain the actor, rule version, affected order, relevant before/after values, timestamp, and correlation identifier. Protect audit data and control access to it. Do not log tokens, passwords, or unnecessarily complete personal or payment information. Alert on patterns such as repeated permission failures and unusual pricing-rule changes; collecting logs without a response process is incomplete. [OWASP logging guidance](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)

**“How would you remediate a scanner finding?”** “First I reproduce and understand the affected path, including which deployed versions and data are exposed. I fix the root cause, add a regression case that demonstrates the exploit no longer succeeds, run relevant behavior checks, deploy through the agreed process, and verify the deployed result. If a patch cannot be immediate, I document the temporary containment, accountable owner, and deadline. I do not close a finding just because one scan turns green.”

## Java 8 versus current Spring versions

Java 8-compatible pricing classes are a reasonable interview deliverable. That does not mean current Spring releases run on Java 8. Spring Boot 2.7.18 documents Java 8 as its minimum; Spring's announcement identifies it as the end of open-source support for Boot 2.x. Current Spring Security documentation requires Java 17 or higher. Keep the pricing algorithm's Java 8 compatibility separate from the production framework lifecycle; do not downgrade a newer application merely to demonstrate a Java 8 calculation. [Boot 2.7.18 requirements](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/getting-started.html), [Boot 2.x support announcement](https://spring.io/blog/2023/11/23/spring-boot-2-7-18-available-now/), [Spring Security requirements](https://docs.spring.io/spring-security/reference/prerequisites.html)


---

# Architecture, consistency, scale, and bulk data: interview preparation

The examples below are proposed designs, not claims about Capgemini's or the customer's actual architecture. An order system is a useful teaching example, but a bank's ledger has additional accounting and settlement constraints. Do not imply that inventory and money can always be treated the same way.

## A strong opening answer

“I would first identify the rules that must never be broken. We must not charge a customer twice for one payment attempt, confirm stock we cannot fulfil, allow a user to access another customer's order, or change the price of an already accepted order because today's promotion changed. Then I would separate the immediate decision from background work. Pricing, authorization, and a reliable order record belong on the critical path. Notifications and reporting can happen later. I choose transaction boundaries, service boundaries, and recovery behaviour around those rules.”

This sounds more senior than opening with a list of technologies. Follow it with requirements: expected peak traffic, average items per order, latency target, acceptable stale data, stock oversell policy, payment provider behaviour, recovery time, acceptable data loss, geography, and audit retention.

## The request's journey through the backend

A typical flow is: client → gateway/load balancer → security checks → controller → application service → domain pricing and order rules → repository/database → durable background events.

- The gateway limits abusive traffic, routes requests, and can reject obviously invalid tokens. A service still checks the caller's permission to perform the specific operation on the specific order.
- The controller handles HTTP, input shape, validation, and response codes. It should not contain promotion arithmetic or payment workflow decisions.
- The application service coordinates the use case: load trusted product data, calculate price, reserve resources, save the order, and arrange follow-up work.
- Domain logic decides what is allowed: discount eligibility, fee calculation, whether an order may be cancelled, and which transitions are legal.
- The repository performs database access. The database also enforces rules using unique keys, foreign keys, and constraints. Validation in Java alone cannot protect against two application instances racing.

The client sends product identifiers, quantities, and the selected service tier. It does not authoritatively send the final price, discount percentage, customer entitlement, or payment-success flag. The server obtains those from trusted systems and its own policy.

An accepted order should keep a price snapshot: product description, unit price, quantity, currency, discount amount and rule version, priority-fee amount and rate, tax, and total. Otherwise a future price change can silently rewrite the meaning of a past order. A returned item is refunded from the recorded original amounts, according to the cancellation/refund policy.

`@Transactional` commonly controls work in a local database; it does not make an HTTP payment call or a second service's database part of the same atomic operation. In Spring's default proxy mode, a method calling another transactional method on the same object bypasses that proxy. Default rollback rules also distinguish unchecked and checked exceptions; configure the intended rollback behaviour and test a real rollback path. [Spring transaction documentation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

An interviewer may ask why network calls should not be held inside a database transaction. Explain: “While I wait for a payment provider, I may hold locks and a scarce database connection. A slow provider then slows unrelated orders. I would normally commit a durable workflow state and perform the remote step outside the local transaction, with idempotency and recovery.”

## Data consistency: start with the business rule

“Consistency means different things in different conversations. A database transaction's consistency means it preserves our declared rules. Distributed read consistency is about what version different readers can observe. Eventual consistency means copies or views can lag and should converge when updates are delivered. I state which meaning applies.”

| Information or action | Reasonable proposed choice | Why |
|---|---|---|
| Product description in a search page | May be briefly stale | A slightly old description is usually tolerable |
| Analytics/order count | Asynchronous projection | Reporting delay does not authorize money movement |
| Order immediately after creation | Return committed state; route sensitive reads appropriately | The customer should not see an order disappear because a replica lags |
| Final stock reservation | Atomic decision at inventory owner | Two buyers must not independently claim the same final item |
| Payment-attempt identity | Unique durable constraint | A repeated request must map to the same attempt |
| Ledger posting | Defined accounting invariants in a strong transaction boundary | Two independent account updates can create or destroy apparent money |

Do not solve every consistency issue by putting `synchronized` on a Java method. That protects only threads using the same lock in one process. It does not coordinate several pods or application servers.

### Two customers buy the last item

Unsafe approach: read available quantity as one, then later write zero. Both requests can read one and both succeed.

A simple database-owned reservation, with a strictly positive validated quantity, can use a conditional update:

```sql
UPDATE inventory
SET available_quantity = available_quantity - :quantity,
    reserved_quantity  = reserved_quantity + :quantity
WHERE product_id = :productId
  AND warehouse_id = :warehouseId
  AND available_quantity >= :quantity;
```

Require exactly one updated row. Zero rows means the reservation did not succeed; distinguish missing inventory from insufficient stock as required by the API. In the same database transaction, insert a reservation identified by the order and item, enforce a unique reservation identity, and write any needed outbox event. If a duplicate reservation insertion fails, roll back the inventory update and return the existing reservation through a controlled duplicate path. Without this identity, retrying the update can reserve stock twice.

For several items, acquire/update inventory rows in a stable order to reduce deadlocks, and define whether the order requires all items or allows partial fulfilment. Reservation expiry and checkout confirmation must atomically compete to transition the same reservation state; an expiry worker must not release already committed stock. Use a state/version check, not a read-then-act decision.

### Optimistic versus pessimistic locking

Optimistic locking means “save only if nobody changed this record since I read it.” A version column appears in the update predicate. A mismatch forces the caller to reread and decide whether retrying is appropriate. It suits modest contention and short work, but repeated retries can be expensive for a hot product.

Pessimistic locking means “reserve this database row for my transaction while I decide.” A locking read such as `SELECT ... FOR UPDATE` blocks conflicting changes until transaction end. Keep the transaction short and acquire locks in a consistent order; deadlocks and lock timeouts still need handling. [PostgreSQL row-lock documentation](https://www.postgresql.org/docs/current/explicit-locking.html)

Isolation levels describe which concurrent effects a transaction may observe. Read committed does not make arbitrary read-then-write business logic safe. Repeatable-read behaviour differs across databases. Serializable provides the effect of some serial execution, but a database may abort conflicting transactions; the application must retry the whole transaction, with a limit. “I set serializable, so nothing can fail” is incorrect. [PostgreSQL isolation documentation](https://www.postgresql.org/docs/current/transaction-iso.html)

### CAP in one clear answer

“If two parts of a distributed system cannot communicate, I cannot always both answer every request and guarantee a single immediately consistent view. For a balance-changing operation I may delay or reject the operation when I cannot establish the authoritative state. For a catalogue page I may show cached information. I choose behaviour per operation rather than label the whole application with one slogan.”

CAP consistency is a strong visibility guarantee, not merely database validation. Availability in the theorem is also more specific than an uptime percentage. Do not say every system simply chooses any two of three at all times.

## Idempotency: repeated requests, one intended effect

“The network can lose the response after the server has completed the work. The customer then retries. Therefore a retry must refer to the same business intention, not create a new order or charge.”

For `POST /orders`, require or support an idempotency key. Scope it to the authenticated customer/tenant and operation. Store the key, a canonical request fingerprint, processing state, order identifier, and appropriate result. Enforce uniqueness in the database. Same key and same request returns the original result or current operation status; same key with different inputs is rejected. Two simultaneous copies must race on the same durable unique constraint, not an in-memory map. Recording the identity and committing the local effect should be atomic. [AWS idempotent API guidance](https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/)

For remote payments, persist a stable payment-attempt identifier and reuse the provider's idempotency mechanism when retrying that attempt. Read the provider's contract: retention, request matching, and cached error behaviour vary. Stripe, for example, documents parameter matching and that keys can be removed after they are at least 24 hours old. That is an example of a provider rule, not a universal lifetime for financial deduplication. [Stripe idempotency contract](https://docs.stripe.com/api/idempotent_requests)

Do not generate a new key for every retry. Do not assume a deterministic hash of product and quantity always identifies intent: buying the same product again tomorrow is a legitimate new order.

## Saga: complete or compensate a business workflow

“A saga is a sequence of local transactions across services. Each service commits its own part. If a later business step cannot complete, the workflow starts defined compensating actions for earlier steps. It does not turn all services into one ACID transaction.”

In choreography, services react to one another's events. In orchestration, a coordinator keeps track of the workflow and commands the next step. Choreography can work for a small flow; orchestration makes a complex flow and its recovery decisions easier to inspect. The orchestrator should use durable state and safe concurrent ownership so another instance can resume after a crash. [AWS Saga overview](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/saga-patterns.html)

For this interview example, propose a durable sequence:

| Step | Durable outcome | If later cancellation is required |
|---|---|---|
| Create order | Pending order with accepted price snapshot | Mark cancelled; retain history |
| Reserve inventory | Reservation with identity and expiry | Release that reservation once |
| Authorize payment | Provider reference and authorized amount | Void authorization when provider permits |
| Commit reservation / arrange fulfilment | Stock and fulfilment state recorded | Cancel fulfilment if still reversible |
| Capture payment, at the chosen business point | Captured payment reference | Request refund if capture succeeded |
| Confirm order | Confirmed business state | Follow the explicit cancellation/refund process |

This is an illustrative sequence. The exact authorization/capture point depends on fulfilment, provider capability, expiry, and business rules. A useful senior answer states the assumption rather than claiming one ordering fits every business.

Compensation creates a new business action. A refund does not erase the original charge; fees, settlement, customer notifications, and elapsed time may remain. A parcel already handed to a carrier may require a return process. Compensation can itself fail and must be tracked, retried safely, and escalated to operations when necessary. The workflow may remain in a visible `REFUND_PENDING` or `MANUAL_REVIEW` state. [Microsoft compensating-transaction pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/compensating-transaction)

Saga state should include workflow ID, order ID, step, state version, attempts, deadlines, next retry time, command/event identity, and relevant external references. State transitions must be conditional: a duplicate or late event cannot move a cancelled order back to confirmed. Durable timers and a sweeper find workflows stuck beyond their deadlines. [AWS Saga orchestration considerations](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/saga-orchestration.html)

### The hardest payment question

Interviewer: “The payment service timed out. Will you cancel the order and ask the customer to pay again?”

Answer: “A timeout tells me that I did not receive an answer. It does not prove the bank did nothing. I mark the attempt as outcome unknown, keep its reference, query the provider, process authenticated callbacks, and reconcile. If I retry, I reuse the same provider-supported payment identity. I do not initiate a fresh charge while the first attempt may already have succeeded.”

The reservation needs a defined policy during this uncertainty: extend within a bounded time, hold for investigation, or release and arrange a refund if a late success arrives. The callback handler and expiry process must resolve this through guarded transitions. Payment providers commonly expose a lifecycle rather than a single success/failure flag; design around the actual contract. [Stripe payment lifecycle](https://docs.stripe.com/payments/payment-intents)

### Why not use Saga for every bank transfer?

For two accounts controlled by one ledger, the debit and credit should normally be posted in one controlled ledger transaction, with balanced entries, unique transfer identity, and an immutable audit trail. A failed workflow must not expose an arbitrary partially posted transfer as completed. A reversal should be a linked correcting entry rather than erasing history.

Cross-bank settlement is a broader process involving holds, messages, settlement states, reconciliation, and institution-specific rules. A saga may coordinate surrounding work, but “eventual consistency will fix it” is not an accounting model. State the ledger's invariants, authority, and settlement semantics before selecting a pattern. These are proposed engineering principles; the customer's financial product and jurisdiction determine its exact requirements.

## Transactional outbox: do not lose the event after committing the order

The failure is easy to describe: the database commit succeeds; the process dies before publishing `OrderCreated`. The order exists, but inventory never hears about it. Publishing first has the opposite risk: another service acts on an order that later rolls back.

Write the order and an outbox row in the same local database transaction. A separate relay publishes committed outbox rows. If it crashes after sending but before marking delivery, it may resend. Therefore consumers still need deduplication. For a consumer's local database effect, insert a unique consumed-message identity and apply the effect in one transaction. External effects need their own idempotency contract. Preserve necessary aggregate ordering with keys and sequence/version checks. [AWS transactional outbox guidance](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)

The outbox resolves the local database/event dual-write gap. It does not atomically commit two independent service databases and does not eliminate all duplicates.

## Message delivery and ordering

“At most once can lose work. At least once can repeat work. For important business events I usually design for durable delivery with duplicates and make the business effect idempotent.”

Kafka's idempotent producers and transactions offer important guarantees within their documented scope. Kafka-to-Kafka processing is different from charging an external provider or writing an unrelated database. Those external effects require coordinated state or idempotency. Ordering is per partition; choose an order or account key where relevant, and handle duplicates and stale versions at the consumer. A poison message needs an explicit quarantine/retry process and an operator-visible business status. [Apache Kafka delivery semantics](https://kafka.apache.org/41/design/design/)

If a message changes a payment or order state, do not blindly apply it because its timestamp looks newest. Clocks can differ and delivery can be delayed. Use legal state transitions and explicit versions/sequence numbers appropriate to the source.

## Scaling: find the bottleneck before adding servers

“I would measure where time and capacity are going. More application instances help only if the database, provider, or hot inventory row is not already the limiting resource.”

Start with an estimate. Suppose the stated peak is 1,000 requests per second and average time in the system is 0.2 seconds. That implies roughly 200 requests in flight on average. This is an illustrative application of arrival rate × time, not a complete capacity plan. Tail latency, bursts, dependencies, and failure headroom still matter.

Measure request rate, errors, the 95th/99th percentile latency, active requests, database query time, lock waits, connection-pool wait, garbage collection, queue age, and external-provider latency. Average latency alone hides the customers experiencing very slow service.

Useful changes, chosen by evidence:

- Keep application instances stateless, with durable business state outside process memory. Scale horizontally behind a load balancer.
- Fix missing indexes, inefficient joins, and one-query-per-item problems before increasing database connection counts.
- Cache product descriptions and other suitable reads with defined expiry/invalidation. Recalculate authoritative checkout prices and reserve stock at their owning services.
- Use read replicas where lag is acceptable. Route read-after-write-sensitive operations to a suitable authority or use a deliberate consistency strategy.
- Move emails, report generation, and large imports to bounded background workers. A queue absorbs a temporary burst; it cannot make an indefinitely overloaded consumer catch up.
- Protect online traffic from bulk work with separate concurrency budgets and database pools where appropriate.
- Partition only when necessary, and choose the key from access patterns. A single popular product remains a hot key even with many partitions.
- Rate-limit and shed load before every thread, connection, and queue is exhausted. Return a truthful retriable response rather than accept work that cannot be durably handled.

Do not assume `parallelStream()` makes database work faster. It can flood a limited connection pool, reorder processing, and put unrelated work onto shared execution resources. For blocking work use a deliberately bounded executor and concurrency limit based on downstream capacity; transactions do not automatically transfer to new threads.

### Timeouts, retries, circuit breakers, and bulkheads in easy language

A timeout is how long this call is allowed to consume resources. A retry is another attempt for a failure that may be temporary. A circuit breaker temporarily stops calls to a dependency that is repeatedly failing. A bulkhead gives different work separate capacity so one failure cannot consume everything.

Use an overall deadline as well as sensible connect/read timeouts. Retry only appropriate failures and only safe operations, with a small bounded count, increasing delays, and random variation to avoid synchronized retry storms. Avoid independent retries at every layer: three attempts at each of five layers can produce 243 downstream attempts. [AWS retry guidance](https://docs.aws.amazon.com/wellarchitected/latest/framework/rel_mitigate_interaction_failure_limit_retries.html)

A circuit breaker needs defined open, test/half-open, and closed behaviour. Its fallback must preserve truth: a cached product description can be acceptable; invented payment success cannot. Isolate payment and catalogue capacity if their failure modes differ. [AWS circuit-breaker pattern](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/circuit-breaker.html), [AWS bulkhead guidance](https://docs.aws.amazon.com/wellarchitected/2023-10-03/framework/rel_fault_isolation_use_bulkhead.html)

### Priority customers: fee and execution are separate decisions

Adding a 5% or 10% fee changes the bill. It does not make an order execute earlier.

If the promised service is expedited handling, record a trusted service tier with the order and schedule work accordingly. One approach is separate normal and priority queues with reserved worker capacity or weighted scheduling. Include aging or a minimum normal-service share so ordinary customers cannot wait forever. A concrete example is choosing up to three priority jobs then one normal job when both queues have work; actual weights require capacity testing and a service policy.

Do not reorder financially dependent operations for the same account simply because one customer bought priority. Preserve required per-account or per-order sequence. Priority admission must be bounded: if every customer buys it and capacity stays fixed, the promise cannot be met. Track waiting time per tier, and define what happens if the promised service is missed.

## Huge files and millions of rows

“I would first ask how large the data is in bytes and records, how quickly it must finish, whether records are independent, and whether the business needs all-or-nothing activation or can accept per-record results. I would not load a ten-gigabyte file into a Java list.”

A proposed import flow:

1. Accept the file through a controlled upload path, validate size and type, and store it durably. Record its checksum, source, owner, and import identity.
2. Return a job ID and an accepted response once the job is durably registered. Provide a status endpoint rather than hold an HTTP request open for hours.
3. Read progressively, validate each record, and write bounded batches. Stream parsing reduces application memory only if downstream collections and persistence caches remain bounded too.
4. Record checkpoints and make each business record safe to reprocess. If the process dies after committing data but before an external acknowledgment, some work can be presented again.
5. Keep malformed rows in a report with reason codes and counts. Define when one bad row rejects the file, rejects a group, or is allowed to be skipped. Do not silently skip a failed financial posting.
6. Partition independent work by a stable key/range. Preserve order for dependent changes to the same account/product. Cap workers based on database and provider capacity.
7. Compare accepted, rejected, duplicate, and processed totals. For monetary data, reconcile amounts per currency and source control totals, not just record counts.
8. Publish completion only after the defined validation and reconciliation steps finish. Keep restart and operator repair procedures auditable.

Spring Batch provides chunk-based reading, processing, and writing, transaction boundaries, job/step metadata, checkpoints, restart behaviour, and configurable skip/retry support. A chunk size is a tradeoff: larger chunks can reduce overhead but increase memory, locks, and work repeated after failure. Benchmark a starting value against the workload instead of claiming one universal size. The 4.3 documentation is relevant to historical Java 8-era code; current framework compatibility must be checked separately. [Spring Batch 4.3 reference](https://docs.spring.io/spring-batch/docs/4.3.x/reference/html/index-single.html)

For database scans, keyset pagination avoids repeatedly skipping ever-larger offsets:

```sql
SELECT id, product_id, quantity
FROM import_rows
WHERE import_id = :importId
  AND id > :lastId
ORDER BY id
LIMIT :batchSize;
```

This example assumes a stable staged import. A changing live dataset needs a snapshot or a carefully defined selection boundary; a cursor alone does not guarantee that all concurrent changes are captured. Save `lastId` only with the corresponding committed effects, or rely on safe replay if metadata and business updates cannot share a transaction.

For exports, stream to a durable output artifact and offer a controlled download. Do not send millions of rows as one enormous interactive API response. For an entire catalogue that must switch atomically, load and validate a new version in staging, then activate the version in one short transaction. Chunking a load is compatible with atomic publication, but does not itself provide it.

## Design tradeoffs the architect may ask about

**Why microservices?** “Independent ownership, deployment, scaling, and fault isolation can justify service boundaries. But remote calls, distributed recovery, observability, and operational cost increase. If the team and scale do not justify that cost, a modular application with clear boundaries can be a better starting point.”

**Why a relational database for orders?** “Orders and their lines have relationships and important uniqueness and transaction rules. A relational database is a strong starting point. I would use a search index for searching and a cache for suitable reads, while keeping an explicit source of truth.”

**One database per service?** “Each service owns writes to its data. Separate ownership matters more than claiming that every service must have separate hardware. Letting services freely update one another's tables creates hidden coupling and makes safe deployments difficult.”

**Will Redis prevent overselling?** “A cached stock count can help display availability. It should not accidentally become a competing authority. I need an atomic reservation rule and a recovery model at the chosen inventory authority. A distributed lock alone is not a substitute for durable invariants, especially when leases expire.”

**What if compensation fails?** “Persist the pending compensation, retry it safely, expose the unresolved business state, alert on its age, and provide an audited repair path. Do not declare the workflow rolled back when money or stock is still unresolved.”

**How do you troubleshoot an order stuck for 20 minutes?** “Follow its order ID and workflow ID through API traces, saga state, outbox age, broker lag, consumer failures, payment reference, and inventory reservation. Determine the last durable fact before deciding a recovery action. Replaying everything blindly can create further effects.”

**What proves the design works?** “Tests should cover concurrent last-item orders, repeated create requests, same key with changed inputs, process death after database commit, redelivered events, delayed payment success, reservation expiry racing confirmation, and compensation failure. Load tests should measure tail latency, queue age, and downstream capacity, not only requests per second.”

**What about disaster recovery?** “Define how quickly the service must recover and how much committed data it may lose; these are different objectives. Test restoring backups and replaying events. Reconcile state after failover, especially with external payments. Having backups is not proof that the business can recover.”

## Closing answer to rehearse aloud

“My design keeps prices and permissions authoritative on the server, stores accepted financial details immutably, and protects stock with an atomic reservation. Every meaningful operation has a durable identity so retries do not repeat the business effect. Local transactions protect local data; a saga coordinates service-level work; an outbox makes event publication recoverable. I represent uncertainty explicitly, especially around payments. Then I scale the measured bottleneck and keep slow background work from exhausting checkout capacity. The main goal is a system whose state we can explain and recover after a failure.”


---

# Oral practice: possible architect follow-up questions

These are practice prompts, not predictions. Answer aloud before reading the suggested response.

## 1. How would you apply a 10% discount and a 5% priority fee?

**Say:** “I first agree on the calculation order. Assuming the priority fee applies after the discount, a ₹1,000 eligible product becomes ₹900, then attracts a ₹45 priority fee, giving ₹945 before tax. I use `BigDecimal`, an explicit rounding rule, and a breakdown containing base amount, discount, fee, and final amount.”

**Deeper trap:** A 10% discount followed by a 10% fee gives ₹990, not ₹1,000. Explain whether rounding occurs per unit, line, or order; record the agreed rule.

## 2. Should this change the product's stored selling price?

**Say:** “Usually I keep the catalogue price separate from the customer's order calculation. Eligibility and service tier determine the adjustments for that purchase. I persist the accepted unit price and adjustments on the order, so a later catalogue update does not change yesterday's bill.”

**Deeper trap:** A browser's old quote is not automatically a valid current price. Use an explicit quote expiry/version or recalculate at confirmation, with customer acceptance when required.

## 3. Charging priority is easy. How do you actually provide priority?

**Say:** “The fee changes pricing; scheduling changes service. I store the authorized tier and route work through weighted queues or reserved capacity. I protect a minimum share for normal customers and measure waiting time for both groups.”

**Deeper trap:** If everyone buys priority, capacity does not increase. Admission limits and a missed-service policy matter. Preserve necessary ordering for dependent operations on the same order or account.

## 4. Two customers buy the last available item. What happens?

**Say:** “The inventory owner makes one atomic reservation decision. A conditional database update subtracts quantity only when sufficient stock remains. I check the affected-row count and save a unique reservation identity in the same transaction. Only one conflicting buyer can claim the final unit.”

**Deeper trap:** A successful conditional update can still be executed twice on a retry. Reservation deduplication is separate. A Java lock protects only one process, not all application instances.

## 5. What belongs in the controller versus the service?

**Say:** “The controller translates HTTP input and output. The service coordinates the business use case, and domain logic calculates pricing and enforces valid state changes. Repositories handle persistence. The server loads trusted prices and checks the authenticated customer's access; it does not trust a submitted final total.”

**Deeper trap:** Validation annotations cannot prevent every race. Unique database constraints and guarded updates remain necessary. Also, a local transaction does not include a remote payment API.

## 6. How do you prevent duplicate orders after a timeout?

**Say:** “I give one intended operation a stable idempotency key scoped to the customer and action. A durable unique constraint coordinates duplicates. The same key and same request returns the original result or current status. The same key with changed inputs is rejected.”

**Deeper trap:** Generating a fresh key for each retry defeats the mechanism. Storing the key separately from the local business commit creates a crash window; design that boundary deliberately.

## 7. A JWT has a valid signature. Why might you still reject it?

**Say:** “The signature proves it was signed by a key we trust; that is only one check. I also validate the expected issuer, intended audience, time restrictions, and required permissions. Then I check that the caller may act on this particular order or account.”

**Deeper trap:** A token intended for another API must not become acceptable merely because the issuer is shared. An ID token describes authentication to a client; it is not automatically an API access token.

## 8. Why does an HTTPS call fail when the certificate exists?

**Say:** “A certificate file alone does not establish trust. I inspect the certificate chain, expiry, hostname, trusted roots, clock, and protocol compatibility. For mutual TLS I also check the client certificate and private-key configuration. I fix the cause and test the actual connection path.”

**Deeper trap:** Disabling hostname checks or accepting every certificate hides the failure by removing authentication. If TLS ends at a gateway, explain how the next network hop is protected too.

## 9. A customer changes the order ID and sees another customer's order. Fix it.

**Say:** “This is a broken object-level authorization problem. Every read and mutation must verify that the authenticated identity is allowed to access that order. I include ownership or tenant scope in the query or enforce an equivalent server-side policy, then add cross-customer tests.”

**Deeper trap:** Random UUIDs make guessing harder but do not establish authorization. Authentication and a general `CUSTOMER` role do not prove ownership of a particular order.

## 10. How would you process a file with ten million records?

**Say:** “I register a durable background job and return its identifier. Workers read progressively, validate records, and commit bounded batches. I preserve checkpoints, deduplicate business records, report rejected rows, and reconcile counts and financial totals before declaring completion.”

**Deeper trap:** Chunking means several commits, not whole-file atomicity. If activation must be all-or-nothing, stage and validate a version, then switch the active version. Do not silently skip failed monetary entries.

## 11. More application instances made the database slower. Why?

**Say:** “Each instance adds concurrent queries and often its own connection pool. Together they may exceed database capacity and increase lock or connection waits. I inspect slow queries, pool wait, active connections, and database saturation, then limit concurrency and fix the bottleneck.”

**Deeper trap:** A larger pool is not automatically faster. Separate or budget bulk-processing capacity so imports cannot consume all checkout connections. Scale using measured latency and resource pressure.

## 12. Payment timed out during a Saga. Should you immediately compensate?

**Say:** “A timeout leaves the outcome unknown. I preserve the payment-attempt identity, query the provider, and process verified callbacks and reconciliation. A safe retry reuses the same provider-supported identity. I do not start a new charge while the first may have succeeded.”

**Deeper trap:** Reservation expiry can race with a late payment success. Define the permitted state transitions and resulting refund or fulfilment action. A refund can also fail and remain pending.

## 13. Does an outbox guarantee that the consumer acts exactly once?

**Say:** “The outbox commits the business change and event record together. The relay may resend if it crashes after publishing. The consumer therefore records a unique message identity with its database effect in one transaction, or uses an equivalent safe design.”

**Deeper trap:** Broker guarantees do not automatically cover an external charge or email. Each external effect needs its own contract, identity, and recovery behaviour.

## 14. Why not split every component into a microservice?

**Say:** “I split where ownership, independent scaling, deployment, or fault isolation justify it. Every remote boundary adds latency and partial failure. Clear modules in one application may be simpler initially. I would explain the business reason for each service boundary.”

**Deeper trap:** More services do not automatically mean better scaling. A shared hot database or tightly coupled synchronous call chain can retain the bottleneck while adding operational work.

## A realistic preparation schedule

For **Tuesday, 29 September 2026, at 8:00 p.m. IST**, plan **4 hours 30 minutes of focused study**, plus breaks. Adjust daytime blocks around work commitments.

| When | Focus |
|---|---|
| Tonight, 60 minutes | Trace the Java pricing code; calculate normal, discounted, 5% priority, and 10% priority examples aloud |
| Tonight, 10-minute break | Step away from the screen |
| Tonight, 45 minutes | Security: identity versus permission, OAuth flow, audience, TLS, and concrete vulnerability fixes |
| Tomorrow, 45 minutes | Draw order → inventory → payment; explain retries, uncertainty, Saga, and outbox |
| Tomorrow, 10-minute break | Rest before the next block |
| Tomorrow, 45 minutes | Practise last-item concurrency, huge imports, database capacity, and failure recovery |
| Tomorrow, 6:00–6:45 p.m. | Mock interview: answer these prompts without reading; revisit only weak answers |
| Tomorrow, 6:45–7:00 p.m. | Break |
| Tomorrow, 7:00–7:30 p.m. | Rehearse two truthful project examples and your opening explanation |
| Tomorrow, 7:30–8:00 p.m. | Check the meeting setup, keep water nearby, and settle in |

Use truthful project examples: problem, decision, tradeoff, and observed result. Describe unfamiliar patterns as proposed designs rather than invented production experience.
