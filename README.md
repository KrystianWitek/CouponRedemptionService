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

## Verification

```bash
./gradlew check
```

Docker is required because integration tests use PostgreSQL Testcontainers.
