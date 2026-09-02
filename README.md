# Coupon Redemption Service

[![CI](https://github.com/KrystianWitek/CouponRedemptionService/actions/workflows/ci.yml/badge.svg)](https://github.com/KrystianWitek/CouponRedemptionService/actions/workflows/ci.yml)

REST service for creating country-restricted discount coupons and recording their redemption.

## Guarantees

- Coupon codes are unique regardless of letter case.
- Usage limits and one redemption per user remain consistent under concurrent requests.
- A redemption record and the corresponding usage count are committed together.
- Redemption is rejected when the caller's country cannot be resolved or does not match the coupon country.

## Requirements

- Docker with Docker Compose
- JDK 21 for running Gradle tasks locally

## Running the application

```bash
docker compose up --build
```

The application is available at [http://localhost:8080](http://localhost:8080).

Health endpoints:

- [Health](http://localhost:8080/actuator/health)
- [Liveness](http://localhost:8080/actuator/health/liveness)
- [Readiness](http://localhost:8080/actuator/health/readiness)

Stop the application:

```bash
docker compose down
```

Add `--volumes` to remove the persisted PostgreSQL data and the collected metric history.

### Running the published image

Every push to `main` publishes a container image to GitHub Container Registry, tagged with `latest`
and with the full commit SHA:

```bash
docker run --rm -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/coupon_redemption_service \
  -e GEO_IP_BASE_URL=https://ipwho.is \
  -e GEO_IP_CONNECT_TIMEOUT=2s \
  -e GEO_IP_READ_TIMEOUT=2s \
  -e GEO_IP_EXCLUDED_ADDRESSES=127.0.0.1,::1 \
  ghcr.io/krystianwitek/couponredemptionservice:latest
```

## Configuration

Every setting comes from an environment variable. The `GEO_IP_*` variables have **no defaults**, so
the application refuses to start without them — `./gradlew bootRun` needs them exported, while
`compose.yml` already provides the values below.

| Variable                    | Default                                                      | Description                                                             |
|-----------------------------|--------------------------------------------------------------|-------------------------------------------------------------------------|
| `DATABASE_URL`              | `jdbc:postgresql://localhost:5432/coupon_redemption_service` | JDBC URL of the PostgreSQL database                                     |
| `DATABASE_USERNAME`         | `postgres`                                                   | Database user                                                           |
| `DATABASE_PASSWORD`         | `postgres`                                                   | Database password                                                       |
| `GEO_IP_BASE_URL`           | none                                                         | Base URL of the GeoIP provider, e.g. `https://ipwho.is`                 |
| `GEO_IP_CONNECT_TIMEOUT`    | none                                                         | Connect timeout of the GeoIP call, e.g. `2s`                            |
| `GEO_IP_READ_TIMEOUT`       | none                                                         | Read timeout of the GeoIP call, e.g. `2s`                               |
| `GEO_IP_EXCLUDED_ADDRESSES` | none                                                         | Addresses rejected without calling the provider, e.g. `127.0.0.1,::1`   |
| `SPRING_PROFILES_ACTIVE`    | none                                                         | `local` raises application logging to `DEBUG` and logs request payloads |

The database schema is owned by Flyway and applied on startup; Hibernate only validates it.

## API

Coupon API endpoints are versioned with the `/api/v1` path prefix, so a future breaking change can ship
under `/api/v2` without breaking existing clients:

- `POST /api/v1/coupons` — create a coupon
- `POST /api/v1/coupons/redeem` — record a coupon redemption for a user

Requests, responses and the error contract are described in [`http/README.md`](http/README.md).

Interactive OpenAPI documentation is available while the application is running:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI specification: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Coupon redemption resolves the caller's country through a public GeoIP provider. Local and private IP addresses cannot be resolved by that provider, so redemption requests made through localhost are expected to fail GeoIP resolution.

## GeoIP resilience

The default local/Docker setup points at a provider-controlled endpoint whose quotas and SLA may
change; see the [official provider documentation](https://ipwhois.io/documentation) for current
limits. Production deployments can configure an appropriate endpoint or plan through `GEO_IP_BASE_URL`.

The remote provider call is guarded by a circuit breaker:

- the breaker reacts to transport-level failures (HTTP errors, timeouts, empty or unparsable
  responses); provider responses that reject a single address (`success: false`) are handled per
  request and do not trip it;
- the circuit opens after at least 5 recorded calls with at least 50% failures within the last
  10 calls;
- while the circuit is open, lookups fail immediately without calling the provider; after
  10 seconds the next incoming calls (up to 3) are let through as trials that decide whether the
  circuit closes again;
- the breaker is local to each application instance and its state is not shared;
- failed lookups are deliberately not retried, so a redemption request fails fast instead of
  adding more load to a struggling provider.

GeoIP lookup failures are fail-closed: the API responds with HTTP 503 and the `GEO_IP_LOOKUP_FAILED`
error code, and a failed lookup never consumes coupon usage or records a redemption.

## Local observability

`docker compose up` also starts Prometheus and Grafana defined in
[`compose.override.yml`](compose.override.yml); run `docker compose -f compose.yml up --build` to
start the application without them.

- Grafana: [http://localhost:3000](http://localhost:3000) — opens the provisioned Coupon Redemption
  Service dashboard as its home page
- Prometheus: [http://localhost:9090](http://localhost:9090)

Dashboard panels, retention and the metrics configuration are described in
[`observability/README.md`](observability/README.md).

## Verification

```bash
./gradlew check
```

Docker is required because integration tests use PostgreSQL Testcontainers.
