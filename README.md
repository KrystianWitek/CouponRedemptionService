# Coupon Redemption Service

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

The application is available at `http://localhost:8080`.

Health endpoints:

- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

Stop the application:

```bash
docker compose down
```

Add `--volumes` to remove the persisted PostgreSQL data.

## API

Coupon API endpoints are versioned with the `/api/v1` path prefix, so a future breaking change can ship
under `/api/v2` without breaking existing clients:

- `POST /api/v1/coupons` — create a coupon
- `POST /api/v1/coupons/redeem` — record a coupon redemption for a user

Interactive OpenAPI documentation is available while the application is running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI specification: `http://localhost:8080/v3/api-docs`

Runnable IntelliJ HTTP Client requests are available in [`http/coupons.http`](http/coupons.http).

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

## Verification

```bash
./gradlew check
```

Docker is required because integration tests use PostgreSQL Testcontainers.
