# Architecture decision records

Two decisions needed more than a paragraph: how the service stays correct when requests race, and
what happens when the country behind an IP cannot be established. Each record states the context, the
decision, the alternatives that were rejected and why, and the consequences accepted along with it.
Both were written up in retrospect, dated to the day the decision was made. The smaller choices are
summarised in the [README](../../README.md#design-decisions).

1. 2026-08-28 — [Concurrency: the database is the arbiter](2026-08-28-concurrency-the-database-is-the-arbiter.md)
2. 2026-08-28 — [GeoIP is fail-closed](2026-08-28-geoip-is-fail-closed.md)
