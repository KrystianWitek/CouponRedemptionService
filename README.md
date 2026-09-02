# Coupon Redemption Service

[![CI](https://github.com/KrystianWitek/CouponRedemptionService/actions/workflows/ci.yml/badge.svg)](https://github.com/KrystianWitek/CouponRedemptionService/actions/workflows/ci.yml)

REST service for creating country-restricted discount coupons and recording their redemption. It is
built around one hard problem — staying correct when many requests race for the last use of the same
coupon — and the reasoning behind every notable choice is in [`docs/adr/`](docs/adr/README.md).

## Guarantees

- Usage limits and one-redemption-per-user hold under concurrent requests, on any number of
  instances — [decisions 1 and 5](#design-decisions).
- A redemption row and its usage count commit together; a rejected attempt consumes neither —
  [decision 2](#design-decisions).
- Coupon codes are unique regardless of letter case — [decision 3](#design-decisions).
- Redemption is refused when the country is unresolvable or wrong — [decision 4](#design-decisions).

## What it does

- `POST /api/v1/coupons` — create a coupon: unique code, maximum number of uses, target country.
- `POST /api/v1/coupons/redeem` — record a use of a coupon by a user.

Rules enforced on redemption: the code must exist (codes are case-insensitive); the coupon must not
have reached its maximum number of uses, first come first served; the caller's country, resolved from
the client IP through a GeoIP provider, must match the coupon's country; and a given user may redeem
a given coupon only once — that last rule was **optional** in the assignment and is implemented,
including under concurrency. Every rejection has its own `errorCode` ([`http/README.md`](http/README.md)).

## Quick start

```bash
docker compose up --build
```

The application listens on [http://localhost:8080](http://localhost:8080), with
[liveness](http://localhost:8080/actuator/health/liveness) and
[readiness](http://localhost:8080/actuator/health/readiness) probes under
[`/actuator/health`](http://localhost:8080/actuator/health); runnable requests are in
[`http/README.md`](http/README.md). `docker compose down` stops it, `--volumes` also drops the data.

Redemption cannot succeed from your own machine: loopback callers are refused before the GeoIP
provider is contacted, and callers seen through the Docker network gateway carry a private address the
provider cannot geolocate. Both answer `503 GEO_IP_LOOKUP_FAILED` — [decision 4](#design-decisions).

## API

Endpoints are versioned with the `/api/v1` path prefix, so a future breaking change can ship under
`/api/v2` without breaking existing clients. Requests, responses and the error contract are in
[`http/README.md`](http/README.md); while the application runs, the same contract is browsable as
[Swagger UI](http://localhost:8080/swagger-ui.html) or [OpenAPI](http://localhost:8080/v3/api-docs).

## Data model

A coupon carries the five fields the domain needs — a unique `code`, a `created_at` timestamp,
`max_usage_count`, `current_usage_count`, a `country_code` — plus a surrogate `id`. Each use is a row
in `coupon_redemption` (`coupon_id`, `user_id`, `redeemed_at`), an auditable log rather than only a
number. Two unique constraints in
`V1__create_coupon_tables.sql` are contract, not incidental indexes: `coupon.code` makes a code
collision a database decision, and `UNIQUE (coupon_id, user_id)` detects a repeat user with no
`SELECT`. Flyway owns the schema; Hibernate only validates the entities against it.

## Design decisions

1. **The database is the arbiter of both contended rules.** Each is one conditional statement —
   `UPDATE ... WHERE current_usage_count < max_usage_count` and
   `INSERT ... ON CONFLICT (coupon_id, user_id) DO NOTHING`, the adapter reading `affectedRows == 1`;
   never a read-modify-write in Kotlin. Rejected: `@Version` optimistic locking (correct, but a hot
   coupon becomes a retry storm), `SELECT ... FOR UPDATE` (locks a row for a whole transaction),
   application locks (one JVM only, so they break on scale-out)
   ([ADR](docs/adr/2026-09-02-concurrency-the-database-is-the-arbiter.md)).
2. **The redemption row is inserted before the counter is incremented,** in a transaction opened
   through an injected `TransactionOperations` rather than `@Transactional` on the service. Insert
   first lets the unique constraint detect a repeat user with no `SELECT` and no TOCTOU window; a
   failed increment throws and rolls that insert back, so a rejected attempt never consumes a use.
   Lookup and GeoIP stay outside, so no pooled connection waits on an HTTP round-trip
   ([ADR](docs/adr/2026-09-02-redemption-is-inserted-before-the-counter-is-incremented.md)).
3. **`CouponCode`, `CountryCode` and `UserId` are value classes with a private constructor and a
   `from()` factory** that normalises and then validates, so an invalid instance cannot be built.
   Case-insensitivity then comes for free at the API boundary and a plain `UNIQUE` on `code`
   suffices — no `LOWER(code)` index, no `citext`, no query path that forgot to normalise.
   `CountryCode` also rejects anything outside `Locale.getISOCountries`, making `XX` a `400` rather
   than an unredeemable coupon ([ADR](docs/adr/2026-09-02-value-objects-with-a-private-constructor-and-a-from-factory.md)).
4. **GeoIP is fail-closed and the client IP is `remoteAddr`.** Excluded address, `success: false`,
   HTTP error, timeout, unparsable country — every failure mode becomes one `GeoIpLookupException`
   and a `503`, because a country-restricted coupon must not become redeemable just because the
   provider is down ([ADR](docs/adr/2026-09-02-geoip-is-fail-closed.md)). `X-Forwarded-For` is
   ignored: trusting it without a trusted-proxy allow-list would let any caller spoof its country
   ([ADR](docs/adr/2026-09-02-client-ip-comes-from-remote-address.md)).
5. **One feature slice, ports and adapters, beans wired by hand.** The dependency direction is
   `api → application → domain ← infrastructure`, `domain/` is free of Spring, Jakarta and Hibernate
   imports, and JPA entities stay separate from the domain models so the adapter can issue the native
   SQL decision 1 rests on.
   `Default*` implementations are `internal` and constructed in `CouponConfiguration` rather than
   annotated `@Service`, keeping the application layer unit-testable with a plain constructor
   ([ADR](docs/adr/2026-09-02-one-feature-slice-ports-and-adapters-manual-wiring.md)).

## Scaling out

The application holds no state of its own: no in-memory counters, no caches, no sticky sessions, no
application-level locks. Every invariant is enforced by the database, so N instances behind a load
balancer behave exactly like one — threads racing inside one JVM and replicas racing across a cluster
meet the same statements. The only contention point is a single coupon row; unrelated coupons never
contend. `db` is in the readiness group, so an instance that loses its database leaves the rotation.

## Deliberate scope boundaries

Left out on purpose, not overlooked:

- **Authentication and authorization** — out of scope for the assignment; the user identifier is an
  opaque string from the request body.
- **Rate limiting** — belongs at the ingress; its absence does not affect correctness.
- **Coupon validity window and deactivation** — not part of the specified coupon model.
- **Idempotency keys** — a client retrying after a timeout may create a redemption it never saw the
  response for; the per-user rule bounds that to one use.
- **Read endpoints** — nothing required needs one; exposing coupon state is a product decision.

## What I would add next, in priority order

1. **Coupon validity window** — `valid_from`/`valid_until` as a migration plus one more predicate in
   the conditional `UPDATE`, so time is decided by the statement that decides the limit.
2. **Idempotency key on redeem** — unique per coupon and user, so a retried request returns the
   original result instead of racing itself.
3. **Pooled HTTP client with cache and circuit breaker for GeoIP** — `SimpleClientHttpRequestFactory`
   pays connection setup per redemption, and a breaker would keep a slow provider off request threads.
4. **Trusted-proxy configuration for forwarded headers** — required before a load balancer fronts it.
5. **A coupon read endpoint** — to inspect remaining uses without querying the database.

## Testing

Two source sets, split by what they need to run:

- `./gradlew test` — `src/test`, no Docker, seconds. Services run against hand-written fakes
  (`InMemoryCouponRepository`, `FakeTransactionOperations`), the controller through `@WebMvcTest`.
- `./gradlew integrationTest` — `src/integration`, on a real `postgres:16-alpine` Testcontainer,
  because the guarantees under test are the database's. `./gradlew check` runs both, as CI does.

Decisions 1 and 2 are verified empirically, not asserted: `CouponUsageIntegrationTest` fires 20
concurrent increments at a coupon limited to 5 uses, and `CouponRedemptionConcurrencyIntegrationTest`
drives the whole flow with 20 concurrent requests — one repeated user, then 20 distinct users against
a limit of 5 — checking the resulting rows as well as the counter.

## Configuration

Every setting comes from an environment variable. The `GEO_IP_*` variables have **no defaults**, so
`./gradlew bootRun` needs them exported; `compose.yml` sets the values in the last column.

| Variable                    | Default                                                      | `compose.yml`                    | Description                                                             |
|-----------------------------|--------------------------------------------------------------|----------------------------------|-------------------------------------------------------------------------|
| `DATABASE_URL`              | `jdbc:postgresql://localhost:5432/coupon_redemption_service` | points at the `postgres` service | JDBC URL of the PostgreSQL database                                     |
| `DATABASE_USERNAME`         | `postgres`                                                   | `postgres`                       | Database user                                                           |
| `DATABASE_PASSWORD`         | `postgres`                                                   | `postgres`                       | Database password                                                       |
| `GEO_IP_BASE_URL`           | none                                                         | `https://ipwho.is`               | Base URL of the GeoIP provider                                          |
| `GEO_IP_CONNECT_TIMEOUT`    | none                                                         | `2s`                             | Connect timeout of the GeoIP call                                       |
| `GEO_IP_READ_TIMEOUT`       | none                                                         | `2s`                             | Read timeout of the GeoIP call                                          |
| `GEO_IP_EXCLUDED_ADDRESSES` | none                                                         | `127.0.0.1,::1`                  | Addresses refused without calling the provider                          |
| `SPRING_PROFILES_ACTIVE`    | none                                                         | `local`                          | `local` raises application logging to `DEBUG` and logs request payloads |

Because `compose.yml` activates the `local` profile, the documented way of running the service logs at
`DEBUG` with full request and response payloads; run without that profile for `INFO` and no payloads.

## Local observability

`docker compose up` also starts Prometheus and Grafana defined in
[`compose.override.yml`](compose.override.yml); run `docker compose -f compose.yml up --build` to
start the application without them.

- Grafana: [http://localhost:3000](http://localhost:3000) — opens the provisioned Coupon Redemption
  Service dashboard as its home page
- Prometheus: [http://localhost:9090](http://localhost:9090)

Dashboard panels, retention and the metrics configuration are described in
[`observability/README.md`](observability/README.md).

## Prerequisites

Docker with Docker Compose, and JDK 21 for running Gradle tasks locally.

## Verification

```bash
./gradlew check
```

Docker is required for the Testcontainers integration tests; `./gradlew test` alone needs none.

## Running the published image

Every push to `main` publishes a container image to GitHub Container Registry, tagged `latest` and
`sha-<commit SHA>` (`docker/metadata-action`'s prefix plus the full 40-character SHA). The command below
expects PostgreSQL running on the host, with database `coupon_redemption_service` and `postgres`/`postgres`.

```bash
docker run --rm -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/coupon_redemption_service \
  -e GEO_IP_BASE_URL=https://ipwho.is \
  -e GEO_IP_CONNECT_TIMEOUT=2s \
  -e GEO_IP_READ_TIMEOUT=2s \
  -e GEO_IP_EXCLUDED_ADDRESSES=127.0.0.1,::1 \
  ghcr.io/krystianwitek/couponredemptionservice:latest
```
