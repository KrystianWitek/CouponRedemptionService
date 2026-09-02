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

Both endpoints answer with `201 Created` on success. Coupon codes are trimmed and upper-cased before
they are stored or looked up, so `welcome10` and `WELCOME10` address the same coupon.

### Create a coupon

```http
POST /api/v1/coupons
Content-Type: application/json

{
  "code": "WELCOME10",
  "maxUsageCount": 100,
  "countryCode": "PL"
}
```

```json
{
  "id": "0f2b8f1e-6a5c-4f7b-9d3e-2c1a5b8e7d40",
  "code": "WELCOME10",
  "createdAt": "2026-09-02T10:15:30.123456Z",
  "maxUsageCount": 100,
  "currentUsageCount": 0,
  "countryCode": "PL"
}
```

### Redeem a coupon

The caller's country is resolved from the client IP address of the request; no country is taken from
the payload.

```http
POST /api/v1/coupons/redeem
Content-Type: application/json

{
  "code": "WELCOME10",
  "userId": "user-123"
}
```

```json
{
  "id": "6d4c9a02-1f77-4a58-8f0b-9c3d21e4b5a6",
  "code": "WELCOME10",
  "userId": "user-123",
  "redeemedAt": "2026-09-02T10:16:04.987654Z"
}
```

### Errors

Failures return the same body for every case — `errorCode` for clients to branch on, `details` as a
human-readable message, and `invalidFields` only for bean-validation failures:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "details": "Request validation failed",
  "invalidFields": ["countryCode", "maxUsageCount"]
}
```

| Status | `errorCode`                  | Raised when                                                                       |
|--------|------------------------------|---------------------------------------------------------------------------------|
| 400    | `VALIDATION_ERROR`           | the request body fails bean validation; `invalidFields` lists the rejected fields |
| 400    | `INVALID_COUNTRY_CODE`       | `countryCode` is not an ISO 3166-1 alpha-2 country                                |
| 403    | `COUPON_COUNTRY_MISMATCH`    | the caller's country differs from the coupon country                              |
| 404    | `COUPON_NOT_FOUND`           | no coupon exists for the given code                                               |
| 409    | `COUPON_ALREADY_EXISTS`      | the coupon code is already taken                                                  |
| 409    | `COUPON_ALREADY_REDEEMED`    | the user has already redeemed this coupon                                         |
| 409    | `COUPON_USAGE_LIMIT_REACHED` | the coupon reached `maxUsageCount`                                                |
| 503    | `GEO_IP_LOOKUP_FAILED`       | the country could not be resolved — provider error, timeout or excluded address   |

### Trying it out

Interactive OpenAPI documentation is available while the application is running:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI specification: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Runnable IntelliJ HTTP Client requests are available in [`http/coupons.http`](http/coupons.http).

Coupon redemption resolves the caller's country through a public GeoIP provider. Local and private IP addresses cannot be resolved by that provider, so redemption requests made through localhost are expected to fail GeoIP resolution.

## Redemption under concurrent load

Loading the coupon, resolving the caller's country and comparing the two happen **outside** any
transaction, so a slow GeoIP provider never holds a database transaction open. Only the two writes
run inside one transaction, in this order:

1. `INSERT INTO coupon_redemption ... ON CONFLICT (coupon_id, user_id) DO NOTHING` — the unique
   constraint decides whether the user already redeemed the coupon, so no preceding `SELECT` can go
   stale between the check and the write.
2. `UPDATE coupon SET current_usage_count = current_usage_count + 1 WHERE id = ?
   AND current_usage_count < max_usage_count` — the limit is evaluated by the database in the very
   statement that consumes the use.

Both statements report the number of affected rows, and zero rows means rejection
(`COUPON_ALREADY_REDEEMED`, `COUPON_USAGE_LIMIT_REACHED`). Because the rejection is an exception
raised inside the transaction, the redemption record is rolled back with it — a rejected attempt
never consumes a use. Neither invariant is guarded by read-modify-write in application code; both
live in SQL and in the constraints created by the Flyway migration.

`CouponUsageIntegrationTest` (20 concurrent threads against a coupon with a limit of 5) and
`CouponRedemptionConcurrencyIntegrationTest` verify this against a real PostgreSQL container.

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
