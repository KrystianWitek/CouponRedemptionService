// S4 — degraded GeoIP: the provider is slow (1.5 s), not down.
// Little's law: 140 rps x ~1.5 s ≈ 210 requests in flight — above the 200 Tomcat
// worker threads, so the pool stays pinned at ~200 while the database sits idle.
// Watch monitor.sh: tomcat_busy ≈ 200, hikari ≈ 0.
//
// What this demonstrates: a slow-but-correct dependency exhausts the server thread
// pool. A purely failure-based circuit breaker does NOT open here (every response
// is a valid 201) — the protections this scenario motivates are slow-call-rate
// breaking, limiting concurrent calls to the provider (bulkhead), or an HTTP client
// with pooling and tighter latency budgets. Set DELAY_MS >= 2500 to exercise the
// other regime instead: every request fails closed with 503 after the 2 s read
// timeout — those failures are what a failure-based circuit breaker reacts to.
// Avoid values right at 2000: delay == timeout produces a nondeterministic mix.
//
// The "production-like" latency thresholds below are EXPECTED to fail — a non-zero
// exit code is the point of the demonstration, not a broken test.
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:18080';
// Host-side default; inside the compose network pass -e WIREMOCK_URL=http://wiremock:8080.
const WIREMOCK = __ENV.WIREMOCK_URL || 'http://localhost:8081';
const RATE = Number(__ENV.RATE || 140);
const DELAY_MS = Number(__ENV.DELAY_MS || 1500);
const HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    slow_geoip: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: '90s',
      preAllocatedVUs: 500,
      maxVUs: 2000,
    },
  },
  thresholds: {
    'http_req_duration{expected_response:true}': ['p(95)<300'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  // Defensive: restore file-based mappings first, in case a previous interrupted
  // run left a slow stub behind (the 20 ms base stub comes back).
  const reset = http.post(`${WIREMOCK}/__admin/mappings/reset`);
  if (reset.status !== 200) {
    throw new Error(`setup: failed to reset WireMock mappings: ${reset.status} ${reset.body}`);
  }

  // Create the coupon BEFORE the slow stub: if coupon creation fails, k6 skips
  // teardown(), and a stub registered first would silently slow every subsequent
  // scenario down (75x) with the request journal disabled.
  const code = `PERFS4G${Date.now()}`;
  const res = http.post(
    `${BASE}/api/v1/coupons`,
    JSON.stringify({ code, maxUsageCount: 2000000, countryCode: 'PL' }),
    { headers: HEADERS },
  );
  if (res.status !== 201) {
    throw new Error(`setup: failed to create the coupon: ${res.status} ${res.body}`);
  }

  const stub = http.post(
    `${WIREMOCK}/__admin/mappings`,
    JSON.stringify({
      priority: 1,
      request: { method: 'GET', urlPathPattern: '/.*' },
      response: {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        jsonBody: { success: true, country_code: 'PL' },
        fixedDelayMilliseconds: DELAY_MS,
      },
    }),
    { headers: HEADERS },
  );
  if (stub.status !== 201) {
    throw new Error(`setup: failed to slow WireMock down: ${stub.status} ${stub.body}`);
  }
  console.log(`S4 coupon code: ${code}, wiremock delay: ${DELAY_MS}ms`);
  return { code };
}

export default function (data) {
  const res = http.post(
    `${BASE}/api/v1/coupons/redeem`,
    JSON.stringify({ code: data.code, userId: `u-${__VU}-${__ITER}` }),
    { headers: HEADERS, timeout: '15s' },
  );
  check(res, { 'status 201': (r) => r.status === 201 });
}

export function teardown() {
  // Restores the mappings loaded from files (the 20 ms stub), removing the slow
  // runtime stub regardless of its id.
  const res = http.post(`${WIREMOCK}/__admin/mappings/reset`);
  if (res.status !== 200) {
    console.error('teardown: failed to reset WireMock mappings — restart the wiremock container');
  }
}
