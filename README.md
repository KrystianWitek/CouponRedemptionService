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

Create a coupon:

```bash
curl --request POST http://localhost:8080/coupons \
  --header "Content-Type: application/json" \
  --data '{"code":"WELCOME10","maxUsageCount":100,"countryCode":"PL"}'
```

Redeem a coupon:

```bash
curl --request POST http://localhost:8080/coupons/redeem \
  --header "Content-Type: application/json" \
  --data '{"code":"WELCOME10","userId":"user-123"}'
```

Coupon redemption resolves the caller's country through a public GeoIP provider. Local and private IP addresses cannot be resolved by that provider.

## Verification

```bash
./gradlew check
```

Docker is required because integration tests use PostgreSQL Testcontainers.
