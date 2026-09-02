# Architecture decision records

One record per decision, in reading order. Each states the context, the decision, the alternatives
that were rejected and why, and the consequences that were accepted along with it.

1. [Concurrency: the database is the arbiter](2026-09-02-concurrency-the-database-is-the-arbiter.md)
2. [Redemption is inserted before the counter is incremented](2026-09-02-redemption-is-inserted-before-the-counter-is-incremented.md)
3. [Value objects with a private constructor and a `from()` factory](2026-09-02-value-objects-with-a-private-constructor-and-a-from-factory.md)
4. [GeoIP is fail-closed](2026-09-02-geoip-is-fail-closed.md)
5. [Client IP comes from `remoteAddr`, with no forwarded-header handling](2026-09-02-client-ip-comes-from-remote-address.md)
6. [One feature slice, ports and adapters, beans wired by hand](2026-09-02-one-feature-slice-ports-and-adapters-manual-wiring.md)
