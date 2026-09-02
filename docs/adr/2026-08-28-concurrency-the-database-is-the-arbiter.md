# Concurrency: the database is the arbiter

## Context

Two rules of the service are contended by definition, because several requests can target the same
coupon at the same instant:

- a coupon may be redeemed at most `max_usage_count` times — first come, first served,
- a given user may redeem a given coupon at most once.

Anything that reads the current state into the application, decides, and then writes it back is a
check-then-act sequence. Between the read and the write another request can commit, so two callers
can both see "one use left" and both consume it. The same holds for "has this user already redeemed
it?" — a `SELECT` that returns nothing is not a promise that it will still return nothing when the
`INSERT` runs.

The usage-limit part of this record dates from 2026-08-28; the unique index on `(coupon_id, user_id)`
and the order of the two writes were added on 2026-08-31, when repeat redemptions were handled.

## Decision

Neither invariant is decided in Kotlin. Each is expressed as a single conditional SQL statement whose
predicate is evaluated by PostgreSQL under row-level locking, and the adapter reads the outcome from
the affected-row count:

```sql
INSERT INTO coupon_redemption (id, coupon_id, user_id, redeemed_at)
VALUES (:id, :couponId, :userId, :redeemedAt)
ON CONFLICT (coupon_id, user_id) DO NOTHING;

UPDATE coupon
SET current_usage_count = current_usage_count + 1
WHERE id = :couponId
  AND current_usage_count < max_usage_count;
```

`CouponRedemptionRepositoryAdapter.createIfAbsent` and
`CouponRepositoryAdapter.incrementUsageIfAvailable` both return `affectedRows == 1`, so "did I win?"
is answered by the database, not inferred from a prior read. Coupon creation follows the same shape
with `ON CONFLICT (code) DO NOTHING`. The predicates rest on schema-level guarantees declared in
`V1__create_coupon_tables.sql`: `code` is `UNIQUE` and `coupon_redemption` carries
`UNIQUE (coupon_id, user_id)`. There is no read-modify-write anywhere on these two paths, and no
application-level lock.

### Order of the writes and the transaction boundary

`DefaultCouponRedemptionService.redeem` loads the coupon, resolves the country through GeoIP and
compares it — all outside any transaction — then runs the two statements above, in that order, inside
`transactionOperations.execute { ... }`.

The insert goes first because the unique `(coupon_id, user_id)` constraint is what detects a repeat
user, and it can only do so once the row is offered to the database: the duplicate shows up as
`affectedRows == 0`, with no `SELECT` and therefore no check-then-act window. Increment-first would
take the lock on the contended `coupon` row before the insert and hold it across both writes;
insert-first acquires the hot-row lock last and holds it for the duration of one statement. A failed
increment throws `CouponUsageLimitReachedException` out of the `execute` block, so the transaction
rolls back and the redemption row goes with it — a rejected attempt consumes nothing.

The lookup and the GeoIP call stay outside the transaction because the country resolution is an
outbound HTTP call: inside, it would pin a pooled JDBC connection for the whole round-trip, and
provider latency would turn into connection-pool exhaustion.

The boundary is the injected `TransactionOperations`, not `@Transactional` on the service: the
`execute` block encloses the two statements and nothing else, whereas an annotation on the service
method would also enclose the coupon lookup and the GeoIP call. The `@Transactional` annotations on
the adapter methods merely join the surrounding transaction.

### Rejected alternatives

**Optimistic locking with `@Version`.** Correct and idiomatic JPA, but it converts contention into
failure: of N concurrent redemptions of one hot coupon, one commits and N-1 fail with
`OptimisticLockingFailureException`. Those are not business rejections — the coupon may well have
uses left — so each has to be retried, which means writing, tuning and testing a retry loop (attempt
budget, backoff, behaviour when the budget runs out) and either re-running the GeoIP lookup or
carefully arranging not to. The conditional `UPDATE` needs none of that: a loser is either a
legitimate business rejection or nothing at all.

**Pessimistic locking with `SELECT ... FOR UPDATE`.** Also correct, but it holds the coupon row
locked for the remainder of the transaction rather than for one statement, and tempts one to put the
GeoIP call inside the lock, where a slow external HTTP call serialises every request for that coupon.
It also buys nothing here — the protected work is a single arithmetic update the database can already
perform atomically.

**Application-level locking (`synchronized`, a `ReentrantLock`, a striped lock keyed by coupon
code).** Cheap to write, and it passes a single-process test suite, which is what makes it dangerous.
It guards one JVM: two instances behind a load balancer share no monitor, so the invariant silently
stops holding the moment the service is scaled out — and scalability is a stated requirement.

## Consequences and Risks

- Correctness under load does not depend on the number of application instances. Every instance races
  against the same rows and the same constraints, so N replicas behave like one.
- The write side of these invariants lives in SQL, not in the domain model. `Coupon.init` still checks
  `currentUsageCount <= maxUsageCount`, but that is a read/mapping assertion against a bad row or a
  bad mapper; it is not what stops the counter overshooting. Anyone extending these paths must keep
  the rule in the statement — adding a "convenient" `coupon.copy(currentUsageCount = ...)` save would
  reintroduce the race.
- `DefaultCouponRedemptionService.rejectExhaustedCoupon` runs *before* the transaction, on the coupon
  loaded a moment earlier, so an already exhausted coupon is rejected without paying for a GeoIP
  round-trip and a transaction. It is safe by construction because `current_usage_count` only ever
  increases — the only statement touching it increments it, and a rejected attempt is rolled back, so
  a use is never given back. A stale read can therefore only be *lower* than the committed value,
  producing at worst a false "proceed" that the conditional `UPDATE` catches, and never a false
  rejection.
- `application` gains one Spring import, `org.springframework.transaction.support.TransactionOperations`
  — a narrow two-method interface, the only framework type the layer touches, and replaceable in
  tests with a hand-written stand-in.
- A repeat request from a user who already redeemed the coupon still pays for a GeoIP round-trip
  before the duplicate is detected, because the country check precedes the transaction. That is the
  cost of not doing a pre-`SELECT`.
- The contention point is one row per coupon: a single extremely hot coupon serialises on it, while
  unrelated coupons do not contend at all. Sharding the counter across rows would relieve that at the
  cost of an exact limit, and is not warranted at this scale.
- The statements are PostgreSQL-flavoured native SQL, so the persistence adapter is tied to
  PostgreSQL — accepted, because the database is a chosen part of the design rather than a detail kept
  swappable.

## Status

ACCEPTED

## Authors

Krystian Witek
