# Coupon Redemption Service

REST service for creating country-restricted discount coupons and recording their redemption. Coupon usage limits are enforced atomically in PostgreSQL.

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

- `POST /coupons` creates a coupon
- `POST /coupons/redeem` redeems a coupon

Runnable IntelliJ HTTP Client requests are available in [`http/coupons.http`](http/coupons.http).

Coupon redemption resolves the caller's country through a public GeoIP provider. Local and private IP addresses cannot be resolved by that provider.

## Verification

```bash
./gradlew check
```

Docker is required because integration tests use PostgreSQL Testcontainers.
