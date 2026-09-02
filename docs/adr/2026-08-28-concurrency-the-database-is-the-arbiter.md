# Concurrency: the database is the arbiter

## Context

Two rules of the service are only ever at risk from concurrency, because several requests can target
the same coupon at the same instant: a coupon may be redeemed at most `max_usage_count` times, first
come, first served, and a given user may redeem a given coupon at most once.

Anything that reads the current state into the application, decides, and then writes it back is a
check-then-act sequence: another request can commit between the read and the write, so two callers
can both see "one use left" and both consume it, and a `SELECT` that finds no earlier redemption is
no promise that the `INSERT` will still be first.

## Decision

Neither invariant is decided in Kotlin. Each is expressed as a single conditional SQL statement whose
predicate is evaluated by PostgreSQL under row-level locking:

```sql
INSERT INTO coupon_redemption (id, coupon_id, user_id, redeemed_at)
VALUES (:id, :couponId, :userId, :redeemedAt)
ON CONFLICT (coupon_id, user_id) DO NOTHING;

UPDATE coupon
SET current_usage_count = current_usage_count + 1
WHERE id = :couponId
  AND current_usage_count < max_usage_count;
```

The adapters return `affectedRows == 1`, so "did I win?" is answered by the database. Coupon creation
follows the same shape with `ON CONFLICT (code) DO NOTHING`. The predicates depend on two
schema-level constraints, `code` unique and `(coupon_id, user_id)` unique; there is no
read-modify-write anywhere on these paths, and no application-level lock.

### Order of the writes and the transaction boundary

The transaction encloses the two statements above, in that order, and nothing else. The coupon
lookup, the early rejection of an exhausted coupon, the GeoIP call and the country comparison all run
before it, because resolving the country is an outbound HTTP call: inside the transaction it would pin
a pooled JDBC connection for the whole round-trip, and provider latency would turn into
connection-pool exhaustion. For the same reason the boundary is programmatic (the one Spring type the
application layer touches) rather than `@Transactional` on the service method, which would enclose
the lookup and the GeoIP call as well.

The insert goes first because the unique `(coupon_id, user_id)` constraint is what detects a repeat
user, and it can only do so once the row is offered to the database: the duplicate shows up as
`affectedRows == 0`, with no `SELECT` and therefore no check-then-act window. A row lock lives until
the transaction ends, so increment-first would take the lock on the contended `coupon` row before the
insert and hold it across both writes, while insert-first takes that lock in the final statement and
holds it only until commit. A failed increment rolls the transaction back, and the redemption row
with it: a rejected attempt consumes nothing.

### Rejected alternatives

**Optimistic locking with `@Version`.** Correct and idiomatic JPA, but it converts contention into
failure: of N concurrent redemptions of one hot coupon, one commits and N-1 fail with an
optimistic-locking exception. Those are not business rejections (the coupon may well have uses left),
so each has to be retried, which means writing, tuning and testing a retry loop and either re-running
the GeoIP lookup or carefully arranging not to. The conditional `UPDATE` needs none of that: a
request either succeeds or gets a business rejection, and there is no third outcome to retry.

**Pessimistic locking with `SELECT ... FOR UPDATE`.** Also correct, but it locks the coupon row at
the start of the transaction instead of in its final statement, and tempts one to put the GeoIP call
inside the lock, where a slow external HTTP call serialises every request for that coupon. It also
buys nothing here: the protected work is a single arithmetic update the database can already perform
atomically.

**Application-level locking (`synchronized`, a `ReentrantLock`, a striped lock keyed by coupon
code).** Cheap to write, and it passes a single-process test suite, which is what makes it dangerous.
It guards one JVM: two instances behind a load balancer share no monitor, so the invariant silently
stops holding the moment the service is scaled out, and scalability is a stated requirement.

## Consequences and Risks

- Correctness under load does not depend on the number of application instances. Every instance races
  against the same rows and the same constraints, so N replicas behave like one.
- The write side of these invariants lives in SQL, not in the domain model. `Coupon.init` still checks
  `currentUsageCount <= maxUsageCount`, but that is a read/mapping assertion against a bad row or a
  bad mapper, not what stops the counter overshooting. Anyone extending these paths must keep the
  rule in the statement: a "convenient" `coupon.copy(currentUsageCount = ...)` save would reintroduce
  the race.
- An exhausted coupon is rejected *before* the transaction, on the coupon loaded a moment earlier, so
  it pays for neither a GeoIP round-trip nor a transaction. That is safe by construction because
  `current_usage_count` only ever increases: the only statement touching it increments it, and a
  rejected attempt is rolled back, so a use is never given back. A stale read can therefore only be
  *lower* than the committed value, producing at worst a false "proceed" that the conditional
  `UPDATE` catches, and never a false rejection.
- A repeat request from a user who already redeemed the coupon still pays for a GeoIP call before the
  duplicate is detected: the cost of not doing a pre-`SELECT`.
- The contention point is one row per coupon: a single extremely hot coupon serialises on it, while
  unrelated coupons do not contend at all. Sharding the counter across rows would relieve that at the
  cost of an exact limit, and is not warranted at this scale.
- The statements are PostgreSQL-flavoured native SQL, so the persistence adapter is tied to
  PostgreSQL. Accepted; PostgreSQL is not meant to be swappable here.

## Status

ACCEPTED. The usage limit was decided on 2026-08-28; the unique index on `(coupon_id, user_id)` and
the order of the two writes followed on 2026-08-31, when repeat redemptions were handled.

## Authors

Krystian Witek
