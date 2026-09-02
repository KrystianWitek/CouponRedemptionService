# Local observability

Prometheus and Grafana are defined in [`compose.override.yml`](../compose.override.yml) and start
together with the application (`docker compose up`); `docker compose -f compose.yml up --build`
starts the application without them. Both are bound to the loopback interface and are meant for
local development only.

## Services

- Prometheus: [http://localhost:9090](http://localhost:9090) — scrapes the application every
  5 seconds and keeps 1 day of history. The scraped application metrics are available at
  [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus). The
  remote-write receiver is enabled, so a load-testing tool such as k6 can push its client-side
  metrics into the same Prometheus.
- Grafana: [http://localhost:3000](http://localhost:3000) — anonymous viewer access; sign in as
  `admin`/`admin` to use Explore or edit anything. The Prometheus data source and the dashboard are
  provisioned automatically from [`grafana/provisioning`](grafana/provisioning).

## Dashboard

The [Coupon Redemption Service dashboard](http://localhost:3000/d/coupon-redemption-service/coupon-redemption-service)
is the Grafana home page and has two rows:

- **HTTP** — request rate, p50/p95/p99 latency (from Micrometer histogram buckets), and response
  rate grouped by status code, all limited to `/api/**` traffic.
- **Runtime** — Tomcat thread pool (busy/current/max), Hikari connections (active/pending/max),
  process CPU usage, and JVM heap (used/max).

## Metrics configuration

The `prometheus` actuator endpoint is enabled only through an environment override in
`compose.override.yml`; the base configuration exposes just `health`. The same overrides turn on the
latency histogram buckets and the Tomcat MBean registry — without them the latency and thread-pool
panels stay empty. A production deployment would restrict the metrics endpoint to monitoring
infrastructure instead of exposing it publicly.

`docker compose down --volumes` removes the collected metric history along with the database data.
