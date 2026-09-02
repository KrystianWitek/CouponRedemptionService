# Coupon Redemption Service

[![CI](https://github.com/KrystianWitek/CouponRedemptionService/actions/workflows/ci.yml/badge.svg)](https://github.com/KrystianWitek/CouponRedemptionService/actions/workflows/ci.yml)

REST service for creating country-restricted discount coupons and recording their redemption. The
only non-trivial part is redemption under load: many requests racing for the last use of the same
coupon. The decisions behind it are in [`docs/adr/`](docs/adr/README.md).

## What it does

- `POST /api/v1/coupons` creates a coupon: unique code, maximum number of uses, target country.
- `POST /api/v1/coupons/redeem` records a use of a coupon by a user.

Redemption is refused unless all four rules hold:

- the code exists (codes are case-insensitive);
- the coupon has not reached its maximum number of uses, first come first served;
- the caller's country, resolved from the client IP through a GeoIP provider, matches the coupon's;
- the user has not redeemed this coupon before.

The last rule was **optional** in the assignment and is implemented, including under concurrency.
Every rejection has its own `errorCode` ([`http/README.md`](http/README.md)).

## Quick start

Docker with Docker Compose is required, plus JDK 21 for running Gradle tasks locally.

```bash
docker compose up --build
```

The application listens on [http://localhost:8080](http://localhost:8080), with
[liveness](http://localhost:8080/actuator/health/liveness) and
[readiness](http://localhost:8080/actuator/health/readiness) probes under
[`/actuator/health`](http://localhost:8080/actuator/health); readiness includes the database, so an
instance that loses it leaves the rotation. Runnable requests are in
[`http/README.md`](http/README.md). `docker compose down` stops it, `--volumes` also drops the data.

Redemption cannot succeed from your own machine: loopback callers are refused before the GeoIP
provider is contacted, and callers seen through the Docker network gateway carry a private address the
provider cannot geolocate. Both answer `503 GEO_IP_LOOKUP_FAILED` ([decision 3](#design-decisions)).

## API

Endpoints are versioned with the `/api/v1` path prefix, so a future breaking change can ship under
`/api/v2` without breaking existing clients. Requests, responses and the error contract are in
[`http/README.md`](http/README.md); while the application runs, the same contract is browsable as
[Swagger UI](http://localhost:8080/swagger-ui.html) or [OpenAPI](http://localhost:8080/v3/api-docs).

## Data model

A coupon carries a surrogate `id` plus the five fields the domain needs: a unique `code`, a
`created_at` timestamp, `max_usage_count`, `current_usage_count` and a `country_code`. Each use is a
row in `coupon_redemption` (`coupon_id`, `user_id`, `redeemed_at`), so each use is auditable. Two
unique constraints in `V1__create_coupon_tables.sql` are part of the contract: `coupon.code` makes a
code collision a database decision, and `UNIQUE (coupon_id, user_id)` detects a repeat user with no
`SELECT`. Flyway owns the schema; Hibernate only validates the entities against it.

## Design decisions

1. **The database is the arbiter of both contended rules.** Each is one conditional statement
   (`INSERT ... ON CONFLICT (coupon_id, user_id) DO NOTHING`, then
   `UPDATE ... WHERE current_usage_count < max_usage_count`) in a transaction that holds nothing else,
   so a repeat user is detected without a `SELECT` and a failed increment rolls the redemption row back
   ([ADR](docs/adr/2026-08-28-concurrency-the-database-is-the-arbiter.md)). The application holds no
   state of its own, so N instances behind a load balancer behave like one.
2. **`CouponCode`, `CountryCode` and `UserId` are value classes with a private constructor and a
   `from()` factory** that normalises and then validates, so an invalid instance cannot be built.
   Case-insensitivity comes for free at the API boundary, a plain `UNIQUE` on `code` suffices, and
   `CountryCode` rejects anything outside `Locale.getISOCountries`, making `XX` a `400` rather than
   an unredeemable coupon.
3. **GeoIP is fail-closed and the client IP is `remoteAddr`.** Every lookup failure becomes one
   `GeoIpLookupException` and a `503`, because a country-restricted coupon must not become redeemable
   while the provider is down; `X-Forwarded-For` is ignored because without a trusted-proxy allow-list
   it would let any caller spoof its country ([ADR](docs/adr/2026-08-28-geoip-is-fail-closed.md)).
4. **One feature slice, ports and adapters, beans wired by hand.** The dependency direction is
   `api → application → domain ← infrastructure`, and JPA entities stay separate from the domain models
   so the adapter can issue the native SQL that decision 1 rests on
   ([ADR](docs/adr/2026-08-27-one-feature-slice-ports-and-adapters-manual-wiring.md)).

## Out of scope

- **Authentication and authorization**: not required by the assignment; the user identifier is an
  opaque string from the request body.
- **Rate limiting**: belongs at the ingress.
- **Coupon validity window and deactivation**: not part of the specified coupon model.
- **Idempotency keys**: a client retrying after a timeout may create a redemption it never saw the
  response for; the per-user rule bounds that to one use.
- **Read endpoints**: nothing required needs one; exposing coupon state is a product decision.

## What I would add next, in priority order

1. **Coupon validity window**: `valid_from`/`valid_until` as a migration plus one more predicate in
   the conditional `UPDATE`.
2. **Idempotency key on redeem**: unique per coupon and user, so a retried request returns the
   original result instead of racing itself.
3. **Pooled HTTP client with cache and circuit breaker for GeoIP**: `SimpleClientHttpRequestFactory`
   pays connection setup per redemption, and a breaker would keep a slow provider off request threads.
4. **Trusted-proxy configuration for forwarded headers**: required before a load balancer fronts it.
5. **A coupon read endpoint**: to inspect remaining uses without querying the database.

## Testing

Two source sets, split by what they need to run:

- `./gradlew test`: `src/test`, no Docker, seconds. Services run against hand-written fakes
  (`InMemoryCouponRepository`, `FakeTransactionOperations`), the controller through `@WebMvcTest`.
- `./gradlew integrationTest`: `src/integration`, on a real `postgres:16-alpine` Testcontainer,
  because the guarantees under test are the database's. `./gradlew check` runs both, as CI does.

Decision 1 is covered by two concurrency tests: `CouponUsageIntegrationTest` fires 20 concurrent
increments at a coupon limited to 5 uses, and `CouponRedemptionConcurrencyIntegrationTest` drives the
whole flow with 20 concurrent requests (one repeated user, then 20 distinct users against a limit of
5), checking the resulting rows as well as the counter.

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

## Local observability

`docker compose up` also starts Prometheus and Grafana defined in
[`compose.override.yml`](compose.override.yml); run `docker compose -f compose.yml up --build` to
start the application without them.

- Grafana: [http://localhost:3000](http://localhost:3000); its home page is the provisioned Coupon
  Redemption Service dashboard
- Prometheus: [http://localhost:9090](http://localhost:9090)

Dashboard panels, retention and the metrics configuration are described in
[`observability/README.md`](observability/README.md).

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
