# Four-service interview lab — 2026-10-07

The learner explicitly requested this implementation and selected Keycloak and
locally generated certificates. This resumes the requested feature work. Earlier
class walkthroughs and learner checkpoints remain open.

See [walkthrough](../MICROSERVICES-LAB.md) and
[architecture decision](../decisions/0005-four-services-durable-saga-and-trust.md).

## Trainer verification

Staging verification on 2026-10-07 completed with Java 21 / Spring Boot 4.1.1:

- Full `./dev verify`: 93 tests, no failures, errors or skips: security 6, gateway 22,
  inventory 18, payment 13, order/Saga 10 and preserved pricing 24.
- Real PostgreSQL tests cover concurrent/idempotent reservation, competing stock
  claims, durable payment ledger rollback, payload conflicts, leased Saga recovery,
  compensation retry and a simulated lost payment response.
- `./scripts/platform-smoke --resilience`: 13 integration groups passed with real
  Keycloak tokens and generated certificates, including JWT/roles/ownership,
  cache hit/invalidation, checkout/replay/conflict, decline/release, mTLS identity,
  untrusted CA and hostname rejection, OPEN payment circuit, OrderService restart,
  payment recovery and inventory outage/recovery. Fixture product 1 ended at stock
  31 after nine purchased units; declined checkout restored its reservation.
- Live integration initially rejected tokens without `sub`. Adding Keycloak's
  `basic` default client scope fixed the realm template; validation stayed strict.
- Independent review found no blocking Saga correctness issue. Runtime review
  corrected custom PKI directory handling and rejects mismatched existing DB ports.

These checks establish implementation behavior, not learner understanding.
Local lab artifacts are ignored; no credentials, keys or access tokens are committed.

## Learner practice and review: pending

- Trace a JWT from Keycloak through gateway and OrderService; distinguish token
  validation, authorization and resource ownership from TLS server/client identity.
- Submit the same checkout twice; predict one durable order and one reservation/payment.
- Stop PaymentService. Explain why the order stays pending and stock stays reserved.
- Observe OPEN, HALF_OPEN and CLOSED behavior; explain timeout versus circuit versus retry.
- Restart OrderService while an order is pending and explain the DB lease and stable keys.
- Decline payment and trace the stock-release compensation, including release failure.
- Explain why a cached stock value cannot authorize checkout and why stale display
  availability does not make the entire system AP.
- Explain what changes for a real provider, multiple replicas, managed certificates,
  a browser login, and replicated database failover.

No learner execution or reviewed explanation is inferred from trainer tests.
The previous timeout/idempotency prediction remains the only reviewed answer for
this distributed-checkout slice. Curriculum completion remains 0/95 and 0/14.

## Verified in the learner's checkout

The implementation was installed in `/Users/pavtiwar/orderflow` and tested again
with newly generated CA/app certificates, a fresh platform database cluster and
fresh Keycloak realm. The original learning database was retained.

- `./dev verify`: all 93 platform tests passed again, no failures/errors/skips.
- `./dev -f legacy-monolith/pom.xml verify`: all 74 preserved application tests
  passed after correcting their moved configuration path.
- `./scripts/platform-smoke --resilience`: all 13 groups passed in the installed
  checkout; fixture product 2 finished at stock 31 as expected.
- Java 21 startup was verified in each application log. Local launchers now prefer
  the project JDK over a different shell default and handle exited/zombie processes
  safely while preserving checks against signaling unrelated processes.
- The live harness waits for Keycloak issuer readiness before requesting tokens.
- Keycloak's `start-dev` command forces HTTP on. The launcher now explicitly uses
  `start --db=dev-file --cache=local --http-enabled=false`; the final startup log
  lists only `https://127.0.0.1:8443`. File storage/local cache remain lab limitations.
- All applications were restored after the outage exercises. Generated keys,
  credentials, bearer tokens, database files, build outputs and logs remain ignored.

These observations are trainer verification on 2026-10-07. No additional learner
answer, live exercise, study duration, or curriculum completion is inferred.
