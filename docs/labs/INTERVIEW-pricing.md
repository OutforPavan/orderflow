# Interview pricing implementation evidence — 2026-09-28

Status: prepared and trainer-verified; learner practice and explanation not yet reviewed.

The explicit request was a 10% product campaign plus 5%/10% paid priority charges, with Java 8 example code. The learner selected fee-after-discount. Five dependency-free classes implement that calculation; the application remains Java 21 / Spring Boot 4.

## Observed verification before applying to the working project

- Compiled the five core/demo files with `javac --release 8`; passed. This establishes source/API/bytecode targeting, not an actual JDK 8 runtime execution.
- Ran the standalone demo: eligible product standard 900.00, priority 5% 945.00, priority 10% 990.00; ineligible priority 5% 1050.00; original price remained 1000.00.
- Staged Java 21 Maven verification: 74 tests, zero failures/errors/skips, and package creation passed.
- Applied V2 to the dedicated `orderflow_test` database during integration verification. No learning/production database migration was run by these tests.
- The test suite checks rounding, selected/all/no campaign, invalid input, defensive copying, both paid tiers, client money-field rejection, persisted snapshots, later price changes, database monetary constraints, rollback, and simultaneous attempts to buy the last item.
- In the concurrent test, exactly one buyer succeeded, stock became zero, and exactly one order existed.
- Test-created rows were cleaned up. Existing unrelated project files were not part of the staged source change.

## Deployment and scope limits

The V2 migration is for a stopped single-version learning app. Old app instances cannot keep inserting after it. Production rolling deployment and large-table backfill need a staged plan.

This feature calculates and stores pricing. It does not add authentication, customer entitlements, order idempotency, payment collection, priority execution, Saga, outbox, or full currency/tax/refund handling. Those are prepared reference topics in [the interview guide](../ARCHITECT-INTERVIEW-GUIDE.md).

## Learner exercise, still pending

Run a standard, 5%, and 10% order for the same eligible product. Predict the rounding example for 0.05 × 3. Explain why later catalogue changes do not alter the saved order, why a row lock is different from idempotency, and why a fee does not provide scheduling priority. No learner answer or elapsed study time is inferred from the prepared material.

## Final working-project verification

After applying the reviewed changes to `/Users/pavtiwar/orderflow`, the required `./dev verify` completed successfully: 74 tests, zero failures/errors/skips, and packaged application produced. The standalone Java 8-compatible demonstration and staged verification are recorded above. This records trainer verification, not learner practice.
