# Performance tests

Load tests for the redemption path, run against the real application and PostgreSQL with a
WireMock GeoIP stub (20 ms fixed delay — never the real provider). Client-side latency
percentiles (p95/p99) come from k6; server-side saturation (Tomcat threads, Hikari pool,
Postgres lock waits) from `monitor.sh`.

Results measured on a laptop with generator and SUT on the same host are **indicative, not
absolute** — container CPU/memory limits in `compose.perf.yml` keep them at least repeatable,
but never present these numbers as production capacity. `fsync` inside the Docker Desktop VM is
much slower than on a production Linux host, so absolute write ceilings will be higher there;
the *shape* of the saturation (what saturates first and in which order) is what transfers.

## Setup

```bash
docker compose -f perf/compose.perf.yml up --build -d   # app (perf profile) + postgres + wiremock
docker compose -f perf/compose.perf.yml ps              # wait until healthy
mkdir -p perf-results
```

Host ports are high and localhost-only so the stack can run next to the regular dev compose
stack: application `http://localhost:18080`, its management port (actuator)
`http://localhost:19090`, WireMock admin `http://localhost:8081`.

The `perf` Spring profile disables Logbook request logging and raises log levels (otherwise you
benchmark the logger), and exposes `/actuator/metrics` on a **separate management port** — during
full saturation the application port stops answering, but the monitor keeps seeing the inside.

In a second terminal, record what happens inside the SUT (requires `curl`, `jq`, `docker`):

```bash
./perf/monitor.sh > perf-results/monitor-s1.csv
```

Columns: `tomcat_busy` (busy worker threads), `hikari_active`/`hikari_pending` (connections in
use / threads waiting for a connection), `pg_lock_waits` (Postgres sessions blocked on a lock —
row contention made visible).

## Scenarios

Run k6 in the compose network (`BASE_URL=http://app:8080`); the image version is pinned for
reproducibility:

```bash
K6="docker run --rm --network coupon-perf_default -v $PWD/perf/k6:/scripts:ro \
    -e BASE_URL=http://app:8080 -e WIREMOCK_URL=http://wiremock:8080 grafana/k6:2.2.0 run"

$K6 /scripts/s1-hot-coupon.js     # single hot coupon — row-serialization ceiling
$K6 /scripts/s2-spread.js         # 1000 coupons — realistic spread, Hikari becomes the bottleneck
$K6 /scripts/s3-flash-sale.js     # up to 80k offered attempts, limit 10k — perf + correctness
$K6 /scripts/s4-slow-geoip.js     # GeoIP at 1.5 s — thread-pool exhaustion demo
```

Without `-e BASE_URL`, scripts default to `http://localhost:18080` (the perf stack seen from the
host) — they never point at the dev stack, whose GeoIP is the real external provider.

Environment knobs differ per scenario:

| Scenario | Variables (defaults) | Profile |
|---|---|---|
| s1, s2 | `RATE_LOW=300`, `RATE_HIGH=3000`, `HOLD=2m` (s2 also `COUPONS=1000`) | warmup 30 s → `low` plateau → ramp 30 s → `high` plateau |
| s3 | `RATE=2000`, `LIMIT=10000`, `ATTEMPTS=80000` | constant arrival for `ATTEMPTS/RATE` seconds |
| s4 | `RATE=140`, `DELAY_MS=1500` | constant arrival, fixed 90 s |

Smoke run (s1/s2 only): `-e RATE_LOW=100 -e RATE_HIGH=200 -e HOLD=20s`.

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
(transport failures observed by the client) and `coupon_unexpected` (must be zero).

## Reading the results

- **p95/p99** — from the k6 summary (`http_req_duration`). In s1/s2 the SLO thresholds
  (p95 < 200 ms, p99 < 500 ms) apply only to the `low` scenario tag; the `high` plateau is
  *expected* to saturate — its failed checks and `dropped_iterations` are the finding
  (the capacity ceiling), not a broken test.
- **Exit codes**: s4 exits non-zero by design (its "production-like" thresholds must fail to
  demonstrate the degradation); s1/s2 exit non-zero whenever the `high` plateau saturates the
  system, which is the expected outcome on a laptop. Do not wire these exit codes into CI as-is.
- **`dropped_iterations` > 0** — the open-model generator could not sustain the target arrival
  rate because the SUT saturated: you found the capacity ceiling.
- **`pg_lock_waits` climbing in S1** — requests queueing on the hot coupon row: the expected
  serialization ceiling (the row lock is held for the whole INSERT + UPDATE + commit).
- **`hikari_pending` climbing in S2** — the 10-connection pool is the bottleneck; rerun with a
  bigger pool (`-e SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=30` on the app service) to compare.
- **`tomcat_busy` pinned at ~200 in S4** — 140 rps × ~1.5 s ≈ 210 in-flight requests exceed the
  200 worker threads: every thread waits on GeoIP while the database sits idle; the motivation
  for the circuit breaker. With `DELAY_MS >= 2500` every request turns into a fail-closed 503
  after the 2 s read timeout instead.

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
