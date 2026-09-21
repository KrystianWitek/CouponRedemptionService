# Coupon Redemption Service

[![CI](https://github.com/KrystianWitek/CouponRedemptionService/actions/workflows/ci.yml/badge.svg)](https://github.com/KrystianWitek/CouponRedemptionService/actions/workflows/ci.yml)

REST service for creating country-restricted discount coupons and recording their redemption.

- `POST /api/v1/coupons` creates a coupon: unique code, maximum number of uses, target country.
- `POST /api/v1/coupons/redeem` records a use of a coupon by a user.

Redemption is refused unless the code exists (codes are case-insensitive), the coupon has uses left,
the caller's country resolved from the client IP matches the coupon's, and the user has not redeemed
this coupon before.

## Quick start

Requires Docker with Docker Compose; JDK 21 for the Gradle tasks.

```bash
docker compose up -d
```

This starts the application, PostgreSQL, WireMock, Prometheus and Grafana. After changing application
code, use `docker compose up -d --build` to rebuild its image.

The application listens on [http://localhost:8080](http://localhost:8080) with liveness and
readiness probes under [`/actuator/health`](http://localhost:8080/actuator/health).
`docker compose down --volumes` stops it and drops the data.

To connect from IntelliJ IDEA or DataGrip, add a PostgreSQL data source, select
[**URL only**](https://www.jetbrains.com/help/idea/postgresql.html), paste this JDBC URL and click
**Test Connection**. The URL includes the database name, username and password:

```text
jdbc:postgresql://localhost:5432/coupon_redemption_service?user=postgres&password=postgres
```

This uses the default port published by `compose.yml`. If you override it locally, run
`docker compose port postgres 5432` and use the published port in the URL (for example, `5434`).

### Local GeoIP simulation

The automatically loaded `compose.override.yml` sends GeoIP HTTP requests to WireMock and allows
local addresses. The [`GeoIP mapping`](wiremock/mappings/geoip.json) returns `PL` for every lookup,
so use a coupon with
`countryCode: "PL"`. Open [`http/coupons.http`](http/coupons.http), select **local** and run
**Create coupon**, then **Redeem coupon**. Use a new coupon code if `WELCOME10` already exists;
each user can redeem a coupon only once.

WireMock's [request journal](http://localhost:8081/__admin/requests) shows the actual GeoIP calls.
To use the real GeoIP service without the local tools, run
`docker compose -f compose.yml up -d --remove-orphans`. Local redemption then returns
`503 GEO_IP_LOOKUP_FAILED` because loopback and Docker-gateway addresses cannot be geolocated.

## API

Requests, responses and the error contract: [`http/README.md`](http/README.md), with runnable
requests in [`http/coupons.http`](http/coupons.http). While the application runs, the same contract
is browsable as [Swagger UI](http://localhost:8080/swagger-ui.html) or
[OpenAPI](http://localhost:8080/v3/api-docs).

## Configuration

Every setting comes from an environment variable. The `GEO_IP_*` variables have **no defaults**, so
`./gradlew bootRun` needs them exported; `compose.yml` sets the values in the last column.
The automatically loaded `compose.override.yml` overrides the GeoIP URL with `http://wiremock:8080`
and clears excluded addresses for local testing.

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

## Testing

- `./gradlew test` — `src/test`, no Docker: services against hand-written fakes, the controller
  through `@WebMvcTest`.
- `./gradlew integrationTest` — `src/integration` on a `postgres:16-alpine` Testcontainer, including
  the two concurrency tests that cover redemption under load.
- `./gradlew check` runs both, as CI does.

## Design decisions

Concurrency and the GeoIP policy are documented in [`docs/adr/`](docs/adr/README.md).

## Local observability

`docker compose up` also starts Grafana ([:3000](http://localhost:3000), provisioned dashboard) and
Prometheus ([:9090](http://localhost:9090)) from
[`compose.override.yml`](compose.override.yml); see
[`observability/README.md`](observability/README.md). Use `docker compose -f compose.yml up --build`
to start the application alone.

## Published image

Every push to `main` publishes `ghcr.io/krystianwitek/couponredemptionservice`, tagged `latest` and
`sha-<commit SHA>`.
