# Decision 0003: calculate order adjustments without changing catalogue prices

Status: accepted for the explicitly requested interview pricing slice, 2026-09-28.

Keep the Spring Boot 4 application on its existing Java 21 runtime. Put the discount and priority-fee calculation in five dependency-free Java 8-compatible classes. Do not downgrade the framework to satisfy an algorithm demonstration.

A 10% campaign covers all or selected product IDs. Apply the user-approved rule: round the line discount, subtract it, then round a 5% or 10% paid priority fee on the remainder. Store all amounts and the selected service level with the order. The currency assumption is two decimal places; currency/tax/refund models remain future work.

Priority is an explicitly selected paid service, not authenticated customer membership. The app has no account model. Membership-based eligibility requires a trusted customer source; actual priority execution requires a separate durable scheduling policy.

Use a short product-row lock for stock reservation and catalogue price writes. It prevents lost updates between these application paths at the cost of serializing work on a hot product. Multi-product lock ordering, lock timeouts, and API idempotency are separate future concerns.

V2 preserves old monetary totals and adds database checks matching the calculation. This teaching migration requires old writers to stop; it is not rolling-upgrade compatible. Production large-table upgrades require staged compatibility and backfill work.

The scope does not add authentication, OAuth, TLS configuration, payments, Saga, outbox, or queue processing. Those subjects are prepared interview explanations, not implemented guarantees.
