# Performance tests

Load tests for the redemption path, run against the real application and PostgreSQL with a
WireMock GeoIP stub (20 ms fixed delay — never the real provider). Client-side latency
percentiles (p95/p99) come from k6; server-side saturation (Tomcat threads, Hikari pool,
Postgres lock waits) from `monitor.sh` and from the perf stack's own Grafana dashboard.

Results measured on a laptop with generator and SUT on the same host are **indicative, not
absolute** — container CPU/memory limits in `compose.perf.yml` keep them at least repeatable,
but never present these numbers as production capacity. `fsync` inside the Docker Desktop VM is
much slower than on a production Linux host, so absolute write ceilings will be higher there;
the *shape* of the saturation (what saturates first and in which order) is what transfers.

## Setup

```bash
docker compose -f perf/compose.perf.yml up --build -d   # app (perf profile) + postgres + wiremock + prometheus + grafana
docker compose -f perf/compose.perf.yml ps              # wait until healthy
mkdir -p perf-results
```

The perf Compose project (`coupon-perf`) is self-contained: it starts its own application,
PostgreSQL, WireMock, Prometheus and Grafana. Host ports are high and localhost-only so it can
run next to the regular dev compose stack (`8080`/`9090`/`3000`):

- application: [http://localhost:18080](http://localhost:18080)
- health (separate management port): [http://localhost:19090/actuator/health](http://localhost:19090/actuator/health)
- Actuator metrics: [http://localhost:19090/actuator/metrics](http://localhost:19090/actuator/metrics)
- WireMock admin: [http://localhost:8081/__admin](http://localhost:8081/__admin)
- Prometheus: [http://localhost:19091](http://localhost:19091)
- Grafana: [http://localhost:13000](http://localhost:13000)
- Coupon Redemption Service dashboard:
  [http://localhost:13000/d/coupon-redemption-service/coupon-redemption-service](http://localhost:13000/d/coupon-redemption-service/coupon-redemption-service)

The `perf` Spring profile disables Logbook request logging and raises log levels (otherwise you
benchmark the logger), pins the pool sizes the scenarios reason about (Tomcat `threads.max: 200`,
Hikari `maximum-pool-size: 10`), and exposes `health`, `metrics` and `prometheus` on a
**separate management port** — during full saturation the application port stops answering, but
the monitor and Prometheus keep seeing the inside.

## Observability

The perf stack has its own monitoring, independent of the regular one in
[`../compose.override.yml`](../compose.override.yml). Same pinned images, same provisioning and
the same dashboard JSON (mounted read-only from [`../observability`](../observability)), but a
separate Prometheus ([`prometheus/prometheus.yml`](prometheus/prometheus.yml)), a separate Grafana
and separate named volumes. The perf Prometheus scrapes only `app:9090/actuator/prometheus`
inside the `coupon-perf` network every 5 seconds, so **perf metrics never mix with the regular
stack's history** — each Grafana shows exactly one application. Anonymous viewer access; sign in
as `admin`/`admin` to use Explore.

The dashboard's p50/p95/p99 panels need the HTTP latency histogram buckets, which the `perf`
profile enables (`percentiles-histogram.http.server.requests`); Tomcat thread metrics need the
MBean registry, also enabled there.

In a second terminal, record what happens inside the SUT (requires `curl`, `jq`, `docker`):

```bash
./perf/monitor.sh > perf-results/monitor-s1.csv
```

Columns: `tomcat_busy` (busy worker threads), `hikari_active`/`hikari_pending` (connections in
use / threads waiting for a connection), `pg_lock_waits` (Postgres sessions blocked on a lock —
row contention made visible). The CSV is the record you keep with the results; the dashboard is
for watching the same saturation live and correlating it with the k6 timeline.

## Scenarios

Run k6 inside the compose network. A shell function keeps the paths quoted (the repository path
may contain spaces) and the image version pinned:

```bash
k6() {
  docker run --rm --network coupon-perf_default -v "$PWD/perf/k6:/scripts:ro" \
    -e BASE_URL=http://app:8080 -e WIREMOCK_URL=http://wiremock:8080 \
    grafana/k6:2.2.0 run "$@"
}

k6 /scripts/s1-hot-coupon.js     # S1 Single hot coupon — row-serialization ceiling
k6 /scripts/s2-spread.js         # S2 Traffic spread across many coupons — app CPU + GeoIP client
k6 /scripts/s3-flash-sale.js     # S3 Flash sale — exact usage limit under full load
k6 /scripts/s4-slow-geoip.js     # S4 Slow GeoIP provider — thread-pool exhaustion
```

Each script opens with a banner (`scenario-banner.js`, logged once from `setup()`) naming the
scenario, the question it answers, the outcome that means *working as designed* — including the
thresholds that are supposed to fail — the knobs actually in effect, and the signals to watch.
It is the first thing in the saved output, so a result file read later still says what it
measured:

```
S3 — Flash sale
  checks:  whether the usage limit stays exact when attempts far exceed it
  expects: exactly 10000 successes, the rest 409; nothing unexpected, no transport loss
  load:    2000 rps for 40s, up to 80000 attempts, limit 10000
  watch:   the coupon_* counters in the summary, then verify-s3.sh against the database
```

Without the env overrides, scripts default to the host-side perf ports
(`BASE_URL=http://localhost:18080`, `WIREMOCK_URL=http://localhost:8081`) — they never point at
the dev stack, whose GeoIP is the real external provider.

Environment knobs differ per scenario:

| Scenario | Variables (defaults) | Profile |
|---|---|---|
| s1, s2 | `RATE_LOW=300`, `RATE_HIGH=3000`, `HOLD=2m` (s2 also `COUPONS=1000`) | warmup 30 s → `low` plateau → ramp 30 s → `high` plateau |
| s3 | `RATE=2000`, `LIMIT=10000`, `ATTEMPTS=80000` | constant arrival for `ATTEMPTS/RATE` seconds |
| s4 | `RATE=140`, `DELAY_MS=1500` | constant arrival, fixed 90 s |

Smoke run (s1/s2 only): `-e RATE_LOW=100 -e RATE_HIGH=200 -e HOLD=20s` added to the
`docker run` line.

After S3, verify invariants directly in the database — **this is the source of truth**, k6
counters only observe the client side (the coupon code is printed by setup):

```bash
./perf/verify-s3.sh <COUPON_CODE>
# exit 0: counter == rows == limit; exit 1: invariant broken; exit 2: limit not exhausted
```

S3 classifies every response: `coupon_success` (201, must equal the limit),
`coupon_limit_reached` (409 — pressure that reached the database; the `>= LIMIT` threshold
guards the run applied real pressure), `coupon_geoip_rejected` (503 — fail-closed rejections:
under extreme rps the pool-less GeoIP client saturates before the database does; nothing is
persisted, invariants unaffected — and a finding in its own right), `coupon_client_error`
(transport failures observed by the client; must be zero, otherwise the client-side picture is
inconclusive and `verify-s3.sh` settles it) and `coupon_unexpected` (must be zero).

## Reading the results

- **p95/p99** — from the k6 summary (`http_req_duration`). In s1/s2 the SLO thresholds
  (p95 < 200 ms, p99 < 500 ms) apply only to the `low` scenario tag; the `high` plateau is
  *expected* to saturate — its failed checks and `dropped_iterations` are the finding
  (the capacity ceiling), not a test defect.
- **Exit codes**: s1/s2 exit non-zero **only** when the `low` plateau misses its SLO — the
  `high` plateau carries no thresholds, so read its results (dropped iterations, failure
  percentages) from the summary, not from the exit code. s4 exits non-zero by design (its
  "production-like" thresholds must fail to demonstrate the degradation). s3 exits zero only
  when every response is accounted for and the invariants hold on the client side. Do not wire
  these exit codes into CI as-is.
- **`dropped_iterations` > 0** — the target arrival rate was not sustained. Usually that is the
  SUT saturating, but a generator sharing the host can also fall behind — cross-check with
  `monitor.sh` (a saturated SUT shows busy threads / pending connections; an idle SUT with
  drops points at the generator).
- **`pg_lock_waits` climbing in S1** — requests queueing on the hot coupon row: the expected
  serialization ceiling (the row lock is acquired by the conditional UPDATE and held until the
  transaction commits).
- **`hikari_pending` climbing in S2** — the 10-connection pool is the bottleneck; rerun with a
  bigger pool (`-e SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=30` on the app service) to compare.
- **`tomcat_busy` pinned at ~200 in S4** — 140 rps × ~1.5 s ≈ 210 in-flight requests exceed the
  200 worker threads: every thread waits on a slow-but-correct GeoIP while the database sits
  idle. Note what this does and does not motivate: a purely failure-based circuit breaker would
  NOT open here (all responses are valid 201s) — the fixes this points at are slow-call-rate
  breaking, a bulkhead on concurrent provider calls, or an HTTP client with pooling. With
  `DELAY_MS >= 2500` every request instead fails closed with 503 after the 2 s read timeout —
  that failure stream is what a failure-based circuit breaker reacts to.

## Hypotheses (write down before running)

- 300 rps: all scenarios comfortably green (the `low` thresholds hold).
- 3000 rps on a **single** coupon (S1): not sustainable — ceiling ≈ 1 / transaction time on the
  hot row (order of 300–2000 tps depending on fsync speed); excess load becomes latency, then
  drops. A bigger connection pool does NOT lift this ceiling.
- 3000 rps spread over 1000 coupons (S2): bottleneck shifts to the connection pool; a bigger
  pool should lift the ceiling here, unlike in S1.
- S3: exactly `LIMIT` successes regardless of load — anything else is a correctness bug
  (confirm with `verify-s3.sh`).

## Teardown

```bash
docker compose -f perf/compose.perf.yml down --volumes
```

`--volumes` also drops the perf Prometheus history and Grafana state (`coupon-perf_*` volumes
only — the regular stack's volumes are untouched).
