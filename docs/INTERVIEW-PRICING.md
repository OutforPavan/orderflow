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
