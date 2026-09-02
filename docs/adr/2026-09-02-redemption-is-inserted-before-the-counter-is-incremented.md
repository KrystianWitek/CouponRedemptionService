# Redemption is inserted before the counter is incremented

## Context

A successful redemption produces two writes: a row in `coupon_redemption` and an increment of
`coupon.current_usage_count`. They must agree — the counter is meaningful only if it equals the
number of redemption rows for that coupon — and both can fail for legitimate business reasons: the
user may already have redeemed the coupon, or the coupon may have run out of uses since it was
loaded.

Given that
[the database decides both outcomes](2026-09-02-concurrency-the-database-is-the-arbiter.md), two
questions remain: in which order the two statements run, and where the transaction boundary is
declared.

## Decision

`DefaultCouponRedemptionService.redeem` performs the coupon lookup, the GeoIP country resolution and
the country comparison **outside** any transaction, then wraps exactly the two writes:

```kotlin
transactionOperations.execute {
    val couponRedemption = createRedemption(coupon.id, userId)

    saveRedemption(couponRedemption)   // INSERT ... ON CONFLICT DO NOTHING
    incrementUsage(coupon)             // UPDATE ... WHERE current_usage_count < max_usage_count

    couponRedemption
}
```

The insert goes first, and the boundary is the injected `TransactionOperations`, not `@Transactional`
on the service. The `@Transactional` annotations on the repository adapters merely join the
surrounding transaction; they exist so the adapters are also usable on their own.

**Why the insert is first.** The unique `(coupon_id, user_id)` constraint is what detects a repeat
user, and it can only do so once the row is offered to the database. With the insert first, the
duplicate is discovered by `affectedRows == 0` on the `INSERT ... ON CONFLICT DO NOTHING` — no
`SELECT` is issued, so there is no window between "checked" and "acted" for a concurrent request to
slip into. Reversing the order would mean incrementing the counter first and only then discovering
the duplicate; the increment would have to be undone, and until the rollback completed the coupon
would have one fewer use available than it really did.

**Why a failed increment still rolls back cleanly.** `incrementUsage` throws
`CouponUsageLimitReachedException` when the conditional `UPDATE` matches no row. That propagates out
of the `execute` block, so the transaction rolls back and the redemption row that was just inserted
disappears with it. A rejected attempt therefore consumes nothing: no use, no redemption row, and no
possibility for the counter to drift away from the number of rows.

**Why the lookup and the GeoIP call are outside.** The country resolution is an outbound HTTP call to
a third-party provider. Inside a transaction it would pin a pooled JDBC connection for the whole
round-trip, so provider latency would translate directly into connection-pool exhaustion. Outside,
the transaction lives only as long as the two statements. Nothing is lost by resolving the country
first: the country check cannot be affected by a concurrent write, since neither the coupon's
country nor the caller's IP changes mid-request.

**Why `TransactionOperations` instead of `@Transactional`.** The boundary is a deliberate design
element of this flow — the two statements belong together and nothing else does — so it is stated in
the code that owns it rather than inferred from a proxy. It is also honest about scope: an
annotation on the service class would enclose the coupon lookup and the GeoIP call as well, which is
precisely what must not happen.

## Consequences and Risks

- `application` gains one Spring import, `org.springframework.transaction.support.TransactionOperations`.
  This is an accepted, bounded cost: it is a narrow two-method interface rather than an annotation
  contract with proxying semantics, it is the only framework type the layer touches, and it is
  substitutable — the unit tests inject `FakeTransactionOperations` and run the service with a plain
  constructor, no Spring context involved.
- The write order is pinned by `CouponRedemptionTransactionIntegrationTest`, which drives the service
  against a real database: an increment that loses to the limit leaves no redemption row behind, and
  an already exhausted coupon is rejected without the GeoIP provider being called at all.
- The window between the country check and the transaction is real but harmless — the only state that
  can change in it is the usage count, and the conditional `UPDATE` re-evaluates that anyway.
- A repeat request from a user who already redeemed the coupon still pays for a GeoIP round-trip
  before the duplicate is detected, because the country check precedes the transaction. That is a
  cost of not doing a pre-`SELECT`; see
  [the GeoIP decision](2026-09-02-geoip-is-fail-closed.md) for the options that would remove it.

## Status

ACCEPTED

## Authors

Krystian Witek
