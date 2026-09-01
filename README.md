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

## Local observability

`docker compose up` also starts a local monitoring stack defined in
[`compose.override.yml`](compose.override.yml); run `docker compose -f compose.yml up --build` to
start the application without it. The stack is bound to the loopback interface and is meant for
local development only:

- Prometheus: [http://localhost:9090](http://localhost:9090) — scrapes the application every
  5 seconds and keeps 1 day of history. The collected application metrics are available at
  [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus).
- Grafana: [http://localhost:3000](http://localhost:3000) — anonymous viewer access; sign in as
  `admin`/`admin` to use Explore or edit anything.
- Coupon Redemption Service dashboard:
  [http://localhost:3000/d/coupon-redemption-service/coupon-redemption-service](http://localhost:3000/d/coupon-redemption-service/coupon-redemption-service).
  The Prometheus data source and dashboard are provisioned automatically, and the dashboard is
  the home page.

The dashboard has two rows:

- **HTTP** — request rate, p50/p95/p99 latency (from Micrometer histogram buckets), and response
  rate grouped by status code, all limited to `/api/**` traffic.
- **Runtime** — Tomcat thread pool (busy/current/max), Hikari connections (active/pending/max),
  process CPU usage, and JVM heap (used/max).

The `prometheus` actuator endpoint is enabled only through an environment override in
`compose.override.yml`; the base configuration exposes just `health`. A production deployment
would restrict the metrics endpoint to monitoring infrastructure instead of exposing it publicly.

## API

Coupon API endpoints are versioned with the `/api/v1` path prefix, so a future breaking change can ship
under `/api/v2` without breaking existing clients:

- `POST /api/v1/coupons` — create a coupon
- `POST /api/v1/coupons/redeem` — record a coupon redemption for a user

Interactive OpenAPI documentation is available while the application is running:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI specification: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Runnable IntelliJ HTTP Client requests are available in [`http/coupons.http`](http/coupons.http).

Coupon redemption resolves the caller's country through a public GeoIP provider. Local and private IP addresses cannot be resolved by that provider, so redemption requests made through localhost are expected to fail GeoIP resolution.

## Verification

```bash
./gradlew check
```

Docker is required because integration tests use PostgreSQL Testcontainers.
